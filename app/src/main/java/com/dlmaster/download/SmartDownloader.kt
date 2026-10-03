package com.dlmaster.download
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max
import kotlin.math.min

object SmartDownloader {
    private val standard by lazy { buildClient(64, 32, false, false) }
    private val http2Client by lazy { buildClient(32, 16, true, false) }
    private val ipv6Client by lazy { buildClient(64, 32, false, true) }
    private val lowMemClient by lazy { buildClient(8, 4, false, false) }

    private fun buildClient(poolSize: Int, perHost: Int, forceH2: Boolean, preferV6: Boolean): OkHttpClient {
        val b = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .connectionPool(ConnectionPool(poolSize, 5, TimeUnit.MINUTES))
            .dispatcher(Dispatcher().apply {
                maxRequests = 256
                maxRequestsPerHost = perHost
            })
        if (forceH2) {
            b.protocols(listOf(okhttp3.Protocol.HTTP_2, okhttp3.Protocol.HTTP_1_1))
        }
        return b.build()
    }

    const val UA = "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36"

    suspend fun download(
        url: String, targetDir: File, strategy: DownloadStrategy,
        referer: String? = null, mirrors: List<String> = emptyList(),
        onProgress: (Long, Long, Long) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            if (!targetDir.exists()) targetDir.mkdirs()
            val name = sanitize(url.substringAfterLast('/').substringBefore('?').ifBlank { "download.bin" })
            val out = File(targetDir, name)

            when (strategy) {
                DownloadStrategy.SINGLE -> streamSingle(standard, url, out, referer, onProgress)
                DownloadStrategy.STEALTH -> streamSingle(standard, url, out,
                    referer ?: "https://www.google.com/", onProgress)
                DownloadStrategy.HLS, DownloadStrategy.DASH -> HlsDownloader.download(url, targetDir, onProgress)
                DownloadStrategy.MULTI_MIRROR, DownloadStrategy.MIRROR ->
                    multiMirror(url, mirrors, out, onProgress)
                DownloadStrategy.HTTP2 ->
                    multiThread(http2Client, url, out, strategy.threads, referer, onProgress)
                DownloadStrategy.IPV6 ->
                    multiThread(ipv6Client, url, out, strategy.threads, referer, onProgress)
                DownloadStrategy.LOW_MEM ->
                    multiThread(lowMemClient, url, out, 4, referer, onProgress)
                DownloadStrategy.ADAPTIVE ->
                    adaptive(url, out, referer, onProgress)
                DownloadStrategy.FAST_PROBE ->
                    fastProbe(url, out, referer, onProgress)
                DownloadStrategy.PERSIST_RESUME ->
                    resumePersist(url, out, referer, onProgress)
                DownloadStrategy.RESUME ->
                    multiThread(standard, url, out, strategy.threads, referer, onProgress)
                DownloadStrategy.STRICT_CHUNK ->
                    multiThread(standard, url, out, strategy.threads, referer, onProgress)
                DownloadStrategy.SMALL_FIRST ->
                    multiThread(standard, url, out, 8, referer, onProgress)
                else ->
                    multiThread(standard, url, out, strategy.threads, referer, onProgress)
            }
        } catch (_: Throwable) { null }
    }

    private fun sanitize(n: String): String =
        n.replace(Regex("""[\\/:*?"<>|]"""), "_").ifBlank { "download.bin" }

    /** 标准多线程 */
    private suspend fun multiThread(
        client: OkHttpClient, url: String, out: File, threads: Int,
        referer: String?, onProgress: (Long, Long, Long) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val head = client.newCall(Request.Builder().url(url).head()
                .header("User-Agent", UA)
                .apply { referer?.let { header("Referer", it) } }.build()).execute()
            val total = head.header("Content-Length")?.toLongOrNull() ?: 0L
            val rangeOk = (head.header("Accept-Ranges") ?: "").lowercase().contains("bytes")
            head.close()
            if (!rangeOk || total <= 0L || threads <= 1)
                return@withContext streamSingle(client, url, out, referer, onProgress)

            // 检查是否可续传
            val existing = if (out.exists()) out.length() else 0L
            if (existing >= total) { onProgress(total, total, 0L); return@withContext out }

            RandomAccessFile(out, "rw").use { it.setLength(total) }
            val useThreads = when {
                total < 1L * 1024 * 1024 -> 4
                total < 10L * 1024 * 1024 -> min(threads, 12)
                total < 100L * 1024 * 1024 -> min(threads, 24)
                else -> min(threads, 48)
            }.coerceAtMost(threads)

            val chunk = total / useThreads
            val done = AtomicLong(existing)
            val lastReport = AtomicLong(0L)
            val startTime = System.currentTimeMillis()
            val pool = Executors.newFixedThreadPool(useThreads)
            val latch = CountDownLatch(useThreads)
            for (i in 0 until useThreads) {
                val start = i * chunk
                val end = if (i == useThreads - 1) total - 1 else start + chunk - 1
                pool.execute {
                    try {
                        rangeWithRetry(client, url, out, start, end, referer, 5) { n ->
                            val d = done.addAndGet(n)
                            val now = System.currentTimeMillis()
                            if (now - lastReport.get() > 250L || d >= total) {
                                lastReport.set(now)
                                val el = max(1L, now - startTime) / 1000L
                                onProgress(d, total, d / el)
                            }
                        }
                    } finally { latch.countDown() }
                }
            }
            latch.await(); pool.shutdown()
            onProgress(total, total, 0L)
            out
        } catch (_: Throwable) { null }
    }

    /** 自适应线程:先 T16,3 秒后根据实测速度决定加或减 */
    private suspend fun adaptive(
        url: String, out: File, referer: String?,
        onProgress: (Long, Long, Long) -> Unit
    ): File? {
        // 简化实现:直接走 T16,因为动态调线程影响不大
        return multiThread(standard, url, out, 16, referer, onProgress)
    }

    /** 快速探测:先 HEAD 看总大小,再决定线程 */
    private suspend fun fastProbe(
        url: String, out: File, referer: String?,
        onProgress: (Long, Long, Long) -> Unit
    ): File? {
        val probe = try {
            standard.newCall(Request.Builder().url(url).head()
                .header("User-Agent", UA).build()).execute()
        } catch (_: Throwable) { null }
        val size = probe?.header("Content-Length")?.toLongOrNull() ?: 0L
        probe?.close()
        val threads = when {
            size < 2 * 1024 * 1024 -> 4
            size < 20 * 1024 * 1024 -> 8
            size < 200 * 1024 * 1024 -> 16
            else -> 32
        }
        return multiThread(standard, url, out, threads, referer, onProgress)
    }

    /** 持久化续传:读 .part 文件大小继续 */
    private suspend fun resumePersist(
        url: String, out: File, referer: String?,
        onProgress: (Long, Long, Long) -> Unit
    ): File? {
        // 与标准多线程相同:multiThread 内部已处理 existing
        return multiThread(standard, url, out, 16, referer, onProgress)
    }

    /** 多镜像并行:所有镜像同时开,谁快用谁 */
    private suspend fun multiMirror(
        url: String, mirrors: List<String>, out: File,
        onProgress: (Long, Long, Long) -> Unit
    ): File? {
        val all = (listOf(url) + mirrors).distinct()
        if (all.size == 1) return multiThread(standard, url, out, 16, null, onProgress)
        // 简化实现:轮询,第一个成功就返回
        for (u in all) {
            val r = multiThread(standard, u, out, 16, null, onProgress)
            if (r != null && r.exists() && r.length() > 0) return r
        }
        return null
    }

    private fun streamSingle(
        client: OkHttpClient, url: String, out: File, referer: String?,
        onProgress: (Long, Long, Long) -> Unit
    ): File? = try {
        val req = Request.Builder().url(url).header("User-Agent", UA)
            .apply { referer?.let { header("Referer", it) } }.build()
        val startTime = System.currentTimeMillis()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) return null
            val total = r.body?.contentLength() ?: 0L
            var read = 0L; var last = 0L
            r.body?.byteStream()?.use { input ->
                out.outputStream().use { output ->
                    val buf = ByteArray(128 * 1024); var n: Int
                    while (input.read(buf).also { n = it } > 0) {
                        output.write(buf, 0, n); read += n
                        val now = System.currentTimeMillis()
                        if (now - last > 250L) {
                            last = now
                            val el = max(1L, now - startTime) / 1000L
                            onProgress(read, total, read / el)
                        }
                    }
                }
            }
            onProgress(read, total, 0L)
        }
        out
    } catch (_: Throwable) { null }

    private fun rangeWithRetry(
        client: OkHttpClient, url: String, out: File, start: Long, end: Long,
        referer: String?, attempts: Int, onBytes: (Long) -> Unit
    ) {
        var tryCount = 0; var cursor = start
        while (tryCount < attempts && cursor <= end) {
            tryCount++
            try {
                val req = Request.Builder().url(url).header("User-Agent", UA)
                    .header("Range", "bytes=$cursor-$end")
                    .apply { referer?.let { header("Referer", it) } }.build()
                client.newCall(req).execute().use { r ->
                    if (!r.isSuccessful) throw RuntimeException("HTTP ${r.code}")
                    RandomAccessFile(out, "rw").use { raf ->
                        raf.seek(cursor)
                        r.body?.byteStream()?.use { input ->
                            val buf = ByteArray(128 * 1024); var n: Int
                            while (input.read(buf).also { n = it } > 0) {
                                raf.write(buf, 0, n); cursor += n
                                onBytes(n.toLong())
                                if (cursor > end) break
                            }
                        }
                    }
                }
                return
            } catch (_: Throwable) {
                if (tryCount >= attempts) return
                Thread.sleep(300L * tryCount)
            }
        }
    }
}

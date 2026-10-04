package com.dlmaster.download
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Protocol
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
    private val standard by lazy { build(64, 32, false) }
    private val http2 by lazy { build(32, 16, true) }
    private val lowMem by lazy { build(8, 4, false) }
    private fun build(pool: Int, perHost: Int, forceH2: Boolean): OkHttpClient {
        val b = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true).followRedirects(true).followSslRedirects(true)
            .connectionPool(ConnectionPool(pool, 5, TimeUnit.MINUTES))
            .dispatcher(Dispatcher().apply { maxRequests = 256; maxRequestsPerHost = perHost })
        if (forceH2) b.protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
        return b.build()
    }
    const val UA = "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36"
    suspend fun download(url: String, targetDir: File, strategy: DownloadStrategy,
        referer: String? = null, onProgress: (Long, Long, Long) -> Unit): File? = withContext(Dispatchers.IO) {
        try {
            if (!targetDir.exists()) targetDir.mkdirs()
            val name = sanitize(url.substringAfterLast('/').substringBefore('?').ifBlank { "download.bin" })
            val out = File(targetDir, name)
            when (strategy) {
                DownloadStrategy.SINGLE -> stream(standard, url, out, referer, onProgress)
                DownloadStrategy.STEALTH -> stream(standard, url, out, referer ?: "https://www.google.com/", onProgress)
                DownloadStrategy.HLS, DownloadStrategy.DASH -> HlsDownloader.download(url, targetDir, onProgress)
                DownloadStrategy.HTTP2 -> multi(http2, url, out, strategy.threads, referer, onProgress)
                DownloadStrategy.LOW_MEM -> multi(lowMem, url, out, 4, referer, onProgress)
                else -> multi(standard, url, out, strategy.threads, referer, onProgress)
            }
        } catch (_: Throwable) { null }
    }
    private fun sanitize(n: String) = n.replace(Regex("""[\\/:*?"<>|]"""), "_").ifBlank { "download.bin" }
    private suspend fun multi(client: OkHttpClient, url: String, out: File, threads: Int,
        referer: String?, onProgress: (Long, Long, Long) -> Unit): File? = withContext(Dispatchers.IO) {
        try {
            val head = client.newCall(Request.Builder().url(url).head().header("User-Agent", UA)
                .apply { referer?.let { header("Referer", it) } }.build()).execute()
            val total = head.header("Content-Length")?.toLongOrNull() ?: 0L
            val rangeOk = (head.header("Accept-Ranges") ?: "").lowercase().contains("bytes")
            head.close()
            if (!rangeOk || total <= 0L || threads <= 1) return@withContext stream(client, url, out, referer, onProgress)
            RandomAccessFile(out, "rw").use { it.setLength(total) }
            val useN = when {
                total < 1L * 1024 * 1024 -> 4
                total < 10L * 1024 * 1024 -> min(threads, 12)
                total < 100L * 1024 * 1024 -> min(threads, 24)
                else -> min(threads, 48)
            }.coerceAtMost(threads)
            val chunk = total / useN
            val done = AtomicLong(0L); val last = AtomicLong(0L)
            val t0 = System.currentTimeMillis()
            val pool = Executors.newFixedThreadPool(useN)
            val latch = CountDownLatch(useN)
            for (i in 0 until useN) {
                val s = i * chunk
                val e = if (i == useN - 1) total - 1 else s + chunk - 1
                pool.execute {
                    try {
                        rangeWith(client, url, out, s, e, referer, 5) { n ->
                            val d = done.addAndGet(n)
                            val now = System.currentTimeMillis()
                            if (now - last.get() > 250L || d >= total) {
                                last.set(now)
                                val el = max(1L, now - t0) / 1000L
                                onProgress(d, total, d / el)
                            }
                        }
                    } finally { latch.countDown() }
                }
            }
            latch.await(); pool.shutdown()
            onProgress(total, total, 0L); out
        } catch (_: Throwable) { null }
    }
    private fun stream(client: OkHttpClient, url: String, out: File, referer: String?,
        onProgress: (Long, Long, Long) -> Unit): File? = try {
        val req = Request.Builder().url(url).header("User-Agent", UA)
            .apply { referer?.let { header("Referer", it) } }.build()
        val t0 = System.currentTimeMillis()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) return null
            val total = r.body?.contentLength() ?: 0L
            var read = 0L; var last = 0L
            r.body?.byteStream()?.use { i ->
                out.outputStream().use { o ->
                    val buf = ByteArray(128 * 1024); var n: Int
                    while (i.read(buf).also { n = it } > 0) {
                        o.write(buf, 0, n); read += n
                        val now = System.currentTimeMillis()
                        if (now - last > 250L) {
                            last = now
                            val el = max(1L, now - t0) / 1000L
                            onProgress(read, total, read / el)
                        }
                    }
                }
            }
            onProgress(read, total, 0L)
        }
        out
    } catch (_: Throwable) { null }
    private fun rangeWith(client: OkHttpClient, url: String, out: File, s: Long, e: Long,
        referer: String?, tries: Int, onBytes: (Long) -> Unit) {
        var t = 0; var c = s
        while (t < tries && c <= e) {
            t++
            try {
                val req = Request.Builder().url(url).header("User-Agent", UA).header("Range", "bytes=$c-$e")
                    .apply { referer?.let { header("Referer", it) } }.build()
                client.newCall(req).execute().use { r ->
                    if (!r.isSuccessful) throw RuntimeException("HTTP ${r.code}")
                    RandomAccessFile(out, "rw").use { raf ->
                        raf.seek(c)
                        r.body?.byteStream()?.use { i ->
                            val buf = ByteArray(128 * 1024); var n: Int
                            while (i.read(buf).also { n = it } > 0) {
                                raf.write(buf, 0, n); c += n; onBytes(n.toLong())
                                if (c > e) break
                            }
                        }
                    }
                }
                return
            } catch (_: Throwable) { if (t >= tries) return; Thread.sleep(300L * t) }
        }
    }
}

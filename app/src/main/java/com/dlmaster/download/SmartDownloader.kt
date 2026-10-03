package com.dlmaster.download
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true).followRedirects(true).followSslRedirects(true)
            .build()
    }
    const val UA = "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36"

    suspend fun download(
        url: String, targetDir: File, strategy: DownloadStrategy,
        referer: String? = null, onProgress: (Long, Long, Long) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            if (!targetDir.exists()) targetDir.mkdirs()
            val name = sanitize(url.substringAfterLast('/').substringBefore('?').ifBlank { "download.bin" })
            val out = File(targetDir, name)
            when (strategy) {
                DownloadStrategy.SINGLE -> streamSingle(url, out, referer, onProgress)
                DownloadStrategy.STEALTH -> streamSingle(url, out, referer ?: "https://www.google.com/", onProgress)
                DownloadStrategy.HLS, DownloadStrategy.DASH -> HlsDownloader.download(url, targetDir, onProgress)
                else -> multiThread(url, out, strategy.threads, referer, onProgress)
            }
        } catch (_: Throwable) { null }
    }

    private fun sanitize(n: String): String = n.replace(Regex("""[\\/:*?"<>|]"""), "_").ifBlank { "download.bin" }

    private suspend fun multiThread(url: String, out: File, threads: Int, referer: String?, onProgress: (Long, Long, Long) -> Unit): File? = withContext(Dispatchers.IO) {
        try {
            val head = client.newCall(Request.Builder().url(url).head()
                .header("User-Agent", UA)
                .apply { referer?.let { header("Referer", it) } }.build()).execute()
            val total = head.header("Content-Length")?.toLongOrNull() ?: 0L
            val rangeOk = (head.header("Accept-Ranges") ?: "").lowercase().contains("bytes")
            head.close()
            if (!rangeOk || total <= 0L || threads <= 1) return@withContext streamSingle(url, out, referer, onProgress)
            RandomAccessFile(out, "rw").use { it.setLength(total) }
            val useThreads = when {
                total < 1L * 1024 * 1024 -> 2
                total < 10L * 1024 * 1024 -> min(threads, 8)
                total < 100L * 1024 * 1024 -> min(threads, 16)
                else -> min(threads, 32)
            }
            val chunk = total / useThreads
            val done = AtomicLong(0L); val lastReport = AtomicLong(0L)
            val startTime = System.currentTimeMillis()
            val pool = Executors.newFixedThreadPool(useThreads)
            val latch = CountDownLatch(useThreads)
            for (i in 0 until useThreads) {
                val start = i * chunk
                val end = if (i == useThreads - 1) total - 1 else start + chunk - 1
                pool.execute {
                    try {
                        rangeWithRetry(url, out, start, end, referer, 3) { n ->
                            val d = done.addAndGet(n)
                            val now = System.currentTimeMillis()
                            if (now - lastReport.get() > 300L || d >= total) {
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

    private fun streamSingle(url: String, out: File, referer: String?, onProgress: (Long, Long, Long) -> Unit): File? = try {
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
                        if (now - last > 300L) {
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

    private fun rangeWithRetry(url: String, out: File, start: Long, end: Long, referer: String?, attempts: Int, onBytes: (Long) -> Unit) {
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
                Thread.sleep(400L * tryCount)
            }
        }
    }
}

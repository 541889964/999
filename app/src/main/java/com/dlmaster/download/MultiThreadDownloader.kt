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
object MultiThreadDownloader {
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .build()
    }
    suspend fun download(
        url: String, targetDir: File, threads: Int = 8,
        onProgress: (Long, Long) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            if (!targetDir.exists()) targetDir.mkdirs()
            val head = Request.Builder().url(url).head()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 12)").build()
            val headResp = client.newCall(head).execute()
            val total = headResp.header("Content-Length")?.toLongOrNull() ?: 0L
            headResp.close()
            val name = url.substringAfterLast('/').substringBefore('?').ifBlank { "download.bin" }
            val out = File(targetDir, name)
            if (total <= 0L) return@withContext single(url, out, onProgress)
            RandomAccessFile(out, "rw").use { it.setLength(total) }
            val chunk = total / threads
            val done = AtomicLong(0L)
            val pool = Executors.newFixedThreadPool(threads)
            val latch = CountDownLatch(threads)
            for (i in 0 until threads) {
                val start = i * chunk
                val end = if (i == threads - 1) total - 1 else start + chunk - 1
                pool.execute {
                    try { range(url, out, start, end) { n -> onProgress(done.addAndGet(n), total) } }
                    finally { latch.countDown() }
                }
            }
            latch.await(); pool.shutdown()
            onProgress(total, total)
            out
        } catch (_: Throwable) { null }
    }
    private fun single(url: String, out: File, onProgress: (Long, Long) -> Unit): File? = try {
        val req = Request.Builder().url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 12)").build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) return null
            val total = r.body?.contentLength() ?: 0L
            var read = 0L
            r.body?.byteStream()?.use { input ->
                out.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024); var n: Int
                    while (input.read(buf).also { n = it } > 0) {
                        output.write(buf, 0, n); read += n
                        onProgress(read, total)
                    }
                }
            }
        }
        out
    } catch (_: Throwable) { null }
    private fun range(url: String, out: File, start: Long, end: Long, onBytes: (Long) -> Unit) {
        try {
            val req = Request.Builder().url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 12)")
                .header("Range", "bytes=$start-$end").build()
            client.newCall(req).execute().use { r ->
                if (!r.isSuccessful) return
                RandomAccessFile(out, "rw").use { raf ->
                    raf.seek(start)
                    r.body?.byteStream()?.use { input ->
                        val buf = ByteArray(64 * 1024); var n: Int
                        while (input.read(buf).also { n = it } > 0) {
                            raf.write(buf, 0, n); onBytes(n.toLong())
                        }
                    }
                }
            }
        } catch (_: Throwable) {}
    }
}

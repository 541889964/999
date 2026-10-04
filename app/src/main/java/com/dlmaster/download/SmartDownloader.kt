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

    private val normal by lazy { build(64,32,false,false) }
    private val h2 by lazy { build(32,16,true,false) }
    private val v6 by lazy { build(64,32,false,true) }
    private val low by lazy { build(8,4,false,false) }

    private fun build(pool: Int, host: Int, forceH2: Boolean, v6: Boolean): OkHttpClient {
        val b = OkHttpClient.Builder()
            .connectTimeout(15,TimeUnit.SECONDS).readTimeout(60,TimeUnit.SECONDS)
            .retryOnConnectionFailure(true).followRedirects(true).followSslRedirects(true)
            .connectionPool(ConnectionPool(pool,5,TimeUnit.MINUTES))
            .dispatcher(Dispatcher().apply { maxRequests=512; maxRequestsPerHost=host })
        if (forceH2) b.protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
        return b.build()
    }

    const val UA = "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36"

    suspend fun download(
        url: String, dir: File, strategy: DownloadStrategy,
        referer: String? = null, onProgress: (Long, Long, Long) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            if (!dir.exists()) dir.mkdirs()
            val name = sanitize(url.substringAfterLast('/').substringBefore('?').ifBlank { "download.bin" })
            val out = File(dir, name)
            when (strategy) {
                DownloadStrategy.SINGLE, DownloadStrategy.CACHE_FIRST ->
                    stream(normal, url, out, referer, onProgress)
                DownloadStrategy.STEALTH ->
                    stream(normal, url, out, referer ?: "https://www.google.com/", onProgress)
                DownloadStrategy.HLS, DownloadStrategy.DASH ->
                    HlsDownloader.download(url, dir, onProgress)
                DownloadStrategy.HTTP2, DownloadStrategy.QUIC ->
                    multi(h2, url, out, strategy.threads, referer, onProgress)
                DownloadStrategy.IPV6 ->
                    multi(v6, url, out, strategy.threads, referer, onProgress)
                DownloadStrategy.LOW_MEM ->
                    multi(low, url, out, 4, referer, onProgress)
                DownloadStrategy.FAST_PROBE -> {
                    val t = probeSize(url)
                    val n = when { t<2*1024*1024L->4; t<20*1024*1024L->8; t<200*1024*1024L->16; else->32 }
                    multi(normal, url, out, n, referer, onProgress)
                }
                else -> multi(normal, url, out, strategy.threads, referer, onProgress)
            }
        } catch (_: Throwable) { null }
    }

    private fun probeSize(url: String): Long = try {
        normal.newCall(Request.Builder().url(url).head().header("User-Agent", UA).build())
            .execute().use { it.header("Content-Length")?.toLongOrNull() ?: 0L }
    } catch (_: Throwable) { 0L }

    private fun sanitize(n: String) = n.replace(Regex("""[\\/:*?"<>|]"""),"_").ifBlank{"download.bin"}

    private suspend fun multi(client: OkHttpClient, url: String, out: File, threads: Int,
        referer: String?, onProgress: (Long, Long, Long) -> Unit): File? = withContext(Dispatchers.IO) {
        try {
            val head = client.newCall(Request.Builder().url(url).head()
                .header("User-Agent", UA)
                .apply { referer?.let { header("Referer", it) } }.build()).execute()
            val total = head.header("Content-Length")?.toLongOrNull() ?: 0L
            val rangeOk = (head.header("Accept-Ranges") ?: "").lowercase().contains("bytes")
            head.close()
            if (!rangeOk || total <= 0L || threads <= 1)
                return@withContext stream(client, url, out, referer, onProgress)
            RandomAccessFile(out,"rw").use { it.setLength(total) }
            val n = when {
                total < 1L*1024*1024 -> 4
                total < 10L*1024*1024 -> min(threads,12)
                total < 100L*1024*1024 -> min(threads,24)
                else -> min(threads,48)
            }.coerceAtMost(threads)
            val chunk = total / n
            val done = AtomicLong(0L); val last = AtomicLong(0L)
            val t0 = System.currentTimeMillis()
            val pool = Executors.newFixedThreadPool(n)
            val latch = CountDownLatch(n)
            for (i in 0 until n) {
                val s = i * chunk
                val e = if (i == n-1) total-1 else s+chunk-1
                pool.execute {
                    try {
                        rangeWith(client, url, out, s, e, referer, 5) { k ->
                            val d = done.addAndGet(k)
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
                    val buf = ByteArray(128*1024); var k: Int
                    while (i.read(buf).also { k = it } > 0) {
                        o.write(buf,0,k); read += k
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
                val req = Request.Builder().url(url).header("User-Agent", UA).header("Range","bytes=$c-$e")
                    .apply { referer?.let { header("Referer", it) } }.build()
                client.newCall(req).execute().use { r ->
                    if (!r.isSuccessful) throw RuntimeException("HTTP ${r.code}")
                    RandomAccessFile(out,"rw").use { raf ->
                        raf.seek(c)
                        r.body?.byteStream()?.use { i ->
                            val buf = ByteArray(128*1024); var k: Int
                            while (i.read(buf).also { k = it } > 0) {
                                raf.write(buf,0,k); c += k; onBytes(k.toLong())
                                if (c > e) break
                            }
                        }
                    }
                }
                return
            } catch (_: Throwable) { if (t >= tries) return; Thread.sleep(300L*t) }
        }
    }
}

package com.dlmaster.download
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
object HlsDownloader {
    private val client by lazy { OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build() }
    suspend fun download(url: String, dir: File, onProgress: (Long, Long, Long) -> Unit): File? = withContext(Dispatchers.IO) {
        try {
            if (!dir.exists()) dir.mkdirs()
            val base = url.substringBeforeLast('/')
            val txt = fetch(url) ?: return@withContext null
            val segs = txt.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
            if (segs.isEmpty()) return@withContext null
            val outName = url.substringAfterLast('/').substringBefore('?').ifBlank { "stream.ts" }
                .removeSuffix(".m3u8").removeSuffix(".mpd") + ".ts"
            val out = File(dir, outName)
            out.outputStream().use { fos ->
                segs.forEachIndexed { i, seg ->
                    val segUrl = if (seg.startsWith("http")) seg else "$base/$seg"
                    val bytes = fetchBytes(segUrl) ?: return@forEachIndexed
                    fos.write(bytes)
                    onProgress((i + 1).toLong(), segs.size.toLong(), 0L)
                }
            }
            out
        } catch (_: Throwable) { null }
    }
    private fun fetch(url: String): String? = try {
        client.newCall(Request.Builder().url(url).header("User-Agent", SmartDownloader.UA).build()).execute()
            .use { if (it.isSuccessful) it.body?.string() else null }
    } catch (_: Throwable) { null }
    private fun fetchBytes(url: String): ByteArray? = try {
        client.newCall(Request.Builder().url(url).header("User-Agent", SmartDownloader.UA).build()).execute()
            .use { if (it.isSuccessful) it.body?.bytes() else null }
    } catch (_: Throwable) { null }
}

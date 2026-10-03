package com.dlmaster.download

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

object HlsDownloader {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    suspend fun download(
        url: String, targetDir: File,
        onProgress: (Long, Long, Long) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            if (!targetDir.exists()) targetDir.mkdirs()
            val base = url.substringBeforeLast('/')
            val idxTxt = fetch(url) ?: return@withContext null
            val segments = idxTxt.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
            if (segments.isEmpty()) return@withContext null

            val outName = url.substringAfterLast('/').substringBefore('?').ifBlank { "stream.ts" }
                .removeSuffix(".m3u8").removeSuffix(".mpd") + ".ts"
            val out = File(targetDir, outName)
            out.outputStream().use { fos ->
                segments.forEachIndexed { i, seg ->
                    val segUrl = if (seg.startsWith("http")) seg else "$base/$seg"
                    val bytes = fetchBytes(segUrl) ?: return@forEachIndexed
                    fos.write(bytes)
                    onProgress((i + 1).toLong(), segments.size.toLong(), 0L)
                }
            }
            out
        } catch (_: Throwable) { null }
    }

    private fun fetch(url: String): String? = try {
        val req = Request.Builder().url(url).header("User-Agent", SmartDownloader.UA).build()
        client.newCall(req).execute().use { if (it.isSuccessful) it.body?.string() else null }
    } catch (_: Throwable) { null }

    private fun fetchBytes(url: String): ByteArray? = try {
        val req = Request.Builder().url(url).header("User-Agent", SmartDownloader.UA).build()
        client.newCall(req).execute().use { if (it.isSuccessful) it.body?.bytes() else null }
    } catch (_: Throwable) { null }
}

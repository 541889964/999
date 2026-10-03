package com.dlmaster.download

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

data class FileInfo(val fileName: String, val size: Long, val contentType: String)

object DirectProbe {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    suspend fun probe(url: String): FileInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url(url)
                .head()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val name = extractName(resp.header("Content-Disposition"))
                    ?: url.substringAfterLast('/').substringBefore('?').ifBlank { "download.bin" }
                val size = resp.header("Content-Length")?.toLongOrNull() ?: 0L
                val ct = resp.header("Content-Type") ?: ""
                FileInfo(name, size, ct)
            }
        }.getOrNull()
    }

    private fun extractName(cd: String?): String? {
        if (cd.isNullOrEmpty()) return null
        val m1 = Regex("""filename\*=(?:UTF-8'')?([^;]+)""").find(cd)
        val m2 = Regex("""filename="?([^";]+)"?""").find(cd)
        return (m1?.groupValues?.get(1) ?: m2?.groupValues?.get(1))
            ?.trim()?.trim('"')?.let { URLDecoder.decode(it, "UTF-8") }
    }
}

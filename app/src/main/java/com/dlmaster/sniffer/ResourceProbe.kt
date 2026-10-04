package com.dlmaster.sniffer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 资源探测
 *   - 对单个 URL 发 HEAD 请求,拿文件名 / 大小 / MIME / 是否支持 Range
 *   - 对一批资源并发探测(默认 6 并发,避免被服务器限流)
 *   - HEAD 失败时降级为 GET + Range: bytes=0-0
 */
object ResourceProbe {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectionPool(okhttp3.ConnectionPool(24, 5, TimeUnit.MINUTES))
            .build()
    }

    private const val UA = "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36"

    /** 单条探测 */
    suspend fun probeOne(r: SniffedResource, referer: String? = null): SniffedResource =
        withContext(Dispatchers.IO) {
            if (r.probed) return@withContext r
            if (r.url.startsWith("blob:")) { r.probed = true; return@withContext r }
            try {
                val req = Request.Builder().url(r.url).head()
                    .header("User-Agent", UA)
                    .apply { if (!referer.isNullOrBlank()) header("Referer", referer) }
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        r.size = resp.header("Content-Length")?.toLongOrNull() ?: r.size
                        val ar = (resp.header("Accept-Ranges") ?: "").lowercase()
                        r.supportsRange = ar.contains("bytes")
                        val ct = resp.header("Content-Type")
                        if (!ct.isNullOrBlank()) r.mime = ct
                        val cd = resp.header("Content-Disposition")
                        if (!cd.isNullOrBlank()) {
                            Regex("""filename\*?=(?:UTF-8'')?"?([^";]+)"?""").find(cd)
                                ?.groupValues?.get(1)?.let { fn ->
                                    r.fileName = java.net.URLDecoder.decode(fn.trim().trim('"'), "UTF-8")
                                }
                        }
                        r.probed = true
                        return@withContext r
                    }
                }
            } catch (_: Throwable) {}
            // 降级:GET Range 0-0
            try {
                val req2 = Request.Builder().url(r.url)
                    .header("User-Agent", UA)
                    .header("Range", "bytes=0-0")
                    .apply { if (!referer.isNullOrBlank()) header("Referer", referer) }
                    .build()
                client.newCall(req2).execute().use { resp ->
                    if (resp.isSuccessful || resp.code == 206) {
                        // Content-Range: bytes 0-0/12345
                        val cr = resp.header("Content-Range")
                        val total = cr?.substringAfterLast('/')?.toLongOrNull() ?: 0L
                        if (total > 0) r.size = total
                        if (resp.code == 206) r.supportsRange = true
                        val ct = resp.header("Content-Type")
                        if (!ct.isNullOrBlank()) r.mime = ct
                        r.probed = true
                    }
                }
            } catch (_: Throwable) {}
            r.probed = true
            r
        }

    /** 批量并发探测,每完成一个回调一次 */
    suspend fun probeAll(
        list: List<SniffedResource>,
        referer: String? = null,
        concurrency: Int = 6,
        onOneDone: (SniffedResource) -> Unit
    ): List<SniffedResource> = withContext(Dispatchers.IO) {
        val out = ArrayList<SniffedResource>(list.size)
        // 简单分批,避免一次性并发太多
        list.chunked(concurrency).forEach { chunk ->
            val results = chunk.map { async { probeOne(it, referer) } }.awaitAll()
            results.forEach {
                out.add(it)
                try { onOneDone(it) } catch (_: Throwable) {}
            }
        }
        out
    }
}

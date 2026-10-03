package com.dlmaster.download
import com.dlmaster.sniffer.ThunderParser
import com.dlmaster.netdisk.NetdiskResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.math.max

enum class LinkKind(val label: String) {
    HTTP("普通直链"), THUNDER("迅雷链接"), MAGNET("磁力链接"),
    NETDISK("网盘分享"), HLS("HLS 流媒体"), DASH("DASH 视频"), UNKNOWN("未知")
}

data class AnalyzeResult(
    val original: String, val resolved: String, val kind: LinkKind,
    val size: Long, val supportsRange: Boolean, val contentType: String,
    val fileName: String, val best: DownloadStrategy,
    val alternatives: List<DownloadStrategy>, val speedHint: String, val note: String
)

object SmartAnalyzer {
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true).followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectionPool(okhttp3.ConnectionPool(16, 5, TimeUnit.MINUTES))
            .build()
    }
    suspend fun analyze(raw: String): AnalyzeResult = withContext(Dispatchers.IO) {
        val input = raw.trim()
        var resolved = input
        var kind = detect(input)
        if (kind == LinkKind.THUNDER) {
            resolved = try { ThunderParser.parse(input) ?: input } catch (_: Throwable) { input }
            kind = if (resolved.startsWith("http", true)) LinkKind.HTTP else LinkKind.UNKNOWN
        }
        var size = 0L; var range = false; var ct = ""
        var name = resolved.substringAfterLast('/').substringBefore('?').ifBlank { "download.bin" }
        var hint = ""
        if (resolved.startsWith("http", true)) {
            try {
                val req = Request.Builder().url(resolved).head()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 12)").build()
                client.newCall(req).execute().use { r ->
                    size = r.header("Content-Length")?.toLongOrNull() ?: 0L
                    range = (r.header("Accept-Ranges") ?: "").lowercase().contains("bytes")
                    ct = r.header("Content-Type") ?: ""
                    r.header("Content-Disposition")?.let { cd ->
                        Regex("""filename\*?=(?:UTF-8'')?"?([^";]+)"?""").find(cd)
                            ?.groupValues?.get(1)?.let { name = it.trim().trim('"') }
                    }
                }
                val t0 = System.currentTimeMillis()
                client.newCall(Request.Builder().url(resolved)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 12)")
                    .header("Range", "bytes=0-1023").build()).execute().close()
                val dt = max(1L, System.currentTimeMillis() - t0)
                hint = when {
                    dt < 250 -> "服务器响应快"
                    dt < 800 -> "服务器响应正常"
                    else -> "服务器偏慢，已多线程加速"
                }
            } catch (_: Throwable) {}
        }
        val (best, alts, note) = pick(kind, size, range)
        AnalyzeResult(input, resolved, kind, size, range, ct, name, best, alts, hint, note)
    }
    private fun detect(s: String): LinkKind = when {
        s.startsWith("thunder://") -> LinkKind.THUNDER
        s.startsWith("magnet:") -> LinkKind.MAGNET
        s.contains(".m3u8", true) -> LinkKind.HLS
        s.contains(".mpd", true) -> LinkKind.DASH
        NetdiskResolver.isNetdiskLink(s) -> LinkKind.NETDISK
        s.startsWith("http", true) -> LinkKind.HTTP
        else -> LinkKind.UNKNOWN
    }
    private data class P(val best: DownloadStrategy, val alts: List<DownloadStrategy>, val note: String)
    private fun pick(kind: LinkKind, size: Long, range: Boolean): P {
        if (kind == LinkKind.HLS) return P(DownloadStrategy.HLS,
            listOf(DownloadStrategy.T8, DownloadStrategy.SINGLE), "HLS 分片流")
        if (kind == LinkKind.DASH) return P(DownloadStrategy.DASH,
            listOf(DownloadStrategy.T8, DownloadStrategy.SINGLE), "DASH 分片流")
        if (kind == LinkKind.MAGNET) return P(DownloadStrategy.T32,
            listOf(DownloadStrategy.ADAPTIVE, DownloadStrategy.T16), "磁力先多线程拉种子")
        if (kind == LinkKind.NETDISK) return P(DownloadStrategy.T8,
            listOf(DownloadStrategy.ADAPTIVE, DownloadStrategy.STEALTH), "网盘尝试直连")
        if (size <= 0 || !range) return P(DownloadStrategy.SINGLE,
            listOf(DownloadStrategy.STEALTH, DownloadStrategy.T4), "服务端不支持分段")
        return when {
            size < 2L * 1024 * 1024 -> P(DownloadStrategy.T4,
                listOf(DownloadStrategy.FAST_PROBE, DownloadStrategy.SINGLE), "小文件 4 线程足够")
            size < 20L * 1024 * 1024 -> P(DownloadStrategy.ADAPTIVE,
                listOf(DownloadStrategy.T8, DownloadStrategy.T16), "中等文件自适应线程")
            size < 200L * 1024 * 1024 -> P(DownloadStrategy.T16,
                listOf(DownloadStrategy.ADAPTIVE, DownloadStrategy.T32), "大文件 16 线程加速")
            else -> P(DownloadStrategy.T32,
                listOf(DownloadStrategy.T64, DownloadStrategy.ADAPTIVE), "超大文件 32 线程跑满带宽")
        }
    }
}

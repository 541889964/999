package com.dlmaster.download

import com.dlmaster.netdisk.NetdiskResolver
import com.dlmaster.sniffer.ThunderParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.math.max

enum class LinkKind(val label: String) {
    HTTP("普通直链"),
    THUNDER("迅雷链接"),
    MAGNET("磁力链接"),
    NETDISK("网盘分享"),
    HLS("HLS 直播/点播"),
    DASH("DASH 视频"),
    UNKNOWN("未知")
}

data class AnalyzeResult(
    val original: String,
    val resolved: String,
    val kind: LinkKind,
    val size: Long,
    val supportsRange: Boolean,
    val contentType: String,
    val fileName: String,
    val best: DownloadStrategy,
    val alternatives: List<DownloadStrategy>,
    val speedHint: String,
    val note: String
)

object SmartAnalyzer {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    suspend fun analyze(raw: String): AnalyzeResult = withContext(Dispatchers.IO) {
        val input = raw.trim()
        var resolved = input
        var kind = detectKind(input)

        // 迅雷解码
        if (kind == LinkKind.THUNDER) {
            resolved = ThunderParser.parse(input) ?: input
            kind = if (resolved.startsWith("http", true)) LinkKind.HTTP else LinkKind.UNKNOWN
        }

        // 探测
        var size = 0L
        var range = false
        var ct = ""
        var name = resolved.substringAfterLast('/').substringBefore('?').ifBlank { "download.bin" }
        var speedHint = ""

        if (resolved.startsWith("http", true)) {
            try {
                val req = Request.Builder().url(resolved).head()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 12)")
                    .build()
                client.newCall(req).execute().use { r ->
                    size = r.header("Content-Length")?.toLongOrNull() ?: 0L
                    range = (r.header("Accept-Ranges") ?: "").lowercase().contains("bytes")
                    ct = r.header("Content-Type") ?: ""
                    r.header("Content-Disposition")?.let { cd ->
                        val m = Regex("""filename\*?=(?:UTF-8'')?"?([^";]+)"?""").find(cd)
                        m?.groupValues?.get(1)?.let { name = it.trim().trim('"') }
                    }
                }
            } catch (_: Throwable) {}

            // 小测速：并发请求 2 个 Range，测响应时间
            try {
                val t0 = System.currentTimeMillis()
                val r1 = client.newCall(
                    Request.Builder().url(resolved)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 12)")
                        .header("Range", "bytes=0-1023").build()
                ).execute()
                val dt = max(1L, System.currentTimeMillis() - t0)
                r1.close()
                speedHint = when {
                    dt < 250 -> "该服务器响应很快"
                    dt < 800 -> "服务器响应正常"
                    else -> "该服务器响应偏慢，已启用多线程加速"
                }
            } catch (_: Throwable) {}
        }

        // 决定最佳策略
        val (best, alts, note) = pick(kind, size, range, ct, input)

        AnalyzeResult(
            original = input,
            resolved = resolved,
            kind = kind,
            size = size,
            supportsRange = range,
            contentType = ct,
            fileName = name,
            best = best,
            alternatives = alts,
            speedHint = speedHint,
            note = note
        )
    }

    private fun detectKind(s: String): LinkKind = when {
        s.startsWith("thunder://") -> LinkKind.THUNDER
        s.startsWith("magnet:") -> LinkKind.MAGNET
        s.contains(".m3u8", true) -> LinkKind.HLS
        s.contains(".mpd", true) -> LinkKind.DASH
        NetdiskResolver.isNetdiskLink(s) -> LinkKind.NETDISK
        s.startsWith("http", true) -> LinkKind.HTTP
        else -> LinkKind.UNKNOWN
    }

    private data class Pick(
        val best: DownloadStrategy,
        val alternatives: List<DownloadStrategy>,
        val note: String
    )

    private fun pick(
        kind: LinkKind, size: Long, range: Boolean, ct: String, original: String
    ): Pick {
        // HLS / DASH 直接指定
        if (kind == LinkKind.HLS) {
            return Pick(DownloadStrategy.HLS, listOf(DownloadStrategy.T8, DownloadStrategy.SINGLE), "识别为 HLS 分片流")
        }
        if (kind == LinkKind.DASH) {
            return Pick(DownloadStrategy.DASH, listOf(DownloadStrategy.T8, DownloadStrategy.SINGLE), "识别为 DASH 分片流")
        }
        if (kind == LinkKind.MAGNET) {
            return Pick(DownloadStrategy.T32, listOf(DownloadStrategy.T16, DownloadStrategy.SINGLE), "磁力先用多线程拉取种子")
        }
        if (kind == LinkKind.NETDISK) {
            return Pick(DownloadStrategy.T8, listOf(DownloadStrategy.T16, DownloadStrategy.SINGLE), "网盘链接尝试直连")
        }

        // HTTP 场景
        if (size <= 0 || !range) {
            return Pick(DownloadStrategy.SINGLE, listOf(DownloadStrategy.T4, DownloadStrategy.STEALTH), "服务端不支持分段，走单线程")
        }
        return when {
            size < 2L * 1024 * 1024 ->
                Pick(DownloadStrategy.T4, listOf(DownloadStrategy.T8, DownloadStrategy.SINGLE), "小文件，4 线程足够")
            size < 20L * 1024 * 1024 ->
                Pick(DownloadStrategy.T8, listOf(DownloadStrategy.T16, DownloadStrategy.RESUME), "中等文件，8 线程均衡")
            size < 200L * 1024 * 1024 ->
                Pick(DownloadStrategy.T16, listOf(DownloadStrategy.T32, DownloadStrategy.RESUME), "大文件，16 线程加速")
            else ->
                Pick(DownloadStrategy.T32, listOf(DownloadStrategy.T16, DownloadStrategy.MIRROR), "超大文件，32 线程跑满带宽")
        }
    }
}

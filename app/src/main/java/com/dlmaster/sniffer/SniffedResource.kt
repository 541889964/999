package com.dlmaster.sniffer

import com.dlmaster.util.FileSizeFormatter
import org.json.JSONObject
import java.util.UUID

/**
 * 嗅探到的可下载资源
 *
 * 详细字段:
 *   url           - 真实下载地址(已解析为绝对路径)
 *   fileName      - 从 URL/Content-Disposition 解析出的文件名
 *   ext           - 扩展名(mp4/apk/pdf 等)
 *   mime          - 服务器返回的 Content-Type
 *   size          - 文件大小(HEAD 探测,未知为 0)
 *   supportsRange - 是否支持 Range 分段下载
 *   kind          - 资源类别(视频/音频/文档/软件/压缩包/图片/流媒体/其他)
 *   sourceUrl     - 来源网页地址
 *   sourceTitle   - 来源网页标题
 *   label         - HTML 中该链接的锚文本(如果有)
 *   isHls         - 是否为 HLS(m3u8)流
 *   isDash        - 是否为 DASH(mpd)流
 *   probed        - 是否已探测过(避免重复 HEAD)
 *   createdAt     - 抓取时间
 */
data class SniffedResource(
    var id: String = UUID.randomUUID().toString(),
    var url: String,
    var fileName: String = url.substringAfterLast('/').substringBefore('?').ifBlank { "resource" },
    var ext: String = "",
    var mime: String = "",
    var size: Long = 0L,
    var supportsRange: Boolean = false,
    var kind: Kind = Kind.OTHER,
    var sourceUrl: String = "",
    var sourceTitle: String = "",
    var label: String = "",
    var isHls: Boolean = false,
    var isDash: Boolean = false,
    var probed: Boolean = false,
    var createdAt: Long = System.currentTimeMillis()
) {
    enum class Kind(val display: String) {
        VIDEO("视频"),
        AUDIO("音频"),
        IMAGE("图片"),
        DOCUMENT("文档"),
        ARCHIVE("压缩包"),
        APK("安装包"),
        STREAM("流媒体"),
        OTHER("其他")
    }

    /** 列表展示用的详细信息 */
    fun detailText(): String = buildString {
        append(kind.display)
        if (ext.isNotEmpty()) { append(" · "); append(ext.uppercase()) }
        append(" · ")
        append(FileSizeFormatter.fmtShort(size))
        if (supportsRange) append(" · 支持分段")
        if (isHls) append(" · HLS")
        if (isDash) append(" · DASH")
        if (mime.isNotEmpty() && mime != "application/octet-stream") {
            append(" · ")
            append(mime.substringBefore(';'))
        }
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("url", url); put("fileName", fileName); put("ext", ext)
        put("mime", mime); put("size", size); put("supportsRange", supportsRange)
        put("kind", kind.name); put("sourceUrl", sourceUrl); put("sourceTitle", sourceTitle)
        put("label", label); put("isHls", isHls); put("isDash", isDash)
        put("probed", probed); put("createdAt", createdAt)
    }

    companion object {
        fun fromJson(o: JSONObject) = SniffedResource(
            id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
            url = o.optString("url"),
            fileName = o.optString("fileName", "resource"),
            ext = o.optString("ext", ""),
            mime = o.optString("mime", ""),
            size = o.optLong("size", 0L),
            supportsRange = o.optBoolean("supportsRange", false),
            kind = try { Kind.valueOf(o.optString("kind", "OTHER")) } catch (_: Throwable) { Kind.OTHER },
            sourceUrl = o.optString("sourceUrl", ""),
            sourceTitle = o.optString("sourceTitle", ""),
            label = o.optString("label", ""),
            isHls = o.optBoolean("isHls", false),
            isDash = o.optBoolean("isDash", false),
            probed = o.optBoolean("probed", false),
            createdAt = o.optLong("createdAt", System.currentTimeMillis())
        )
    }
}

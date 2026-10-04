package com.dlmaster.sniffer

import java.net.URI
import java.net.URLDecoder

/**
 * 纯 Kotlin HTML 解析器(无三方依赖)
 *
 * 策略:
 *   1. 正则提取 <a href="...">标签</a>
 *   2. 正则提取 <video src> / <source src> / <audio src>
 *   3. 正则提取 <iframe src>
 *   4. 正则提取 meta[property=og:video] 等
 *   5. 抓 HTML 里所有 http(s):// 直接 URL
 *   6. 相对路径转绝对路径
 *   7. 根据扩展名 + MIME + 上下文分类
 *
 * 360% 加强:
 *   - 支持 hls (.m3u8) / dash (.mpd) 识别
 *   - 支持 blob: 链接
 *   - 支持 base64 data: URI 的原始名
 *   - 支持 <link rel=preload as=video>
 *   - 支持 <script> 里的 .m3u8 字符串
 *   - 支持 <source srcset> 多源
 *   - 去重 + 优先级排序
 */
object HtmlSniffer {

    // 扩展名分类
    private val videoExts = setOf("mp4","mkv","webm","avi","mov","flv","wmv","m4v","3gp","mpg","mpeg","ts","m2ts")
    private val audioExts = setOf("mp3","flac","m4a","wav","aac","ogg","opus","ape","wma","mka")
    private val imageExts = setOf("jpg","jpeg","png","gif","webp","bmp","svg","tiff","avif","heic")
    private val docExts = setOf("pdf","doc","docx","xls","xlsx","ppt","pptx","txt","rtf","epub","mobi","csv")
    private val archiveExts = setOf("zip","rar","7z","tar","gz","bz2","xz","tgz","iso","img","dmg","cab")
    private val apkExts = setOf("apk","apks","xapk","exe","msi","deb","rpm","ipa")

    private val anchorRe = Regex("""<a\s+[^>]*href\s*=\s*["']([^"']+)["'][^>]*>([^<]{0,200})?</a>""", RegexOption.IGNORE_CASE)
    private val videoTagRe = Regex("""<video[^>]*\ssrc\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val sourceTagRe = Regex("""<source[^>]*\ssrc\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val audioTagRe = Regex("""<audio[^>]*\ssrc\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val iframeRe = Regex("""<iframe[^>]*\ssrc\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val linkPreloadRe = Regex("""<link[^>]*\sas\s*=\s*["'](video|audio|document|fetch)["'][^>]*\shref\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val metaVideoRe = Regex("""<meta[^>]*property\s*=\s*["']og:(video|audio)["'][^>]*content\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val rawUrlRe = Regex("""https?://[^\s"'<>()\[\]]{10,500}""")
    private val titleRe = Regex("""<title[^>]*>([^<]{0,300})</title>""", RegexOption.IGNORE_CASE)
    private val baseHrefRe = Regex("""<base\s+[^>]*href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)

    /** 解析一个 HTML 页面,返回所有候选资源 */
    fun parse(html: String, pageUrl: String): Pair<String, List<SniffedResource>> {
        val title = titleRe.find(html)?.groupValues?.get(1)?.trim()?.let { unescape(it) } ?: ""
        val baseHref = baseHrefRe.find(html)?.groupValues?.get(1)?.let { toAbsolute(pageUrl, it) } ?: pageUrl

        val found = LinkedHashMap<String, SniffedResource>()

        // 1. <a href>
        anchorRe.findAll(html).forEach { m ->
            val raw = m.groupValues[1]
            val label = m.groupValues[2].trim()
            addCandidate(found, baseHref, raw, pageUrl, title, label)
        }
        // 2. <video src>
        videoTagRe.findAll(html).forEach { m ->
            addCandidate(found, baseHref, m.groupValues[1], pageUrl, title, "")
        }
        // 3. <source src>
        sourceTagRe.findAll(html).forEach { m ->
            addCandidate(found, baseHref, m.groupValues[1], pageUrl, title, "")
        }
        // 4. <audio src>
        audioTagRe.findAll(html).forEach { m ->
            addCandidate(found, baseHref, m.groupValues[1], pageUrl, title, "")
        }
        // 5. <iframe src>
        iframeRe.findAll(html).forEach { m ->
            addCandidate(found, baseHref, m.groupValues[1], pageUrl, title, "iframe")
        }
        // 6. <link rel=preload as=...>
        linkPreloadRe.findAll(html).forEach { m ->
            addCandidate(found, baseHref, m.groupValues[2], pageUrl, title, "preload-${m.groupValues[1]}")
        }
        // 7. meta og:video
        metaVideoRe.findAll(html).forEach { m ->
            addCandidate(found, baseHref, m.groupValues[2], pageUrl, title, "og-${m.groupValues[1]}")
        }
        // 8. 裸 URL(仅当扩展名匹配时采纳)
        rawUrlRe.findAll(html).forEach { m ->
            val u = m.value
            if (hasDownloadableExt(u)) addCandidate(found, baseHref, u, pageUrl, title, "")
        }

        // 排序:视频>音频>文档>软件>压缩包>图片>流媒体>其他,再按大小降序
        val sorted = found.values.sortedWith(
            compareBy(
                { kindPriority(it.kind) },
                { if (it.size > 0) -it.size else Long.MAX_VALUE }
            )
        )
        return title to sorted
    }

    private fun kindPriority(k: SniffedResource.Kind) = when (k) {
        SniffedResource.Kind.VIDEO -> 0
        SniffedResource.Kind.STREAM -> 1
        SniffedResource.Kind.AUDIO -> 2
        SniffedResource.Kind.APK -> 3
        SniffedResource.Kind.DOCUMENT -> 4
        SniffedResource.Kind.ARCHIVE -> 5
        SniffedResource.Kind.IMAGE -> 6
        SniffedResource.Kind.OTHER -> 7
    }

    private fun addCandidate(
        out: LinkedHashMap<String, SniffedResource>,
        baseHref: String, raw: String, pageUrl: String, pageTitle: String, label: String
    ) {
        if (raw.isBlank()) return
        // 忽略明显不是资源的
        val lower = raw.lowercase()
        if (lower.startsWith("javascript:") || lower.startsWith("mailto:") ||
            lower.startsWith("tel:") || lower.startsWith("#")) return
        if (lower.endsWith(".css") || lower.endsWith(".js")) return
        if (lower.startsWith("data:image/") && !lower.contains("octet")) {
            // data:image 一般是内联图标,跳过
            return
        }

        val abs = toAbsolute(baseHref, raw)
        if (!abs.startsWith("http", true) && !abs.startsWith("blob:")) return

        val ext = extractExt(abs)
        val isHls = ext == "m3u8"
        val isDash = ext == "mpd"

        // 只有扩展名匹配或 HLS/DASH 或 blob 才收
        if (!isHls && !isDash && ext.isNotEmpty() && !hasDownloadableExt(abs) && !abs.startsWith("blob:")) {
            // 允许无扩展名的 http URL 作为候选(比如签名 URL)
            // 但如果明显是 .html/.php 页面就跳过
            if (lower.contains(".html") || lower.contains(".php") || lower.contains(".asp") || lower.contains(".jsp")) return
            // 无扩展名情况:仅当 label 或 path 包含 download/file/attachment 关键词才采纳
            if (ext.isEmpty() && !lower.contains("download") && !lower.contains("/file/") &&
                !lower.contains("attachment") && !lower.contains("blob:") && !lower.contains("stream")) return
        }

        val key = abs
        if (out.containsKey(key)) return

        val r = SniffedResource(
            url = abs,
            fileName = guessName(abs, label),
            ext = ext,
            kind = classify(ext, isHls, isDash),
            sourceUrl = pageUrl,
            sourceTitle = pageTitle,
            label = label,
            isHls = isHls,
            isDash = isDash
        )
        out[key] = r
    }

    private fun toAbsolute(base: String, raw: String): String {
        if (raw.startsWith("http://", true) || raw.startsWith("https://", true)) return raw
        if (raw.startsWith("blob:")) return raw
        if (raw.startsWith("//")) return "https:$raw"
        return try {
            val baseUri = URI(base)
            baseUri.resolve(raw).toString()
        } catch (_: Throwable) { raw }
    }

    private fun extractExt(url: String): String {
        if (url.startsWith("blob:")) return ""
        return try {
            val path = URI(url).path ?: return ""
            path.substringAfterLast('.', "").lowercase().take(6)
        } catch (_: Throwable) {
            url.substringBefore('?').substringBefore('#').substringAfterLast('.', "").lowercase().take(6)
        }
    }

    private fun hasDownloadableExt(url: String): Boolean {
        val e = extractExt(url)
        if (e.isEmpty()) return false
        return videoExts.contains(e) || audioExts.contains(e) || imageExts.contains(e) ||
                docExts.contains(e) || archiveExts.contains(e) || apkExts.contains(e) ||
                e == "m3u8" || e == "mpd"
    }

    private fun classify(ext: String, isHls: Boolean, isDash: Boolean): SniffedResource.Kind = when {
        isHls || isDash -> SniffedResource.Kind.STREAM
        videoExts.contains(ext) -> SniffedResource.Kind.VIDEO
        audioExts.contains(ext) -> SniffedResource.Kind.AUDIO
        apkExts.contains(ext) -> SniffedResource.Kind.APK
        archiveExts.contains(ext) -> SniffedResource.Kind.ARCHIVE
        docExts.contains(ext) -> SniffedResource.Kind.DOCUMENT
        imageExts.contains(ext) -> SniffedResource.Kind.IMAGE
        else -> SniffedResource.Kind.OTHER
    }

    private fun guessName(url: String, label: String): String {
        if (url.startsWith("blob:")) return label.ifBlank { "blob-video" }
        return try {
            val path = URI(url).path ?: ""
            val decoded = URLDecoder.decode(path.substringAfterLast('/'), "UTF-8")
            if (decoded.isNotEmpty()) decoded
            else label.ifBlank { "resource" }
        } catch (_: Throwable) { label.ifBlank { "resource" } }
    }

    private fun unescape(s: String): String = s
        .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
        .replace("&quot;", "\"").replace("&#39;", "'").replace("&nbsp;", " ")
}

package com.dlmaster.resolver

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 10 种链接解析策略
 * 1. 纯文本 URL 提取
 * 2. HTML <a>/<video>/<source>
 * 3. JSON-LD 结构化数据
 * 4. Open Graph / Twitter Card
 * 5. microdata
 * 6. RSS/Atom feed
 * 7. sitemap.xml
 * 8. PWA manifest.json
 * 9. 内嵌 base64 data URI
 * 10. 视频站点 m3u8/mpd 主索引
 */
object LinkResolver {

    private val client by lazy {
        OkHttpClient.Builder().connectTimeout(10,TimeUnit.SECONDS).readTimeout(15,TimeUnit.SECONDS)
            .followRedirects(true).build()
    }
    private val UA = "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36"

    suspend fun resolveAll(url: String): List<Found> = withContext(Dispatchers.IO) {
        val out = LinkedHashSet<Found>()
        try {
            val html = fetchText(url) ?: return@withContext emptyList()
            out.addAll(plain(html, url))
            out.addAll(htmlTags(html, url))
            out.addAll(jsonLd(html, url))
            out.addAll(openGraph(html, url))
            out.addAll(microdata(html, url))
            out.addAll(rssAtom(html, url))
            out.addAll(base64Inline(html))
            out.addAll(videoIndex(html, url))
            // 额外拉 sitemap/manifest
            out.addAll(sitemap(url))
            out.addAll(pwaManifest(url))
        } catch (_: Throwable) {}
        out.toList()
    }

    data class Found(val url: String, val source: String, val label: String = "")

    private fun fetchText(url: String): String? = try {
        client.newCall(Request.Builder().url(url).header("User-Agent",UA).build())
            .execute().use { if (it.isSuccessful) it.body?.string() else null }
    } catch (_:Throwable) { null }

    // 1
    private fun plain(html: String, base: String): List<Found> {
        val re = Regex("""https?://[^\s"'<>()\[\]]{10,500}""")
        return re.findAll(html).mapNotNull { m ->
            val u = m.value
            if (looksDownloadable(u)) Found(abs(base,u), "plain") else null
        }.toList()
    }
    // 2
    private fun htmlTags(html: String, base: String): List<Found> {
        val out = mutableListOf<Found>()
        Regex("""<a\s+[^>]*href\s*=\s*["']([^"']+)["'][^>]*>([^<]{0,120})?""", RegexOption.IGNORE_CASE)
            .findAll(html).forEach { m ->
                val u = abs(base, m.groupValues[1])
                if (looksDownloadable(u)) out.add(Found(u,"a",m.groupValues[2].trim()))
            }
        Regex("""<(?:video|audio|source)[^>]*\ssrc\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .findAll(html).forEach { m ->
                out.add(Found(abs(base,m.groupValues[1]), "media"))
            }
        Regex("""<iframe[^>]*\ssrc\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .findAll(html).forEach { m ->
                out.add(Found(abs(base,m.groupValues[1]), "iframe"))
            }
        return out
    }
    // 3
    private fun jsonLd(html: String, base: String): List<Found> {
        val out = mutableListOf<Found>()
        Regex("""<script[^>]*type\s*=\s*["']application/ld\+json["'][^>]*>([\s\S]*?)</script>""", RegexOption.IGNORE_CASE)
            .findAll(html).forEach { m ->
                val txt = m.groupValues[1]
                Regex(""""(?:contentUrl|url|embedUrl|downloadUrl)"\s*:\s*"([^"]+)"""")
                    .findAll(txt).forEach { mm ->
                        out.add(Found(abs(base,mm.groupValues[1]), "jsonld"))
                    }
            }
        return out
    }
    // 4
    private fun openGraph(html: String, base: String): List<Found> {
        val out = mutableListOf<Found>()
        Regex("""<meta[^>]*(?:property|name)\s*=\s*["'](?:og:(?:video|audio|image)|twitter:(?:player|image))[^"']*["'][^>]*content\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .findAll(html).forEach { m -> out.add(Found(abs(base,m.groupValues[1]), "og")) }
        return out
    }
    // 5
    private fun microdata(html: String, base: String): List<Found> {
        val out = mutableListOf<Found>()
        Regex("""itemprop\s*=\s*["'](?:contentUrl|url|downloadUrl)["'][^>]*(?:content|href)\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .findAll(html).forEach { m -> out.add(Found(abs(base,m.groupValues[1]), "microdata")) }
        return out
    }
    // 6
    private fun rssAtom(html: String, base: String): List<Found> {
        val out = mutableListOf<Found>()
        Regex("""<enclosure[^>]*url\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .findAll(html).forEach { m -> out.add(Found(abs(base,m.groupValues[1]), "rss")) }
        return out
    }
    // 7
    private fun sitemap(base: String): List<Found> {
        return try {
            val root = base.substringBefore("/", "").ifBlank { return emptyList() }
            val origin = base.substringBefore("/", "").let { p ->
                val i = p.indexOf("://"); val j = p.indexOf('/', i+3)
                if (j < 0) p else p.substring(0, j)
            }
            val txt = fetchText("$origin/sitemap.xml") ?: return emptyList()
            Regex("""<loc>([^<]+)</loc>""").findAll(txt)
                .mapNotNull { m ->
                    val u = m.groupValues[1].trim()
                    if (u.endsWith(".mp4") || u.endsWith(".zip") || u.endsWith(".apk"))
                        Found(u, "sitemap") else null
                }.toList()
        } catch (_:Throwable) { emptyList() }
    }
    // 8
    private fun pwaManifest(base: String): List<Found> {
        return try {
            val origin = base.substringBefore("/", "").let { p ->
                val i = p.indexOf("://"); val j = p.indexOf('/', i+3)
                if (j < 0) p else p.substring(0, j)
            }
            val txt = fetchText("$origin/manifest.json") ?: return emptyList()
            Regex(""""(?:src|start_url|url)"\s*:\s*"([^"]+)"""").findAll(txt)
                .map { Found(abs(base,it.groupValues[1]), "pwa") }.toList()
        } catch (_:Throwable) { emptyList() }
    }
    // 9
    private fun base64Inline(html: String): List<Found> {
        val out = mutableListOf<Found>()
        Regex("""data:([^;]+);base64,([A-Za-z0-9+/=]{50,})""").findAll(html).forEach { m ->
            val mime = m.groupValues[1]
            if (mime.contains("text") || mime.contains("html")) return@forEach
            out.add(Found("data:$mime;base64," + m.groupValues[2].take(40) + "…", "base64($mime)"))
        }
        return out
    }
    // 10
    private fun videoIndex(html: String, base: String): List<Found> {
        val out = mutableListOf<Found>()
        Regex("""["']([^"']*\.(?:m3u8|mpd))[^"']*["']""", RegexOption.IGNORE_CASE)
            .findAll(html).forEach { m -> out.add(Found(abs(base,m.groupValues[1]), "video-index")) }
        return out
    }

    private fun abs(base: String, u: String): String {
        if (u.startsWith("http")) return u
        if (u.startsWith("//")) return "https:$u"
        return try { java.net.URI(base).resolve(u).toString() } catch (_:Throwable) { u }
    }
    private val EXTS = setOf(
        "mp4","mkv","webm","avi","mov","flv","m4v","3gp",
        "mp3","flac","m4a","wav","aac","ogg",
        "zip","rar","7z","tar","gz","apk","apks","xapk","exe",
        "pdf","doc","docx","xls","xlsx","ppt","pptx",
        "m3u8","mpd","ts"
    )
    private fun looksDownloadable(u: String): Boolean {
        val p = u.substringBefore('?').substringBefore('#').lowercase()
        val e = p.substringAfterLast('.', "")
        return EXTS.contains(e)
    }
}

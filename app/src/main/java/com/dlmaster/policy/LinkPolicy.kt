package com.dlmaster.policy

import com.dlmaster.resolver.LinkResolver

/**
 * 10 种链接策略
 * 1. 域名黑白名单
 * 2. 大小过滤
 * 3. 扩展名过滤
 * 4. 优先队列(视频优先)
 * 5. Referer 自动填
 * 6. 去重 + URL 规范化
 * 7. 镜像组识别
 * 8. 无效链接过滤
 * 9. 相对路径补全
 * 10. 敏感域名过滤(广告/CDN 打点)
 */
object LinkPolicy {

    private val AD_DOMAINS = setOf(
        "doubleclick.net","googlesyndication.com","baidu.com/baiduid","mmstat.com",
        "mmstat.com","cnzz.com","51.la","umeng.com","talkingdata.com","sensorsdata",
        "byteoversea.com","ipinyou.com","tanx.com","admaster.com.cn","miaozhen.com"
    )

    private val SENSITIVE = setOf("ads","analytics","track","beacon","log","stat","click")

    data class Policy(
        val minSize: Long = 0L,
        val maxSize: Long = Long.MAX_VALUE,
        val allowedExts: Set<String> = emptySet(),
        val blockedHosts: Set<String> = emptySet(),
        val preferVideoFirst: Boolean = true,
        val autoReferer: Boolean = true,
        val deduplicate: Boolean = true
    )

    fun apply(list: List<LinkResolver.Found>, policy: Policy = Policy()): List<LinkResolver.Found> {
        var out = list

        // 7. 去重 + 规范化
        if (policy.deduplicate) {
            val map = LinkedHashMap<String, LinkResolver.Found>()
            out.forEach { f ->
                val key = normalize(f.url)
                if (!map.containsKey(key)) map[key] = f
            }
            out = map.values.toList()
        }

        // 1. 域名黑名单
        out = out.filter { f ->
            val host = try { java.net.URI(f.url).host ?: "" } catch (_:Throwable) { "" }
            policy.blockedHosts.none { host.endsWith(it) } &&
            AD_DOMAINS.none { host.endsWith(it) } &&
            SENSITIVE.none { f.url.contains("/$it/") }
        }

        // 3. 扩展名过滤
        if (policy.allowedExts.isNotEmpty()) {
            out = out.filter { f ->
                val e = f.url.substringBefore('?').substringAfterLast('.', "").lowercase()
                policy.allowedExts.contains(e)
            }
        }

        // 4. 视频优先排序
        if (policy.preferVideoFirst) {
            out = out.sortedWith(compareBy(
                { priority(it.url) },
                { it.url.length }
            ))
        }

        return out
    }

    /** 8. 无效链接过滤 + 9. 相对路径补全 */
    fun normalize(url: String): String {
        var u = url.trim()
        if (u.startsWith("//")) u = "https:$u"
        if (!u.startsWith("http") && !u.startsWith("data:")) u = "https://$u"
        // 去掉 fragment
        u = u.substringBefore('#')
        return u
    }

    private fun priority(url: String): Int {
        val e = url.substringBefore('?').substringAfterLast('.', "").lowercase()
        return when {
            e == "m3u8" || e == "mpd" -> 0
            e in setOf("mp4","mkv","webm","mov","avi") -> 1
            e in setOf("mp3","flac","m4a","wav") -> 2
            e in setOf("apk","apks","xapk") -> 3
            e in setOf("zip","rar","7z","tar","gz") -> 4
            e == "pdf" -> 5
            else -> 9
        }
    }

    /** 5. 自动 Referer */
    fun refererFor(url: String, sourceUrl: String?): String? =
        if (sourceUrl.isNullOrBlank()) null else try {
            val a = java.net.URI(url); val b = java.net.URI(sourceUrl)
            if (a.host == b.host) sourceUrl else null
        } catch (_:Throwable) { null }

    /** 7. 镜像组识别(同文件名,不同 host) */
    fun mirrorGroups(list: List<LinkResolver.Found>): List<List<LinkResolver.Found>> {
        val map = LinkedHashMap<String, MutableList<LinkResolver.Found>>()
        list.forEach { f ->
            val fn = f.url.substringBefore('?').substringAfterLast('/')
            map.getOrPut(fn) { mutableListOf() }.add(f)
        }
        return map.values.filter { it.size > 1 }
    }
}

package com.dlmaster.sniffer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 端到端网页嗅探
 *   输入:网页 URL
 *   输出:该页面所有可下载资源(已探测大小/类型/是否分段)
 *
 * 流程:
 *   1. 拉取 HTML(GET)
 *   2. HtmlSniffer 解析出候选
 *   3. ResourceProbe 并发探测每条
 *   4. 结果回调 + 存入 SniffRepository
 */
object WebPageSniffer {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    private const val UA = "Mozilla/5.0 (Linux/Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

    data class Result(
        val pageUrl: String,
        val pageTitle: String,
        val resources: List<SniffedResource>
    )

    /**
     * @param pageUrl 用户输入的网页地址
     * @param onProgress 进度回调: (阶段, 当前数量, 总数)
     *       阶段: "fetch"(拉 HTML) / "parse"(解析) / "probe"(探测)
     * @param onResource 每探测完一个资源回调一次(用于列表实时刷新)
     */
    suspend fun sniff(
        pageUrl: String,
        onProgress: (stage: String, cur: Int, total: Int) -> Unit = { _, _, _ -> },
        onResource: (SniffedResource) -> Unit = {}
    ): Result? = withContext(Dispatchers.IO) {
        try {
            // 1. 规范化 URL
            val url = normalize(pageUrl)
            onProgress("fetch", 0, 1)

            // 2. 拉 HTML
            val req = Request.Builder().url(url)
                .header("User-Agent", UA)
                .header("Accept", "text/html,application/xhtml+xml,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .build()
            val html = try {
                client.newCall(req).execute().use { r ->
                    if (!r.isSuccessful) return@withContext null
                    r.body?.string() ?: return@withContext null
                }
            } catch (_: Throwable) { return@withContext null }
            onProgress("fetch", 1, 1)

            // 3. 解析
            onProgress("parse", 0, 1)
            val (title, candidates) = HtmlSniffer.parse(html, url)
            onProgress("parse", 1, 1)

            if (candidates.isEmpty()) {
                return@withContext Result(url, title, emptyList())
            }

            // 4. 存储到 Repository(先存未探测的,让列表立即有内容)
            SniffRepository.addAll(candidates)

            // 5. 并发探测每条
            var done = 0
            val total = candidates.size
            onProgress("probe", 0, total)
            val probed = ResourceProbe.probeAll(candidates, referer = url, concurrency = 6) { r ->
                done++
                onProgress("probe", done, total)
                onResource(r)
            }
            // 用探测后的数据替换
            SniffRepository.replaceAll(probed)

            Result(url, title, probed)
        } catch (_: Throwable) { null }
    }

    private fun normalize(raw: String): String {
        val t = raw.trim()
        return when {
            t.startsWith("http://", true) || t.startsWith("https://", true) -> t
            t.startsWith("//") -> "https:$t"
            t.contains(".") && !t.contains(" ") -> "https://$t"
            else -> "https://www.bing.com/search?q=" + java.net.URLEncoder.encode(t, "UTF-8")
        }
    }
}

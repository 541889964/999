package com.dlmaster.sniffer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.net.URLDecoder
import kotlin.coroutines.resume

/**
 * WebView 内核嗅探器
 *
 * 相比纯 HTTP 抓 HTML 的优势:
 *   1. 执行 JavaScript —— 能拿到动态生成的 URL
 *   2. 拦截所有网络请求 —— 包括 JS 触发的 .apk 下载
 *   3. 模拟点击"下载"按钮 —— 触发真实下载请求
 *   4. 等待页面完全加载
 *
 * 用法:
 *   val sniffer = WebViewSniffer(context)
 *   sniffer.load(url) { resources -> ... }
 */
class WebViewSniffer(private val context: Context) {

    private val intercepted = LinkedHashSet<String>()
    private val handler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private var onPageReady: ((List<SniffedResource>) -> Unit)? = null
    private var pageTitle = ""
    private var pageUrl = ""

    private val downloadExts = setOf(
        "mp4","mkv","webm","avi","mov","flv","wmv","m4v","3gp",
        "mp3","flac","m4a","wav","aac","ogg",
        "zip","rar","7z","tar","gz",
        "apk","apks","xapk","exe","msi","deb",
        "pdf","doc","docx","xls","xlsx","ppt","pptx",
        "m3u8","mpd","ts"
    )

    @SuppressLint("SetJavaScriptEnabled")
    fun load(url: String, onReady: (List<SniffedResource>) -> Unit) {
        onPageReady = onReady
        intercepted.clear()

        handler.post {
            try {
                val wv = WebView(context.applicationContext)
                webView = wv
                wv.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    userAgentString = "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    allowFileAccess = true
                }

                wv.addJavascriptInterface(object {
                    @JavascriptInterface
                    fun report(json: String) {
                        handler.post { parseJsResult(json) }
                    }
                }, "AndroidBridge")

                wv.webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?, req: WebResourceRequest?
                    ): WebResourceResponse? {
                        val u = req?.url?.toString() ?: return null
                        if (looksDownloadable(u)) intercepted.add(u)
                        return null
                    }

                    override fun onPageStarted(view: WebView?, u: String?, fav: Bitmap?) {
                        pageUrl = u ?: ""
                    }

                    override fun onPageFinished(view: WebView?, u: String?) {
                        pageUrl = u ?: pageUrl
                        pageTitle = view?.title ?: ""
                        // 等 JS 执行完再抓
                        handler.postDelayed({
                            try {
                                // 1. 模拟点击"下载"按钮
                                view?.evaluateJavascript(simulateClickJs(), null)
                                // 2. 等 1.5 秒让下载请求触发
                                handler.postDelayed({
                                    try {
                                        // 3. 抓取所有链接 + 脚本里的 URL
                                        view?.evaluateJavascript(extractJs()) { result ->
                                            handler.post { parseJsResult(result) }
                                        }
                                    } catch (_: Throwable) {}
                                }, 1500)
                            } catch (_: Throwable) {}
                        }, 500)
                    }
                }

                wv.loadUrl(url)
            } catch (_: Throwable) {
                onReady(emptyList())
            }
        }
    }

    fun destroy() {
        handler.post {
            try {
                webView?.stopLoading()
                webView?.destroy()
            } catch (_: Throwable) {}
            webView = null
        }
    }

    private fun looksDownloadable(u: String): Boolean {
        if (u.isBlank()) return false
        val lower = u.lowercase()
        // 扩展名匹配
        val path = u.substringBefore('?').substringBefore('#').lowercase()
        val ext = path.substringAfterLast('.', "")
        if (ext.isNotEmpty() && downloadExts.contains(ext)) return true
        // 关键词匹配 (360 商店那种没扩展名的下载 URL)
        if (lower.contains("/download") || lower.contains("download/")) return true
        if (lower.contains("shouji.360tpcdn") || lower.contains("qq.com/") && lower.contains("apk")) return true
        if (lower.contains("/apk/") || lower.contains("/app/") && lower.contains("id=")) return true
        return false
    }

    private fun parseJsResult(json: String) {
        val list = mutableListOf<SniffedResource>()

        // 1. 网络拦截到的 URL
        intercepted.forEach { u ->
            list.add(makeResource(u, "", ""))
        }

        // 2. JS 抓到的 URL
        try {
            val cleaned = json.removeSurrounding("\"").replace("\\\"", "\"")
                .replace("\\\\", "\\")
            val obj = JSONObject(cleaned)
            val links = obj.optJSONArray("links")
            if (links != null) {
                for (i in 0 until links.length()) {
                    val o = links.optJSONObject(i) ?: continue
                    val u = o.optString("url", "")
                    val text = o.optString("text", "")
                    val tag = o.optString("tag", "")
                    if (u.isNotBlank() && looksDownloadable(u)) {
                        list.add(makeResource(u, text, tag))
                    }
                }
            }
            val urlsInScript = obj.optJSONArray("urlsInScript")
            if (urlsInScript != null) {
                for (i in 0 until urlsInScript.length()) {
                    val u = urlsInScript.optString(i, "")
                    if (u.isNotBlank() && looksDownloadable(u)) {
                        list.add(makeResource(u, "script", ""))
                    }
                }
            }
        } catch (_: Throwable) {}

        // 去重
        val map = LinkedHashMap<String, SniffedResource>()
        list.forEach { map[it.url] = it }
        val result = map.values.toList()

        handler.post {
            onPageReady?.invoke(result)
        }
    }

    private fun makeResource(url: String, label: String, tag: String): SniffedResource {
        val clean = url.substringBefore('#')
        val path = clean.substringBefore('?')
        val ext = path.substringAfterLast('.', "").lowercase()
        val name = try {
            val fn = path.substringAfterLast('/')
            if (fn.isNotEmpty()) URLDecoder.decode(fn, "UTF-8") else label.ifBlank { "resource" }
        } catch (_: Throwable) { label.ifBlank { "resource" } }

        return SniffedResource(
            url = url,
            fileName = name,
            ext = ext,
            kind = classify(ext),
            sourceUrl = pageUrl,
            sourceTitle = pageTitle,
            label = label,
            isHls = ext == "m3u8",
            isDash = ext == "mpd"
        )
    }

    private fun classify(ext: String): SniffedResource.Kind = when (ext) {
        "mp4","mkv","webm","avi","mov","flv","wmv","m4v","3gp" -> SniffedResource.Kind.VIDEO
        "mp3","flac","m4a","wav","aac","ogg" -> SniffedResource.Kind.AUDIO
        "jpg","jpeg","png","gif","webp","bmp" -> SniffedResource.Kind.IMAGE
        "pdf","doc","docx","xls","xlsx","ppt","pptx" -> SniffedResource.Kind.DOCUMENT
        "zip","rar","7z","tar","gz" -> SniffedResource.Kind.ARCHIVE
        "apk","apks","xapk","exe","msi","deb" -> SniffedResource.Kind.APK
        "m3u8","mpd","ts" -> SniffedResource.Kind.STREAM
        else -> SniffedResource.Kind.OTHER
    }

    /** JS: 模拟点击包含"下载/安装/立即"等字样的元素 */
    private fun simulateClickJs(): String = """
        (function() {
            try {
                var keywords = ['下载', '安装', '立即', '获取', 'Download', 'Install'];
                var clicked = [];
                document.querySelectorAll('a, button, [onclick], [data-href], .btn, .button').forEach(function(el) {
                    var txt = ((el.innerText || el.textContent || '') + '').trim();
                    for (var i = 0; i < keywords.length; i++) {
                        if (txt.indexOf(keywords[i]) >= 0 && txt.length < 30) {
                            try { el.click(); clicked.push(txt); } catch(e) {}
                            break;
                        }
                    }
                });
                return JSON.stringify(clicked);
            } catch(e) { return '[]'; }
        })();
    """.trimIndent()

    /** JS: 抓取页面所有可能的链接 */
    private fun extractJs(): String = """
        (function() {
            try {
                var links = [];
                // 1. <a href>
                document.querySelectorAll('a[href]').forEach(function(a) {
                    links.push({
                        url: a.href || '',
                        text: ((a.innerText || a.textContent || '') + '').trim().slice(0, 80),
                        tag: 'a'
                    });
                });
                // 2. video/audio/source
                document.querySelectorAll('video, audio').forEach(function(v) {
                    if (v.src) links.push({ url: v.src, text: 'video', tag: 'video' });
                    var src = v.currentSrc || '';
                    if (src && src !== v.src) links.push({ url: src, text: 'video-src', tag: 'video' });
                    v.querySelectorAll('source').forEach(function(s) {
                        if (s.src) links.push({ url: s.src, text: 'source', tag: 'source' });
                    });
                });
                // 3. iframe
                document.querySelectorAll('iframe[src]').forEach(function(f) {
                    links.push({ url: f.src, text: 'iframe', tag: 'iframe' });
                });
                // 4. link[rel=preload]
                document.querySelectorAll('link[rel]').forEach(function(l) {
                    var rel = (l.getAttribute('rel') || '').toLowerCase();
                    if (rel === 'preload' || rel === 'prefetch') {
                        links.push({ url: l.href, text: rel, tag: 'link' });
                    }
                });
                // 5. 按钮上的 data-href / data-url / data-download
                document.querySelectorAll('[data-href],[data-url],[data-download],[data-src]').forEach(function(el) {
                    var u = el.getAttribute('data-href') || el.getAttribute('data-url')
                         || el.getAttribute('data-download') || el.getAttribute('data-src') || '';
                    if (u) links.push({ url: u, text: 'data-attr', tag: 'attr' });
                });

                // 6. 所有 script 里出现的 http(s) URL
                var urlsInScript = [];
                document.querySelectorAll('script').forEach(function(s) {
                    var txt = s.textContent || '';
                    var m = txt.match(/https?:\/\/[^\s"'<>()]{15,500}/g);
                    if (m) {
                        m.forEach(function(u) { urlsInScript.push(u); });
                    }
                });
                // 7. body 里的纯文本 URL
                var bodyText = document.body ? (document.body.innerText || '') : '';
                var bm = bodyText.match(/https?:\/\/[^\s"'<>()]{15,500}/g);
                if (bm) {
                    bm.forEach(function(u) { urlsInScript.push(u); });
                }

                return JSON.stringify({
                    links: links,
                    urlsInScript: urlsInScript.slice(0, 200),
                    pageTitle: document.title || '',
                    pageUrl: location.href || ''
                });
            } catch(e) {
                return JSON.stringify({ links: [], urlsInScript: [], pageTitle: '', pageUrl: '' });
            }
        })();
    """.trimIndent()
}

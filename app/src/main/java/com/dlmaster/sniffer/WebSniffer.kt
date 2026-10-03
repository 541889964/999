package com.dlmaster.sniffer
import android.webkit.WebView
import com.dlmaster.download.DownloadRepository

class WebSniffer(private val webView: WebView) {
    private val exts = setOf(
        "mp4","mkv","webm","avi","mov","flv","wmv","m4v","3gp",
        "mp3","flac","m4a","wav","aac","ogg","ape",
        "zip","rar","7z","tar","gz","bz2","xz",
        "apk","exe","msi","dmg","iso","img",
        "pdf","doc","docx","xls","xlsx","ppt","pptx",
        "m3u8","mpd","ts","m4s"
    )
    fun isDownloadableUrl(url: String): Boolean {
        if (url.isBlank()) return false
        val path = url.substringBefore('?').substringBefore('#').lowercase()
        val ext = path.substringAfterLast('.', "")
        return exts.contains(ext)
    }
    fun scanDom(onResult: (List<String>) -> Unit) {
        try {
            webView.evaluateJavascript("""
                (function() {
                    var out = [];
                    document.querySelectorAll('a[href]').forEach(function(a){
                        var h = a.href || '';
                        if (h.startsWith('http') || h.startsWith('blob:')) out.push(h);
                    });
                    document.querySelectorAll('video, video source, audio, audio source').forEach(function(v){
                        var s = v.src || v.currentSrc || '';
                        if (s) out.push(s);
                    });
                    document.querySelectorAll('a[download]').forEach(function(a){
                        if (a.href) out.push(a.href);
                    });
                    return JSON.stringify(out);
                })();
            """.trimIndent()) { result ->
                val list = mutableListOf<String>()
                try {
                    val cleaned = result?.removeSurrounding("\"")?.replace("\\\"", "\"") ?: "[]"
                    Regex("\"(https?://[^\"]+|blob:[^\"]+)\"").findAll(cleaned).forEach {
                        val u = it.groupValues[1]
                        if (looksDownloadable(u)) list.add(u)
                    }
                } catch (_: Throwable) {}
                onResult(list)
            }
        } catch (_: Throwable) { onResult(emptyList()) }
    }
    private fun looksDownloadable(url: String): Boolean {
        if (url.startsWith("blob:")) return true
        val path = url.substringBefore('?').substringBefore('#').lowercase()
        val ext = path.substringAfterLast('.', "")
        return exts.contains(ext)
    }
}

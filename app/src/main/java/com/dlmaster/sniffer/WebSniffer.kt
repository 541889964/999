package com.dlmaster.sniffer
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
class WebSniffer(private val webView: WebView) {
    private val exts = setOf(".mp4",".mkv",".webm",".avi",".mov",".flv",".mp3",".flac",".m4a",".wav",".zip",".rar",".7z",".apk",".exe",".pdf",".m3u8",".mpd",".ts")
    fun interceptRequests() {
        try {
            webView.webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? = null
            }
        } catch (_: Throwable) {}
    }
    fun isDownloadLink(url: String): Boolean {
        val l = url.lowercase()
        return exts.any { l.contains(it) } || l.contains("download")
    }
}

package com.dlmaster.sniffer

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.dlmaster.download.DownloadRepository

class WebSniffer(private val webView: WebView) {

    private val mediaExtensions = setOf(
        ".mp4", ".mkv", ".webm", ".avi", ".mov", ".flv",
        ".mp3", ".flac", ".m4a", ".wav", ".aac",
        ".zip", ".rar", ".7z", ".apk", ".exe", ".iso",
        ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt",
        ".m3u8", ".mpd", ".ts"
    )

    fun interceptRequests() {
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?, request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url?.toString() ?: return null
                if (isDownloadLink(url)) DownloadRepository.addSniffedLink(url)
                return null
            }
        }
    }

    fun isDownloadLink(url: String): Boolean {
        val lower = url.lowercase()
        return mediaExtensions.any { lower.contains(it) } ||
                lower.contains("download") || lower.contains("file=")
    }
}

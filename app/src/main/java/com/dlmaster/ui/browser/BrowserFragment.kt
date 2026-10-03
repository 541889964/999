package com.dlmaster.ui.browser
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.fragment.app.Fragment
import com.dlmaster.R
import com.dlmaster.sniffer.WebSniffer
class BrowserFragment : Fragment() {
    private var webView: WebView? = null
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_browser, container, false)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        try {
            val wv = view.findViewById<WebView>(R.id.web_view)
            webView = wv
            wv.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                userAgentString = "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
            }
            WebSniffer(wv).interceptRequests()
            wv.loadUrl("https://www.baidu.com")
        } catch (_: Throwable) {}
    }
    override fun onDestroyView() {
        try { webView?.destroy() } catch (_: Throwable) {}
        webView = null
        super.onDestroyView()
    }
}

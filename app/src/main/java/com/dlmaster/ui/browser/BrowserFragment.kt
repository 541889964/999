package com.dlmaster.ui.browser
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.dlmaster.R
import com.dlmaster.sniffer.WebSniffer

class BrowserFragment : Fragment() {
    private var webView: WebView? = null
    private var etUrl: EditText? = null
    private var errLayout: FrameLayout? = null
    private var errMsg: TextView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View =
        inflater.inflate(R.layout.fragment_browser, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        etUrl = view.findViewById(R.id.et_web_url)
        errLayout = view.findViewById(R.id.web_error)
        errMsg = view.findViewById(R.id.tv_web_err_msg)
        val wv = view.findViewById<WebView>(R.id.web_view)
        webView = wv
        try {
            wv.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                useWideViewPort = true
                loadWithOverviewMode = true
                builtInZoomControls = true
                displayZoomControls = false
                userAgentString = "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
            }
            wv.webViewClient = object : WebViewClient() {
                override fun onPageStarted(v: WebView?, url: String?, favicon: Bitmap?) {
                    errLayout?.visibility = View.GONE
                    etUrl?.setText(url)
                }
                override fun onPageFinished(v: WebView?, url: String?) { etUrl?.setText(url) }
                override fun onReceivedError(v: WebView?, req: WebResourceRequest?, e: WebResourceError?) {
                    if (req?.isForMainFrame == true) {
                        errLayout?.visibility = View.VISIBLE
                        errMsg?.text = e?.description?.toString() ?: "未知错误"
                    }
                }
            }
            WebSniffer(wv).attach()
            wv.loadUrl("https://www.baidu.com")
        } catch (_: Throwable) {}
        view.findViewById<ImageButton>(R.id.btn_web_go).setOnClickListener { doGo() }
        etUrl?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) { doGo(); true } else false
        }
    }

    private fun doGo() {
        val raw = etUrl?.text?.toString()?.trim() ?: return
        if (raw.isEmpty()) return
        val url = when {
            raw.startsWith("http://") || raw.startsWith("https://") -> raw
            raw.contains(".") && !raw.contains(" ") -> "https://$raw"
            else -> "https://www.baidu.com/s?wd=${android.net.Uri.encode(raw)}"
        }
        try { webView?.loadUrl(url) } catch (_: Throwable) {}
    }

    override fun onDestroyView() {
        try { webView?.destroy() } catch (_: Throwable) {}
        webView = null
        super.onDestroyView()
    }
}

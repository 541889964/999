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
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.dlmaster.R
class BrowserFragment : Fragment() {
    private var webView: WebView? = null
    private var etUrl: EditText? = null
    private var errLayout: FrameLayout? = null
    private var errMsg: TextView? = null
    private var progressBar: ProgressBar? = null
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        i.inflate(R.layout.fragment_browser, c, false)
    override fun onViewCreated(v: View, s: Bundle?) {
        etUrl = v.findViewById(R.id.et_web_url)
        errLayout = v.findViewById(R.id.web_error)
        errMsg = v.findViewById(R.id.tv_web_err_msg)
        progressBar = v.findViewById(R.id.web_progress)
        val wv = v.findViewById<WebView>(R.id.web_view); webView = wv
        try {
            wv.settings.apply {
                javaScriptEnabled = true; domStorageEnabled = true; databaseEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                useWideViewPort = true; loadWithOverviewMode = true
                builtInZoomControls = true; displayZoomControls = false
                userAgentString = "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
            }
            wv.webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, f: Bitmap?) {
                    errLayout?.visibility = View.GONE
                    progressBar?.visibility = View.VISIBLE
                    etUrl?.setText(url)
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    progressBar?.visibility = View.GONE
                    etUrl?.setText(url)
                }
                override fun onReceivedError(view: WebView?, req: WebResourceRequest?, e: WebResourceError?) {
                    if (req?.isForMainFrame == true) {
                        progressBar?.visibility = View.GONE
                        errLayout?.visibility = View.VISIBLE
                        errMsg?.text = e?.description?.toString() ?: "未知错误"
                    }
                }
            }
            wv.loadUrl("https://www.bing.com")
        } catch (_: Throwable) {}

        v.findViewById<ImageButton>(R.id.btn_back).setOnClickListener { if (wv.canGoBack()) wv.goBack() }
        v.findViewById<ImageButton>(R.id.btn_forward).setOnClickListener { if (wv.canGoForward()) wv.goForward() }
        v.findViewById<ImageButton>(R.id.btn_refresh).setOnClickListener { wv.reload() }
        v.findViewById<View>(R.id.btn_web_retry)?.setOnClickListener { wv.reload(); errLayout?.visibility = View.GONE }
        etUrl?.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_GO) { doGo(); true } else false
        }
    }
    private fun doGo() {
        val raw = etUrl?.text?.toString()?.trim() ?: return
        if (raw.isEmpty()) return
        val url = when {
            raw.startsWith("http://") || raw.startsWith("https://") -> raw
            raw.contains(".") && !raw.contains(" ") -> "https://$raw"
            else -> "https://www.bing.com/search?q=${android.net.Uri.encode(raw)}"
        }
        try { webView?.loadUrl(url) } catch (_: Throwable) {}
    }
    override fun onDestroyView() {
        try { webView?.destroy() } catch (_: Throwable) {}
        webView = null; super.onDestroyView()
    }
}

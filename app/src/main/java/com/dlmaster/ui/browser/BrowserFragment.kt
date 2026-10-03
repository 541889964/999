package com.dlmaster.ui.browser
import android.annotation.SuppressLint
import android.app.AlertDialog
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dlmaster.MainActivity
import com.dlmaster.R
import com.dlmaster.download.DownloadRepository
import com.dlmaster.sniffer.WebSniffer
import com.dlmaster.ui.home.SniffAdapter
import com.dlmaster.util.CommandDownloader
import com.dlmaster.util.RvOptimizer
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar

class BrowserFragment : Fragment() {
    private var webView: WebView? = null
    private var etUrl: EditText? = null
    private var errLayout: FrameLayout? = null
    private var errMsg: TextView? = null
    private var progressBar: ProgressBar? = null
    private var sniffer: WebSniffer? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        i.inflate(R.layout.fragment_browser, c, false)

    override fun onViewCreated(v: View, s: Bundle?) {
        etUrl = v.findViewById(R.id.et_web_url)
        errLayout = v.findViewById(R.id.web_error)
        errMsg = v.findViewById(R.id.tv_web_err_msg)
        progressBar = v.findViewById(R.id.web_progress)
        val wv = v.findViewById<WebView>(R.id.web_view)
        webView = wv
        try {
            wv.settings.apply {
                javaScriptEnabled = true; domStorageEnabled = true; databaseEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                useWideViewPort = true; loadWithOverviewMode = true
                builtInZoomControls = true; displayZoomControls = false
                userAgentString = "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
            }
            val sn = WebSniffer(wv)
            sniffer = sn
            wv.webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, f: Bitmap?) {
                    errLayout?.visibility = View.GONE
                    progressBar?.visibility = View.VISIBLE
                    etUrl?.setText(url)
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    progressBar?.visibility = View.GONE
                    etUrl?.setText(url)
                    sn.scanDom { list -> list.forEach { DownloadRepository.addSniffed(it) } }
                }
                override fun onReceivedError(view: WebView?, req: WebResourceRequest?, e: WebResourceError?) {
                    if (req?.isForMainFrame == true) {
                        progressBar?.visibility = View.GONE
                        errLayout?.visibility = View.VISIBLE
                        errMsg?.text = e?.description?.toString() ?: "未知错误"
                    }
                }
                override fun shouldOverrideUrlLoading(view: WebView?, req: WebResourceRequest?): Boolean {
                    val url = req?.url?.toString() ?: return false
                    if (sn.isDownloadableUrl(url)) { promptDownload(url); return true }
                    return false
                }
            }
            wv.loadUrl("https://www.bing.com")
        } catch (_: Throwable) {}
        v.findViewById<ImageButton>(R.id.btn_back).setOnClickListener { if (wv.canGoBack()) wv.goBack() }
        v.findViewById<ImageButton>(R.id.btn_forward).setOnClickListener { if (wv.canGoForward()) wv.goForward() }
        v.findViewById<ImageButton>(R.id.btn_refresh).setOnClickListener { wv.reload() }
        v.findViewById<View>(R.id.btn_web_retry)?.setOnClickListener {
            wv.reload(); errLayout?.visibility = View.GONE
        }
        etUrl?.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_GO) { doGo(); true } else false
        }
        v.findViewById<MaterialButton>(R.id.btn_sniff).setOnClickListener {
            sniffer?.scanDom { list ->
                val merged = (DownloadRepository.sniffed.toList() + list).distinct()
                if (merged.isEmpty()) Snackbar.make(v, "本页没抓到可下载的资源", Snackbar.LENGTH_SHORT).show()
                else showSniffDialog(merged)
            }
        }
    }

    private fun promptDownload(url: String) {
        val dlg = AlertDialog.Builder(requireContext())
            .setTitle("发现下载").setMessage(url.take(120))
            .setPositiveButton("立刻下载") { _, _ ->
                CommandDownloader.sniffDownload(requireContext(), url)
                Snackbar.make(requireView(), "已开跑", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("稍后", null).create()
        dlg.show()
        try {
            dlg.window?.setBackgroundDrawableResource(android.R.color.transparent)
            val dm = resources.displayMetrics
            dlg.window?.setLayout((dm.widthPixels * 0.9).toInt(), -2)
        } catch (_: Throwable) {}
    }

    private fun showSniffDialog(list: List<String>) {
        val dlg = AlertDialog.Builder(requireContext()).create()
        val root = layoutInflater.inflate(R.layout.dialog_sniff, null)
        dlg.setView(root)
        root.findViewById<TextView>(R.id.tv_sniff_count).text = "共 ${list.size} 个资源，点一条直接开跑"
        val rv = root.findViewById<RecyclerView>(R.id.rv_sniff)
        RvOptimizer.config(rv, requireContext())
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = SniffAdapter(list) { url ->
            CommandDownloader.sniffDownload(requireContext(), url)
            dlg.dismiss()
            (activity as? MainActivity)
                ?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_nav)
                ?.selectedItemId = R.id.nav_download
        }
        root.findViewById<MaterialButton>(R.id.btn_sniff_close).setOnClickListener { dlg.dismiss() }
        dlg.show()
        try {
            dlg.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dlg.window?.setWindowAnimations(R.style.DialogAnim)
            val dm = resources.displayMetrics
            dlg.window?.setLayout((dm.widthPixels * 0.9).toInt(), -2)
        } catch (_: Throwable) {}
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

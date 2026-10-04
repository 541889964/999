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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dlmaster.R
import com.dlmaster.download.DownloadStrategy
import com.dlmaster.sniffer.SniffRepository
import com.dlmaster.sniffer.SniffedResource
import com.dlmaster.sniffer.WebPageSniffer
import com.dlmaster.ui.home.SniffAdapter
import com.dlmaster.util.CommandDownloader
import com.dlmaster.util.RvOptimizer
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
class BrowserFragment : Fragment() {
    private var webView: WebView? = null; private var etUrl: EditText? = null
    private var errLayout: FrameLayout? = null; private var errMsg: TextView? = null
    private var progressBar: ProgressBar? = null
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View = i.inflate(R.layout.fragment_browser, c, false)
    override fun onViewCreated(v: View, s: Bundle?) {
        etUrl = v.findViewById(R.id.et_web_url); errLayout = v.findViewById(R.id.web_error)
        errMsg = v.findViewById(R.id.tv_web_err_msg); progressBar = v.findViewById(R.id.web_progress)
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
                override fun onPageStarted(view: WebView?, url: String?, f: Bitmap?) { errLayout?.visibility = View.GONE; progressBar?.visibility = View.VISIBLE; etUrl?.setText(url) }
                override fun onPageFinished(view: WebView?, url: String?) { progressBar?.visibility = View.GONE; etUrl?.setText(url) }
                override fun onReceivedError(view: WebView?, req: WebResourceRequest?, e: WebResourceError?) { if (req?.isForMainFrame == true) { progressBar?.visibility = View.GONE; errLayout?.visibility = View.VISIBLE; errMsg?.text = e?.description?.toString() ?: "未知错误" } }
            }
            wv.loadUrl("https://www.bing.com")
        } catch (_: Throwable) {}
        v.findViewById<ImageButton>(R.id.btn_back).setOnClickListener { if (wv.canGoBack()) wv.goBack() }
        v.findViewById<ImageButton>(R.id.btn_forward).setOnClickListener { if (wv.canGoForward()) wv.goForward() }
        v.findViewById<ImageButton>(R.id.btn_refresh).setOnClickListener { wv.reload() }
        v.findViewById<View>(R.id.btn_web_retry)?.setOnClickListener { wv.reload(); errLayout?.visibility = View.GONE }
        etUrl?.setOnEditorActionListener { _, action, _ -> if (action == IME_GO()) { doGo(); true } else false }
        // 抓取本页:用 WebPageSniffer 拉取当前页 HTML 全量解析
        v.findViewById<MaterialButton>(R.id.btn_sniff).setOnClickListener {
            val cur = webView?.url ?: return@setOnClickListener
            scanCurrentPage(cur)
        }
    }
    private fun IME_GO() = EditorInfo.IME_ACTION_GO
    private fun scanCurrentPage(pageUrl: String) {
        val dlg = AlertDialog.Builder(requireContext()).setCancelable(false).create()
        val root = layoutInflater.inflate(R.layout.dialog_sniff, null)
        dlg.setView(root)
        val tvTitle = root.findViewById<TextView>(R.id.tv_sniff_title)
        val tvCount = root.findViewById<TextView>(R.id.tv_sniff_count)
        val progress = root.findViewById<ProgressBar>(R.id.sniff_progress)
        val rv = root.findViewById<RecyclerView>(R.id.rv_sniff)
        tvTitle.text = "正在扫描当前页…"; tvCount.text = "抓取 HTML…"
        RvOptimizer.config(rv, requireContext())
        rv.layoutManager = LinearLayoutManager(requireContext())
        val live = ArrayList<SniffedResource>()
        rv.adapter = SniffAdapter(live) { r -> pick(r); dlg.dismiss() }
        root.findViewById<View>(R.id.btn_sniff_clear)?.setOnClickListener { live.clear(); rv.adapter?.notifyDataSetChanged() }
        root.findViewById<View>(R.id.btn_sniff_close)?.setOnClickListener { dlg.dismiss() }
        dlg.show()
        try { dlg.window?.setBackgroundDrawableResource(android.R.color.transparent); dlg.window?.setWindowAnimations(R.style.DialogAnim); val dm = resources.displayMetrics; dlg.window?.setLayout((dm.widthPixels * 0.95).toInt(), -2) } catch (_: Throwable) {}
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            WebPageSniffer.sniff(pageUrl,
                onProgress = { stage, cur, total ->
                    if (stage == "probe") { progress.progress = if (total > 0) cur * 100 / total else 0; tvCount.text = "探测中 $cur/$total" }
                },
                onResource = { r -> live.add(r); rv.adapter?.notifyItemInserted(live.size - 1); tvTitle.text = "发现了这些可下载的 (${live.size})" }
            )
            tvTitle.text = if (live.isEmpty()) "没找到可下载的资源" else "发现了这些可下载的 (${live.size})"
            tvCount.text = if (live.isEmpty()) "试试换个页面" else getString(R.string.sniff_sub)
            progress.visibility = View.GONE
        }
    }
    private fun pick(r: SniffedResource) {
        val strategy = when {
            r.isHls -> DownloadStrategy.HLS
            r.isDash -> DownloadStrategy.DASH
            r.supportsRange && r.size > 20L * 1024 * 1024 -> DownloadStrategy.T16
            r.supportsRange -> DownloadStrategy.T8
            else -> DownloadStrategy.SINGLE
        }
        CommandDownloader.fromSniffed(requireContext(), r.url, r.fileName, r.sourceUrl, strategy)
        Snackbar.make(requireView(), "已开始下载 ${r.fileName}", Snackbar.LENGTH_SHORT).show()
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
    override fun onDestroyView() { try { webView?.destroy() } catch (_: Throwable) {}; webView = null; super.onDestroyView() }
}

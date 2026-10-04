package com.dlmaster.ui.home
import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dlmaster.MainActivity
import com.dlmaster.R
import com.dlmaster.anim.Anim
import com.dlmaster.download.AnalyzeResult
import com.dlmaster.download.DownloadRepository
import com.dlmaster.download.DownloadStrategy
import com.dlmaster.download.SmartAnalyzer
import com.dlmaster.sniffer.SniffRepository
import com.dlmaster.sniffer.SniffedResource
import com.dlmaster.sniffer.WebPageSniffer
import com.dlmaster.ui.download.DownloadAdapter
import com.dlmaster.util.CommandDownloader
import com.dlmaster.util.FileSizeFormatter
import com.dlmaster.util.MusicPlayer
import com.dlmaster.util.RvOptimizer
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.Calendar
class HomeFragment : Fragment() {
    private var taskAdapter: DownloadAdapter? = null
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        i.inflate(R.layout.fragment_home, c, false)
    override fun onViewCreated(view: View, s: Bundle?) {
        // 交错入场
        Anim.stagger(arrayOf(
            view.findViewById<View>(R.id.card_greet),
            view.findViewById<View>(R.id.card_actions),
            view.findViewById<View>(R.id.card_search),
            view.findViewById<View>(R.id.card_pagescan)
        ), 70L)
        // 问候语
        val tvGreet = view.findViewById<TextView>(R.id.tv_greet)
        val tvSub = view.findViewById<TextView>(R.id.tv_sub_greet)
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..10 -> { tvGreet.text = "早上好呀 ☀️"; tvSub.text = "新的一天，从下载开始" }
            in 11..13 -> { tvGreet.text = "中午好 🌤"; tvSub.text = "吃了吗，顺手拖个文件" }
            in 14..17 -> { tvGreet.text = "下午好 ☕"; tvSub.text = "慢慢来，一切都好" }
            in 18..22 -> { tvGreet.text = "晚上好呀 🌙"; tvSub.text = "休息一下，让工具干活" }
            else -> { tvGreet.text = "夜深了 🌌"; tvSub.text = "早点睡，下载不用你盯" }
        }
        val et = view.findViewById<EditText>(R.id.et_url)

        // 一键极速下载
        view.findViewById<MaterialButton>(R.id.btn_oneclick).setOnClickListener { v ->
            val t = et.text.toString().trim()
            if (t.isEmpty()) { Snackbar.make(view, "先粘个链接", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener }
            Anim.press(v); vibrate()
            val dlg = AlertDialog.Builder(requireContext()).setMessage("正在找最快的路…").setCancelable(false).create(); dlg.show()
            try { dlg.window?.setBackgroundDrawableResource(android.R.color.transparent); val dm = resources.displayMetrics; dlg.window?.setLayout((dm.widthPixels * 0.7).toInt(), -2) } catch (_: Throwable) {}
            lifecycleScope.launch {
                val r = try { SmartAnalyzer.analyze(t) } catch (_: Throwable) { null }
                if (dlg.isShowing) dlg.dismiss()
                if (r == null) Snackbar.make(view, "分析失败", Snackbar.LENGTH_SHORT).show()
                else { CommandDownloader.launchWith(requireContext(), r, r.best); et.setText(""); refresh(); Snackbar.make(view, "已按「${r.best.displayName}」开跑", Snackbar.LENGTH_SHORT).show() }
            }
        }

        // 先分析
        view.findViewById<MaterialButton>(R.id.btn_analyze).setOnClickListener { v ->
            val t = et.text.toString().trim()
            if (t.isEmpty()) { Snackbar.make(view, "先粘个链接", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener }
            Anim.press(v); vibrate()
            val dlg = AlertDialog.Builder(requireContext()).setMessage("分析中…").setCancelable(false).create(); dlg.show()
            try { dlg.window?.setBackgroundDrawableResource(android.R.color.transparent); val dm = resources.displayMetrics; dlg.window?.setLayout((dm.widthPixels * 0.7).toInt(), -2) } catch (_: Throwable) {}
            lifecycleScope.launch {
                val r = try { SmartAnalyzer.analyze(t) } catch (_: Throwable) { null }
                if (dlg.isShowing) dlg.dismiss()
                if (r == null) Snackbar.make(view, "分析失败", Snackbar.LENGTH_SHORT).show()
                else showAnalyzeResult(r)
            }
        }

        // 剪贴板
        view.findViewById<View>(R.id.btn_clip).setOnClickListener { v ->
            Anim.press(v); vibrate()
            val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = cm.primaryClip
            val txt = if (clip != null && clip.itemCount > 0) clip.getItemAt(0).coerceToText(requireContext()).toString() else ""
            if (txt.isNotEmpty()) { et.setText(txt); Snackbar.make(view, "已粘贴", Snackbar.LENGTH_SHORT).show() }
            else Snackbar.make(view, "剪贴板为空", Snackbar.LENGTH_SHORT).show()
        }

        // 已嗅探 → 展示缓存
        view.findViewById<View>(R.id.btn_sniff).setOnClickListener { v ->
            Anim.press(v); vibrate()
            val list = SniffRepository.items.toList()
            if (list.isEmpty()) { Snackbar.make(view, "还没扫过任何页面", Snackbar.LENGTH_SHORT).show() }
            else showSniffResult(list, "已嗅探的资源", null)
        }

        // 扫描网页
        view.findViewById<View>(R.id.btn_scan).setOnClickListener { v ->
            Anim.press(v); vibrate()
            val etPage = view.findViewById<EditText>(R.id.et_page_url)
            val url = etPage.text.toString().trim()
            if (url.isEmpty()) { Snackbar.make(view, "先粘网页地址", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener }
            startScan(url)
        }

        // 音乐
        val tvMusic = view.findViewById<TextView>(R.id.tv_music_name)
        val btnT = view.findViewById<ImageButton>(R.id.btn_music_toggle)
        val btnN = view.findViewById<ImageButton>(R.id.btn_music_next)
        fun rf() {
            tvMusic.text = MusicPlayer.currentTrackName() ?: getString(R.string.music_idle)
            btnT.setImageResource(if (MusicPlayer.isPlaying()) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
        }
        btnT.setOnClickListener { if (MusicPlayer.isPlaying()) MusicPlayer.pause() else MusicPlayer.resume(); rf() }
        btnN.setOnClickListener { MusicPlayer.next(requireContext()); rf() }
        rf()

        // 内嵌任务列表
        val rv = view.findViewById<RecyclerView>(R.id.rv_tasks)
        RvOptimizer.config(rv, requireContext())
        rv.layoutManager = LinearLayoutManager(requireContext())
        taskAdapter = DownloadAdapter(DownloadRepository.tasks)
        rv.adapter = taskAdapter
        DownloadRepository.live.observe(viewLifecycleOwner) { refresh() }
        refresh()
    }

    /** 扫描网页 */
    private fun startScan(pageUrl: String) {
        val dlg = AlertDialog.Builder(requireContext()).setCancelable(false).create()
        val v = layoutInflater.inflate(R.layout.dialog_sniff, null)
        dlg.setView(v)
        val tvTitle = v.findViewById<TextView>(R.id.tv_sniff_title)
        val tvCount = v.findViewById<TextView>(R.id.tv_sniff_count)
        val progress = v.findViewById<ProgressBar>(R.id.sniff_progress)
        val rv = v.findViewById<RecyclerView>(R.id.rv_sniff)
        tvTitle.text = "正在扫描网页…"
        tvCount.text = "抓取 HTML 中…"
        RvOptimizer.config(rv, requireContext())
        rv.layoutManager = LinearLayoutManager(requireContext())
        // 实时数据模型
        val live = ArrayList<SniffedResource>()
        rv.adapter = SniffAdapter(live) { r -> pickAndDownload(r) }
        v.findViewById<View>(R.id.btn_sniff_clear)?.setOnClickListener { live.clear(); rv.adapter?.notifyDataSetChanged() }
        v.findViewById<View>(R.id.btn_sniff_close)?.setOnClickListener { dlg.dismiss() }
        dlg.show()
        try { dlg.window?.setBackgroundDrawableResource(android.R.color.transparent); dlg.window?.setWindowAnimations(R.style.DialogAnim); val dm = resources.displayMetrics; dlg.window?.setLayout((dm.widthPixels * 0.95).toInt(), -2) } catch (_: Throwable) {}

        // 启动端到端嗅探
        lifecycleScope.launch {
            WebPageSniffer.sniff(
                pageUrl = pageUrl,
                onProgress = { stage, cur, total ->
                    when (stage) {
                        "fetch" -> tvCount.text = if (cur == 0) "正在拉取网页…" else "网页已到手"
                        "parse" -> tvCount.text = "正在解析…"
                        "probe" -> {
                            val pct = if (total > 0) (cur * 100 / total) else 0
                            progress.progress = pct
                            tvCount.text = "探测中 $cur/$total"
                        }
                    }
                },
                onResource = { r ->
                    // 每探测完一个就上列表(实时)
                    live.add(r)
                    rv.adapter?.notifyItemInserted(live.size - 1)
                    tvTitle.text = "发现了这些可下载的 (${live.size})"
                }
            )
            tvTitle.text = if (live.isEmpty()) "没找到可下载的资源" else "发现了这些可下载的 (${live.size})"
            tvCount.text = if (live.isEmpty()) "试试换个页面,或页面需要登录" else getString(R.string.sniff_sub)
            progress.visibility = View.GONE
        }
    }

    private fun pickAndDownload(r: SniffedResource) {
        // 从嗅探结果直接开跑
        val strategy = when {
            r.isHls -> DownloadStrategy.HLS
            r.isDash -> DownloadStrategy.DASH
            r.supportsRange && r.size > 20L * 1024 * 1024 -> DownloadStrategy.T16
            r.supportsRange -> DownloadStrategy.T8
            else -> DownloadStrategy.SINGLE
        }
        CommandDownloader.fromSniffed(requireContext(), r.url, r.fileName, r.sourceUrl, strategy)
        Snackbar.make(requireView(), "已开始下载 ${r.fileName}", Snackbar.LENGTH_SHORT).show()
        refresh()
    }

    private fun showSniffResult(list: List<SniffedResource>, title: String, progressBar: ProgressBar?) {
        val dlg = AlertDialog.Builder(requireContext()).create()
        val root = layoutInflater.inflate(R.layout.dialog_sniff, null)
        dlg.setView(root)
        root.findViewById<TextView>(R.id.tv_sniff_title).text = title
        root.findViewById<TextView>(R.id.tv_sniff_count).text = "共 ${list.size} 个 · 点一条直接开跑"
        progressBar?.visibility = View.GONE
        root.findViewById<ProgressBar>(R.id.sniff_progress).visibility = View.GONE
        val rv = root.findViewById<RecyclerView>(R.id.rv_sniff)
        RvOptimizer.config(rv, requireContext())
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = SniffAdapter(list) { r -> pickAndDownload(r); dlg.dismiss() }
        root.findViewById<View>(R.id.btn_sniff_clear)?.setOnClickListener {
            SniffRepository.clearAll(); dlg.dismiss(); Snackbar.make(requireView(), "已清空", Snackbar.LENGTH_SHORT).show()
        }
        root.findViewById<View>(R.id.btn_sniff_close)?.setOnClickListener { dlg.dismiss() }
        dlg.show()
        try { dlg.window?.setBackgroundDrawableResource(android.R.color.transparent); dlg.window?.setWindowAnimations(R.style.DialogAnim); val dm = resources.displayMetrics; dlg.window?.setLayout((dm.widthPixels * 0.95).toInt(), -2) } catch (_: Throwable) {}
    }

    private fun showAnalyzeResult(r: AnalyzeResult) {
        val dlg = AlertDialog.Builder(requireContext()).create()
        val root = layoutInflater.inflate(R.layout.dialog_analyze, null); dlg.setView(root)
        root.findViewById<TextView>(R.id.tv_dlg_title).text = "分析好了"
        root.findViewById<TextView>(R.id.tv_dlg_sub).text = "给你挑了条最快的路"
        root.findViewById<TextView>(R.id.tv_dlg_type).text = "链接类型：${r.kind.label}"
        root.findViewById<TextView>(R.id.tv_dlg_size).text = buildString {
            append("大小："); append(if (r.size > 0) FileSizeFormatter.fmt(r.size) else "未知")
            append("　·　"); append(if (r.supportsRange) "支持分段" else "不支持分段")
            if (r.speedHint.isNotEmpty()) { append("\n"); append(r.speedHint) }
            if (r.note.isNotEmpty()) { append("\n"); append(r.note) }
        }
        root.findViewById<TextView>(R.id.tv_dlg_best).text = "推荐「${r.best.displayName}」\n${r.best.blurb}"
        val choices = (listOf(r.best) + r.alternatives).distinctBy { it.key }
        var sel = r.best
        val rv = root.findViewById<RecyclerView>(R.id.rv_strategy)
        RvOptimizer.config(rv, requireContext())
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = StrategyAdapter(choices, r.best) { sel = it }
        root.findViewById<MaterialButton>(R.id.btn_cancel).setOnClickListener { dlg.dismiss() }
        root.findViewById<MaterialButton>(R.id.btn_start).setOnClickListener { dlg.dismiss(); CommandDownloader.launchWith(requireContext(), r, sel); refresh() }
        dlg.show()
        try { dlg.window?.setBackgroundDrawableResource(android.R.color.transparent); dlg.window?.setWindowAnimations(R.style.DialogAnim); val dm = resources.displayMetrics; dlg.window?.setLayout((dm.widthPixels * 0.9).toInt(), -2) } catch (_: Throwable) {}
    }

    private fun refresh() {
        val v = view ?: return
        taskAdapter?.notifyDataSetChanged()
        val has = DownloadRepository.tasks.isNotEmpty()
        v.findViewById<View>(R.id.rv_tasks)?.visibility = if (has) View.VISIBLE else View.GONE
        v.findViewById<View>(R.id.layout_empty)?.visibility = if (has) View.GONE else View.VISIBLE
    }

    private fun vibrate() {
        try {
            val v = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            if (android.os.Build.VERSION.SDK_INT >= 26) v.vibrate(android.os.VibrationEffect.createOneShot(12, 40))
            else { @Suppress("DEPRECATION") v.vibrate(12) }
        } catch (_: Throwable) {}
    }
}
class StrategyAdapter(private val items: List<DownloadStrategy>, def: DownloadStrategy, private val onSel: (DownloadStrategy) -> Unit) : RecyclerView.Adapter<StrategyAdapter.VH>() {
    private var selKey = def.key
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val box: android.widget.LinearLayout = v.findViewById(R.id.strategy_box)
        val name: TextView = v.findViewById(R.id.tv_s_name)
        val desc: TextView = v.findViewById(R.id.tv_s_desc)
    }
    override fun onCreateViewHolder(p: ViewGroup, t: Int): VH = VH(LayoutInflater.from(p.context).inflate(R.layout.item_strategy, p, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(h: VH, pos: Int) {
        val s = items[pos]
        h.name.text = s.displayName; h.desc.text = s.blurb
        h.box.setBackgroundResource(if (s.key == selKey) R.drawable.strategy_item_selected else R.drawable.strategy_item_bg)
        h.box.setOnClickListener { selKey = s.key; onSel(s); notifyDataSetChanged(); Anim.press(h.box) }
        Anim.itemEnter(h.itemView, pos)
    }
}

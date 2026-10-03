package com.dlmaster.ui.home
import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageButton
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
import com.dlmaster.util.CommandDownloader
import com.dlmaster.util.FileSizeFormatter
import com.dlmaster.util.MusicPlayer
import com.dlmaster.util.RvOptimizer
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        i.inflate(R.layout.fragment_home, c, false)

    override fun onViewCreated(view: View, s: Bundle?) {
        // 三属性同帧入场,分帧延迟
        val stagger = arrayOf(
            view.findViewById<View>(R.id.tv_title),
            view.findViewById<View>(R.id.tv_subtitle),
            view.findViewById<View>(R.id.card_input),
            view.findViewById<View>(R.id.card_music)
        )
        stagger.forEachIndexed { idx, v -> v?.let { Anim.enter(it, idx * 70L) } }

        val et = view.findViewById<EditText>(R.id.et_url)

        view.findViewById<MaterialButton>(R.id.btn_oneclick).setOnClickListener {
            val t = et.text.toString().trim()
            if (t.isEmpty()) { Snackbar.make(view, "先粘个链接", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener }
            vibrate(view)
            val loading = AlertDialog.Builder(requireContext()).setMessage("正在找最快的路…").setCancelable(false).create()
            loading.show()
            try {
                loading.window?.setBackgroundDrawableResource(android.R.color.transparent)
                val dm = resources.displayMetrics
                loading.window?.setLayout((dm.widthPixels * 0.7).toInt(), -2)
            } catch (_: Throwable) {}
            lifecycleScope.launch {
                val r = try { SmartAnalyzer.analyze(t) } catch (_: Throwable) { null }
                if (loading.isShowing) loading.dismiss()
                if (r == null) Snackbar.make(view, "分析失败", Snackbar.LENGTH_SHORT).show()
                else {
                    CommandDownloader.launchWithStrategy(requireContext(), r, r.best)
                    et.setText("")
                    jump(R.id.nav_download)
                }
            }
        }

        view.findViewById<MaterialButton>(R.id.btn_analyze).setOnClickListener {
            val t = et.text.toString().trim()
            if (t.isEmpty()) { Snackbar.make(view, "先粘个链接", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener }
            vibrate(view)
            val loading = AlertDialog.Builder(requireContext()).setMessage("分析中…").setCancelable(false).create()
            loading.show()
            try {
                loading.window?.setBackgroundDrawableResource(android.R.color.transparent)
                val dm = resources.displayMetrics
                loading.window?.setLayout((dm.widthPixels * 0.7).toInt(), -2)
            } catch (_: Throwable) {}
            lifecycleScope.launch {
                val r = try { SmartAnalyzer.analyze(t) } catch (_: Throwable) { null }
                if (loading.isShowing) loading.dismiss()
                if (r == null) Snackbar.make(view, "分析失败", Snackbar.LENGTH_SHORT).show()
                else showResult(r)
            }
        }

        view.findViewById<View>(R.id.card_browse).setOnClickListener { jump(R.id.nav_browser) }
        view.findViewById<View>(R.id.card_downloads).setOnClickListener { jump(R.id.nav_download) }
        view.findViewById<View>(R.id.card_sniff).setOnClickListener {
            val list = DownloadRepository.sniffed.toList()
            if (list.isEmpty()) {
                Snackbar.make(view, "还没抓到链接，去浏览器逛逛", Snackbar.LENGTH_SHORT).show()
                jump(R.id.nav_browser)
            } else showSniffDialog(list)
        }

        val tvMusic = view.findViewById<TextView>(R.id.tv_music_name)
        val btnT = view.findViewById<ImageButton>(R.id.btn_music_toggle)
        val btnN = view.findViewById<ImageButton>(R.id.btn_music_next)
        fun rf() {
            tvMusic.text = MusicPlayer.currentTrackName() ?: getString(R.string.music_idle)
            btnT.setImageResource(if (MusicPlayer.isPlaying())
                android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
        }
        btnT.setOnClickListener { if (MusicPlayer.isPlaying()) MusicPlayer.pause() else MusicPlayer.resume(); rf() }
        btnN.setOnClickListener { MusicPlayer.next(requireContext()); rf() }
        rf()
    }

    private fun showResult(r: AnalyzeResult) {
        val dlg = AlertDialog.Builder(requireContext()).create()
        val root = layoutInflater.inflate(R.layout.dialog_analyze, null)
        dlg.setView(root)
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
        root.findViewById<MaterialButton>(R.id.btn_start).setOnClickListener {
            dlg.dismiss(); CommandDownloader.launchWithStrategy(requireContext(), r, sel); jump(R.id.nav_download)
        }
        dlg.show()
        try {
            dlg.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dlg.window?.setWindowAnimations(R.style.DialogAnim)
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
            CommandDownloader.sniffDownload(requireContext(), url); dlg.dismiss(); jump(R.id.nav_download)
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

    private fun vibrate(view: View) {
        try {
            val v = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            if (android.os.Build.VERSION.SDK_INT >= 26)
                v.vibrate(android.os.VibrationEffect.createOneShot(12, 40))
            else { @Suppress("DEPRECATION") v.vibrate(12) }
            Anim.press(view)
        } catch (_: Throwable) {}
    }

    private fun jump(id: Int) {
        (activity as? MainActivity)
            ?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_nav)
            ?.selectedItemId = id
    }
}

class StrategyAdapter(
    private val items: List<DownloadStrategy>, def: DownloadStrategy,
    private val onSel: (DownloadStrategy) -> Unit
) : RecyclerView.Adapter<StrategyAdapter.VH>() {
    private var selKey = def.key
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val box: android.widget.LinearLayout = v.findViewById(R.id.strategy_box)
        val name: TextView = v.findViewById(R.id.tv_s_name)
        val desc: TextView = v.findViewById(R.id.tv_s_desc)
    }
    override fun onCreateViewHolder(p: ViewGroup, t: Int): VH =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_strategy, p, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(h: VH, pos: Int) {
        val s = items[pos]
        h.name.text = s.displayName; h.desc.text = s.blurb
        val isSel = s.key == selKey
        h.box.setBackgroundResource(
            if (isSel) R.drawable.strategy_item_selected else R.drawable.strategy_item_bg)
        h.box.setOnClickListener {
            selKey = s.key; onSel(s); notifyDataSetChanged()
            Anim.press(h.box)
        }
        Anim.itemEnter(h.itemView, pos)
    }
}

class SniffAdapter(
    private val items: List<String>, private val onClick: (String) -> Unit
) : RecyclerView.Adapter<SniffAdapter.VH>() {
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tv_sniff_name)
        val ext: TextView = v.findViewById(R.id.tv_sniff_ext)
    }
    override fun onCreateViewHolder(p: ViewGroup, t: Int): VH =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_sniff, p, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(h: VH, pos: Int) {
        val url = items[pos]
        val name = url.substringAfterLast('/').substringBefore('?').ifBlank { "资源" }
        val ext = url.substringAfterLast('.', "").lowercase()
        h.name.text = name
        h.ext.text = if (ext.isNotEmpty()) ".$ext  ·  ${url.take(60)}" else url.take(60)
        h.itemView.setOnClickListener { onClick(url) }
        Anim.itemEnter(h.itemView, pos)
    }
}

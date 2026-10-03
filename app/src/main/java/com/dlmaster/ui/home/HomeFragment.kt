package com.dlmaster.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dlmaster.MainActivity
import com.dlmaster.R
import com.dlmaster.download.AnalyzeResult
import com.dlmaster.download.DownloadStrategy
import com.dlmaster.download.LinkKind
import com.dlmaster.download.SmartAnalyzer
import com.dlmaster.util.CommandDownloader
import com.dlmaster.util.FileSizeFormatter
import com.dlmaster.util.MusicPlayer
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar

class HomeFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View =
        inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, s: Bundle?) {
        val et = view.findViewById<EditText>(R.id.et_url)

        // 智能分析
        view.findViewById<MaterialButton>(R.id.btn_analyze).setOnClickListener {
            val t = et.text.toString().trim()
            if (t.isEmpty()) { Snackbar.make(view, "先粘个链接进来", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener }
            analyzeAndShow(t)
        }

        // 直接智能下载
        view.findViewById<MaterialButton>(R.id.btn_smart).setOnClickListener {
            val t = et.text.toString().trim()
            if (t.isEmpty()) { Snackbar.make(view, "先粘个链接进来", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener }
            CommandDownloader.smartAnalyzeAndDownload(requireContext(), t, lifecycleScope) { r ->
                if (r != null) Snackbar.make(view, "已按「${r.best.displayName}」开工", Snackbar.LENGTH_SHORT).show()
            }
            et.setText("")
            jump(R.id.nav_download)
        }

        // 直链极速
        view.findViewById<MaterialButton>(R.id.btn_direct).setOnClickListener {
            val t = et.text.toString().trim()
            if (t.isEmpty()) { Snackbar.make(view, "先粘个链接进来", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener }
            CommandDownloader.directDownload(t, requireContext())
            Snackbar.make(view, "32 线程直连，已开跑", Snackbar.LENGTH_SHORT).show()
            et.setText("")
            jump(R.id.nav_download)
        }

        // 音乐
        val tvMusic = view.findViewById<TextView>(R.id.tv_music_name)
        val btnToggle = view.findViewById<ImageButton>(R.id.btn_music_toggle)
        val btnNext = view.findViewById<ImageButton>(R.id.btn_music_next)
        fun refresh() {
            tvMusic.text = MusicPlayer.currentTrackName() ?: getString(R.string.music_idle)
            btnToggle.setImageResource(
                if (MusicPlayer.isPlaying()) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play
            )
        }
        btnToggle.setOnClickListener {
            if (MusicPlayer.isPlaying()) MusicPlayer.pause() else MusicPlayer.resume(); refresh()
        }
        btnNext.setOnClickListener { MusicPlayer.next(requireContext()); refresh() }
        refresh()

        view.findViewById<MaterialCardView>(R.id.card_browse).setOnClickListener { jump(R.id.nav_browser) }
        view.findViewById<MaterialCardView>(R.id.card_downloads).setOnClickListener { jump(R.id.nav_download) }
        view.findViewById<MaterialCardView>(R.id.card_rescan).setOnClickListener {
            MusicPlayer.rescan(requireContext()); refresh()
            Snackbar.make(view, "音乐重新扫了一遍", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun analyzeAndShow(url: String) {
        val loading = AlertDialog.Builder(requireContext(), R.style.DialogTheme)
            .setMessage("正在分析，请稍等…").setCancelable(false).create()
        loading.show()
        lifecycleScope.launchWhenStarted {
            val r = try { SmartAnalyzer.analyze(url) } catch (_: Throwable) { null }
            if (loading.isShowing) loading.dismiss()
            if (r == null) {
                Snackbar.make(requireView(), "分析失败，检查下链接或网络", Snackbar.LENGTH_SHORT).show()
                return@launchWhenStarted
            }
            showResult(r)
        }
    }

    private fun showResult(r: AnalyzeResult) {
        val ctx = requireContext()
        val dlg = AlertDialog.Builder(ctx, R.style.DialogTheme).create()
        val root = layoutInflater.inflate(R.layout.dialog_analyze, null)
        dlg.setView(root)

        root.findViewById<TextView>(R.id.tv_dlg_title).text = "分析好了"
        root.findViewById<TextView>(R.id.tv_dlg_sub).text = "给你挑了条最快的路"
        root.findViewById<TextView>(R.id.tv_dlg_type).text = "链接类型：${r.kind.label}"
        root.findViewById<TextView>(R.id.tv_dlg_size).text = buildString {
            append("文件大小：")
            append(if (r.size > 0) FileSizeFormatter.fmt(r.size) else "未知")
            append("　·　")
            append(if (r.supportsRange) "支持分段" else "不支持分段")
            if (r.speedHint.isNotEmpty()) { append("\n"); append(r.speedHint) }
            if (r.note.isNotEmpty()) { append("\n"); append(r.note) }
        }
        root.findViewById<TextView>(R.id.tv_dlg_best).text =
            "推荐走「${r.best.displayName}」\n${r.best.blurb}"

        // 备选列表
        val choices = (listOf(r.best) + r.alternatives).distinctBy { it.key }
        var selected = r.best
        val rv = root.findViewById<RecyclerView>(R.id.rv_strategy)
        rv.layoutManager = LinearLayoutManager(ctx)
        val adapter = StrategyAdapter(choices, r.best) { sel -> selected = sel }
        rv.adapter = adapter

        root.findViewById<MaterialButton>(R.id.btn_cancel).setOnClickListener { dlg.dismiss() }
        root.findViewById<MaterialButton>(R.id.btn_start).setOnClickListener {
            dlg.dismiss()
            CommandDownloader.startWithAnalysis(ctx, r, selected)
            Snackbar.make(requireView(), "「${selected.displayName}」已启动", Snackbar.LENGTH_SHORT).show()
            jump(R.id.nav_download)
        }
        dlg.show()
    }

    private fun jump(id: Int) {
        (activity as? MainActivity)
            ?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_nav)
            ?.selectedItemId = id
    }
}

/** 策略列表适配器 */
class StrategyAdapter(
    private val items: List<DownloadStrategy>,
    private val defaultSelected: DownloadStrategy,
    private val onSelect: (DownloadStrategy) -> Unit
) : RecyclerView.Adapter<StrategyAdapter.VH>() {

    private var selectedKey: String = defaultSelected.key

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val box: FrameLayout = v.findViewById(R.id.strategy_box)
        val name: TextView = v.findViewById(R.id.tv_s_name)
        val desc: TextView = v.findViewById(R.id.tv_s_desc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_strategy, parent, false)
        return VH(v)
    }
    override fun getItemCount() = items.size
    override fun onBindViewHolder(h: VH, pos: Int) {
        val s = items[pos]
        h.name.text = s.displayName
        h.desc.text = s.blurb
        val isSel = s.key == selectedKey
        h.box.setBackgroundResource(if (isSel) R.drawable.strategy_item_selected else R.drawable.strategy_item_bg)
        h.box.setOnClickListener {
            selectedKey = s.key
            onSelect(s)
            notifyDataSetChanged()
        }
    }
}

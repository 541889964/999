package com.dlmaster.ui.home
import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dlmaster.MainActivity
import com.dlmaster.R
import com.dlmaster.download.AnalyzeResult
import com.dlmaster.download.DownloadStrategy
import com.dlmaster.download.SmartAnalyzer
import com.dlmaster.util.CommandDownloader
import com.dlmaster.util.FileSizeFormatter
import com.dlmaster.util.MusicPlayer
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        i.inflate(R.layout.fragment_home, c, false)

    override fun onViewCreated(view: View, s: Bundle?) {
        val et = view.findViewById<EditText>(R.id.et_url)

        view.findViewById<MaterialButton>(R.id.btn_oneclick).setOnClickListener {
            val t = et.text.toString().trim()
            if (t.isEmpty()) { Snackbar.make(view, "先粘个链接", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener }
            val loading = AlertDialog.Builder(requireContext()).setMessage("正在找最快的路…").setCancelable(false).create()
            loading.show()
            lifecycleScope.launch {
                val r = try { SmartAnalyzer.analyze(t) } catch (_: Throwable) { null }
                if (loading.isShowing) loading.dismiss()
                if (r == null) {
                    Snackbar.make(view, "分析失败，检查链接或网络", Snackbar.LENGTH_SHORT).show()
                } else {
                    CommandDownloader.launchWithStrategy(requireContext(), r, r.best)
                    Snackbar.make(view, "「${r.best.displayName}」已启动", Snackbar.LENGTH_SHORT).show()
                    et.setText("")
                    jump(R.id.nav_download)
                }
            }
        }

        view.findViewById<MaterialButton>(R.id.btn_analyze).setOnClickListener {
            val t = et.text.toString().trim()
            if (t.isEmpty()) { Snackbar.make(view, "先粘个链接", Snackbar.LENGTH_SHORT).show(); return@setOnClickListener }
            val loading = AlertDialog.Builder(requireContext()).setMessage("分析中…").setCancelable(false).create()
            loading.show()
            lifecycleScope.launch {
                val r = try { SmartAnalyzer.analyze(t) } catch (_: Throwable) { null }
                if (loading.isShowing) loading.dismiss()
                if (r == null) Snackbar.make(view, "分析失败", Snackbar.LENGTH_SHORT).show()
                else showResult(r)
            }
        }

        view.findViewById<MaterialCardView>(R.id.card_clip).setOnClickListener {
            val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = cm.primaryClip
            val txt = if (clip != null && clip.itemCount > 0) clip.getItemAt(0).coerceToText(requireContext()).toString() else ""
            if (!TextUtils.isEmpty(txt)) { et.setText(txt); Snackbar.make(view, "已粘贴", Snackbar.LENGTH_SHORT).show() }
            else Snackbar.make(view, "剪贴板为空", Snackbar.LENGTH_SHORT).show()
        }

        view.findViewById<MaterialCardView>(R.id.card_batch).setOnClickListener {
            val dlg = AlertDialog.Builder(requireContext()).create()
            val iv = layoutInflater.inflate(android.R.layout.simple_list_item_1, null)
            val ed = android.widget.EditText(requireContext()).apply {
                hint = "每行一条链接"
                setPadding(40, 40, 40, 40)
            }
            dlg.setTitle("批量下载")
            dlg.setView(ed)
            dlg.setButton(AlertDialog.BUTTON_POSITIVE, "开始") { _, _ ->
                val lines = ed.text.toString().lines().filter { it.trim().isNotEmpty() }
                Toast.makeText(requireContext(), "已加入 ${lines.size} 条", Toast.LENGTH_SHORT).show()
                lines.forEach { CommandDownloader.directDownload(it.trim(), requireContext()) }
                jump(R.id.nav_download)
            }
            dlg.setButton(AlertDialog.BUTTON_NEGATIVE, "取消", null as android.content.DialogInterface.OnClickListener?)
            dlg.show()
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

        view.findViewById<MaterialCardView>(R.id.card_browse).setOnClickListener { jump(R.id.nav_browser) }
        view.findViewById<MaterialCardView>(R.id.card_downloads).setOnClickListener { jump(R.id.nav_download) }
        view.findViewById<MaterialCardView>(R.id.card_history).setOnClickListener {
            Snackbar.make(view, "下载历史在任务页搜索框", Snackbar.LENGTH_SHORT).show()
            jump(R.id.nav_download)
        }
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
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = StrategyAdapter(choices, r.best) { sel = it }
        root.findViewById<MaterialButton>(R.id.btn_cancel).setOnClickListener { dlg.dismiss() }
        root.findViewById<MaterialButton>(R.id.btn_start).setOnClickListener {
            dlg.dismiss()
            CommandDownloader.launchWithStrategy(requireContext(), r, sel)
            Snackbar.make(requireView(), "「${sel.displayName}」已启动", Snackbar.LENGTH_SHORT).show()
            jump(R.id.nav_download)
        }
        dlg.show()
        try { dlg.window?.setBackgroundDrawableResource(android.R.color.transparent) } catch (_: Throwable) {}
    }

    private fun jump(id: Int) {
        (activity as? MainActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_nav)?.selectedItemId = id
    }
}

class StrategyAdapter(
    private val items: List<DownloadStrategy>,
    def: DownloadStrategy,
    private val onSel: (DownloadStrategy) -> Unit
) : RecyclerView.Adapter<StrategyAdapter.VH>() {
    private var selKey = def.key
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val box: android.widget.FrameLayout = v.findViewById(R.id.strategy_box)
        val name: TextView = v.findViewById(R.id.tv_s_name)
        val desc: TextView = v.findViewById(R.id.tv_s_desc)
    }
    override fun onCreateViewHolder(p: ViewGroup, t: Int): VH = VH(LayoutInflater.from(p.context).inflate(R.layout.item_strategy, p, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(h: VH, pos: Int) {
        val s = items[pos]
        h.name.text = s.displayName
        h.desc.text = s.blurb
        val isSel = s.key == selKey
        h.box.setBackgroundResource(if (isSel) R.drawable.strategy_item_selected else R.drawable.strategy_item_bg)
        h.box.setOnClickListener { selKey = s.key; onSel(s); notifyDataSetChanged() }
    }
}

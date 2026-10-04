package com.dlmaster.ui.download
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.dlmaster.R
import com.dlmaster.anim.Anim
import com.dlmaster.download.DownloadRepository
import com.dlmaster.download.DownloadTask
import com.dlmaster.util.FileSizeFormatter
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.io.File
import java.util.concurrent.ConcurrentHashMap
class DownloadAdapter(private val source: List<DownloadTask>) : RecyclerView.Adapter<DownloadAdapter.VH>() {
    private var filtered: List<DownloadTask>? = null
    private val animated = ConcurrentHashMap<String, Boolean>()
    class VH(p: ViewGroup) : RecyclerView.ViewHolder(LayoutInflater.from(p.context).inflate(R.layout.item_download, p, false)) {
        val name: TextView = itemView.findViewById(R.id.tv_name)
        val sub: TextView = itemView.findViewById(R.id.tv_sub)
        val progress: LinearProgressIndicator = itemView.findViewById(R.id.progress)
        val action: TextView = itemView.findViewById(R.id.tv_action)
        val ext: TextView = itemView.findViewById(R.id.tv_cover_ext)
    }
    fun setFiltered(list: List<DownloadTask>?) { filtered = list; notifyDataSetChanged() }
    private fun list(): List<DownloadTask> = filtered ?: source
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(p)
    override fun getItemCount() = list().size
    override fun onBindViewHolder(h: VH, pos: Int) {
        val t = list()[pos]
        h.name.text = t.fileName
        h.sub.text = buildString {
            append(t.strategy.title); append(" · ")
            append(FileSizeFormatter.fmt(t.downloadedBytes)); append(" / "); append(FileSizeFormatter.fmt(t.totalBytes))
            if (t.speedBytesPerSec > 0 && t.status == DownloadTask.Status.RUNNING) { append(" · "); append(FileSizeFormatter.fmt(t.speedBytesPerSec)); append("/s") }
        }
        val ext = t.fileName.substringAfterLast('.', "").uppercase()
        h.ext.text = if (ext.length in 2..4) ext else "FILE"
        h.progress.progress = t.progressPercent
        h.action.text = when (t.status) { DownloadTask.Status.DONE -> "${t.progressPercent}%"; DownloadTask.Status.FAILED -> "失败"; DownloadTask.Status.RUNNING -> "${t.progressPercent}%"; else -> "等待" }
        h.itemView.setOnClickListener { Anim.press(h.itemView); if (t.status == DownloadTask.Status.DONE) tryInstall(it.context, t) }
        h.itemView.setOnLongClickListener { showMenu(it.context, t); true }
        val key = t.id
        if (animated[key] != true) { Anim.itemEnter(h.itemView, pos); animated[key] = true }
    }
    private fun showMenu(ctx: Context, t: DownloadTask) {
        val opts = arrayOf("复制链接", if (t.status == DownloadTask.Status.DONE) "安装" else "删除")
        AlertDialog.Builder(ctx).setTitle(t.fileName).setItems(opts) { _, i ->
            when (opts[i]) {
                "复制链接" -> try { val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; cm.setPrimaryClip(ClipData.newPlainText("url", t.url)); Toast.makeText(ctx, "已复制", Toast.LENGTH_SHORT).show() } catch (_: Throwable) {}
                "安装" -> tryInstall(ctx, t)
                "删除" -> DownloadRepository.removeTask(t)
            }
        }.show()
    }
    private fun tryInstall(ctx: Context, t: DownloadTask) = try {
        val f = File(t.savedPath); if (!f.exists()) throw Exception("文件不存在")
        val uri: Uri = if (android.os.Build.VERSION.SDK_INT >= 24) FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f) else Uri.fromFile(f)
        val i = Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/vnd.android.package-archive"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        ctx.startActivity(i)
    } catch (e: Throwable) { Toast.makeText(ctx, "打不开安装: ${e.message}", Toast.LENGTH_SHORT).show() }
}

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
import com.dlmaster.download.DownloadTask
import com.dlmaster.util.FileSizeFormatter
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.io.File
class DownloadAdapter(private val items: MutableList<DownloadTask>) : RecyclerView.Adapter<DownloadAdapter.VH>() {
    private var filtered: List<DownloadTask>? = null
    class VH(p: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(p.context).inflate(R.layout.item_download, p, false)
    ) {
        val name: TextView = itemView.findViewById(R.id.tv_name)
        val pct: TextView = itemView.findViewById(R.id.tv_pct)
        val progress: LinearProgressIndicator = itemView.findViewById(R.id.progress)
        val info: TextView = itemView.findViewById(R.id.tv_info)
        val strategy: TextView = itemView.findViewById(R.id.tv_strategy)
        val action: TextView = itemView.findViewById(R.id.tv_action)
    }
    fun setFiltered(list: List<DownloadTask>) { filtered = list; notifyDataSetChanged() }
    private fun list(): List<DownloadTask> = filtered ?: items
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(p)
    override fun getItemCount() = list().size
    override fun onBindViewHolder(h: VH, pos: Int) {
        val t = list()[pos]
        h.name.text = t.fileName
        h.progress.progress = t.progressPercent
        h.pct.text = "${t.progressPercent}%"
        h.strategy.text = t.strategy.displayName
        h.info.text = "${FileSizeFormatter.fmt(t.downloadedBytes)} / ${FileSizeFormatter.fmt(t.totalBytes)}" +
                (if (t.speedBytesPerSec > 0) "  ${FileSizeFormatter.fmt(t.speedBytesPerSec)}/s" else "")
        h.action.text = when (t.status) {
            DownloadTask.Status.DONE -> "安装"
            DownloadTask.Status.FAILED -> "失败"
            DownloadTask.Status.RUNNING -> "进行中"
            else -> ""
        }
        h.action.setOnClickListener { if (t.status == DownloadTask.Status.DONE) tryInstall(it.context, t) }
        h.itemView.setOnLongClickListener { showMenu(it.context, t); true }
    }
    private fun showMenu(ctx: Context, t: DownloadTask) {
        val opts = arrayOf("复制链接", if (t.status == DownloadTask.Status.DONE) "安装" else "删除")
        AlertDialog.Builder(ctx).setTitle(t.fileName).setItems(opts) { _, i ->
            when (opts[i]) {
                "复制链接" -> try {
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("url", t.url))
                } catch (_: Throwable) {}
                "安装" -> tryInstall(ctx, t)
                "删除" -> { items.remove(t); filtered = null; notifyDataSetChanged() }
            }
        }.show()
    }
    private fun tryInstall(ctx: Context, t: DownloadTask) = try {
        val f = File(t.savedPath); if (!f.exists()) throw Exception("文件不存在")
        val uri: Uri = if (android.os.Build.VERSION.SDK_INT >= 24)
            FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f) else Uri.fromFile(f)
        val i = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(i)
    } catch (e: Throwable) {
        Toast.makeText(ctx, "打不开安装: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

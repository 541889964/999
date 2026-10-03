package com.dlmaster.ui.download
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.dlmaster.R
import com.dlmaster.download.DownloadTask
import com.dlmaster.util.FileSizeFormatter
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.io.File
class DownloadAdapter(private val items: MutableList<DownloadTask>) : RecyclerView.Adapter<DownloadAdapter.VH>() {
    class VH(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_download, parent, false)
    ) {
        val name: TextView = itemView.findViewById(R.id.tv_name)
        val pct: TextView = itemView.findViewById(R.id.tv_pct)
        val progress: LinearProgressIndicator = itemView.findViewById(R.id.progress)
        val info: TextView = itemView.findViewById(R.id.tv_info)
        val strategy: TextView = itemView.findViewById(R.id.tv_strategy)
        val action: TextView = itemView.findViewById(R.id.tv_action)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(parent)
    override fun getItemCount() = items.size
    override fun onBindViewHolder(h: VH, pos: Int) {
        val t = items[pos]
        h.name.text = t.fileName
        h.progress.progress = t.progressPercent
        h.pct.text = "${t.progressPercent}%"
        h.strategy.text = t.strategy.displayName
        val downloaded = FileSizeFormatter.fmt(t.downloadedBytes)
        val total = FileSizeFormatter.fmt(t.totalBytes)
        val speed = if (t.speedBytesPerSec > 0) "  ${FileSizeFormatter.fmt(t.speedBytesPerSec)}/s" else ""
        h.info.text = "$downloaded / $total$speed"
        h.action.text = when (t.status) {
            DownloadTask.Status.DONE -> "安装"
            DownloadTask.Status.FAILED -> "重试"
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
                "删除" -> { items.remove(t); notifyDataSetChanged() }
            }
        }.show()
    }
    private fun tryInstall(ctx: Context, t: DownloadTask) {
        try {
            val f = File(t.savedPath); if (!f.exists()) return
            val uri: Uri = if (android.os.Build.VERSION.SDK_INT >= 24)
                FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f)
            else Uri.fromFile(f)
            val i = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ctx.startActivity(i)
        } catch (e: Throwable) {
            android.widget.Toast.makeText(ctx, "打不开安装: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

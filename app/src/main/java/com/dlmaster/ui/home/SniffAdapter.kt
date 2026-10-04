package com.dlmaster.ui.home
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.dlmaster.R
import com.dlmaster.anim.Anim
import com.dlmaster.sniffer.SniffedResource
import com.dlmaster.util.CommandDownloader
import com.dlmaster.util.FileSizeFormatter
class SniffAdapter(
    private val items: List<SniffedResource>,
    private val onPick: (SniffedResource) -> Unit
) : RecyclerView.Adapter<SniffAdapter.VH>() {
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val kind: TextView = v.findViewById(R.id.tv_sniff_kind)
        val name: TextView = v.findViewById(R.id.tv_sniff_name)
        val size: TextView = v.findViewById(R.id.tv_sniff_size)
        val detail: TextView = v.findViewById(R.id.tv_sniff_detail)
        val url: TextView = v.findViewById(R.id.tv_sniff_url)
    }
    override fun onCreateViewHolder(p: ViewGroup, t: Int): VH =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_sniff, p, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(h: VH, pos: Int) {
        val r = items[pos]
        // 类型标签
        h.kind.text = r.kind.display
        h.kind.setTextColor(when (r.kind) {
            SniffedResource.Kind.VIDEO -> 0xFFFF6B6B.toInt()
            SniffedResource.Kind.AUDIO -> 0xFFB980F0.toInt()
            SniffedResource.Kind.IMAGE -> 0xFFFFB86C.toInt()
            SniffedResource.Kind.DOCUMENT -> 0xFF4DD0E1.toInt()
            SniffedResource.Kind.ARCHIVE -> 0xFFFFD54F.toInt()
            SniffedResource.Kind.APK -> 0xFF69F0AE.toInt()
            SniffedResource.Kind.STREAM -> 0xFF52C7FF.toInt()
            else -> 0xFFB0BEC5.toInt()
        })
        // 文件名
        h.name.text = if (r.label.isNotBlank() && r.fileName.length < 8) r.label else r.fileName
        // 大小
        h.size.text = FileSizeFormatter.fmtShort(r.size)
        // 详细:类型 · 扩展名 · 支持分段 · HLS · MIME
        h.detail.text = r.detailText()
        // URL(截断)
        h.url.text = r.url
        // 点击下载
        h.itemView.setOnClickListener { v ->
            Anim.press(v)
            onPick(r)
        }
        // 长按复制
        h.itemView.setOnLongClickListener { v ->
            try {
                val cm = v.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("url", r.url))
                Toast.makeText(v.context, "已复制链接", Toast.LENGTH_SHORT).show()
            } catch (_: Throwable) {}
            true
        }
        Anim.itemEnter(h.itemView, pos)
    }
}

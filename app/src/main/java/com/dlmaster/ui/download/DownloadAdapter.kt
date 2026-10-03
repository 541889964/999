package com.dlmaster.ui.download
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dlmaster.R
import com.dlmaster.download.DownloadTask
import com.dlmaster.util.FileSizeFormatter
import com.google.android.material.progressindicator.LinearProgressIndicator
class DownloadAdapter(private val items: MutableList<DownloadTask>) : RecyclerView.Adapter<DownloadAdapter.VH>() {
    class VH(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_download, parent, false)
    ) {
        val name: TextView = itemView.findViewById(R.id.tv_name)
        val progress: LinearProgressIndicator = itemView.findViewById(R.id.progress)
        val info: TextView = itemView.findViewById(R.id.tv_info)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(parent)
    override fun getItemCount() = items.size
    override fun onBindViewHolder(h: VH, pos: Int) {
        val t = items[pos]
        h.name.text = t.fileName
        h.progress.progress = t.progressPercent
        h.info.text = "${FileSizeFormatter.fmt(t.downloadedBytes)} / ${FileSizeFormatter.fmt(t.totalBytes)}  ·  ${t.status}"
    }
}

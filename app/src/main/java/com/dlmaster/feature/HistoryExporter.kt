package com.dlmaster.feature
import android.content.Context
import com.dlmaster.download.DownloadRepository
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
object HistoryExporter {
    fun exportCsv(ctx: Context): File? = try {
        val f = File(DownloadDir.get(ctx), "history_${System.currentTimeMillis()}.csv")
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        f.bufferedWriter().use { w ->
            w.write("id,fileName,url,strategy,status,size,createdAt,finishedAt\n")
            DownloadRepository.tasks.forEach { t ->
                w.write("\"${t.id}\",\"${t.fileName.replace("\"","'")}\",\"${t.url}\",")
                w.write("\"${t.strategy.key}\",\"${t.status}\",\"${t.totalBytes}\",")
                w.write("\"${fmt.format(Date(t.createdAt))}\",")
                w.write("\"${if (t.finishedAt>0) fmt.format(Date(t.finishedAt)) else ""}\"\n")
            }
        }
        f
    } catch (_:Throwable) { null }
}

package com.dlmaster.util
import android.content.Context
import com.dlmaster.download.DownloadRepository
import com.dlmaster.download.DownloadTask
import com.dlmaster.download.MultiThreadDownloader
import com.dlmaster.netdisk.NetdiskResolver
import com.dlmaster.sniffer.ThunderParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
object CommandDownloader {
    fun smartDownload(input: String, ctx: Context) {
        try {
            val url = when {
                ThunderParser.isThunder(input) -> ThunderParser.parse(input) ?: return
                input.startsWith("magnet:") -> input
                NetdiskResolver.isNetdiskLink(input) -> input
                input.startsWith("http", true) -> {
                    if (looksLikePage(input)) return else input
                }
                else -> return
            }
            start(ctx, url, 8)
        } catch (_: Throwable) {}
    }
    fun directDownload(url: String, ctx: Context) {
        try { start(ctx, url, 16) } catch (_: Throwable) {}
    }
    private fun start(ctx: Context, url: String, threads: Int) {
        val task = DownloadTask(url = url)
        task.status = DownloadTask.Status.RUNNING
        DownloadRepository.addTask(task)
        val dir = File(ctx.getExternalFilesDir(null) ?: ctx.filesDir, "downloads")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val f = MultiThreadDownloader.download(url, dir, threads) { d, t ->
                    task.downloadedBytes = d
                    if (t > 0) task.totalBytes = t
                }
                if (f != null) {
                    task.fileName = f.name
                    task.status = DownloadTask.Status.DONE
                } else task.status = DownloadTask.Status.FAILED
            } catch (_: Throwable) {
                task.status = DownloadTask.Status.FAILED
            }
        }
    }
    private fun looksLikePage(url: String): Boolean {
        val p = url.substringAfter("://").substringAfter('/', "")
        return p.isEmpty() || p.endsWith(".html") || p.endsWith(".php") ||
                p.endsWith(".asp") || p.endsWith(".jsp") ||
                !p.substringAfterLast('/').contains(".")
    }
}

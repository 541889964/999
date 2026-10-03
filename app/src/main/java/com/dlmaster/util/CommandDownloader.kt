package com.dlmaster.util
import android.content.Context
import com.dlmaster.download.AnalyzeResult
import com.dlmaster.download.DownloadRepository
import com.dlmaster.download.DownloadStrategy
import com.dlmaster.download.DownloadTask
import com.dlmaster.download.SmartAnalyzer
import com.dlmaster.download.SmartDownloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

object CommandDownloader {
    fun oneClick(ctx: Context, url: String, scope: CoroutineScope, onResult: (AnalyzeResult?) -> Unit) {
        scope.launch {
            val r = try { SmartAnalyzer.analyze(url) } catch (_: Throwable) { null }
            onResult(r)
            if (r != null) launchWithStrategy(ctx, r, r.best)
        }
    }
    fun launchWithStrategy(ctx: Context, r: AnalyzeResult, s: DownloadStrategy) {
        val t = DownloadTask(
            url = r.resolved, fileName = r.fileName, strategy = s,
            referer = if (s == DownloadStrategy.STEALTH) "https://www.google.com/" else null
        )
        DownloadRepository.addTask(t)
        run(ctx, t, s)
    }
    fun sniffDownload(ctx: Context, url: String) {
        val name = url.substringAfterLast('/').substringBefore('?').ifBlank { "download.bin" }
        val t = DownloadTask(url = url, fileName = name, strategy = DownloadStrategy.T32)
        DownloadRepository.addTask(t)
        run(ctx, t, DownloadStrategy.T32)
    }
    fun directDownload(url: String, ctx: Context) {
        val t = DownloadTask(url = url, strategy = DownloadStrategy.T32)
        DownloadRepository.addTask(t)
        run(ctx, t, DownloadStrategy.T32)
    }
    private fun run(ctx: Context, task: DownloadTask, s: DownloadStrategy) {
        task.status = DownloadTask.Status.RUNNING
        DownloadRepository.notifyUpdate()
        DownloadNotifier.ensure(ctx)
        val dir = File(ctx.getExternalFilesDir(null) ?: ctx.filesDir, "downloads")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val f = SmartDownloader.download(task.url, dir, s, task.referer) { d, total, speed ->
                    task.downloadedBytes = d
                    if (total > 0) task.totalBytes = total
                    task.speedBytesPerSec = speed
                    DownloadNotifier.update(ctx, task)
                    DownloadRepository.notifyUpdate()
                }
                if (f != null && f.exists()) {
                    task.fileName = f.name
                    task.savedPath = f.absolutePath
                    task.status = DownloadTask.Status.DONE
                    DownloadNotifier.complete(ctx, task)
                } else {
                    task.status = DownloadTask.Status.FAILED
                    DownloadNotifier.fail(ctx, task)
                }
                DownloadRepository.notifyUpdate()
            } catch (_: Throwable) {
                task.status = DownloadTask.Status.FAILED
                DownloadRepository.notifyUpdate()
                DownloadNotifier.fail(ctx, task)
            }
        }
    }
}

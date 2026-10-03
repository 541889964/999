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

    fun startWithAnalysis(ctx: Context, r: AnalyzeResult, strategy: DownloadStrategy) {
        val t = DownloadTask(
            url = r.resolved,
            fileName = r.fileName,
            strategy = strategy,
            referer = if (strategy == DownloadStrategy.STEALTH) "https://www.google.com/" else null
        )
        DownloadRepository.addTask(t)
        run(ctx, t, strategy)
    }

    /** 直接智能下载：先分析，再用推荐方案 */
    fun smartAnalyzeAndDownload(ctx: Context, url: String, scope: CoroutineScope, cb: (AnalyzeResult?) -> Unit) {
        scope.launch {
            val r = try { SmartAnalyzer.analyze(url) } catch (_: Throwable) { null }
            cb(r)
            if (r != null) startWithAnalysis(ctx, r, r.best)
        }
    }

    /** 直接极速下载：跳过分析，走 32 线程 */
    fun directDownload(url: String, ctx: Context) {
        val t = DownloadTask(url = url, strategy = DownloadStrategy.T32)
        DownloadRepository.addTask(t)
        run(ctx, t, DownloadStrategy.T32)
    }

    private fun run(ctx: Context, task: DownloadTask, strategy: DownloadStrategy) {
        task.status = DownloadTask.Status.RUNNING
        DownloadRepository.notifyUpdate()
        DownloadNotifier.ensure(ctx)
        val dir = File(ctx.getExternalFilesDir(null) ?: ctx.filesDir, "downloads")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val f = SmartDownloader.download(task.url, dir, strategy, task.referer) { d, total, speed ->
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

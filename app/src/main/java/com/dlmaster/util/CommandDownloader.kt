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

    fun oneClick(ctx: Context, url: String, scope: CoroutineScope, cb: (AnalyzeResult?) -> Unit) {
        scope.launch {
            val r = try { SmartAnalyzer.analyze(url) } catch (_: Throwable) { null }
            cb(r)
            if (r != null && !r.isPage) launchWith(ctx, r, r.best)
        }
    }

    fun launchWith(ctx: Context, r: AnalyzeResult, s: DownloadStrategy) {
        val t = DownloadTask(
            url = r.resolved,
            fileName = r.fileName,
            strategy = s,
            referer = if (s == DownloadStrategy.STEALTH) r.original else null
        )
        DownloadRepository.addTask(t)
        run(ctx, t, s)
    }

    fun fromSniffed(
        ctx: Context,
        url: String,
        fileName: String,
        referer: String?,
        strategy: DownloadStrategy = DownloadStrategy.T32
    ) {
        val t = DownloadTask(url = url, fileName = fileName, strategy = strategy, referer = referer)
        DownloadRepository.addTask(t)
        run(ctx, t, strategy)
    }

    fun directDownload(url: String, ctx: Context) {
        val t = DownloadTask(url = url, strategy = DownloadStrategy.ADAPTIVE)
        DownloadRepository.addTask(t)
        run(ctx, t, DownloadStrategy.ADAPTIVE)
    }

    /** 智能粘贴 —— 补齐协议头 */
    fun smartPaste(ctx: Context, text: String): String {
        val t = text.trim()
        return when {
            t.startsWith("http") -> t
            t.startsWith("magnet:") -> t
            t.startsWith("thunder://") -> t
            t.startsWith("ftp://") -> t
            t.startsWith("//") -> "https:$t"
            t.contains(".") && !t.contains(" ") -> "https://$t"
            else -> t
        }
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
                    task.finishedAt = System.currentTimeMillis()
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

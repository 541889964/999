package com.dlmaster.feature
import android.content.Context
import com.dlmaster.download.DownloadRepository
import com.dlmaster.download.DownloadTask
object TaskActions {
    fun pause(t: DownloadTask) { t.status = DownloadTask.Status.PAUSED; DownloadRepository.notifyUpdate() }
    fun resume(t: DownloadTask) { t.status = DownloadTask.Status.PENDING; DownloadRepository.notifyUpdate() }
    fun retry(t: DownloadTask) {
        t.status = DownloadTask.Status.PENDING
        t.downloadedBytes = 0L
        t.errorMsg = ""
        DownloadRepository.notifyUpdate()
    }
    fun remove(t: DownloadTask) { DownloadRepository.removeTask(t) }
    fun clearDone() {
        DownloadRepository.tasks.filter { it.status == DownloadTask.Status.DONE }
            .forEach { DownloadRepository.removeTask(it) }
    }
    fun clearFailed() {
        DownloadRepository.tasks.filter { it.status == DownloadTask.Status.FAILED }
            .forEach { DownloadRepository.removeTask(it) }
    }
}

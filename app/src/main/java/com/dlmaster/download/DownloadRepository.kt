package com.dlmaster.download
import java.util.concurrent.CopyOnWriteArrayList
object DownloadRepository {
    val tasks = CopyOnWriteArrayList<DownloadTask>()
    fun addTask(t: DownloadTask) { tasks.add(t) }
}

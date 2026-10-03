package com.dlmaster.download

import java.util.concurrent.CopyOnWriteArrayList

object DownloadRepository {
    val tasks = CopyOnWriteArrayList<DownloadTask>()
    val sniffedLinks = CopyOnWriteArrayList<String>()

    fun addSniffedLink(url: String) {
        if (!sniffedLinks.contains(url)) sniffedLinks.add(url)
    }
    fun addTask(task: DownloadTask) { tasks.add(task) }
}

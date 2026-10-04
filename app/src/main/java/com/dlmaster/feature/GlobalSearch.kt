package com.dlmaster.feature
import com.dlmaster.download.DownloadRepository
import com.dlmaster.download.DownloadTask
import com.dlmaster.sniffer.SniffRepository
import com.dlmaster.sniffer.SniffedResource
object GlobalSearch {
    data class Result(val tasks: List<DownloadTask>, val sniffs: List<SniffedResource>)
    fun search(q: String): Result {
        val qq = q.trim().lowercase()
        if (qq.isEmpty()) return Result(emptyList(), emptyList())
        val t = DownloadRepository.tasks.filter {
            it.fileName.lowercase().contains(qq) || it.url.lowercase().contains(qq)
        }
        val s = SniffRepository.items.filter {
            it.url.lowercase().contains(qq) || it.fileName.lowercase().contains(qq) ||
            it.label.lowercase().contains(qq)
        }
        return Result(t, s)
    }
}

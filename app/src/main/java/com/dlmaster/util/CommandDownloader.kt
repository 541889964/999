package com.dlmaster.util

import android.content.Context
import com.dlmaster.download.Aria2Engine
import com.dlmaster.download.DirectProbe
import com.dlmaster.download.DownloadRepository
import com.dlmaster.download.DownloadTask
import com.dlmaster.netdisk.NetdiskResolver
import com.dlmaster.sniffer.ThunderParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object CommandDownloader {

    fun smartDownload(input: String, context: Context) {
        when {
            ThunderParser.isThunder(input) -> {
                ThunderParser.parse(input)?.let { enqueue(context, it, false) }
            }
            input.startsWith("magnet:") -> enqueue(context, input, false, 32)
            NetdiskResolver.isNetdiskLink(input) -> enqueue(context, input, false)
            input.startsWith("http", ignoreCase = true) -> {
                if (!looksLikePage(input)) enqueue(context, input, true)
            }
        }
    }

    fun directDownload(url: String, context: Context) {
        enqueue(context, url, true)
    }

    private fun enqueue(
        context: Context,
        url: String,
        direct: Boolean,
        connections: Int = 16
    ) {
        val task = DownloadTask(url = url, isDirect = direct)
        DownloadRepository.addTask(task)

        CoroutineScope(Dispatchers.IO).launch {
            if (direct) {
                DirectProbe.probe(url)?.let { info ->
                    task.fileName = info.fileName
                    task.totalBytes = info.size
                }
            }
            Aria2Engine.startDownload(
                context = context,
                url = url,
                savePath = context.getExternalFilesDir(null)?.absolutePath
                    ?: context.filesDir.absolutePath,
                direct = direct,
                connections = connections
            ) { downloaded, total ->
                task.downloadedBytes = downloaded
                if (total > 0) task.totalBytes = total
            }
        }
    }

    private fun looksLikePage(url: String): Boolean {
        val path = url.substringAfter("://").substringAfter('/', "")
        return path.isEmpty() ||
                path.endsWith(".html") || path.endsWith(".php") ||
                path.endsWith(".asp") || path.endsWith(".jsp") ||
                !path.substringAfterLast('/').contains(".")
    }
}

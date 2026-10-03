package com.dlmaster.download
data class DownloadTask(
    val url: String,
    var fileName: String = url.substringAfterLast('/').substringBefore('?').ifBlank { "download.bin" },
    var totalBytes: Long = 0L,
    var downloadedBytes: Long = 0L,
    var status: Status = Status.PENDING
) {
    enum class Status { PENDING, RUNNING, DONE, FAILED }
    val progressPercent: Int
        get() = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
}

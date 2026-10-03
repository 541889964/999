package com.dlmaster.download
data class DownloadTask(
    val url: String,
    var fileName: String = url.substringAfterLast('/').substringBefore('?').ifBlank { "download.bin" },
    var totalBytes: Long = 0L,
    var downloadedBytes: Long = 0L,
    var speedBytesPerSec: Long = 0L,
    var status: Status = Status.PENDING,
    var savedPath: String = "",
    var strategy: DownloadStrategy = DownloadStrategy.T8,
    var referer: String? = null,
    var mirrors: List<String> = emptyList()
) {
    enum class Status { PENDING, RUNNING, DONE, FAILED }
    val progressPercent: Int
        get() = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
}

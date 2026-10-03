package com.dlmaster.download

data class DownloadTask(
    val url: String,
    var fileName: String = url.substringAfterLast('/').substringBefore('?'),
    var totalBytes: Long = 0L,
    var downloadedBytes: Long = 0L,
    var status: Status = Status.PENDING,
    var isDirect: Boolean = false
) {
    enum class Status { PENDING, RUNNING, PAUSED, DONE, FAILED }

    val progressPercent: Int
        get() = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt() else 0

    var speedText: String = "-- MB/s"
}

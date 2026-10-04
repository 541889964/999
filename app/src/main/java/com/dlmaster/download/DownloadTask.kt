package com.dlmaster.download
import org.json.JSONObject
import java.util.UUID
data class DownloadTask(
    var id: String = UUID.randomUUID().toString(),
    val url: String,
    var fileName: String = url.substringAfterLast('/').substringBefore('?').ifBlank { "download.bin" },
    var totalBytes: Long = 0L,
    var downloadedBytes: Long = 0L,
    var speedBytesPerSec: Long = 0L,
    var status: Status = Status.PENDING,
    var savedPath: String = "",
    var strategy: DownloadStrategy = DownloadStrategy.T8,
    var referer: String? = null,
    var createdAt: Long = System.currentTimeMillis()
) {
    enum class Status { PENDING, RUNNING, DONE, FAILED }
    val progressPercent: Int
        get() = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("url", url); put("fileName", fileName)
        put("totalBytes", totalBytes); put("downloadedBytes", downloadedBytes)
        put("speedBytesPerSec", speedBytesPerSec); put("status", status.name)
        put("savedPath", savedPath); put("strategy", strategy.key)
        put("referer", referer ?: ""); put("createdAt", createdAt)
    }
    companion object {
        fun fromJson(o: JSONObject) = DownloadTask(
            id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
            url = o.optString("url"),
            fileName = o.optString("fileName", "download.bin"),
            totalBytes = o.optLong("totalBytes", 0L),
            downloadedBytes = o.optLong("downloadedBytes", 0L),
            speedBytesPerSec = o.optLong("speedBytesPerSec", 0L),
            status = try { Status.valueOf(o.optString("status", "PENDING")) } catch (_: Throwable) { Status.FAILED },
            savedPath = o.optString("savedPath", ""),
            strategy = DownloadStrategy.byKey(o.optString("strategy", "t8")),
            referer = o.optString("referer", "").ifBlank { null },
            createdAt = o.optLong("createdAt", System.currentTimeMillis())
        )
    }
}

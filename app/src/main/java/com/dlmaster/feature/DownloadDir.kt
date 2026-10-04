package com.dlmaster.feature
import android.content.Context
import java.io.File
object DownloadDir {
    fun get(ctx: Context): File {
        val custom = Settings.downloadDir()
        if (custom.isNotBlank()) {
            val f = File(custom)
            if (f.exists() || f.mkdirs()) return f
        }
        return File(ctx.getExternalFilesDir(null) ?: ctx.filesDir, "downloads").also { it.mkdirs() }
    }
}

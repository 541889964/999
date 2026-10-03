package com.dlmaster.util
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.dlmaster.download.DownloadTask
import java.io.File
object DownloadNotifier {
    private const val CH = "dl_channel"
    private const val ID = 1001
    fun ensure(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) try {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CH) == null) {
                val c = NotificationChannel(CH, "下载", NotificationManager.IMPORTANCE_LOW)
                c.setShowBadge(false); nm.createNotificationChannel(c)
            }
        } catch (_: Throwable) {}
    }
    private fun base(ctx: Context, task: DownloadTask) =
        NotificationCompat.Builder(ctx, CH)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(task.fileName).setOngoing(true).setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
    fun update(ctx: Context, task: DownloadTask) = try {
        val b = base(ctx, task).setProgress(100, task.progressPercent, false)
            .setContentText("${task.progressPercent}% · ${FileSizeFormatter.fmt(task.speedBytesPerSec)}/s")
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(ID, b.build())
    } catch (_: Throwable) {}
    fun complete(ctx: Context, task: DownloadTask) = try {
        val b = base(ctx, task).setOngoing(false)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("下载完成 · 点击安装").setContentText(task.fileName).setAutoCancel(true)
        installPending(ctx, task)?.let { b.setContentIntent(it) }
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(ID, b.build())
    } catch (_: Throwable) {}
    fun fail(ctx: Context, task: DownloadTask) = try {
        val b = base(ctx, task).setOngoing(false)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("下载失败").setContentText(task.fileName).setAutoCancel(true)
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(ID, b.build())
    } catch (_: Throwable) {}
    private fun installPending(ctx: Context, task: DownloadTask): PendingIntent? = try {
        val f = File(task.savedPath); if (!f.exists()) null else {
            val uri: Uri = if (Build.VERSION.SDK_INT >= 24)
                FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f) else Uri.fromFile(f)
            val i = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            PendingIntent.getActivity(ctx, 2001, i, if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        }
    } catch (_: Throwable) { null }
}

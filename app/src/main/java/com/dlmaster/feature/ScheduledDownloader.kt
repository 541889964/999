package com.dlmaster.feature
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.dlmaster.download.DownloadRepository
import com.dlmaster.util.CommandDownloader
object ScheduledDownloader {
    const val EXTRA_URL = "sched_url"
    fun schedule(ctx: Context, url: String, triggerAt: Long) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(ctx, Receiver::class.java).putExtra(EXTRA_URL, url)
        val pi = PendingIntent.getBroadcast(ctx, url.hashCode(), i,
            if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } catch (_:Throwable) {
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }
    class Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val url = intent.getStringExtra(EXTRA_URL) ?: return
            try { CommandDownloader.directDownload(url, context) } catch (_:Throwable) {}
        }
    }
}

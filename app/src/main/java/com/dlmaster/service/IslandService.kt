package com.dlmaster.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.dlmaster.download.DownloadRepository
import com.dlmaster.download.DownloadTask
import com.dlmaster.util.MusicPlayer
import com.dlmaster.view.IslandView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class IslandService : Service() {

    private var wm: WindowManager? = null
    private var island: IslandView? = null
    private var scope: CoroutineScope? = null

    companion object {
        const val ACTION_SHOW = "com.dlmaster.island.SHOW"
        private const val CH_ID = "island_ch"
        private const val NOTIF_ID = 8801

        fun start(ctx: Context) {
            try {
                val i = Intent(ctx, IslandService::class.java).setAction(ACTION_SHOW)
                if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i)
                else ctx.startService(i)
            } catch (_: Throwable) {}
        }

        fun stop(ctx: Context) {
            try { ctx.stopService(Intent(ctx, IslandService::class.java)) }
            catch (_: Throwable) {}
        }

        fun canShow(ctx: Context): Boolean =
            if (Build.VERSION.SDK_INT >= 23) Settings.canDrawOverlays(ctx) else true
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android 8+ 必须 5 秒内 startForeground
        startForegroundSafely()
        addIsland()
        startUpdating()
        return START_STICKY
    }

    private fun startForegroundSafely() {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                if (nm.getNotificationChannel(CH_ID) == null) {
                    val ch = NotificationChannel(
                        CH_ID, "灵动岛",
                        NotificationManager.IMPORTANCE_MIN
                    )
                    ch.setShowBadge(false)
                    nm.createNotificationChannel(ch)
                }
            }
            val notif: Notification = NotificationCompat.Builder(this, CH_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("下载工具")
                .setContentText("灵动岛运行中")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .build()
            startForeground(NOTIF_ID, notif)
        } catch (_: Throwable) {}
    }

    private fun addIsland() {
        if (island != null) return
        if (!canShow(this)) return
        try {
            val wmgr = getSystemService(WINDOW_SERVICE) as WindowManager
            wm = wmgr
            val v = IslandView(this)
            val density = resources.displayMetrics.density
            val p = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                (44 * density).toInt(),
                if (Build.VERSION.SDK_INT >= 26)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )
            p.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            p.x = 0
            p.y = (8 * density).toInt()
            wmgr.addView(v, p)
            island = v
        } catch (_: Throwable) {}
    }

    private fun startUpdating() {
        if (scope == null) {
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        }
        scope?.launch {
            while (true) {
                try { updateIsland() } catch (_: Throwable) {}
                delay(500)
            }
        }
    }

    private fun updateIsland() {
        val v = island ?: return
        val running = DownloadRepository.tasks.firstOrNull {
            it.status == DownloadTask.Status.RUNNING
        }
        if (running != null) {
            v.mode = IslandView.Mode.DOWNLOAD
            v.title = running.fileName.take(16)
            v.subtitle = running.strategy.title
            v.rightText = "${running.progressPercent}%"
            v.progress = running.progressPercent / 100f
            v.iconColor = 0xFF69F0AE.toInt()
            return
        }
        if (MusicPlayer.isPlaying()) {
            v.mode = IslandView.Mode.MUSIC
            v.title = MusicPlayer.currentTrackName()?.take(16) ?: "播放中"
            v.subtitle = "音乐"
            v.rightText = "▶"
            v.progress = 0f
            v.iconColor = 0xFFB980F0.toInt()
            return
        }
        // 无任务停止
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        try { scope?.cancel() } catch (_: Throwable) {}
        scope = null
        try { island?.let { wm?.removeView(it) } } catch (_: Throwable) {}
        island = null
        wm = null
    }
}

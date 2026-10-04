package com.dlmaster.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.dlmaster.download.DownloadRepository
import com.dlmaster.download.DownloadTask
import com.dlmaster.feature.Settings
import com.dlmaster.util.MusicPlayer
import com.dlmaster.view.IslandView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IslandService : Service() {

    private var wm: WindowManager? = null
    private var island: IslandView? = null
    private var scope: CoroutineScope? = null
    private var batteryLevel = 100
    private var isCharging = false
    private var lastRxBytes = 0L
    private var lastRxTime = 0L
    private var netSpeed = ""

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                val lvl = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
                if (lvl >= 0 && scale > 0) batteryLevel = lvl * 100 / scale
                val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                             status == BatteryManager.BATTERY_STATUS_FULL
                island?.batteryLevel = batteryLevel
                island?.isCharging = isCharging
            } catch (_: Throwable) {}
        }
    }

    companion object {
        const val ACTION_SHOW = "com.dlmaster.island.SHOW"
        private const val CH_ID = "island_ch"
        private const val NOTIF_ID = 8801
        fun start(ctx: Context) {
            try {
                val i = Intent(ctx, IslandService::class.java).setAction(ACTION_SHOW)
                if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i) else ctx.startService(i)
            } catch (_: Throwable) {}
        }
        fun stop(ctx: Context) { try { ctx.stopService(Intent(ctx, IslandService::class.java)) } catch (_: Throwable) {} }
        fun canShow(ctx: Context): Boolean = if (Build.VERSION.SDK_INT >= 23) Settings.canDrawOverlays(ctx) else true
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundSafely()
        addIsland()
        try { registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) } catch (_: Throwable) {}
        startUpdating()
        return START_STICKY
    }

    private fun startForegroundSafely() {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                if (nm.getNotificationChannel(CH_ID) == null) {
                    nm.createNotificationChannel(NotificationChannel(CH_ID, "灵动岛", NotificationManager.IMPORTANCE_MIN).apply { setShowBadge(false) })
                }
            }
            val n: Notification = NotificationCompat.Builder(this, CH_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("下载工具").setContentText("灵动岛运行中")
                .setPriority(NotificationCompat.PRIORITY_MIN).setOngoing(true).build()
            startForeground(NOTIF_ID, n)
        } catch (_: Throwable) {}
    }

    private fun addIsland() {
        if (island != null) return
        if (!canShow(this)) return
        try {
            val wmgr = getSystemService(WINDOW_SERVICE) as WindowManager
            wm = wmgr
            val v = IslandView(this)
            v.attach()
            // 读取设置
            try {
                v.showBattery = Settings.islandShowBattery()
                v.showTime = Settings.islandShowTime()
                v.showNet = Settings.islandShowNet()
                v.tapExpand = Settings.islandTapExpand()
            } catch (_: Throwable) {}
            v.onTap = { /* toggle 已在 View 里做了 */ }
            v.onNextTrack = { try { MusicPlayer.next(this); v.title = MusicPlayer.currentTrackName() ?: "播放中" } catch (_: Throwable) {} }
            v.onPlayPause = { try { if (MusicPlayer.isPlaying()) MusicPlayer.pause() else MusicPlayer.resume() } catch (_: Throwable) {} }

            val d = resources.displayMetrics.density
            val p = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                (44 * d).toInt(),
                if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )
            p.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            p.x = 0; p.y = (8 * d).toInt()
            wmgr.addView(v, p)
            island = v
        } catch (_: Throwable) {}
    }

    private fun startUpdating() {
        if (scope == null) scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        scope?.launch {
            val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
            while (true) {
                try {
                    val v = island ?: break
                    v.timeText = fmt.format(Date())
                    v.batteryLevel = batteryLevel
                    v.isCharging = isCharging
                    // 网络速率
                    try {
                        val totalRx = TrafficStats.getTotalRxBytes()
                        val now = System.currentTimeMillis()
                        if (lastRxBytes > 0 && lastRxTime > 0) {
                            val dt = now - lastRxTime
                            if (dt > 0) {
                                val rate = (totalRx - lastRxBytes) * 1000 / dt
                                netSpeed = when {
                                    rate > 1024 * 1024 -> "%.1fMB/s".format(rate / 1024.0 / 1024)
                                    rate > 1024 -> "%.0fKB/s".format(rate / 1024.0)
                                    else -> "${rate}B/s"
                                }
                                v.netSpeed = netSpeed
                            }
                        }
                        lastRxBytes = totalRx
                        lastRxTime = now
                    } catch (_: Throwable) {}

                    // 状态切换
                    val running = DownloadRepository.tasks.firstOrNull { it.status == DownloadTask.Status.RUNNING }
                    when {
                        running != null -> {
                            v.mode = IslandView.Mode.DOWNLOAD
                            v.title = running.fileName
                            v.progress = running.progressPercent / 100f
                            v.iconColor = 0xFF69F0AE.toInt()
                            v.supportRange = running.strategy.threads > 1
                            // 有任务自动 PEEK
                            if (v.state == IslandView.State.COLLAPSED) v.toPeek()
                        }
                        MusicPlayer.isPlaying() -> {
                            v.mode = IslandView.Mode.MUSIC
                            v.title = MusicPlayer.currentTrackName() ?: "播放中"
                            v.iconColor = 0xFFB980F0.toInt()
                            v.progress = 0f
                        }
                        isCharging -> {
                            v.mode = IslandView.Mode.CHARGE
                            v.title = "充电中"
                            v.iconColor = 0xFFFFD54F.toInt()
                        }
                        else -> {
                            v.mode = IslandView.Mode.IDLE
                            v.title = fmt.format(Date())
                            v.iconColor = 0xFF4F7CFF.toInt()
                            v.progress = 0f
                            if (v.state == IslandView.State.EXPANDED) v.toCollapsed()
                        }
                    }
                } catch (_: Throwable) {}
                delay(1000)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(batteryReceiver) } catch (_: Throwable) {}
        try { scope?.cancel() } catch (_: Throwable) {}
        scope = null
        try { island?.detach() } catch (_: Throwable) {}
        try { island?.let { wm?.removeView(it) } } catch (_: Throwable) {}
        island = null; wm = null
    }
}

package com.dlmaster.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
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
    private var params: WindowManager.LayoutParams? = null
    private var scope: CoroutineScope? = null

    companion object {
        const val ACTION_SHOW = "com.dlmaster.island.SHOW"
        const val ACTION_HIDE = "com.dlmaster.island.HIDE"
        fun start(ctx: Context) {
            val i = Intent(ctx, IslandService::class.java).setAction(ACTION_SHOW)
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i)
            else ctx.startService(i)
        }
        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, IslandService::class.java))
        }
        fun canShow(ctx: Context): Boolean =
            if (Build.VERSION.SDK_INT >= 23) Settings.canDrawOverlays(ctx) else true
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        addIsland()
        startUpdating()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_HIDE) { stopSelf(); return START_NOT_STICKY }
        return START_STICKY
    }

    private fun addIsland() {
        try {
            val v = IslandView(this)
            val density = resources.displayMetrics.density
            val p = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                (38 * density).toInt(),
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
            p.y = (10 * density).toInt()
            wm?.addView(v, p)
            island = v
            params = p
        } catch (_: Throwable) {}
    }

    private fun startUpdating() {
        scope?.launch {
            while (true) {
                try { updateIsland() } catch (_: Throwable) {}
                delay(500)
            }
        }
    }

    private fun updateIsland() {
        val v = island ?: return

        // 优先显示下载,其次音乐
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

        // 无任务时隐藏
        try { wm?.removeView(v) } catch (_: Throwable) {}
        island = null
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope?.cancel()
        try { island?.let { wm?.removeView(it) } } catch (_: Throwable) {}
        island = null
    }
}

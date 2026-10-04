package com.dlmaster

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings as SysSettings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.dlmaster.feature.Settings as FeatSettings
import com.dlmaster.service.IslandService
import com.dlmaster.ui.browser.BrowserFragment
import com.dlmaster.ui.download.DownloadFragment
import com.dlmaster.ui.home.HomeFragment
import com.dlmaster.ui.settings.SettingsFragment
import com.dlmaster.util.BackgroundList
import com.dlmaster.util.MusicPlayer
import com.dlmaster.util.PermissionHelper
import com.dlmaster.util.Prefs
import com.dlmaster.view.AuroraBackgroundView
import com.dlmaster.view.ParticleView
import com.dlmaster.view.SimpleNavBar
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private var bg1: ImageView? = null
    private var bg2: ImageView? = null
    private var aurora: AuroraBackgroundView? = null
    private var particles: ParticleView? = null
    private var nav: SimpleNavBar? = null

    private val handler = Handler(Looper.getMainLooper())
    private var currentIdx = -1
    private var ids: IntArray = intArrayOf()
    private var paused = false
    private var useFirst = true
    private var firstLoad = true

    private val bgRunnable = object : Runnable {
        override fun run() {
            if (paused) return
            try { switchBg() } catch (_: Throwable) {}
            handler.postDelayed(this, Prefs.bgIntervalMs(this@MainActivity))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_main) }
        catch (_: Throwable) { finish(); return }

        try { WindowCompat.setDecorFitsSystemWindows(window, false) } catch (_: Throwable) {}
        try { applyFrameRate() } catch (_: Throwable) {}

        try { ids = BackgroundList.RES_IDS } catch (_: Throwable) { ids = intArrayOf() }

        bg1 = findViewById(R.id.iv_bg1)
        bg2 = findViewById(R.id.iv_bg2)
        aurora = findViewById(R.id.aurora)
        particles = findViewById(R.id.particles)
        nav = findViewById(R.id.simple_nav)

        try { PermissionHelper.requestNotifications(this) } catch (_: Throwable) {}
        try {
            if (Prefs.musicEnabled(this)) MusicPlayer.start(applicationContext)
        } catch (_: Throwable) {}

        nav?.onTabSelected = { idx ->
            // 延后一帧执行,避免 Fragment 事务和触摸事件冲突
            handler.post {
                try {
                    val f: Fragment = when (idx) {
                        0 -> HomeFragment()
                        1 -> DownloadFragment()
                        2 -> BrowserFragment()
                        else -> SettingsFragment()
                    }
                    supportFragmentManager.beginTransaction()
                        .setCustomAnimations(
                            R.anim.ios_in, R.anim.ios_out,
                            R.anim.ios_in, R.anim.ios_out
                        )
                        .replace(R.id.fragment_container, f)
                        .commitAllowingStateLoss()
                } catch (_: Throwable) {}
            }
        }

        if (savedInstanceState == null) {
            try {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, HomeFragment())
                    .commitAllowingStateLoss()
            } catch (_: Throwable) {}
        }

        if (!Prefs.noticeShown(this)) {
            handler.postDelayed({
                try { showNotice() } catch (_: Throwable) {}
            }, 1000)
        }

        // 灵动岛:只在用户明确开启时启动
        try {
            if (FeatSettings.islandEnabled() && IslandService.canShow(this)) {
                IslandService.start(this)
            }
        } catch (_: Throwable) {}
    }

    private fun applyFrameRate() {
        try {
            val rate = try { FeatSettings.frameRate() } catch (_: Throwable) { 60 }
            if (Build.VERSION.SDK_INT >= 30) {
                val lp = window.attributes
                lp.preferredRefreshRate = rate.toFloat()
                window.attributes = lp
            }
        } catch (_: Throwable) {}
    }

    override fun onResume() {
        super.onResume()
        paused = false
        try { aurora?.resume() } catch (_: Throwable) {}
        try { particles?.resume() } catch (_: Throwable) {}
        try {
            if (Prefs.bgEnabled(this)) {
                handler.removeCallbacks(bgRunnable)
                switchBg()
                handler.postDelayed(bgRunnable, Prefs.bgIntervalMs(this))
            } else {
                bg1?.setImageDrawable(null)
                bg2?.setImageDrawable(null)
            }
        } catch (_: Throwable) {}
        try {
            if (Prefs.musicEnabled(this) && !MusicPlayer.isPlaying()) {
                MusicPlayer.start(applicationContext)
            }
        } catch (_: Throwable) {}
        try {
            if (FeatSettings.islandEnabled() && IslandService.canShow(this)) {
                IslandService.start(this)
            } else {
                IslandService.stop(this)
            }
        } catch (_: Throwable) {}
    }

    override fun onPause() {
        super.onPause()
        paused = true
        try { aurora?.pause() } catch (_: Throwable) {}
        try { particles?.pause() } catch (_: Throwable) {}
        handler.removeCallbacks(bgRunnable)
    }

    private fun switchBg() {
        if (ids.isEmpty()) return
        if (!Prefs.bgEnabled(this)) return
        val a = bg1 ?: return
        val b = bg2 ?: return
        val next: Int = if (ids.size == 1) 0 else {
            var n: Int
            do { n = Random.nextInt(ids.size) } while (n == currentIdx)
            n
        }
        currentIdx = next
        val target = if (useFirst) a else b
        val current = if (useFirst) b else a
        try {
            if (firstLoad) {
                Glide.with(applicationContext).load(ids[next])
                    .centerCrop()
                    .format(DecodeFormat.PREFER_RGB_565)
                    .override(900, 1600)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(target)
                target.alpha = 1f
                current.alpha = 0f
                firstLoad = false
            } else {
                target.alpha = 0f
                Glide.with(applicationContext).load(ids[next])
                    .centerCrop()
                    .format(DecodeFormat.PREFER_RGB_565)
                    .override(900, 1600)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(target)
                target.postDelayed({
                    try {
                        val fast = AnimationUtils.loadInterpolator(
                            this@MainActivity,
                            android.R.interpolator.fast_out_slow_in
                        )
                        val linear = AnimationUtils.loadInterpolator(
                            this@MainActivity,
                            android.R.interpolator.linear_out_slow_in
                        )
                        target.animate().alpha(1f).setDuration(500)
                            .setInterpolator(fast).start()
                        current.animate().alpha(0f).setDuration(500)
                            .setInterpolator(linear).start()
                    } catch (_: Throwable) {}
                }, 300L)
            }
            useFirst = !useFirst
        } catch (_: Throwable) {}
    }

    fun restartBgSchedule() {
        handler.removeCallbacks(bgRunnable)
        paused = false
        try { switchBg() } catch (_: Throwable) {}
        handler.postDelayed(bgRunnable, Prefs.bgIntervalMs(this))
    }

    fun startIsland() {
        try {
            if (IslandService.canShow(this)) {
                IslandService.start(this)
            } else if (Build.VERSION.SDK_INT >= 23) {
                val i = Intent(
                    SysSettings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(i, 9527)
            }
        } catch (_: Throwable) {}
    }

    fun stopIsland() {
        try { IslandService.stop(this) } catch (_: Throwable) {}
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 9527) {
            try {
                if (FeatSettings.islandEnabled() && IslandService.canShow(this)) {
                    IslandService.start(this)
                }
            } catch (_: Throwable) {}
        }
    }

    private fun showNotice() {
        try {
            val dlg = AlertDialog.Builder(this).create()
            val v = LayoutInflater.from(this).inflate(R.layout.dialog_notice, null)
            dlg.setView(v)
            dlg.setCancelable(false)
            v.findViewById<View>(R.id.btn_notice_ok).setOnClickListener {
                Prefs.setNoticeShown(this, true)
                v.animate().alpha(0f).setDuration(200)
                    .withEndAction { dlg.dismiss() }.start()
            }
            dlg.show()
            try {
                dlg.window?.setBackgroundDrawableResource(android.R.color.transparent)
                dlg.window?.setWindowAnimations(0)
                dlg.window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            } catch (_: Throwable) {}
            v.alpha = 0f
            v.animate().alpha(1f).setDuration(300).start()
        } catch (_: Throwable) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(bgRunnable)
    }
}

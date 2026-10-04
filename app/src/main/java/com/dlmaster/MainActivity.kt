package com.dlmaster
import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.dlmaster.ui.browser.BrowserFragment
import com.dlmaster.ui.download.DownloadFragment
import com.dlmaster.ui.home.HomeFragment
import com.dlmaster.ui.settings.SettingsFragment
import com.dlmaster.util.BackgroundList
import com.dlmaster.util.MusicPlayer
import com.dlmaster.util.PermissionHelper
import com.dlmaster.util.Prefs
import com.dlmaster.view.AuroraBackgroundView
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlin.random.Random
class MainActivity : AppCompatActivity() {
    private lateinit var bg1: ImageView; private lateinit var bg2: ImageView
    private lateinit var aurora: AuroraBackgroundView
    private val handler = Handler(Looper.getMainLooper())
    private var currentIdx = -1; private var ids: IntArray = intArrayOf()
    private var paused = false; private var useFirst = true; private var firstLoad = true
    private val runnable = object : Runnable {
        override fun run() {
            if (paused) return
            switchBg()
            handler.postDelayed(this, Prefs.bgIntervalMs(this@MainActivity))
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ids = BackgroundList.RES_IDS
        bg1 = findViewById(R.id.iv_bg1); bg2 = findViewById(R.id.iv_bg2)
        aurora = findViewById(R.id.aurora)
        PermissionHelper.requestNotifications(this)
        if (Prefs.musicEnabled(this)) MusicPlayer.start(applicationContext)
        val nav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        nav.setOnItemSelectedListener { item ->
            val f: Fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_download -> DownloadFragment()
                R.id.nav_browser -> BrowserFragment()
                else -> SettingsFragment()
            }
            try {
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                        android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.fragment_container, f).commitAllowingStateLoss()
            } catch (_: Throwable) {}
            true
        }
        if (savedInstanceState == null) nav.selectedItemId = R.id.nav_home
        if (!Prefs.noticeShown(this)) handler.postDelayed({ showNotice() }, 1000)
    }
    override fun onResume() {
        super.onResume(); paused = false; aurora.resume()
        if (Prefs.bgEnabled(this)) {
            handler.removeCallbacks(runnable); switchBg()
            handler.postDelayed(runnable, Prefs.bgIntervalMs(this))
        } else { bg1.setImageDrawable(null); bg2.setImageDrawable(null) }
        if (Prefs.musicEnabled(this) && !MusicPlayer.isPlaying()) MusicPlayer.start(applicationContext)
    }
    override fun onPause() {
        super.onPause(); paused = true; aurora.pause()
        handler.removeCallbacks(runnable)
    }
    private fun switchBg() {
        if (ids.isEmpty() || !Prefs.bgEnabled(this)) return
        val next: Int = if (ids.size == 1) 0 else {
            var n: Int
            do { n = Random.nextInt(ids.size) } while (n == currentIdx)
            n
        }
        currentIdx = next
        val target = if (useFirst) bg1 else bg2
        val current = if (useFirst) bg2 else bg1
        try {
            val fast = AnimationUtils.loadInterpolator(this, android.R.interpolator.fast_out_slow_in)
            val linear = AnimationUtils.loadInterpolator(this, android.R.interpolator.linear_out_slow_in)
            if (firstLoad) {
                Glide.with(applicationContext).load(ids[next])
                    .centerCrop().format(DecodeFormat.PREFER_RGB_565).override(900, 1600)
                    .diskCacheStrategy(DiskCacheStrategy.ALL).into(target)
                target.alpha = 1f; current.alpha = 0f; firstLoad = false
            } else {
                target.alpha = 0f
                Glide.with(applicationContext).load(ids[next])
                    .centerCrop().format(DecodeFormat.PREFER_RGB_565).override(900, 1600)
                    .diskCacheStrategy(DiskCacheStrategy.ALL).into(target)
                target.animate().withLayer().alpha(1f).setDuration(700).setInterpolator(fast).start()
                current.animate().withLayer().alpha(0f).setDuration(700).setInterpolator(linear).start()
            }
            useFirst = !useFirst
        } catch (_: Throwable) {}
    }
    fun restartBgSchedule() {
        handler.removeCallbacks(runnable); paused = false; switchBg()
        handler.postDelayed(runnable, Prefs.bgIntervalMs(this))
    }
    private fun showNotice() {
        try {
            val dlg = AlertDialog.Builder(this).create()
            val v = LayoutInflater.from(this).inflate(R.layout.dialog_notice, null)
            dlg.setView(v); dlg.setCancelable(false)
            v.findViewById<View>(R.id.btn_notice_ok).setOnClickListener {
                Prefs.setNoticeShown(this, true); dlg.dismiss()
            }
            dlg.show()
            try {
                dlg.window?.setBackgroundDrawableResource(android.R.color.transparent)
                dlg.window?.setWindowAnimations(R.style.DialogAnim)
                val dm = resources.displayMetrics
                dlg.window?.setLayout((dm.widthPixels * 0.9).toInt(), -2)
            } catch (_: Throwable) {}
        } catch (_: Throwable) {}
    }
    override fun onDestroy() { super.onDestroy(); handler.removeCallbacks(runnable) }
}

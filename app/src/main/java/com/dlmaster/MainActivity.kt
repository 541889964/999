package com.dlmaster
import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var bg1: ImageView
    private lateinit var bg2: ImageView
    private val bgHandler = Handler(Looper.getMainLooper())
    private var currentBgIndex = -1
    private var bgIds: IntArray = intArrayOf()
    private var paused = false
    private var useFirst = true
    private var firstLoad = true

    private val bgSwitchRunnable = object : Runnable {
        override fun run() {
            if (paused) return
            switchNextBg()
            bgHandler.postDelayed(this, Prefs.bgIntervalMs(this@MainActivity))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bgIds = BackgroundList.RES_IDS
        bg1 = findViewById(R.id.iv_bg1)
        bg2 = findViewById(R.id.iv_bg2)
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
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                    .replace(R.id.fragment_container, f)
                    .commitAllowingStateLoss()
            } catch (_: Throwable) {}
            true
        }
        if (savedInstanceState == null) nav.selectedItemId = R.id.nav_home
        if (!Prefs.noticeShown(this)) {
            bgHandler.postDelayed({ showNotice() }, 1400)
        }
    }

    override fun onResume() {
        super.onResume(); paused = false
        if (Prefs.bgEnabled(this)) {
            bgHandler.removeCallbacks(bgSwitchRunnable)
            switchNextBg()
            bgHandler.postDelayed(bgSwitchRunnable, Prefs.bgIntervalMs(this))
        } else {
            bg1.setImageDrawable(null); bg2.setImageDrawable(null)
        }
        if (Prefs.musicEnabled(this) && !MusicPlayer.isPlaying()) MusicPlayer.start(applicationContext)
    }

    override fun onPause() {
        super.onPause(); paused = true
        bgHandler.removeCallbacks(bgSwitchRunnable)
    }

    /** 双 ImageView 交叉淡入，彻底消除黑屏 */
    private fun switchNextBg() {
        if (bgIds.isEmpty()) return
        if (!Prefs.bgEnabled(this)) return
        val next: Int = if (bgIds.size == 1) 0 else {
            var n: Int
            do { n = Random.nextInt(bgIds.size) } while (n == currentBgIndex)
            n
        }
        currentBgIndex = next
        val target = if (useFirst) bg1 else bg2
        val current = if (useFirst) bg2 else bg1
        try {
            if (firstLoad) {
                Glide.with(applicationContext).load(bgIds[next])
                    .centerCrop().format(DecodeFormat.PREFER_RGB_565).override(900, 1600)
                    .diskCacheStrategy(DiskCacheStrategy.NONE).into(target)
                target.alpha = 1f
                current.alpha = 0f
                firstLoad = false
            } else {
                target.alpha = 0f
                Glide.with(applicationContext).load(bgIds[next])
                    .centerCrop().format(DecodeFormat.PREFER_RGB_565).override(900, 1600)
                    .diskCacheStrategy(DiskCacheStrategy.NONE).into(target)
                target.animate().alpha(1f).setDuration(900).start()
                current.animate().alpha(0f).setDuration(900).start()
            }
            useFirst = !useFirst
        } catch (_: Throwable) {}
    }

    fun restartBgSchedule() {
        bgHandler.removeCallbacks(bgSwitchRunnable)
        paused = false
        switchNextBg()
        bgHandler.postDelayed(bgSwitchRunnable, Prefs.bgIntervalMs(this))
    }

    private fun showNotice() {
        try {
            val dlg = AlertDialog.Builder(this).create()
            val v = LayoutInflater.from(this).inflate(R.layout.dialog_notice, null)
            dlg.setView(v)
            dlg.setCancelable(false)
            v.findViewById<View>(R.id.btn_notice_ok).setOnClickListener {
                Prefs.setNoticeShown(this, true)
                dlg.dismiss()
            }
            dlg.show()
            try {
                dlg.window?.setBackgroundDrawableResource(android.R.color.transparent)
                dlg.window?.setWindowAnimations(R.style.DialogAnim)
            } catch (_: Throwable) {}
        } catch (_: Throwable) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        bgHandler.removeCallbacks(bgSwitchRunnable)
    }
}

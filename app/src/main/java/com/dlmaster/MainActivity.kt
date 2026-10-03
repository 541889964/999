package com.dlmaster
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
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
    private lateinit var bg: ImageView
    private val bgHandler = Handler(Looper.getMainLooper())
    private var currentBgIndex = -1
    private var bgIds: IntArray = intArrayOf()
    private var paused = false
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
        bg = findViewById(R.id.iv_background)
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
    }
    override fun onResume() {
        super.onResume(); paused = false
        if (Prefs.bgEnabled(this)) {
            bgHandler.removeCallbacks(bgSwitchRunnable)
            switchNextBg()
            bgHandler.postDelayed(bgSwitchRunnable, Prefs.bgIntervalMs(this))
        } else bg.setImageDrawable(null)
        if (Prefs.musicEnabled(this) && !MusicPlayer.isPlaying()) MusicPlayer.start(applicationContext)
    }
    override fun onPause() {
        super.onPause(); paused = true
        bgHandler.removeCallbacks(bgSwitchRunnable)
    }
    private fun switchNextBg() {
        if (bgIds.isEmpty()) return
        if (!Prefs.bgEnabled(this)) { bg.setImageDrawable(null); return }
        val next: Int = if (bgIds.size == 1) 0 else {
            var n: Int
            do { n = Random.nextInt(bgIds.size) } while (n == currentBgIndex)
            n
        }
        currentBgIndex = next
        try {
            Glide.with(applicationContext)
                .load(bgIds[next])
                .transition(DrawableTransitionOptions.withCrossFade(1000))
                .centerCrop()
                .format(DecodeFormat.PREFER_RGB_565)
                .override(900, 1600)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(false)
                .into(bg)
        } catch (_: Throwable) {}
    }
    override fun onDestroy() {
        super.onDestroy()
        bgHandler.removeCallbacks(bgSwitchRunnable)
    }
}

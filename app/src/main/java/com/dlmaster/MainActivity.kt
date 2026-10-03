package com.dlmaster
import android.os.Bundle
import android.widget.ImageView
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
import com.dlmaster.util.Prefs
import com.google.android.material.bottomnavigation.BottomNavigationView
class MainActivity : AppCompatActivity() {
    private lateinit var bg: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bg = findViewById(R.id.iv_background)
        applyBg()
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
    private fun applyBg() {
        try {
            if (!Prefs.bgEnabled(this)) { bg.setImageDrawable(null); return }
            val ids = BackgroundList.RES_IDS
            if (ids.isEmpty()) return
            val i = Prefs.nextBgIndex(this, ids.size)
            Glide.with(applicationContext)
                .load(ids[i])
                .dontAnimate()
                .centerCrop()
                .format(DecodeFormat.PREFER_RGB_565)
                .override(720, 1280)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(bg)
        } catch (_: Throwable) {}
    }
}

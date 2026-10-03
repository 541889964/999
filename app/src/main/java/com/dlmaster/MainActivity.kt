package com.dlmaster

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.dlmaster.ui.browser.BrowserFragment
import com.dlmaster.ui.download.DownloadFragment
import com.dlmaster.ui.home.HomeFragment
import com.dlmaster.ui.settings.SettingsFragment
import com.dlmaster.util.BackgroundList
import com.dlmaster.util.Prefs
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var bgImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bgImage = findViewById(R.id.iv_background)
        applyBackground()

        bottomNav = findViewById(R.id.bottom_nav)
        bottomNav.setOnItemSelectedListener { item ->
            val f: Fragment = when (item.itemId) {
                R.id.nav_home     -> HomeFragment()
                R.id.nav_download -> DownloadFragment()
                R.id.nav_browser  -> BrowserFragment()
                else              -> SettingsFragment()
            }
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.fade_in, R.anim.fade_out,
                    R.anim.fade_in, R.anim.fade_out
                )
                .replace(R.id.fragment_container, f)
                .commit()
            true
        }
        if (savedInstanceState == null) bottomNav.selectedItemId = R.id.nav_home
    }

    override fun onResume() { super.onResume(); applyBackground() }

    private fun applyBackground() {
        if (!Prefs.isBackgroundEnabled(this)) { bgImage.setImageDrawable(null); return }
        val ids = BackgroundList.RES_IDS
        if (ids.isEmpty()) return
        val idx = Prefs.nextBackgroundIndex(this, ids.size)
        Glide.with(this).load(ids[idx]).centerCrop().into(bgImage)
    }
}

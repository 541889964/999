package com.dlmaster
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.dlmaster.util.PermissionHelper

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        PermissionHelper.requestBase(this)
        try {
            findViewById<ImageView>(R.id.iv_logo).startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.logo_enter))
            findViewById<TextView>(R.id.tv_brand).startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.text_fade_in))
            findViewById<TextView>(R.id.tv_slogan).startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.text_fade_in))
        } catch (_: Throwable) {}
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                startActivity(Intent(this, MainActivity::class.java))
                @Suppress("DEPRECATION")
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            } catch (_: Throwable) {}
            finish()
        }, 600)
    }
}

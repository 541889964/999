package com.dlmaster

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.dlmaster.download.DownloadRepository
import com.dlmaster.sniffer.SniffRepository
import com.dlmaster.util.BackgroundList
import com.dlmaster.util.ImgLoader
import com.dlmaster.util.MusicPlayer
import com.dlmaster.util.PermissionHelper
import com.dlmaster.util.Prefs
import com.dlmaster.view.ParticleView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashActivity : AppCompatActivity() {

    private lateinit var progress: ProgressBar
    private lateinit var status: TextView
    private lateinit var particles: ParticleView
    private lateinit var glow: android.view.View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 请求高刷
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                val lp = window.attributes
                lp.preferredRefreshRate = 120f
                window.attributes = lp
            }
        } catch (_: Throwable) {}

        PermissionHelper.requestBase(this)

        progress = findViewById(R.id.splash_progress)
        status = findViewById(R.id.tv_status)
        particles = findViewById(R.id.splash_particles)
        glow = findViewById(R.id.splash_glow)

        // 启动次数
        val bootCount = Prefs.bootCount(this)
        Prefs.setBootCount(this, bootCount + 1)
        val totalMs = if (bootCount == 0) 10000L else 5000L

        // 入场动画
        try {
            val logo = findViewById<ImageView>(R.id.iv_logo)
            val brand = findViewById<TextView>(R.id.tv_brand)
            val slogan = findViewById<TextView>(R.id.tv_slogan)
            logo.startAnimation(AnimationUtils.loadAnimation(this, R.anim.logo_enter))
            brand.startAnimation(AnimationUtils.loadAnimation(this, R.anim.text_fade_in))
            slogan.startAnimation(AnimationUtils.loadAnimation(this, R.anim.text_fade_in))
        } catch (_: Throwable) {}

        // 光晕呼吸
        try {
            glow.animate().scaleX(1.3f).scaleY(1.3f)
                .setDuration(2000)
                .setInterpolator(android.view.animation.AnimationUtils.loadInterpolator(
                    this, android.R.interpolator.accelerate_decelerate))
                .withEndAction {
                    glow.animate().scaleX(1f).scaleY(1f)
                        .setDuration(2000)
                        .setInterpolator(android.view.animation.AnimationUtils.loadInterpolator(
                            this, android.R.interpolator.accelerate_decelerate))
                        .start()
                }.start()
        } catch (_: Throwable) {}

        // 真实加载 —— 每秒推进 10%
        val startTime = System.currentTimeMillis()
        lifecycleScope.launch {
            val steps = if (bootCount == 0) {
                // 首次启动:完整加载流程
                listOf(
                    500L  to ("正在读取配置…" to 5),
                    900L  to ("正在初始化图片缓存…" to 12),
                    1200L to ("正在恢复下载任务…" to 22),
                    1000L to ("正在恢复嗅探记录…" to 32),
                    1400L to ("正在扫描音乐库…" to 45),
                    1600L to ("正在预加载背景图…" to 60),
                    1400L to ("正在预热网络引擎…" to 75),
                    800L  to ("正在检查权限…" to 85),
                    700L  to ("准备就绪" to 100)
                )
            } else {
                // 后续启动:跳过部分
                listOf(
                    400L  to ("正在读取配置…" to 15),
                    700L  to ("正在恢复状态…" to 40),
                    900L  to ("正在扫描音乐…" to 65),
                    800L  to ("正在预加载背景…" to 85),
                    500L  to ("准备就绪" to 100)
                )
            }

            for ((dur, pair) in steps) {
                val (text, pct) = pair
                withContext(Dispatchers.Main) {
                    status.text = text
                    progress.setProgress(pct, true)
                }
                // 实际做事
                runCatching {
                    when (text) {
                        "正在读取配置…" -> Prefs.init(applicationContext)
                        "正在初始化图片缓存…" -> ImgLoader.init(applicationContext)
                        "正在恢复下载任务…" -> DownloadRepository.init(applicationContext)
                        "正在恢复嗅探记录…" -> SniffRepository.init(applicationContext)
                        "正在扫描音乐库…", "正在扫描音乐…" -> withContext(Dispatchers.IO) {
                            MusicPlayer.rescan(applicationContext)
                        }
                        "正在预加载背景图…", "正在预加载背景…" -> withContext(Dispatchers.IO) {
                            try {
                                ImgLoader.preloadBackgrounds(BackgroundList.RES_IDS.take(4).toIntArray())
                            } catch (_: Throwable) {}
                        }
                        "正在预热网络引擎…" -> {
                            // 触发一次网络栈初始化
                            withContext(Dispatchers.IO) {
                                try {
                                    val client = okhttp3.OkHttpClient()
                                    client.connectionPool.evictAll()
                                } catch (_: Throwable) {}
                            }
                        }
                    }
                }
                delay(dur)
            }

            // 补齐剩余时间(减去已用)
            val elapsed = System.currentTimeMillis() - startTime
            val remain = totalMs - elapsed
            if (remain > 0) delay(remain)

            withContext(Dispatchers.Main) {
                try {
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    @Suppress("DEPRECATION")
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                } catch (_: Throwable) {}
                finish()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        try { particles.pause() } catch (_: Throwable) {}
    }

    override fun onResume() {
        super.onResume()
        try { particles.resume() } catch (_: Throwable) {}
    }
}

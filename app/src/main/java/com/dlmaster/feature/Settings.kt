package com.dlmaster.feature
import android.content.Context
import android.content.SharedPreferences

object Settings {
    private const val NAME = "dl_settings"
    private var sp: SharedPreferences? = null
    fun init(c: Context) { sp = c.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE) }
    private fun prefs() = sp

    fun downloadDir(): String = prefs()?.getString("download_dir", "") ?: ""
    fun setDownloadDir(v: String) { prefs()?.edit()?.putString("download_dir", v)?.apply() }

    fun proxyEnabled(): Boolean = prefs()?.getBoolean("proxy_on", false) ?: false
    fun setProxyEnabled(v: Boolean) { prefs()?.edit()?.putBoolean("proxy_on", v)?.apply() }
    fun proxyUrl(): String = prefs()?.getString("proxy_url", "") ?: ""
    fun setProxyUrl(v: String) { prefs()?.edit()?.putString("proxy_url", v)?.apply() }

    fun speedLimit(): Long = prefs()?.getLong("speed_limit", 0L) ?: 0L
    fun setSpeedLimit(v: Long) { prefs()?.edit()?.putLong("speed_limit", v)?.apply() }

    fun wifiOnly(): Boolean = prefs()?.getBoolean("wifi_only", false) ?: false
    fun setWifiOnly(v: Boolean) { prefs()?.edit()?.putBoolean("wifi_only", v)?.apply() }

    fun night(): Boolean = prefs()?.getBoolean("night", true) ?: true
    fun setNight(v: Boolean) { prefs()?.edit()?.putBoolean("night", v)?.apply() }

    fun clipAuto(): Boolean = prefs()?.getBoolean("clip_auto", true) ?: true
    fun setClipAuto(v: Boolean) { prefs()?.edit()?.putBoolean("clip_auto", v)?.apply() }

    fun userAgent(): String = prefs()?.getString("user_agent", "") ?: ""
    fun setUserAgent(v: String) { prefs()?.edit()?.putString("user_agent", v)?.apply() }

    fun notifySound(): Boolean = prefs()?.getBoolean("notify_sound", true) ?: true
    fun setNotifySound(v: Boolean) { prefs()?.edit()?.putBoolean("notify_sound", v)?.apply() }

    fun confirmDownload(): Boolean = prefs()?.getBoolean("confirm_dl", false) ?: false
    fun setConfirmDownload(v: Boolean) { prefs()?.edit()?.putBoolean("confirm_dl", v)?.apply() }

    fun islandEnabled(): Boolean = prefs()?.getBoolean("island_on", false) ?: false
    fun setIslandEnabled(v: Boolean) { prefs()?.edit()?.putBoolean("island_on", v)?.apply() }

    fun frameRate(): Int = prefs()?.getInt("frame_rate", 60) ?: 60
    fun setFrameRate(v: Int) { prefs()?.edit()?.putInt("frame_rate", v)?.apply() }

    fun themeColor(): Int = prefs()?.getInt("theme_color", 0xFFB88FD8.toInt()) ?: 0xFFB88FD8.toInt()
    fun setThemeColor(v: Int) { prefs()?.edit()?.putInt("theme_color", v)?.apply() }
}

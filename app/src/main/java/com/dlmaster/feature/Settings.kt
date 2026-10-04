package com.dlmaster.feature

import android.content.Context
import android.content.SharedPreferences

object Settings {
    private const val NAME = "dl_settings"
    private var sp: SharedPreferences? = null

    private const val K_DOWNLOAD_DIR = "download_dir"
    private const val K_PROXY_ENABLED = "proxy_on"
    private const val K_PROXY_URL = "proxy_url"
    private const val K_SPEED_LIMIT = "speed_limit"
    private const val K_WIFI_ONLY = "wifi_only"
    private const val K_NIGHT = "night"
    private const val K_CLIP_AUTO = "clip_auto"
    private const val K_UA = "user_agent"
    private const val K_NOTIFY_SOUND = "notify_sound"
    private const val K_CONFIRM_DL = "confirm_dl"

    fun init(c: Context) {
        sp = c.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    }

    private fun prefs(): SharedPreferences? = sp

    fun downloadDir(): String = prefs()?.getString(K_DOWNLOAD_DIR, "") ?: ""
    fun setDownloadDir(v: String) { prefs()?.edit()?.putString(K_DOWNLOAD_DIR, v)?.apply() }

    fun proxyEnabled(): Boolean = prefs()?.getBoolean(K_PROXY_ENABLED, false) ?: false
    fun setProxyEnabled(v: Boolean) { prefs()?.edit()?.putBoolean(K_PROXY_ENABLED, v)?.apply() }

    fun proxyUrl(): String = prefs()?.getString(K_PROXY_URL, "") ?: ""
    fun setProxyUrl(v: String) { prefs()?.edit()?.putString(K_PROXY_URL, v)?.apply() }

    fun speedLimit(): Long = prefs()?.getLong(K_SPEED_LIMIT, 0L) ?: 0L
    fun setSpeedLimit(v: Long) { prefs()?.edit()?.putLong(K_SPEED_LIMIT, v)?.apply() }

    fun wifiOnly(): Boolean = prefs()?.getBoolean(K_WIFI_ONLY, false) ?: false
    fun setWifiOnly(v: Boolean) { prefs()?.edit()?.putBoolean(K_WIFI_ONLY, v)?.apply() }

    fun night(): Boolean = prefs()?.getBoolean(K_NIGHT, true) ?: true
    fun setNight(v: Boolean) { prefs()?.edit()?.putBoolean(K_NIGHT, v)?.apply() }

    fun clipAuto(): Boolean = prefs()?.getBoolean(K_CLIP_AUTO, true) ?: true
    fun setClipAuto(v: Boolean) { prefs()?.edit()?.putBoolean(K_CLIP_AUTO, v)?.apply() }

    fun userAgent(): String = prefs()?.getString(K_UA, "") ?: ""
    fun setUserAgent(v: String) { prefs()?.edit()?.putString(K_UA, v)?.apply() }

    fun notifySound(): Boolean = prefs()?.getBoolean(K_NOTIFY_SOUND, true) ?: true
    fun setNotifySound(v: Boolean) { prefs()?.edit()?.putBoolean(K_NOTIFY_SOUND, v)?.apply() }

    fun confirmDownload(): Boolean = prefs()?.getBoolean(K_CONFIRM_DL, false) ?: false
    fun setConfirmDownload(v: Boolean) { prefs()?.edit()?.putBoolean(K_CONFIRM_DL, v)?.apply() }

    fun frameRate(): Int = prefs()?.getInt("frame_rate", 60) ?: 60
    fun setFrameRate(v: Int) { prefs()?.edit()?.putInt("frame_rate", v)?.apply() }

    fun themeColor(): Int = prefs()?.getInt("theme_color", 0xFFB88FD8.toInt()) ?: 0xFFB88FD8.toInt()
    fun setThemeColor(v: Int) { prefs()?.edit()?.putInt("theme_color", v)?.apply() }
}

package com.dlmaster.feature
import android.content.Context
import android.content.SharedPreferences
object Settings {
    private const val NAME = "dl_settings"
    private lateinit var sp: SharedPreferences
    private val mem = HashMap<String, Any>()
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
        mem[K_DOWNLOAD_DIR] = sp.getString(K_DOWNLOAD_DIR, "")
        mem[K_PROXY_ENABLED] = sp.getBoolean(K_PROXY_ENABLED, false)
        mem[K_PROXY_URL] = sp.getString(K_PROXY_URL, "")
        mem[K_SPEED_LIMIT] = sp.getLong(K_SPEED_LIMIT, 0L)
        mem[K_WIFI_ONLY] = sp.getBoolean(K_WIFI_ONLY, false)
        mem[K_NIGHT] = sp.getBoolean(K_NIGHT, true)
        mem[K_CLIP_AUTO] = sp.getBoolean(K_CLIP_AUTO, true)
        mem[K_UA] = sp.getString(K_UA, "")
        mem[K_NOTIFY_SOUND] = sp.getBoolean(K_NOTIFY_SOUND, true)
        mem[K_CONFIRM_DL] = sp.getBoolean(K_CONFIRM_DL, false)
    }
    fun downloadDir(): String = mem[K_DOWNLOAD_DIR] as? String ?: ""
    fun setDownloadDir(v: String) { mem[K_DOWNLOAD_DIR] = v; sp.edit().putString(K_DOWNLOAD_DIR,v).apply() }
    fun proxyEnabled(): Boolean = mem[K_PROXY_ENABLED] as? Boolean ?: false
    fun setProxyEnabled(v: Boolean) { mem[K_PROXY_ENABLED] = v; sp.edit().putBoolean(K_PROXY_ENABLED,v).apply() }
    fun proxyUrl(): String = mem[K_PROXY_URL] as? String ?: ""
    fun setProxyUrl(v: String) { mem[K_PROXY_URL] = v; sp.edit().putString(K_PROXY_URL,v).apply() }
    fun speedLimit(): Long = mem[K_SPEED_LIMIT] as? Long ?: 0L
    fun setSpeedLimit(v: Long) { mem[K_SPEED_LIMIT] = v; sp.edit().putLong(K_SPEED_LIMIT,v).apply() }
    fun wifiOnly(): Boolean = mem[K_WIFI_ONLY] as? Boolean ?: false
    fun setWifiOnly(v: Boolean) { mem[K_WIFI_ONLY] = v; sp.edit().putBoolean(K_WIFI_ONLY,v).apply() }
    fun night(): Boolean = mem[K_NIGHT] as? Boolean ?: true
    fun setNight(v: Boolean) { mem[K_NIGHT] = v; sp.edit().putBoolean(K_NIGHT,v).apply() }
    fun clipAuto(): Boolean = mem[K_CLIP_AUTO] as? Boolean ?: true
    fun setClipAuto(v: Boolean) { mem[K_CLIP_AUTO] = v; sp.edit().putBoolean(K_CLIP_AUTO,v).apply() }
    fun userAgent(): String = mem[K_UA] as? String ?: ""
    fun setUserAgent(v: String) { mem[K_UA] = v; sp.edit().putString(K_UA,v).apply() }
    fun notifySound(): Boolean = mem[K_NOTIFY_SOUND] as? Boolean ?: true
    fun setNotifySound(v: Boolean) { mem[K_NOTIFY_SOUND] = v; sp.edit().putBoolean(K_NOTIFY_SOUND,v).apply() }
    fun confirmDownload(): Boolean = mem[K_CONFIRM_DL] as? Boolean ?: false
    fun setConfirmDownload(v: Boolean) { mem[K_CONFIRM_DL] = v; sp.edit().putBoolean(K_CONFIRM_DL,v).apply() }
}

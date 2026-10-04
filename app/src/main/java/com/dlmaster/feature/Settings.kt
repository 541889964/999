package com.dlmaster.feature
import android.content.Context
import android.content.SharedPreferences
object Settings {
    private var sp: SharedPreferences? = null
    fun init(c: Context) { sp = c.applicationContext.getSharedPreferences("dl_settings", Context.MODE_PRIVATE) }
    private fun p() = sp
    fun islandEnabled(): Boolean = p()?.getBoolean("island_on", false) ?: false
    fun setIslandEnabled(v: Boolean) { p()?.edit()?.putBoolean("island_on", v)?.apply() }
    fun frameRate(): Int = p()?.getInt("frame_rate", 60) ?: 60
    fun setFrameRate(v: Int) { p()?.edit()?.putInt("frame_rate", v)?.apply() }
    fun adaptiveFrameRate(): Boolean = p()?.getBoolean("adaptive_fr", true) ?: true
    fun setAdaptiveFrameRate(v: Boolean) { p()?.edit()?.putBoolean("adaptive_fr", v)?.apply() }
    fun islandTapExpand(): Boolean = p()?.getBoolean("island_tap", true) ?: true
    fun setIslandTapExpand(v: Boolean) { p()?.edit()?.putBoolean("island_tap", v)?.apply() }
    fun downloadDir(): String = p()?.getString("download_dir", "") ?: ""
    fun setDownloadDir(v: String) { p()?.edit()?.putString("download_dir", v)?.apply() }
    fun wifiOnly(): Boolean = p()?.getBoolean("wifi_only", false) ?: false
    fun setWifiOnly(v: Boolean) { p()?.edit()?.putBoolean("wifi_only", v)?.apply() }
    fun speedLimit(): Long = p()?.getLong("speed_limit", 0L) ?: 0L
    fun setSpeedLimit(v: Long) { p()?.edit()?.putLong("speed_limit", v)?.apply() }

    fun islandShowBattery(): Boolean = p()?.getBoolean("island_batt", true) ?: true
    fun setIslandShowBattery(v: Boolean) { p()?.edit()?.putBoolean("island_batt", v)?.apply() }
    fun islandShowTime(): Boolean = p()?.getBoolean("island_time", true) ?: true
    fun setIslandShowTime(v: Boolean) { p()?.edit()?.putBoolean("island_time", v)?.apply() }
    fun islandShowNet(): Boolean = p()?.getBoolean("island_net", true) ?: true
    fun setIslandShowNet(v: Boolean) { p()?.edit()?.putBoolean("island_net", v)?.apply() }
    fun islandTapExpand(): Boolean = p()?.getBoolean("island_tap", true) ?: true
    fun setIslandTapExpand(v: Boolean) { p()?.edit()?.putBoolean("island_tap", v)?.apply() }
}

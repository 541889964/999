package com.dlmaster.util
import android.content.Context
import android.content.SharedPreferences
object Prefs {
    private lateinit var sp: SharedPreferences
    private val cache = HashMap<String, Any>()
    private val FREQ = longArrayOf(3000, 5000, 10000, 20000, 30000, 60000)
    fun init(ctx: Context) {
        sp = ctx.applicationContext.getSharedPreferences("dlmaster", Context.MODE_PRIVATE)
        cache["bg"] = sp.getBoolean("bg", true)
        cache["music"] = sp.getBoolean("music", true)
        cache["freq_idx"] = sp.getInt("freq_idx", 2).coerceIn(0, FREQ.size - 1)
        cache["clip"] = sp.getBoolean("clip", false)
        cache["notice"] = sp.getBoolean("notice", false)
        cache["boot"] = sp.getInt("boot", 0)
    }
    private fun gb(k: String, d: Boolean) = cache[k] as? Boolean ?: d
    private fun gi(k: String, d: Int) = cache[k] as? Int ?: d
    fun bgEnabled(c: Context) = gb("bg", true)
    fun setBgEnabled(c: Context, v: Boolean) { cache["bg"] = v; sp.edit().putBoolean("bg", v).apply() }
    fun musicEnabled(c: Context) = gb("music", true)
    fun setMusicEnabled(c: Context, v: Boolean) { cache["music"] = v; sp.edit().putBoolean("music", v).apply() }
    fun bgFreqIndex(c: Context) = gi("freq_idx", 2).coerceIn(0, FREQ.size - 1)
    fun setBgFreqIndex(c: Context, i: Int) { val v = i.coerceIn(0, FREQ.size - 1); cache["freq_idx"] = v; sp.edit().putInt("freq_idx", v).apply() }
    fun bgIntervalMs(c: Context) = FREQ[bgFreqIndex(c)]
    fun clipboardEnabled(c: Context) = gb("clip", false)
    fun setClipboardEnabled(c: Context, v: Boolean) { cache["clip"] = v; sp.edit().putBoolean("clip", v).apply() }
    fun noticeShown(c: Context) = gb("notice", false)
    fun setNoticeShown(c: Context, v: Boolean) { cache["notice"] = v; sp.edit().putBoolean("notice", v).apply() }
    fun bootCount(c: Context) = gi("boot", 0)
    fun setBootCount(c: Context, v: Int) { cache["boot"] = v; sp.edit().putInt("boot", v).apply() }
}

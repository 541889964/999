package com.dlmaster.util

import android.content.Context
import android.content.SharedPreferences

/**
 * 预加载 + 内存缓存的 SharedPreferences
 * 关键优化:避免主线程读磁盘
 */
object Prefs {
    private const val NAME = "dlmaster"
    private const val K_BG = "bg"; private const val K_MUSIC = "music"
    private const val K_FREQ_IDX = "freq_idx"
    private const val K_CLIP = "clip"; private const val K_NOTICE = "notice"
    private val FREQ = longArrayOf(3000, 5000, 10000, 20000, 30000, 60000)

    private lateinit var sp: SharedPreferences
    private val cache = HashMap<String, Any>()

    fun init(ctx: Context) {
        sp = ctx.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        // 一次性预加载
        cache[K_BG] = sp.getBoolean(K_BG, true)
        cache[K_MUSIC] = sp.getBoolean(K_MUSIC, true)
        cache[K_FREQ_IDX] = sp.getInt(K_FREQ_IDX, 2).coerceIn(0, FREQ.size - 1)
        cache[K_CLIP] = sp.getBoolean(K_CLIP, false)
        cache[K_NOTICE] = sp.getBoolean(K_NOTICE, false)
    }

    private fun getBool(k: String, def: Boolean) = cache[k] as? Boolean ?: def
    private fun getInt(k: String, def: Int) = cache[k] as? Int ?: def

    fun bgEnabled(c: Context) = getBool(K_BG, true)
    fun setBgEnabled(c: Context, v: Boolean) { cache[K_BG] = v; sp.edit().putBoolean(K_BG, v).apply() }

    fun musicEnabled(c: Context) = getBool(K_MUSIC, true)
    fun setMusicEnabled(c: Context, v: Boolean) { cache[K_MUSIC] = v; sp.edit().putBoolean(K_MUSIC, v).apply() }

    fun bgFreqIndex(c: Context) = getInt(K_FREQ_IDX, 2).coerceIn(0, FREQ.size - 1)
    fun setBgFreqIndex(c: Context, i: Int) {
        val v = i.coerceIn(0, FREQ.size - 1)
        cache[K_FREQ_IDX] = v
        sp.edit().putInt(K_FREQ_IDX, v).apply()
    }
    fun bgIntervalMs(c: Context) = FREQ[bgFreqIndex(c)]

    fun clipboardEnabled(c: Context) = getBool(K_CLIP, false)
    fun setClipboardEnabled(c: Context, v: Boolean) { cache[K_CLIP] = v; sp.edit().putBoolean(K_CLIP, v).apply() }

    fun noticeShown(c: Context) = getBool(K_NOTICE, false)
    fun setNoticeShown(c: Context, v: Boolean) { cache[K_NOTICE] = v; sp.edit().putBoolean(K_NOTICE, v).apply() }
}

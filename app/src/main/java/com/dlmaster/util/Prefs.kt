package com.dlmaster.util
import android.content.Context
object Prefs {
    private const val NAME = "dlmaster"
    private const val K_BG = "bg"; private const val K_MUSIC = "music"
    private const val K_TRANS = "trans"; private const val K_FREQ_IDX = "freq_idx"
    private val FREQ = longArrayOf(3000, 5000, 10000, 20000, 30000, 60000)
    private fun sp(c: Context) = c.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    fun bgEnabled(c: Context) = sp(c).getBoolean(K_BG, true)
    fun setBgEnabled(c: Context, v: Boolean) { sp(c).edit().putBoolean(K_BG, v).apply() }
    fun musicEnabled(c: Context) = sp(c).getBoolean(K_MUSIC, true)
    fun setMusicEnabled(c: Context, v: Boolean) { sp(c).edit().putBoolean(K_MUSIC, v).apply() }
    fun translucent(c: Context) = sp(c).getBoolean(K_TRANS, true)
    fun setTranslucent(c: Context, v: Boolean) { sp(c).edit().putBoolean(K_TRANS, v).apply() }
    fun bgFreqIndex(c: Context) = sp(c).getInt(K_FREQ_IDX, 2).coerceIn(0, FREQ.size - 1)
    fun setBgFreqIndex(c: Context, i: Int) { sp(c).edit().putInt(K_FREQ_IDX, i.coerceIn(0, FREQ.size - 1)).apply() }
    fun bgIntervalMs(c: Context) = FREQ[bgFreqIndex(c)]
}

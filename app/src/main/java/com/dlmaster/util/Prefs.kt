package com.dlmaster.util
import android.content.Context
object Prefs {
    private const val NAME = "dlmaster"
    private const val K_BG = "bg"
    private const val K_MUSIC = "music"
    private fun sp(c: Context) = c.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    fun bgEnabled(c: Context) = sp(c).getBoolean(K_BG, true)
    fun setBgEnabled(c: Context, v: Boolean) { sp(c).edit().putBoolean(K_BG, v).apply() }
    fun musicEnabled(c: Context) = sp(c).getBoolean(K_MUSIC, true)
    fun setMusicEnabled(c: Context, v: Boolean) { sp(c).edit().putBoolean(K_MUSIC, v).apply() }
}

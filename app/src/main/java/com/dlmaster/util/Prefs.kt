package com.dlmaster.util
import android.content.Context
object Prefs {
    private const val NAME = "dlmaster"
    private const val K_BG = "bg"
    private const val K_IDX = "idx"
    private fun sp(c: Context) = c.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    fun bgEnabled(c: Context) = sp(c).getBoolean(K_BG, true)
    fun setBgEnabled(c: Context, v: Boolean) { sp(c).edit().putBoolean(K_BG, v).apply() }
    fun nextBgIndex(c: Context, total: Int): Int {
        if (total <= 0) return 0
        val last = sp(c).getInt(K_IDX, -1)
        val next = (last + 1) % total
        sp(c).edit().putInt(K_IDX, next).apply()
        return next
    }
}

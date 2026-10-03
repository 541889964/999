package com.dlmaster.util

import android.content.Context

object Prefs {
    private const val NAME = "dlmaster_prefs"
    private const val KEY_BG_ENABLED = "bg_enabled"
    private const val KEY_BG_INDEX   = "bg_index"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun isBackgroundEnabled(ctx: Context): Boolean =
        sp(ctx).getBoolean(KEY_BG_ENABLED, true)

    fun setBackgroundEnabled(ctx: Context, enabled: Boolean) {
        sp(ctx).edit().putBoolean(KEY_BG_ENABLED, enabled).apply()
    }

    fun nextBackgroundIndex(ctx: Context, total: Int): Int {
        if (total <= 0) return 0
        val last = sp(ctx).getInt(KEY_BG_INDEX, -1)
        val next = (last + 1) % total
        sp(ctx).edit().putInt(KEY_BG_INDEX, next).apply()
        return next
    }
}

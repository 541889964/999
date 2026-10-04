package com.dlmaster.theme
import android.content.Context
object ThemeManager {
    data class Theme(val key: String, val name: String, val primary: Int, val accent: Int)
    val ALL = listOf(
        Theme("purple", "粉紫", 0xFFB88FD8.toInt(), 0xFFE8A0F8.toInt()),
        Theme("blue",   "海蓝", 0xFF4F7CFF.toInt(), 0xFF00E5C0.toInt()),
        Theme("cyan",   "青绿", 0xFF00C2A8.toInt(), 0xFF6EF0D0.toInt()),
        Theme("orange", "暖橙", 0xFFFF8A4C.toInt(), 0xFFFFC46B.toInt()),
        Theme("mono",   "黑白", 0xFFAAAAAA.toInt(), 0xFFFFFFFF.toInt())
    )
    private fun sp(ctx: Context) = ctx.getSharedPreferences("dl_settings", Context.MODE_PRIVATE)
    fun current(ctx: Context): Theme {
        val key = sp(ctx).getString("theme_key", "purple") ?: "purple"
        return ALL.firstOrNull { it.key == key } ?: ALL[0]
    }
    fun setCurrent(ctx: Context, key: String) { sp(ctx).edit().putString("theme_key", key).apply() }
    fun layoutMode(ctx: Context): String = sp(ctx).getString("layout_mode", "list") ?: "list"
    fun setLayoutMode(ctx: Context, mode: String) { sp(ctx).edit().putString("layout_mode", mode).apply() }
}

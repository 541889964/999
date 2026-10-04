package com.dlmaster.util
import android.app.ActivityManager
import android.content.Context
import android.content.ComponentCallbacks2
object MemoryManager {
    fun install(ctx: Context) {}
    fun onTrimMemory(ctx: Context, level: Int) {
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> { ImgLoader.clearMemory(); ImgLoader.clearDisk() }
            level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE -> ImgLoader.clearMemory()
        }
    }
    fun suggestViewHolderCache(ctx: Context): Int {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo(); am.getMemoryInfo(info)
        val mem = info.totalMem / (1024 * 1024)
        return when { mem >= 4096 -> 32; mem >= 2048 -> 24; mem >= 1024 -> 16; else -> 10 }
    }
}

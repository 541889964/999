package com.dlmaster.util

import android.app.ActivityManager
import android.content.Context
import android.content.ComponentCallbacks2

/**
 * 内存分级管理
 * 关键优化(来自玄音 #7):
 *   onTrimMemory 分级清理,后台回来秒开
 */
object MemoryManager {

    fun install(ctx: Context) {
        // 预留:可注册 ActivityLifecycleCallback
    }

    fun onTrimMemory(ctx: Context, level: Int) {
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                ImgLoader.clearMemory()
                ImgLoader.clearDisk()
            }
            level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE -> {
                ImgLoader.clearMemory()
            }
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                // 保持内存缓存,只清临时
            }
            level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                // 用户切到后台,不必清
            }
        }
    }

    /** 建议的 RecyclerView 缓存数,基于设备内存 */
    fun suggestViewHolderCache(ctx: Context): Int {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memClass = ActivityManager.MemoryInfo().let {
            am.getMemoryInfo(it); it.totalMem / (1024 * 1024)
        }
        return when {
            memClass >= 4096 -> 32
            memClass >= 2048 -> 24
            memClass >= 1024 -> 16
            else -> 10
        }
    }
}

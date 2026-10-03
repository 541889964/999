package com.dlmaster
import android.app.Application
import com.dlmaster.util.ImgLoader
import com.dlmaster.util.MemoryManager
import com.dlmaster.util.Prefs

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)          // 预热 SharedPreferences,避免主线程读
        ImgLoader.init(this)      // 初始化 Glide 缓存池
        MemoryManager.install(this)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        MemoryManager.onTrimMemory(this, level)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        ImgLoader.clearMemory()
    }
}

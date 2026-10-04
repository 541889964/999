package com.dlmaster

import android.app.Application
import com.dlmaster.download.DownloadRepository
import com.dlmaster.feature.Settings
import com.dlmaster.sniffer.SniffRepository
import com.dlmaster.util.ImgLoader
import com.dlmaster.util.MemoryManager
import com.dlmaster.util.Prefs

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        try { Prefs.init(this) } catch (_: Throwable) {}
        try { Settings.init(this) } catch (_: Throwable) {}
        try { ImgLoader.init(this) } catch (_: Throwable) {}
        try { MemoryManager.install(this) } catch (_: Throwable) {}
        try { DownloadRepository.init(this) } catch (_: Throwable) {}
        try { SniffRepository.init(this) } catch (_: Throwable) {}
    }
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        try { MemoryManager.onTrimMemory(this, level) } catch (_: Throwable) {}
    }
    override fun onLowMemory() {
        super.onLowMemory()
        try { ImgLoader.clearMemory() } catch (_: Throwable) {}
    }
}

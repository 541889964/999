package com.dlmaster
import android.app.Application
import com.dlmaster.download.DownloadRepository
import com.dlmaster.sniffer.SniffRepository
import com.dlmaster.util.ImgLoader
import com.dlmaster.util.MemoryManager
import com.dlmaster.util.Prefs
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        com.dlmaster.feature.Settings.init(this)
        ImgLoader.init(this)
        MemoryManager.install(this)
        DownloadRepository.init(this)
        SniffRepository.init(this)
    }
    override fun onTrimMemory(level: Int) { super.onTrimMemory(level); MemoryManager.onTrimMemory(this, level) }
    override fun onLowMemory() { super.onLowMemory(); ImgLoader.clearMemory() }
}

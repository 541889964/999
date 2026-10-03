package com.dlmaster.download
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.concurrent.CopyOnWriteArrayList
object DownloadRepository {
    val tasks = CopyOnWriteArrayList<DownloadTask>()
    val sniffed = CopyOnWriteArrayList<String>()
    private val _live = MutableLiveData<Int>(0)
    val live: LiveData<Int> get() = _live
    fun addTask(t: DownloadTask) { tasks.add(0, t); _live.postValue(tasks.size) }
    fun notifyUpdate() { _live.postValue(tasks.size) }
    fun addSniffed(url: String) { if (!sniffed.contains(url)) sniffed.add(url) }
}

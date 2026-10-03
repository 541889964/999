package com.dlmaster.download
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.concurrent.CopyOnWriteArrayList
object DownloadRepository {
    val tasks = CopyOnWriteArrayList<DownloadTask>()
    private val _live = MutableLiveData<Int>(0)
    val live: LiveData<Int> get() = _live
    fun addTask(t: DownloadTask) { tasks.add(t); _live.postValue(tasks.size) }
    fun notifyUpdate() { _live.postValue(tasks.size) }
}

package com.dlmaster.download
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.json.JSONArray
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
object DownloadRepository {
    private val _tasks = CopyOnWriteArrayList<DownloadTask>()
    val tasks: List<DownloadTask> get() = _tasks
    private val _live = MutableLiveData<Int>(0)
    val live: LiveData<Int> get() = _live
    private var storeFile: File? = null
    private val handler = Handler(Looper.getMainLooper())
    private val saveRunnable = Runnable { doSave() }
    fun init(ctx: Context) { storeFile = File(ctx.filesDir, "tasks.json"); load() }
    fun addTask(t: DownloadTask) { _tasks.add(0, t); _live.postValue(_tasks.size); scheduleSave() }
    fun removeTask(t: DownloadTask) { _tasks.remove(t); _live.postValue(_tasks.size); scheduleSave() }
    fun notifyUpdate() { _live.postValue(_tasks.size); scheduleSave() }
    private fun scheduleSave() { handler.removeCallbacks(saveRunnable); handler.postDelayed(saveRunnable, 500) }
    private fun doSave() {
        val f = storeFile ?: return
        val snap = _tasks.toList()
        Thread {
            try { val arr = JSONArray(); snap.forEach { arr.put(it.toJson()) }; f.writeText(arr.toString()) }
            catch (_: Throwable) {}
        }.start()
    }
    private fun load() {
        val f = storeFile ?: return
        if (!f.exists() || f.length() == 0L) return
        try {
            val arr = JSONArray(f.readText())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val t = DownloadTask.fromJson(o)
                if (t.status == DownloadTask.Status.RUNNING) { t.status = DownloadTask.Status.FAILED; t.speedBytesPerSec = 0L }
                if (t.status == DownloadTask.Status.DONE && t.savedPath.isNotEmpty() && !File(t.savedPath).exists())
                    t.status = DownloadTask.Status.FAILED
                _tasks.add(t)
            }
            val sorted = _tasks.sortedByDescending { it.createdAt }
            _tasks.clear(); _tasks.addAll(sorted)
            _live.postValue(_tasks.size)
        } catch (_: Throwable) {}
    }
}

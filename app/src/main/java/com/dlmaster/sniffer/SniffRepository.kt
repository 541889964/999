package com.dlmaster.sniffer

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.json.JSONArray
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 嗅探结果存储
 *   - 内存 + 磁盘双持久化
 *   - 每次变化 500ms 后异步写盘
 *   - App 启动时读回(最近 100 条)
 */
object SniffRepository {

    private val _items = CopyOnWriteArrayList<SniffedResource>()
    val items: List<SniffedResource> get() = _items

    private val _live = MutableLiveData<Int>(0)
    val live: LiveData<Int> get() = _live

    private var storeFile: File? = null
    private val handler = Handler(Looper.getMainLooper())
    private val saveRunnable = Runnable { doSave() }

    fun init(ctx: Context) {
        storeFile = File(ctx.filesDir, "sniffed.json")
        load()
    }

    fun add(r: SniffedResource) {
        // 同 url 去重
        _items.removeAll { it.url == r.url }
        _items.add(0, r)
        // 最多保留 200 条
        while (_items.size > 200) _items.removeAt(_items.size - 1)
        _live.postValue(_items.size)
        scheduleSave()
    }

    fun addAll(list: List<SniffedResource>) {
        list.forEach { r ->
            _items.removeAll { it.url == r.url }
            _items.add(0, r)
        }
        while (_items.size > 200) _items.removeAt(_items.size - 1)
        _live.postValue(_items.size)
        scheduleSave()
    }

    fun replaceAll(list: List<SniffedResource>) {
        _items.clear()
        _items.addAll(list)
        _live.postValue(_items.size)
        scheduleSave()
    }

    fun clearAll() {
        _items.clear()
        _live.postValue(0)
        scheduleSave()
    }

    fun remove(r: SniffedResource) {
        _items.remove(r)
        _live.postValue(_items.size)
        scheduleSave()
    }

    private fun scheduleSave() {
        handler.removeCallbacks(saveRunnable)
        handler.postDelayed(saveRunnable, 500)
    }

    private fun doSave() {
        val f = storeFile ?: return
        val snap = _items.toList()
        Thread {
            try {
                val arr = JSONArray()
                snap.forEach { arr.put(it.toJson()) }
                f.writeText(arr.toString())
            } catch (_: Throwable) {}
        }.start()
    }

    private fun load() {
        val f = storeFile ?: return
        if (!f.exists() || f.length() == 0L) return
        try {
            val arr = JSONArray(f.readText())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                _items.add(SniffedResource.fromJson(o))
            }
            _live.postValue(_items.size)
        } catch (_: Throwable) {}
    }
}

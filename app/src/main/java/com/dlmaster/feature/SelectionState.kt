package com.dlmaster.feature
import com.dlmaster.download.DownloadTask
object SelectionState {
    val selected = mutableSetOf<String>()
    fun toggle(t: DownloadTask): Boolean {
        return if (selected.contains(t.id)) { selected.remove(t.id); false }
        else { selected.add(t.id); true }
    }
    fun clear() { selected.clear() }
    fun count() = selected.size
}

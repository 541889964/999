package com.dlmaster.util
import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
object RvOptimizer {
    fun config(rv: RecyclerView, ctx: Context) {
        rv.setHasFixedSize(true)
        rv.itemAnimator = null
        val cache = MemoryManager.suggestViewHolderCache(ctx)
        rv.setItemViewCacheSize(cache)
        rv.recycledViewPool.setMaxRecycledViews(0, cache)
        rv.setHasTransientState(false)
        (rv.layoutManager as? LinearLayoutManager)?.initialPrefetchItemCount = 10
        rv.isNestedScrollingEnabled = true
        rv.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
    }
}

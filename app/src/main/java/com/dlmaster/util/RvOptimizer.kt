package com.dlmaster.util

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView 一键优化配置
 */
object RvOptimizer {

    fun config(rv: RecyclerView, ctx: Context) {
        rv.setHasFixedSize(true)
        rv.itemAnimator = null
        rv.setItemViewCacheSize(MemoryManager.suggestViewHolderCache(ctx))
        rv.recycledViewPool.setMaxRecycledViews(0, MemoryManager.suggestViewHolderCache(ctx))
        rv.setHasTransientState(false)   // 背景重绘不影响 RecyclerView
        val lm = rv.layoutManager as? LinearLayoutManager
        lm?.initialPrefetchItemCount = 10
        rv.isNestedScrollingEnabled = true
        rv.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
    }
}

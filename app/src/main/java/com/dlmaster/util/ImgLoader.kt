package com.dlmaster.util

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.dlmaster.R

/**
 * 图片加载统一封装
 * 关键优化(来自玄音):
 *   1) DiskCacheStrategy.ALL - 原图 + 变换后都缓存
 *   2) crossFade 200ms - 避免闪白
 *   3) RGB_565 - 内存省一半
 *   4) 统一 placeholder/error
 */
object ImgLoader {

    private lateinit var appContext: Context
    private var ready = false

    fun init(ctx: Context) {
        appContext = ctx.applicationContext
        ready = true
    }

    /** 背景图:大图,RGB_565,交叉淡入 600ms */
    fun loadBackground(iv: ImageView, resId: Int) {
        if (!ready) return
        try {
            Glide.with(appContext)
                .load(resId)
                .apply(RequestOptions()
                    .format(DecodeFormat.PREFER_RGB_565)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .override(900, 1600)
                    .centerCrop()
                )
                .transition(DrawableTransitionOptions.withCrossFade(600))
                .into(iv)
        } catch (_: Throwable) {}
    }

    /** 列表项缩略图:小图,交叉淡入 200ms,有 placeholder */
    fun loadThumb(iv: ImageView, resId: Int) {
        if (!ready) return
        try {
            Glide.with(appContext)
                .load(resId)
                .apply(RequestOptions()
                    .format(DecodeFormat.PREFER_RGB_565)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_empty_box)
                    .error(R.drawable.ic_empty_box)
                    .centerCrop()
                )
                .transition(DrawableTransitionOptions.withCrossFade(200))
                .into(iv)
        } catch (_: Throwable) {}
    }

    /** 从 URL 加载图标 */
    fun loadUrl(iv: ImageView, url: String) {
        if (!ready || url.isBlank()) return
        try {
            Glide.with(appContext)
                .load(url)
                .apply(RequestOptions()
                    .format(DecodeFormat.PREFER_RGB_565)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_empty_box)
                    .error(R.drawable.ic_empty_box)
                )
                .transition(DrawableTransitionOptions.withCrossFade(200))
                .into(iv)
        } catch (_: Throwable) {}
    }

    fun clearMemory() {
        try { Glide.get(appContext).clearMemory() } catch (_: Throwable) {}
    }

    fun clearDisk() {
        try {
            Thread { Glide.get(appContext).clearDiskCache() }.start()
        } catch (_: Throwable) {}
    }

    /** 预加载若干张背景,滚动/切换时不卡 */
    fun preloadBackgrounds(ids: IntArray) {
        if (!ready) return
        try {
            ids.take(6).forEach { id ->
                Glide.with(appContext)
                    .load(id)
                    .apply(RequestOptions()
                        .format(DecodeFormat.PREFER_RGB_565)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .override(900, 1600)
                        .centerCrop()
                    )
                    .preload()
            }
        } catch (_: Throwable) {}
    }
}

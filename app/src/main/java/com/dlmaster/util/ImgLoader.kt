package com.dlmaster.util
import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
object ImgLoader {
    private var appCtx: Context? = null
    fun init(ctx: Context) { appCtx = ctx.applicationContext }
    fun loadBackground(iv: ImageView, resId: Int) {
        val c = appCtx ?: return
        try {
            Glide.with(c).load(resId)
                .apply(RequestOptions().format(DecodeFormat.PREFER_RGB_565)
                    .diskCacheStrategy(DiskCacheStrategy.ALL).override(900, 1600).centerCrop())
                .transition(DrawableTransitionOptions.withCrossFade(600)).into(iv)
        } catch (_: Throwable) {}
    }
    fun preloadBackgrounds(ids: IntArray) {
        val c = appCtx ?: return
        try {
            ids.take(6).forEach { id ->
                Glide.with(c).load(id).apply(RequestOptions()
                    .format(DecodeFormat.PREFER_RGB_565).diskCacheStrategy(DiskCacheStrategy.ALL)
                    .override(900, 1600).centerCrop()).preload()
            }
        } catch (_: Throwable) {}
    }
    fun clearMemory() { val c = appCtx ?: return; try { Glide.get(c).clearMemory() } catch (_: Throwable) {} }
    fun clearDisk() { val c = appCtx ?: return; try { Thread { Glide.get(c).clearDiskCache() }.start() } catch (_: Throwable) {} }
}

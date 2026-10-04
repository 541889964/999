package com.dlmaster.util
import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.dlmaster.R
object ImgLoader {
    private lateinit var appCtx: Context
    private var ready = false
    fun init(ctx: Context) { appCtx = ctx.applicationContext; ready = true }
    fun loadBackground(iv: ImageView, resId: Int) {
        if (!ready) return
        try {
            Glide.with(appCtx).load(resId)
                .apply(RequestOptions().format(DecodeFormat.PREFER_RGB_565)
                    .diskCacheStrategy(DiskCacheStrategy.ALL).override(900, 1600).centerCrop())
                .transition(DrawableTransitionOptions.withCrossFade(600)).into(iv)
        } catch (_: Throwable) {}
    }
    fun clearMemory() { try { Glide.get(appCtx).clearMemory() } catch (_: Throwable) {} }
    fun clearDisk() { try { Thread { Glide.get(appCtx).clearDiskCache() }.start() } catch (_: Throwable) {} }
}

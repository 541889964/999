package com.dlmaster.feature
import android.content.ClipboardManager
import android.content.Context
object ClipboardWatcher {
    private var last = ""
    private var listener: ClipboardManager.OnPrimaryClipChangedListener? = null
    fun start(ctx: Context, onLink: (String) -> Unit) {
        if (!Settings.clipAuto()) return
        try {
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            stop(ctx)
            listener = ClipboardManager.OnPrimaryClipChangedListener {
                try {
                    val clip = cm.primaryClip ?: return@OnPrimaryClipChangedListener
                    if (clip.itemCount == 0) return@OnPrimaryClipChangedListener
                    val txt = clip.getItemAt(0).coerceToText(ctx).toString().trim()
                    if (txt == last || txt.isBlank()) return@OnPrimaryClipChangedListener
                    last = txt
                    val looks = txt.startsWith("http") || txt.startsWith("magnet:") ||
                                txt.startsWith("thunder://") || txt.contains("pan.baidu")
                    if (looks) onLink(txt)
                } catch (_:Throwable) {}
            }.also { cm.addPrimaryClipChangedListener(it) }
        } catch (_:Throwable) {}
    }
    fun stop(ctx: Context) {
        try {
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            listener?.let { cm.removePrimaryClipChangedListener(it) }
        } catch (_:Throwable) {}
        listener = null
    }
}

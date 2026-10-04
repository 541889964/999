package com.dlmaster.anim
import android.view.View
import android.view.animation.Interpolator
import android.view.animation.OvershootInterpolator
object Anim {
    private val ENTER: Interpolator = OvershootInterpolator(1.0f)
    private val ITEM: Interpolator = OvershootInterpolator(0.85f)
    private val PRESS: Interpolator = OvershootInterpolator(2.4f)
    private val RISE: Interpolator = OvershootInterpolator(0.6f)
    @JvmStatic fun enter(v: View, delay: Long = 0) {
        v.alpha = 0f; v.translationY = 60f; v.scaleX = 0.92f; v.scaleY = 0.92f
        v.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
            .setDuration(540).setStartDelay(delay).setInterpolator(ENTER).start()
    }
    @JvmStatic fun itemEnter(v: View, pos: Int) {
        v.alpha = 0f; v.translationY = 72f; v.scaleX = 0.90f; v.scaleY = 0.90f
        v.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
            .setDuration(600).setStartDelay(kotlin.math.min(pos, 14) * 42L)
            .setInterpolator(ITEM).start()
    }
    @JvmStatic fun press(v: View) {
        v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80)
            .withEndAction {
                v.animate().scaleX(1f).scaleY(1f).setDuration(320)
                    .setInterpolator(PRESS).start()
            }.start()
    }
    @JvmStatic fun rise(v: View, delay: Long = 0) {
        v.alpha = 0f; v.translationY = 14f
        v.animate().alpha(1f).translationY(0f).setDuration(340)
            .setStartDelay(delay).setInterpolator(RISE).start()
    }
    @JvmStatic fun stagger(views: Array<View?>, gap: Long = 70L) {
        views.forEachIndexed { i, v -> v?.let { enter(it, i * gap) } }
    }
}

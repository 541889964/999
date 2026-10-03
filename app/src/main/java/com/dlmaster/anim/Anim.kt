package com.dlmaster.anim

import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.recyclerview.widget.RecyclerView

/**
 * 动画工具
 * 全部基于 ViewPropertyAnimator (走 RenderThread)
 * 不用老 XML Animation / ObjectAnimator
 */
object Anim {

    /** 卡片入场:三属性同帧,540ms Overshoot 1.0 */
    @JvmStatic
    fun enter(v: View, delay: Long = 0) {
        v.alpha = 0f
        v.translationY = 60f
        v.scaleX = 0.92f
        v.scaleY = 0.92f
        v.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(540)
            .setStartDelay(delay)
            .setInterpolator(OvershootInterpolator(1.0f))
            .start()
    }

    /** 列表 3D 倾斜入场:分帧启动 */
    @JvmStatic
    fun staggerIn(rv: RecyclerView, stepMs: Long = 42L, cap: Int = 14) {
        for (i in 0 until rv.childCount) {
            val v = rv.getChildAt(i) ?: continue
            v.alpha = 0f
            v.translationY = 72f
            v.scaleX = 0.90f
            v.scaleY = 0.90f
            v.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(600)
                .setStartDelay(kotlin.math.min(i, cap) * stepMs)
                .setInterpolator(OvershootInterpolator(0.85f))
                .start()
        }
    }

    /** 单 item 入场(用于 RecyclerView onBindViewHolder) */
    @JvmStatic
    fun itemEnter(v: View, pos: Int, stepMs: Long = 42L, cap: Int = 14) {
        v.alpha = 0f
        v.translationY = 72f
        v.scaleX = 0.90f
        v.scaleY = 0.90f
        v.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(600)
            .setStartDelay(kotlin.math.min(pos, cap) * stepMs)
            .setInterpolator(OvershootInterpolator(0.85f))
            .start()
    }

    /** 点击回弹:两段式 (80ms + 280ms) */
    @JvmStatic
    fun press(v: View) {
        v.animate()
            .scaleX(0.90f).scaleY(0.90f)
            .setDuration(80)
            .withEndAction {
                v.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(280)
                    .setInterpolator(OvershootInterpolator(2.6f))
                    .start()
            }
            .start()
    }

    /** 文字上浮 */
    @JvmStatic
    fun rise(v: View, delay: Long = 0) {
        v.alpha = 0f
        v.translationY = 14f
        v.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(340)
            .setStartDelay(delay)
            .setInterpolator(OvershootInterpolator(0.6f))
            .start()
    }

    /** 淡入淡出 */
    @JvmStatic
    fun fadeIn(v: View, dur: Long = 200) {
        v.alpha = 0f
        v.animate().alpha(1f).setDuration(dur).start()
    }

    @JvmStatic
    fun fadeOut(v: View, dur: Long = 150) {
        v.animate().alpha(0f).setDuration(dur).start()
    }
}

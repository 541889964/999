package com.dlmaster.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

/**
 * 骨架屏 shimmer
 * 关键优化(来自玄音文档 #9):
 *   用 ValueAnimator 改 Matrix,不重建 shader
 */
class ShimmerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val matrix = Matrix()
    private var shader: LinearGradient? = null
    private var animator: ValueAnimator? = null
    private var w = 0f; private var h = 0f

    private val lightColors = intArrayOf(
        0x00FFFFFF, 0x40FFFFFF, 0x00FFFFFF
    )
    private val lightPos = floatArrayOf(0f, 0.5f, 1f)

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        this.w = w.toFloat(); this.h = h.toFloat()
        shader = LinearGradient(
            -w.toFloat(), 0f, 0f, 0f,
            lightColors, lightPos, Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val s = shader ?: return
        val t = (animator?.animatedValue as? Float) ?: 0f
        matrix.setTranslate(t * w * 2, 0f)
        s.setLocalMatrix(matrix)
        paint.shader = s
        canvas.drawRect(0f, 0f, w, h, paint)
    }

    fun start() {
        if (animator?.isRunning == true) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1400
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { invalidate() }
            start()
        }
    }

    fun stop() {
        animator?.cancel(); animator = null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow(); start()
    }

    override fun onDetachedFromWindow() {
        stop(); super.onDetachedFromWindow()
    }
}

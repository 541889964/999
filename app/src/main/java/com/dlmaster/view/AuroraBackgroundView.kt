package com.dlmaster.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/**
 * 极光背景
 * 核心参考玄音:3 个 RadialGradient + 慢速正弦运动
 * 关键优化:
 *   1) shader 参数变化 < 1px 时跳过重建
 *   2) setClickable(false) + setFocusable(false),减少事件分发
 *   3) 走 RenderThread,动画本质是 shader 参数变化
 */
class AuroraBackgroundView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var phase = 0f
    private val colors = intArrayOf(
        Color.parseColor("#803D7EFF"),
        Color.parseColor("#4000E5C0"),
        Color.TRANSPARENT
    )
    private val stops = floatArrayOf(0f, 0.5f, 1f)

    private var g1: RadialGradient? = null
    private var g2: RadialGradient? = null
    private var g3: RadialGradient? = null

    private var lastX1 = -1f; private var lastY1 = -1f; private var lastR1 = -1f
    private var lastX2 = -1f; private var lastY2 = -1f; private var lastR2 = -1f
    private var lastX3 = -1f; private var lastY3 = -1f; private var lastR3 = -1f

    private var w = 0f; private var h = 0f

    init {
        setClickable(false)
        setFocusable(false)
        setWillNotDraw(false)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        this.w = w.toFloat()
        this.h = h.toFloat()
        g1 = null; g2 = null; g3 = null   // 强制重建
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (w <= 0 || h <= 0) return
        if (paused) return

        val x1 = w * (0.25f + 0.12f * sin(phase * 6.28f))
        val y1 = h * (0.20f + 0.10f * cos(phase * 6.28f))
        val r1 = w * 0.55f

        val x2 = w * (0.78f + 0.10f * cos(phase * 6.28f + 1f))
        val y2 = h * (0.65f + 0.08f * sin(phase * 6.28f + 1f))
        val r2 = w * 0.45f

        val x3 = w * (0.50f + 0.08f * sin(phase * 6.28f + 2f))
        val y3 = h * (0.85f + 0.06f * cos(phase * 6.28f + 2f))
        val r3 = w * 0.38f

        // shader 缓存:位移大于 1px 才重建
        if (g1 == null || kotlin.math.abs(x1 - lastX1) > 1f ||
            kotlin.math.abs(y1 - lastY1) > 1f || kotlin.math.abs(r1 - lastR1) > 1f) {
            g1 = RadialGradient(x1, y1, r1, colors, stops, Shader.TileMode.CLAMP)
            lastX1 = x1; lastY1 = y1; lastR1 = r1
        }
        if (g2 == null || kotlin.math.abs(x2 - lastX2) > 1f ||
            kotlin.math.abs(y2 - lastY2) > 1f || kotlin.math.abs(r2 - lastR2) > 1f) {
            g2 = RadialGradient(x2, y2, r2, colors, stops, Shader.TileMode.CLAMP)
            lastX2 = x2; lastY2 = y2; lastR2 = r2
        }
        if (g3 == null || kotlin.math.abs(x3 - lastX3) > 1f ||
            kotlin.math.abs(y3 - lastY3) > 1f || kotlin.math.abs(r3 - lastR3) > 1f) {
            g3 = RadialGradient(x3, y3, r3, colors, stops, Shader.TileMode.CLAMP)
            lastX3 = x3; lastY3 = y3; lastR3 = r3
        }

        g1?.let { paint.shader = it; canvas.drawCircle(x1, y1, r1, paint) }
        g2?.let { paint.shader = it; canvas.drawCircle(x2, y2, r2, paint) }
        g3?.let { paint.shader = it; canvas.drawCircle(x3, y3, r3, paint) }

        phase += 0.0025f
        if (phase > 1f) phase -= 1f
        postInvalidateOnAnimation()
    }

    fun pause() { paused = true }
    fun resume() { paused = false; postInvalidateOnAnimation() }
    private var paused = false
}

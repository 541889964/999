package com.dlmaster.view
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
class AuroraBackgroundView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private var phase = 0f
    private val colors = intArrayOf(Color.parseColor("#803D7EFF"), Color.parseColor("#4000E5C0"), Color.TRANSPARENT)
    private val stops = floatArrayOf(0f, 0.5f, 1f)
    private var g1: RadialGradient? = null; private var g2: RadialGradient? = null; private var g3: RadialGradient? = null
    private var lx1 = -1f; private var ly1 = -1f; private var lr1 = -1f
    private var lx2 = -1f; private var ly2 = -1f; private var lr2 = -1f
    private var lx3 = -1f; private var ly3 = -1f; private var lr3 = -1f
    private var w = 0f; private var h = 0f
    private var paused = false
    private var frameSkip = 0
    init { setClickable(false); setFocusable(false); setWillNotDraw(false) }
    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        this.w = w.toFloat(); this.h = h.toFloat()
        g1 = null; g2 = null; g3 = null
    }
    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        if (w <= 0 || h <= 0 || paused) return
        val x1 = w * (0.25f + 0.12f * sin(phase * 6.28f))
        val y1 = h * (0.20f + 0.10f * cos(phase * 6.28f))
        val r1 = w * 0.55f
        val x2 = w * (0.78f + 0.10f * cos(phase * 6.28f + 1f))
        val y2 = h * (0.65f + 0.08f * sin(phase * 6.28f + 1f))
        val r2 = w * 0.45f
        val x3 = w * (0.50f + 0.08f * sin(phase * 6.28f + 2f))
        val y3 = h * (0.85f + 0.06f * cos(phase * 6.28f + 2f))
        val r3 = w * 0.38f
        if (g1 == null || abs(x1 - lx1) > 1f || abs(y1 - ly1) > 1f || abs(r1 - lr1) > 1f) {
            g1 = RadialGradient(x1, y1, r1, colors, stops, Shader.TileMode.CLAMP); lx1 = x1; ly1 = y1; lr1 = r1
        }
        if (g2 == null || abs(x2 - lx2) > 1f || abs(y2 - ly2) > 1f || abs(r2 - lr2) > 1f) {
            g2 = RadialGradient(x2, y2, r2, colors, stops, Shader.TileMode.CLAMP); lx2 = x2; ly2 = y2; lr2 = r2
        }
        if (g3 == null || abs(x3 - lx3) > 1f || abs(y3 - ly3) > 1f || abs(r3 - lr3) > 1f) {
            g3 = RadialGradient(x3, y3, r3, colors, stops, Shader.TileMode.CLAMP); lx3 = x3; ly3 = y3; lr3 = r3
        }
        g1?.let { paint.shader = it; c.drawCircle(x1, y1, r1, paint) }
        g2?.let { paint.shader = it; c.drawCircle(x2, y2, r2, paint) }
        g3?.let { paint.shader = it; c.drawCircle(x3, y3, r3, paint) }
        phase += 0.0025f
        if (phase > 1f) phase -= 1f
        frameSkip = (frameSkip + 1) % 2
        if (frameSkip == 0) postInvalidateOnAnimation() else postInvalidate()
    }
    fun pause() { paused = true }
    fun resume() { paused = false; postInvalidateOnAnimation() }
}

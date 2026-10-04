package com.dlmaster.view
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.random.Random
class ParticleView @JvmOverloads constructor(c: Context, a: AttributeSet? = null) : View(c, a) {
    private class P { var x = 0f; var y = 0f; var r = 1f; var vy = 0f; var vx = 0f; var alpha = 0f; var alphaDir = 1f }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val list = ArrayList<P>()
    private var w = 0f; private var h = 0f; private var paused = false; private var frameSkip = 0
    private val count get() = if (w * h > 800 * 1600) 24 else 16
    init { setClickable(false); setFocusable(false); setWillNotDraw(false) }
    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh); this.w = w.toFloat(); this.h = h.toFloat()
        list.clear(); repeat(count) { list.add(spawn(true)) }
    }
    private fun spawn(anywhere: Boolean): P {
        val p = P()
        p.x = Random.nextFloat() * w; p.y = if (anywhere) Random.nextFloat() * h else h + Random.nextFloat() * 40f
        p.r = 1f + Random.nextFloat() * 1.8f
        p.vy = -(0.4f + Random.nextFloat() * 1.2f); p.vx = (Random.nextFloat() - 0.5f) * 0.3f
        p.alpha = 0.15f + Random.nextFloat() * 0.45f; p.alphaDir = 1f
        return p
    }
    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        if (w <= 0 || h <= 0 || paused) return
        for (p in list) {
            paint.alpha = (p.alpha * 255).toInt().coerceIn(0, 255)
            c.drawCircle(p.x, p.y, p.r, paint)
            p.y += p.vy; p.x += p.vx; p.alpha += p.alphaDir * 0.004f
            if (p.alpha > 0.7f) { p.alpha = 0.7f; p.alphaDir = -1f }
            if (p.alpha < 0.1f) { p.alpha = 0.1f; p.alphaDir = 1f }
            if (p.y < -20f || abs(p.x - w / 2) > w) {
                p.x = Random.nextFloat() * w; p.y = h + Random.nextFloat() * 40f
                p.r = 1f + Random.nextFloat() * 1.8f
                p.vy = -(0.4f + Random.nextFloat() * 1.2f); p.vx = (Random.nextFloat() - 0.5f) * 0.3f
                p.alpha = 0.15f + Random.nextFloat() * 0.45f
            }
        }
        frameSkip = (frameSkip + 1) % 3
        if (frameSkip == 0) postInvalidateOnAnimation() else postInvalidate()
    }
    fun pause() { paused = true }
    fun resume() { paused = false; postInvalidateOnAnimation() }
}

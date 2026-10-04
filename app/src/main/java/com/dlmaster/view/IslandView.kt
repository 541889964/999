package com.dlmaster.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import kotlin.math.sin

class IslandView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val density = resources.displayMetrics.density
    private var expandProgress = 1f
    private var expandSpring: SpringAnimation? = null
    private var iconScale = 1f
    private var iconSpring: SpringAnimation? = null
    private var glowAlpha = 0f
    private var phase = 0f
    private var wavePhase = 0f
    private var scanPhase = 0f
    private var paused = false

    var title: String = ""
        set(v) { if (field != v) { field = v; invalidate() } }
    var subtitle: String = ""
        set(v) { if (field != v) { field = v; invalidate() } }
    var rightText: String = ""
        set(v) { if (field != v) { field = v; invalidate() } }
    var progress: Float = 0f
        set(v) { field = v.coerceIn(0f, 1f); invalidate() }
    var mode: Mode = Mode.IDLE
        set(v) { if (field != v) { field = v; invalidate() } }
    var iconColor: Int = 0xFF69F0AE.toInt()
        set(v) { field = v; invalidate() }

    enum class Mode { IDLE, DOWNLOAD, MUSIC }

    init { setClickable(false); setFocusable(false); setWillNotDraw(false) }

    fun expand() {
        if (expandProgress >= 0.99f) return
        animateExpandTo(1f); animateIconTo(1.10f)
    }
    fun collapse() {
        if (expandProgress <= 0.01f) return
        animateExpandTo(0f); animateIconTo(1.00f)
    }
    private fun animateExpandTo(target: Float) {
        expandSpring?.cancel()
        val holder = FloatValueHolder(expandProgress)
        val anim = SpringAnimation(holder)
        anim.spring = SpringForce(target).apply {
            stiffness = if (target > 0.5f) 220f else 280f
            dampingRatio = if (target > 0.5f) 0.78f else 0.85f
        }
        anim.addUpdateListener { _, value, _ ->
            expandProgress = value.coerceIn(0f, 1f)
            glowAlpha = ((expandProgress - 0.3f) / 0.7f).coerceIn(0f, 1f)
            invalidate()
        }
        anim.start(); expandSpring = anim
    }
    private fun animateIconTo(target: Float) {
        iconSpring?.cancel()
        val holder = FloatValueHolder(iconScale)
        val anim = SpringAnimation(holder)
        anim.spring = SpringForce(target).apply { stiffness = 400f; dampingRatio = 0.55f }
        anim.addUpdateListener { _, value, _ ->
            iconScale = value.coerceIn(0.8f, 1.3f); invalidate()
        }
        anim.start(); iconSpring = anim
    }

    fun pause() { paused = true }
    fun resume() { paused = false; postInvalidateOnAnimation() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (paused) return
        val vw = width.toFloat(); val vh = height.toFloat()
        if (vw <= 0 || vh <= 0) return

        val expandedW = vw * 0.92f
        val collapsedW = 110f * density
        val currentW = collapsedW + (expandedW - collapsedW) * expandProgress
        val cx = vw / 2f; val cy = vh / 2f
        val r = vh / 2f - 1.5f * density
        val left = cx - currentW / 2f; val right = cx + currentW / 2f
        val top = cy - r; val bottom = cy + r

        if (glowAlpha > 0.02f) {
            val glowRadius = currentW * 0.75f
            val gColors = intArrayOf((iconColor and 0x00FFFFFF) or 0x40000000, 0x00000000)
            val breathe = 1f + 0.06f * sin(phase * 0.8f)
            val grad = RadialGradient(cx, cy, glowRadius * breathe,
                gColors, floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
            paint.style = Paint.Style.FILL; paint.shader = grad
            paint.alpha = (glowAlpha * 180).toInt().coerceIn(0, 255)
            canvas.drawCircle(cx, cy, glowRadius * breathe, paint)
            paint.shader = null
        }

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#FF000000")
        paint.alpha = 255
        val bg = RectF(left, top, right, bottom)
        canvas.drawRoundRect(bg, r, r, paint)

        if (expandProgress > 0.5f) {
            scanPhase += 0.008f; if (scanPhase > 1f) scanPhase -= 1f
            val scanX = left + currentW * scanPhase
            val grad2 = LinearGradient(scanX - 40f * density, 0f, scanX + 40f * density, 0f,
                intArrayOf(0x00FFFFFF, 0x18FFFFFF, 0x00FFFFFF), floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP)
            paint.style = Paint.Style.FILL; paint.shader = grad2
            paint.alpha = 255
            canvas.drawRoundRect(bg, r, r, paint)
            paint.shader = null
        }

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.6f * density
        paint.color = Color.parseColor("#1AFFFFFF")
        paint.alpha = 255
        canvas.drawRoundRect(bg, r, r, paint)

        val contentAlpha = ((expandProgress - 0.4f) / 0.6f).coerceIn(0f, 1f)
        if (contentAlpha <= 0.05f) {
            phase += 0.06f; wavePhase += 0.15f
            if (phase > 100f) phase = 0f; if (wavePhase > 100f) wavePhase = 0f
            postInvalidateOnAnimation(); return
        }

        val iconBaseR = r * 0.62f
        val iconR = iconBaseR * iconScale
        val iconCx = left + r * 0.72f + iconBaseR * 0.15f
        val iconCy = cy

        val igrad = RadialGradient(iconCx, iconCy, iconR * 1.8f,
            intArrayOf((iconColor and 0x00FFFFFF) or 0x60FFFFFF.toInt(), 0x00FFFFFF),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        paint.style = Paint.Style.FILL; paint.shader = igrad
        paint.alpha = (contentAlpha * 120).toInt().coerceIn(0, 255)
        canvas.drawCircle(iconCx, iconCy, iconR * 1.8f, paint)
        paint.shader = null

        paint.style = Paint.Style.FILL
        paint.color = iconColor
        paint.alpha = (contentAlpha * 255).toInt()
        canvas.drawCircle(iconCx, iconCy, iconR, paint)

        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.8f * density
        paint.strokeCap = Paint.Cap.ROUND
        paint.alpha = (contentAlpha * 255).toInt()
        when (mode) {
            Mode.DOWNLOAD -> {
                val s = iconR * 0.55f
                canvas.drawLine(iconCx, iconCy - s, iconCx, iconCy + s * 0.7f, paint)
                canvas.drawLine(iconCx - s * 0.6f, iconCy + s * 0.1f, iconCx, iconCy + s * 0.7f, paint)
                canvas.drawLine(iconCx + s * 0.6f, iconCy + s * 0.1f, iconCx, iconCy + s * 0.7f, paint)
            }
            Mode.MUSIC -> {
                for (i in -1..1) {
                    val bx = iconCx + i * 3.5f * density
                    val bh = (4f + 3f * sin(wavePhase * 2f + i)) * density
                    canvas.drawLine(bx, iconCy - bh / 2f, bx, iconCy + bh / 2f, paint)
                }
            }
            Mode.IDLE -> {}
        }

        paint.style = Paint.Style.FILL
        paint.alpha = (contentAlpha * 255).toInt()
        paint.color = Color.WHITE
        paint.textSize = 12f * density
        paint.textAlign = Paint.Align.LEFT
        val textLeft = iconCx + iconBaseR + 8f * density
        val textBaseline = cy + paint.textSize * 0.35f
        val displayTitle = if (title.length > 14) title.take(13) + "…" else title
        canvas.drawText(displayTitle, textLeft, textBaseline, paint)

        if (rightText.isNotEmpty()) {
            paint.textSize = 12f * density
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(rightText, right - r * 0.7f, textBaseline, paint)
        }

        if (mode == Mode.DOWNLOAD && progress > 0f && expandProgress > 0.7f) {
            val barH = 1.8f * density
            val barTop = bottom - barH - 4f * density
            val barLeft = iconCx + iconBaseR + 8f * density
            val barRight = right - r * 0.7f
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#30FFFFFF")
            paint.alpha = (contentAlpha * 255).toInt()
            canvas.drawRoundRect(RectF(barLeft, barTop, barRight, barTop + barH),
                barH / 2f, barH / 2f, paint)
            paint.color = iconColor
            val fillRight = barLeft + (barRight - barLeft) * progress
            canvas.drawRoundRect(RectF(barLeft, barTop, fillRight, barTop + barH),
                barH / 2f, barH / 2f, paint)
        }

        phase += 0.06f; wavePhase += 0.15f
        if (phase > 100f) phase = 0f; if (wavePhase > 100f) wavePhase = 0f
        postInvalidateOnAnimation()
    }
}

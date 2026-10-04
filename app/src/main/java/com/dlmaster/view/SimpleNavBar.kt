package com.dlmaster.view
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
class SimpleNavBar @JvmOverloads constructor(c: Context, a: AttributeSet? = null) : View(c, a) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    var onTabSelected: ((Int) -> Unit)? = null
    var selectedIndex: Int = 0
        set(v) { if (field != v) { field = v; invalidate(); try { onTabSelected?.invoke(v) } catch (_: Throwable) {} } }
    var primaryColor: Int = 0xFFB88FD8.toInt()
        set(v) { field = v; invalidate() }
    var accentColor: Int = 0xFFE8A0F8.toInt()
        set(v) { field = v; invalidate() }
    private var pressed = false
    private val labels = arrayOf("首页", "任务", "浏览", "设置")
    private val density = resources.displayMetrics.density
    init { isClickable = true; isFocusable = true }
    private fun drawHome(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        paint.color = color; paint.style = Paint.Style.FILL
        path.reset(); path.moveTo(cx, cy - s * 0.9f); path.lineTo(cx - s * 0.9f, cy)
        path.lineTo(cx + s * 0.9f, cy); path.close(); c.drawPath(path, paint)
        c.drawRect(cx - s * 0.6f, cy, cx + s * 0.6f, cy + s * 0.8f, paint)
    }
    private fun drawTask(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        paint.color = color; paint.style = Paint.Style.STROKE
        paint.strokeWidth = density * 2f; paint.strokeCap = Paint.Cap.ROUND
        c.drawLine(cx, cy - s * 0.9f, cx, cy + s * 0.3f, paint)
        path.reset(); path.moveTo(cx - s * 0.5f, cy - s * 0.1f); path.lineTo(cx, cy + s * 0.3f)
        path.lineTo(cx + s * 0.5f, cy - s * 0.1f); c.drawPath(path, paint)
        c.drawLine(cx - s * 0.8f, cy + s * 0.9f, cx + s * 0.8f, cy + s * 0.9f, paint)
    }
    private fun drawBrowser(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        paint.color = color; paint.style = Paint.Style.STROKE; paint.strokeWidth = density * 2f
        c.drawCircle(cx, cy, s * 0.9f, paint)
        c.drawLine(cx, cy - s * 0.9f, cx, cy + s * 0.9f, paint)
        c.drawLine(cx - s * 0.9f, cy, cx + s * 0.9f, cy, paint)
    }
    private fun drawSettings(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        paint.color = color; paint.style = Paint.Style.FILL
        val r = s * 0.4f; val o = s * 0.9f
        for (i in 0 until 6) {
            val a = i * Math.PI / 3.0
            c.drawCircle(cx + (o * cos(a)).toFloat(), cy + (o * sin(a)).toFloat(), r * 0.6f, paint)
        }
        c.drawCircle(cx, cy, r * 0.9f, paint)
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0 || h <= 0) return
        paint.style = Paint.Style.FILL; paint.color = Color.parseColor("#E6261438")
        val rf = RectF(0f, 0f, w, h); val radius = h / 2f
        canvas.drawRoundRect(rf, radius, radius, paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = density * 1.2f
        paint.color = primaryColor; paint.alpha = 180
        canvas.drawRoundRect(rf, radius, radius, paint); paint.alpha = 255
        val tw = w / 4f; val ic = 11f * density
        for (i in 0 until 4) {
            val cx = tw * i + tw / 2f; val cy = h * 0.42f; val sel = (i == selectedIndex)
            if (sel) {
                paint.style = Paint.Style.FILL; paint.color = accentColor; paint.alpha = 90
                val hw = tw * 0.38f; val hh = h * 0.36f
                canvas.drawRoundRect(RectF(cx - hw, h * 0.12f, cx + hw, h * 0.12f + hh * 2), hh, hh, paint)
                paint.alpha = 255
            }
            val col = if (sel) accentColor else Color.parseColor("#99FFFFFF")
            when (i) {
                0 -> drawHome(canvas, cx, cy, ic, col)
                1 -> drawTask(canvas, cx, cy, ic, col)
                2 -> drawBrowser(canvas, cx, cy, ic, col)
                3 -> drawSettings(canvas, cx, cy, ic, col)
            }
            paint.style = Paint.Style.FILL; paint.color = col
            paint.textSize = 11f * density; paint.textAlign = Paint.Align.CENTER
            canvas.drawText(labels[i], cx, h * 0.86f, paint)
        }
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> { pressed = true; return true }
            MotionEvent.ACTION_UP -> {
                if (pressed) {
                    pressed = false
                    val i = (event.x / (width.toFloat() / 4f)).toInt().coerceIn(0, 3)
                    if (i != selectedIndex) selectedIndex = i
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> { pressed = false; return true }
        }
        return super.onTouchEvent(event)
    }
}

package com.dlmaster.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.sin

/**
 * 灵动岛视图
 * 仿 iQOO 原子通知胶囊
 *   - 黑色圆角胶囊
 *   - 左侧圆形图标
 *   - 中间文字 + 波纹动画
 *   - 右侧百分比/状态
 */
class IslandView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val density = resources.displayMetrics.density
    private var phase = 0f
    private var paused = false

    // 状态
    var title: String = "下载中"
        set(v) { field = v; invalidate() }
    var subtitle: String = ""
        set(v) { field = v; invalidate() }
    var rightText: String = ""
        set(v) { field = v; invalidate() }
    var progress: Float = 0f  // 0.0 - 1.0
        set(v) { field = v.coerceIn(0f, 1f); invalidate() }
    var mode: Mode = Mode.DOWNLOAD
        set(v) { field = v; invalidate() }
    var iconColor: Int = 0xFF69F0AE.toInt()
        set(v) { field = v; invalidate() }

    enum class Mode { DOWNLOAD, MUSIC, CHARGE, IDLE }

    fun pause() { paused = true }
    fun resume() { paused = false; postInvalidateOnAnimation() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (paused) return
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val pad = 3f * density
        val r = (h - pad * 2) / 2f

        // 胶囊背景
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#F0101010")
        val bgRect = RectF(pad, pad, w - pad, h - pad)
        canvas.drawRoundRect(bgRect, r, r, paint)

        // 内层高光边
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f * density
        paint.color = Color.parseColor("#30FFFFFF")
        canvas.drawRoundRect(bgRect, r, r, paint)

        // 左侧图标圆(封面)
        val iconCx = pad + r
        val iconCy = h / 2f
        val iconR = r - 4f * density
        paint.style = Paint.Style.FILL
        paint.color = iconColor
        canvas.drawCircle(iconCx, iconCy, iconR, paint)

        // 图标内部图案(下载箭头/音乐/闪电)
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f * density
        paint.strokeCap = Paint.Cap.ROUND
        when (mode) {
            Mode.DOWNLOAD -> {
                canvas.drawLine(iconCx, iconCy - 6f * density, iconCx, iconCy + 4f * density, paint)
                canvas.drawLine(iconCx - 4f * density, iconCy, iconCx, iconCy + 4f * density, paint)
                canvas.drawLine(iconCx + 4f * density, iconCy, iconCx, iconCy + 4f * density, paint)
            }
            Mode.MUSIC -> {
                // 三条竖线(波纹)
                for (i in -1..1) {
                    val bx = iconCx + i * 4f * density
                    val bh = (6f + 4f * sin(phase * 6f + i)) * density
                    canvas.drawLine(bx, iconCy - bh / 2f, bx, iconCy + bh / 2f, paint)
                }
            }
            Mode.CHARGE -> {
                canvas.drawLine(iconCx + 2f * density, iconCy - 7f * density,
                                iconCx - 2f * density, iconCy, paint)
                canvas.drawLine(iconCx - 2f * density, iconCy,
                                iconCx + 2f * density, iconCy, paint)
                canvas.drawLine(iconCx + 2f * density, iconCy,
                                iconCx - 2f * density, iconCy + 7f * density, paint)
            }
            Mode.IDLE -> { /* 空 */ }
        }

        // 中间文字
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textSize = 13f * density
        paint.textAlign = Paint.Align.LEFT
        val textX = iconCx + iconR + 12f * density
        val midY = h / 2f + paint.textSize / 3f
        canvas.drawText(title, textX, midY, paint)

        // 副标题(小)
        if (subtitle.isNotEmpty()) {
            paint.color = Color.parseColor("#B0FFFFFF")
            paint.textSize = 10f * density
            canvas.drawText(subtitle, textX, midY + 14f * density, paint)
        }

        // 右侧文字
        paint.color = Color.WHITE
        paint.textSize = 14f * density
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(rightText, w - pad - 14f * density, midY, paint)

        // 底部进度线
        if (mode == Mode.DOWNLOAD && progress > 0f) {
            val barY = h - 5f * density
            val barLeft = iconCx + iconR + 12f * density
            val barRight = w - pad - 14f * density
            paint.color = Color.parseColor("#30FFFFFF")
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(RectF(barLeft, barY, barRight, barY + 2f * density),
                                1f * density, 1f * density, paint)
            paint.color = 0xFF69F0AE.toInt()
            val fillRight = barLeft + (barRight - barLeft) * progress
            canvas.drawRoundRect(RectF(barLeft, barY, fillRight, barY + 2f * density),
                                1f * density, 1f * density, paint)
        }

        phase += 0.06f
        if (phase > 100f) phase = 0f
        postInvalidateOnAnimation()
    }
}

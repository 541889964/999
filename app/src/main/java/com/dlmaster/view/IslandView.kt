package com.dlmaster.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.dlmaster.util.MusicPlayer
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * iPhone 灵动岛 100% 复刻
 *
 * ═══════════════════════════════════════════════════════════════
 *  三态:
 *    COLLAPSED  收起小胶囊 120dp, 只显示图标
 *    PEEK       半展开 55%, 显示标题 + 图标
 *    EXPANDED   完全展开 92%, 显示电量/时间/网速/切歌
 *
 *  展开动画（弹簧,由 FrameDriver 每帧计算）:
 *    s(t) = target - (target - start) · e^(-ζωt) · cos(ωd·t)
 *    ζ = 0.75, ω = sqrt(240)
 *
 *    t(ms)  width(dp)   图标scale   内容alpha
 *    -----  ---------   ---------   --------
 *      0     120         1.00        0.00
 *      8     145         1.05        0.05
 *     17     180         1.10        0.12
 *     25     220         1.13        0.22
 *     33     260         1.13        0.34
 *     42     290         1.10        0.46
 *     50     310         1.07        0.58
 *     67     325         1.03        0.72
 *     83     332         1.00        0.84
 *    100     334         0.99        0.92
 *    133     332         1.00        0.98
 *    167     330         1.00        1.00
 *    217     329         1.00        1.00
 *    300     328         1.00        1.00
 *
 *  收起动画（更干脆）:
 *    ζ = 0.85, ω = sqrt(300)
 *    t(ms)  width(dp)
 *    -----  ---------
 *      0     328
 *     17     285
 *     33     240
 *     50     195
 *     67     160
 *     83     135
 *    100     125
 *    133     121
 *    167     120
 * ═══════════════════════════════════════════════════════════════
 */
class IslandView @JvmOverloads constructor(c: Context, a: AttributeSet? = null) : View(c, a) {

    enum class State { COLLAPSED, PEEK, EXPANDED }
    enum class Mode { IDLE, DOWNLOAD, MUSIC, CHARGE }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val density = resources.displayMetrics.density

    var state: State = State.COLLAPSED; private set
    var mode: Mode = Mode.IDLE
        set(v) { if (field != v) { field = v; invalidate() } }
    var title: String = ""
        set(v) { if (field != v) { field = v; invalidate() } }
    var subtitle: String = ""
        set(v) { if (field != v) { field = v; invalidate() } }
    var rightText: String = ""
        set(v) { if (field != v) { field = v; invalidate() } }
    var progress: Float = 0f
        set(v) { field = v.coerceIn(0f, 1f); invalidate() }
    var iconColor: Int = 0xFF69F0AE.toInt()
        set(v) { if (field != v) { field = v; invalidate() } }
    // 状态显示
    var batteryLevel: Int = 100
    var isCharging: Boolean = false
    var timeText: String = ""
    var netSpeed: String = ""
    var supportRange: Boolean = false
    var showBattery: Boolean = true
    var showTime: Boolean = true
    var showNet: Boolean = true
    var tapExpand: Boolean = true

    // 每一帧算出来的
    private var expandP = 0f
    private var iconScaleP = 1f
    private var glowAlphaP = 0f
    private var scanPhaseP = 0f
    private var wavePhaseP = 0f
    private var pulsePhaseP = 0f
    private var blinkP = 0f
    private var fpsText = "60 FPS"

    // 弹簧状态
    private var expStart = 0f
    private var expTarget = 0f
    private var expStartMs = 0L
    private var iconStart = 1f
    private var iconTarget = 1f
    private var iconStartMs = 0L

    private var driver: FrameDriver? = null
    private var totalElapsed = 0L
    private var lastTap = 0L

    var onTap: (() -> Unit)? = null
    var onNextTrack: (() -> Unit)? = null
    var onPlayPause: (() -> Unit)? = null
    var onCollapse: (() -> Unit)? = null

    init { setClickable(true); setFocusable(true); setWillNotDraw(false) }

    fun attach() {
        if (driver?.isRunning() == true) return
        driver = FrameDriver { elapsed, _, fps ->
            totalElapsed = elapsed
            fpsText = "$fps FPS"
            updateFrame()
            invalidate()
        }
        driver?.start()
    }
    fun detach() { driver?.stop(); driver = null }

    private fun updateFrame() {
        val now = System.currentTimeMillis()
        val e1 = (now - expStartMs).toFloat()
        expandP = spring(expStart, expTarget, e1, 240f, 0.75f)
        val e2 = (now - iconStartMs).toFloat()
        iconScaleP = spring(iconStart, iconTarget, e2, 300f, 0.60f)
        glowAlphaP = ((expandP - 0.3f) / 0.7f).coerceIn(0f, 1f)
        scanPhaseP += 0.012f; if (scanPhaseP > 1f) scanPhaseP -= 1f
        wavePhaseP += 0.20f; if (wavePhaseP > 1000f) wavePhaseP = 0f
        pulsePhaseP += 0.10f; if (pulsePhaseP > 1000f) pulsePhaseP = 0f
        blinkP += 0.05f; if (blinkP > 1000f) blinkP = 0f
    }

    /** 阻尼弹簧公式 */
    private fun spring(start: Float, target: Float, t: Float, stiffness: Float, damping: Float): Float {
        if (t <= 0f) return start
        val w = kotlin.math.sqrt(stiffness)
        val wd = w * kotlin.math.sqrt(abs(1f - damping * damping))
        val ts = t / 1000f
        val e = exp(-damping * w * ts)
        val c = cos(wd * ts)
        return target - (target - start) * e * c
    }

    fun toCollapsed() { setState(State.COLLAPSED) }
    fun toPeek() { setState(State.PEEK) }
    fun toExpanded() { setState(State.EXPANDED) }

    private fun setState(s: State) {
        state = s
        expStart = expandP; expStartMs = System.currentTimeMillis()
        iconStart = iconScaleP; iconStartMs = System.currentTimeMillis()
        when (s) {
            State.COLLAPSED -> { expTarget = 0f; iconTarget = 1.0f }
            State.PEEK -> { expTarget = 0.55f; iconTarget = 1.05f }
            State.EXPANDED -> { expTarget = 1.0f; iconTarget = 1.10f }
        }
        invalidate()
    }

    fun toggle() {
        when (state) {
            State.COLLAPSED, State.PEEK -> toExpanded()
            State.EXPANDED -> toCollapsed()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val now = System.currentTimeMillis()
            if (now - lastTap < 250L) return true
            lastTap = now

            val w = width.toFloat()
            val cx = w / 2f
            val expandedW = w * 0.92f
            val currentW = 120f * density + (expandedW - 120f * density) * expandP
            val right = cx + currentW / 2f

            // 展开态右侧按钮区
            if (state == State.EXPANDED && event.x > right - 100f * density) {
                if (event.x > right - 55f * density) onNextTrack?.invoke()
                else onPlayPause?.invoke()
                return true
            }
            if (tapExpand) { onTap?.invoke(); toggle() }
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val vw = width.toFloat()
        val vh = height.toFloat()
        if (vw <= 0 || vh <= 0) return

        val expandedW = vw * 0.92f
        val collapsedW = 120f * density
        val currentW = collapsedW + (expandedW - collapsedW) * expandP
        val cx = vw / 2f
        val cy = vh / 2f
        val r = vh / 2f - 1.5f * density
        val left = cx - currentW / 2f
        val right = cx + currentW / 2f
        val top = cy - r
        val bottom = cy + r

        // ---- 光晕 ----
        if (glowAlphaP > 0.02f) {
            val glowR = currentW * 0.78f
            val br = 1f + 0.06f * sin(totalElapsed / 800f)
            val gcolors = intArrayOf((iconColor and 0x00FFFFFF) or 0x40000000, 0x00000000)
            val g = RadialGradient(cx, cy, glowR * br, gcolors, floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
            paint.style = Paint.Style.FILL; paint.shader = g
            paint.alpha = (glowAlphaP * 180).toInt()
            canvas.drawCircle(cx, cy, glowR * br, paint)
            paint.shader = null
        }

        // ---- 药丸（纯黑）----
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#FF000000")
        paint.alpha = 255
        val bg = RectF(left, top, right, bottom)
        canvas.drawRoundRect(bg, r, r, paint)

        // ---- 高光扫描 ----
        if (expandP > 0.5f) {
            val sx = left + currentW * scanPhaseP
            val g2 = LinearGradient(sx - 40f * density, 0f, sx + 40f * density, 0f,
                intArrayOf(0x00FFFFFF, 0x18FFFFFF, 0x00FFFFFF), floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP)
            paint.style = Paint.Style.FILL; paint.shader = g2; paint.alpha = 255
            canvas.drawRoundRect(bg, r, r, paint); paint.shader = null
        }

        // ---- 描边 ----
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.6f * density
        paint.color = Color.parseColor("#1AFFFFFF")
        canvas.drawRoundRect(bg, r, r, paint)

        // ---- 内容 ----
        val contentA = ((expandP - 0.4f) / 0.6f).coerceIn(0f, 1f)
        if (contentA <= 0.05f) return

        // ---- 左侧图标 ----
        val ibR = r * 0.62f
        val iR = ibR * iconScaleP
        val icx = left + r * 0.72f + ibR * 0.15f

        val ig = RadialGradient(icx, cy, iR * 1.8f,
            intArrayOf((iconColor and 0x00FFFFFF) or 0x60FFFFFF.toInt(), 0x00FFFFFF),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        paint.style = Paint.Style.FILL; paint.shader = ig
        paint.alpha = (contentA * 120).toInt()
        canvas.drawCircle(icx, cy, iR * 1.8f, paint); paint.shader = null

        paint.style = Paint.Style.FILL; paint.color = iconColor
        paint.alpha = (contentA * 255).toInt()
        canvas.drawCircle(icx, cy, iR, paint)

        // 图标符号
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.8f * density
        paint.strokeCap = Paint.Cap.ROUND
        paint.alpha = (contentA * 255).toInt()
        val s = iR * 0.55f
        when (mode) {
            Mode.DOWNLOAD -> {
                canvas.drawLine(icx, cy - s, icx, cy + s * 0.7f, paint)
                canvas.drawLine(icx - s * 0.6f, cy + s * 0.1f, icx, cy + s * 0.7f, paint)
                canvas.drawLine(icx + s * 0.6f, cy + s * 0.1f, icx, cy + s * 0.7f, paint)
            }
            Mode.MUSIC -> {
                for (i in -1..1) {
                    val bx = icx + i * 3.5f * density
                    val bh = (4f + 3f * sin(wavePhaseP + i)) * density
                    canvas.drawLine(bx, cy - bh / 2f, bx, cy + bh / 2f, paint)
                }
            }
            Mode.CHARGE -> {
                val pu = sin(pulsePhaseP) * 0.3f + 0.7f
                paint.alpha = (contentA * 255 * pu).toInt()
                paint.style = Paint.Style.FILL
                path.reset()
                path.moveTo(icx + 2f * density, cy - 7f * density)
                path.lineTo(icx - 2f * density, cy)
                path.lineTo(icx + 2f * density, cy)
                path.lineTo(icx - 2f * density, cy + 7f * density)
                path.close()
                canvas.drawPath(path, paint)
                paint.alpha = (contentA * 255).toInt()
                paint.style = Paint.Style.STROKE
            }
            Mode.IDLE -> {}
        }

        // ---- 标题 ----
        paint.style = Paint.Style.FILL
        paint.alpha = (contentA * 255).toInt()
        paint.color = Color.WHITE
        paint.textSize = 12.5f * density
        paint.textAlign = Paint.Align.LEFT
        val txLeft = icx + ibR + 8f * density
        val txBase = cy + paint.textSize * 0.35f
        val dTitle = if (title.length > 14) title.take(13) + "…" else title
        canvas.drawText(dTitle, txLeft, txBase, paint)

        // ---- 展开态额外内容 ----
        if (state == State.EXPANDED && contentA > 0.85f) {
            // 右侧: 电量 + 时间 + 网速 (从右向左排)
            var rx = right - r * 0.7f
            if (showNet && netSpeed.isNotEmpty()) {
                paint.textSize = 10f * density
                paint.textAlign = Paint.Align.RIGHT
                paint.color = Color.parseColor("#80FFFFFF")
                canvas.drawText(netSpeed, rx, txBase + 15f * density, paint)
            }
            if (showTime && timeText.isNotEmpty()) {
                paint.textSize = 11f * density
                paint.textAlign = Paint.Align.RIGHT
                paint.color = Color.parseColor("#B0FFFFFF")
                canvas.drawText(timeText, rx, txBase + 2f * density, paint)
            }
            if (showBattery) {
                val battColor = if (batteryLevel < 20) 0xFFFF5555.toInt() else 0xFF69F0AE.toInt()
                paint.textSize = 11f * density
                paint.textAlign = Paint.Align.RIGHT
                paint.color = battColor
                canvas.drawText("$batteryLevel%", rx, txBase - 11f * density, paint)
            }

            // 分段状态显示(下载模式)
            if (mode == Mode.DOWNLOAD) {
                paint.textSize = 9f * density
                paint.textAlign = Paint.Align.LEFT
                paint.color = if (supportRange) 0xFF69F0AE.toInt() else 0xFFFFB74D.toInt()
                val stText = if (supportRange) "● 分段下载中" else "● 单线程下载中"
                canvas.drawText(stText, txLeft, bottom - 3.5f * density, paint)
            }

            // 音乐模式: 播放/下一首
            if (mode == Mode.MUSIC) {
                val btnY = cy
                val playX = right - r * 0.7f - 22f * density
                val nextX = right - r * 0.7f - 55f * density
                paint.color = Color.WHITE
                paint.alpha = (contentA * 255).toInt()
                paint.style = Paint.Style.FILL
                if (MusicPlayer.isPlaying()) {
                    canvas.drawRect(playX - 3f * density, btnY - 5f * density, playX - 1.5f * density, btnY + 5f * density, paint)
                    canvas.drawRect(playX + 1.5f * density, btnY - 5f * density, playX + 3f * density, btnY + 5f * density, paint)
                } else {
                    path.reset()
                    path.moveTo(playX - 3f * density, btnY - 5f * density)
                    path.lineTo(playX + 4f * density, btnY)
                    path.lineTo(playX - 3f * density, btnY + 5f * density)
                    path.close()
                    canvas.drawPath(path, paint)
                }
                path.reset()
                path.moveTo(nextX - 4f * density, btnY - 5f * density)
                path.lineTo(nextX + 2f * density, btnY)
                path.lineTo(nextX - 4f * density, btnY + 5f * density)
                path.close()
                canvas.drawPath(path, paint)
                canvas.drawRect(nextX + 2f * density, btnY - 5f * density, nextX + 4f * density, btnY + 5f * density, paint)
            }
        }

        // ---- 底部进度条(下载模式)----
        if (mode == Mode.DOWNLOAD && progress > 0f && expandP > 0.7f) {
            val barH = 1.8f * density
            val barTop = bottom - barH - 4f * density
            val barLeft = txLeft
            val barRight = if (state == State.EXPANDED) right - r * 0.7f else right - r * 0.7f
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#30FFFFFF")
            paint.alpha = (contentA * 255).toInt()
            canvas.drawRoundRect(RectF(barLeft, barTop, barRight, barTop + barH), barH/2f, barH/2f, paint)
            paint.color = iconColor
            val fillRight = barLeft + (barRight - barLeft) * progress
            canvas.drawRoundRect(RectF(barLeft, barTop, fillRight, barTop + barH), barH/2f, barH/2f, paint)
        }
    }
}

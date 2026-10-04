package com.dlmaster.view

import android.os.Build
import android.view.Choreographer
import com.dlmaster.feature.Settings

/**
 * 逐帧引擎
 *
 * Choreographer 每帧回调一次,时间由 GPU 的 vsync 决定:
 *   60Hz  → 16.67ms
 *   90Hz  → 11.11ms
 *   120Hz → 8.33ms
 *   144Hz → 6.94ms
 *
 * 动画不用 ValueAnimator,因为它是"时长驱动",按系统时钟插值,
 * 有可能跳过帧或者画多余的帧。我们要的是严格"每一帧都算一次",
 * 所以直接用 Choreographer。
 */
class FrameDriver(private val onFrame: (elapsedMs: Long, dtMs: Long, fps: Int) -> Unit) {

    private var running = false
    private var startTime = 0L
    private var lastFrame = 0L
    private var fpsAccum = 0
    private var currentFps = 60
    private var lastFpsTime = 0L
    private var targetFps = 60

    private val cb = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            val now = frameTimeNanos / 1_000_000L
            val elapsed = now - startTime
            val dt = if (lastFrame == 0L) 16L else now - lastFrame
            lastFrame = now
            fpsAccum++

            if (now - lastFpsTime >= 500L) {
                val elapsedRange = now - lastFpsTime
                currentFps = if (elapsedRange > 0) (fpsAccum * 1000L / elapsedRange).toInt().coerceIn(1, 240) else 60
                fpsAccum = 0
                lastFpsTime = now
            }

            val minDt = 1000L / targetFps
            if (dt >= minDt - 1 || targetFps >= 120) {
                try { onFrame(elapsed, dt, currentFps) } catch (_: Throwable) {}
            }

            if (running) Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun setTargetFps(fps: Int) { targetFps = fps.coerceIn(30, 144) }

    fun start() {
        if (running) return
        running = true
        startTime = System.nanoTime() / 1_000_000L
        lastFrame = 0L
        lastFpsTime = startTime
        fpsAccum = 0
        try { targetFps = Settings.frameRate() } catch (_: Throwable) {}
        if (Build.VERSION.SDK_INT >= 16) Choreographer.getInstance().postFrameCallback(cb)
    }

    fun stop() {
        running = false
        try { Choreographer.getInstance().removeFrameCallback(cb) } catch (_: Throwable) {}
    }

    fun isRunning() = running
}

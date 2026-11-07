package com.example.a2dgamebox.city_helicopter

import android.graphics.RectF
import android.view.MotionEvent
import kotlin.math.hypot

/** Pointer IDs (not indices) keep simultaneous joystick, collective and fire stable. */
class TouchControls(private val engine: GameEngine, w: Float, h : Float) {
    var width = w
    var height = h
    val stickX get() = 115f
    val stickY get() = height - 160f
    val stickRadius = 77f
    val throttleRect get() = RectF(width - 113f, height - 335f, width - 36f, height - 95f)
    // Calibrated to the visible track in the supplied Range.png after transparent crop.
    val throttleTrackX get() = throttleRect.left + throttleRect.width() * .35f
    val throttleTrackTop get() = throttleRect.top + throttleRect.height() * .10f
    val throttleTrackBottom get() = throttleRect.bottom - throttleRect.height() * .10f

    val fireRect get() = RectF(width - 285f, height - 205f, width - 137f, height - 109f)
    val soundRect get() = RectF(width / 2 + 38f, 18f, width / 2 + 97f, 63f)
    val pauseRect get() = RectF(width / 2 - 27f, 18f, width / 2 + 27f, 63f)
    private var stickPointer = -1
    private var throttlePointer = -1
    private var firePointer = -1
    var knobX = 0f; private set
    var knobY = 0f; private set

    fun reset() {
        stickPointer = -1; throttlePointer = -1; firePointer = -1
        knobX = 0f; knobY = 0f; engine.clearInput()
    }
    fun handle(event: MotionEvent, scale: Float, offsetX: Float, offsetY: Float) {
        val index = event.actionIndex
        val id = event.getPointerId(index)
        fun x(i: Int) = (event.getX(i) - offsetX) / scale
        fun y(i: Int) = (event.getY(i) - offsetY) / scale
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val px = x(index); val py = y(index)
                when {
                    pauseRect.contains(px, py) -> { engine.pause(); reset() }
                    stickPointer < 0 && hypot(px - stickX, py - stickY) < stickRadius + 35 -> { stickPointer = id; moveStick(px, py) }
                    throttlePointer < 0 && throttleRect.contains(px, py) -> { throttlePointer = id; moveThrottle(py) }
                    firePointer < 0 && fireRect.contains(px, py) -> { firePointer = id; engine.firing = true; engine.requestShot() }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val pointer = event.getPointerId(i)
                    if (pointer == stickPointer) moveStick(x(i), y(i))
                    if (pointer == throttlePointer) moveThrottle(y(i))
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (id == stickPointer) { stickPointer = -1; knobX = 0f; knobY = 0f; engine.movement.set(0f, 0f) }
                if (id == throttlePointer) throttlePointer = -1 // Collective stays at selected power.
                if (id == firePointer) { firePointer = -1; engine.firing = false }
            }
            MotionEvent.ACTION_CANCEL -> reset()
        }
    }
    private fun moveStick(x: Float, y: Float) {
        val dx = x - stickX; val dy = y - stickY
        val len = hypot(dx, dy)
        val factor = if (len > stickRadius) stickRadius / len else 1f
        knobX = dx * factor; knobY = dy * factor
        val amount = (len / stickRadius).coerceAtMost(1f)
        if (amount < .10f) engine.movement.set(0f, 0f)
        else engine.movement.set(dx / len * (amount - .10f) / .90f, dy / len * (amount - .10f) / .90f)
    }
    private fun moveThrottle(y: Float) {
        engine.throttle = ((throttleTrackBottom - y) / (throttleTrackBottom - throttleTrackTop)).coerceIn(0f, 1f)
    }
}

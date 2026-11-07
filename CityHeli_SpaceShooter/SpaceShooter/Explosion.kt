package com.example.a2dgamebox.air_shooter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import androidx.core.graphics.scale
import com.example.a2dgamebox.R

class Explosion(
    context: Context,
    var x: Float,
    var y: Float,
    enemyWidth: Int,
    enemyHeight: Int
) {
    private val frames: List<Bitmap>
    private var currentFrame = 0
    private var frameTimer = 0f

    private val frameDuration = 0.05f   // 50 ms per frame

    val isFinished: Boolean
        get() = currentFrame >= frames.size

    init {
        val originalFrames = listOf(
            BitmapFactory.decodeResource(context.resources, R.drawable.explosion1),
            BitmapFactory.decodeResource(context.resources, R.drawable.explosion2),
            BitmapFactory.decodeResource(context.resources, R.drawable.explosion3),
            BitmapFactory.decodeResource(context.resources, R.drawable.explosion4),
            BitmapFactory.decodeResource(context.resources, R.drawable.explosion5),
            BitmapFactory.decodeResource(context.resources, R.drawable.explosion6),
            BitmapFactory.decodeResource(context.resources, R.drawable.explosion7),
            BitmapFactory.decodeResource(context.resources, R.drawable.explosion8),
            BitmapFactory.decodeResource(context.resources, R.drawable.explosion9),
            BitmapFactory.decodeResource(context.resources, R.drawable.explosion10),
            BitmapFactory.decodeResource(context.resources, R.drawable.explosion11),
            BitmapFactory.decodeResource(context.resources, R.drawable.explosion12),
            BitmapFactory.decodeResource(context.resources, R.drawable.explosion13),
            BitmapFactory.decodeResource(context.resources, R.drawable.explosion14)
        )

        frames = originalFrames.map { it.scale(enemyWidth, enemyHeight, false) }
    }

    fun update(dt: Float) {
        if (isFinished) return

        frameTimer += dt

        while (frameTimer >= frameDuration && currentFrame < frames.size) {
            currentFrame++
            frameTimer -= frameDuration
        }
    }

    fun draw(canvas: Canvas) {
        if (currentFrame < frames.size) {
            canvas.drawBitmap(frames[currentFrame], x, y, null)
        }
    }
}
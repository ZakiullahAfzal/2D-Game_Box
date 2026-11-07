package com.example.gamebox2d.air_shooter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import androidx.core.graphics.scale
import com.example.a2dgamebox.R

class Bullet(
    context: Context,
    var x: Float,
    var y: Float,
    var state: Boolean
) {
    private val bitmap: Bitmap
    private val speed = 1800f   // pixels per second
    val width: Int
    val height: Int

    init {
        val screenWidth = context.resources.displayMetrics.widthPixels
        val original = BitmapFactory.decodeResource(context.resources, R.drawable.bullet)

        val scaledWidth = (screenWidth * 0.03f).toInt()
        val scaledHeight = (original.height * scaledWidth) / original.width
        bitmap = original.scale(scaledWidth, scaledHeight)

        width = bitmap.width
        height = bitmap.height
    }

    fun update(dt: Float) {
        if (state) {
            y -= speed * dt
        } else {
            y += speed * dt
        }
    }

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, x - width / 2f, y, null)
    }

    fun getRect(): RectF {
        return RectF(
            x - width / 2f,
            y,
            x + width / 2f,
            y + height
        )
    }
}
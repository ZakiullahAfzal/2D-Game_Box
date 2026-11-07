package com.example.gamebox2d.air_shooter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import androidx.core.graphics.scale
import com.example.a2dgamebox.R

class Boss(private val context: Context) {

    var x = 0f
    var y = -200f
    private var direction = 1f
    var health = 30

    private val bitmap: Bitmap
    val width: Int
    val height: Int

    // speed in pixels per second
    private val enterSpeed = 360f
    private val moveSpeed = 480f

    init {
        val original = BitmapFactory.decodeResource(context.resources, R.drawable.boss)
        val scaleFactor = 0.40
        val scaleWidth = context.resources.displayMetrics.widthPixels * scaleFactor
        val scaleHeight = (original.height * scaleWidth) / original.width

        bitmap = original.scale(scaleWidth.toInt(), scaleHeight.toInt())
        width = bitmap.width
        height = bitmap.height
    }

    fun update(screenWidth: Int, dt: Float) {
        if (y < 100f) {
            y += enterSpeed * dt

            if (y > 100f) {
                y = 100f
            }
        } else {
            x += direction * moveSpeed * dt

            if (x <= 0f) {
                x = 0f
                direction = 1f
            } else if (x + width >= screenWidth) {
                x = (screenWidth - width).toFloat()
                direction = -1f
            }
        }
    }

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, x, y, null)
    }

    fun getRect(): RectF {
        return RectF(x, y, x + width, y + height)
    }

    fun shoot(): List<Bullet> {
        val bullets = mutableListOf<Bullet>()
        val spacing = width / 5f

        for (i in 1..4) {
            bullets.add(Bullet(context, x + i * spacing, y + height, false))
        }

        return bullets
    }
}
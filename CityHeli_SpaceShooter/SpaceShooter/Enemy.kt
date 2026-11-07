package com.example.gamebox2d.air_shooter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import androidx.core.graphics.scale
import com.example.a2dgamebox.R
import kotlin.random.Random

class Enemy(context: Context, screenWidth: Int, enemyType: Int) {

    private val bitmap: Bitmap
    var x: Float
    var y: Float

    private var enemyHealth: Int = 1
    private val speed: Float
    private var width: Int = 0
    private var height: Int = 0

    init {
        var type = R.drawable.enemy
        enemyHealth = 1

        when (enemyType) {
            1 -> {
                type = R.drawable.enemy
                enemyHealth = 1
            }
            2 -> {
                type = R.drawable.start_breaker
                enemyHealth = 2
            }
            3 -> {
                type = R.drawable.star_war
                enemyHealth = 3
            }
            4 -> {
                type = R.drawable.star_ship
                enemyHealth = 3
            }
        }

        val original = BitmapFactory.decodeResource(context.resources, type)

        val scaleFactor = 0.17f
        val scaledWidth = (screenWidth * scaleFactor).toInt()
        val scaledHeight = (original.height * scaledWidth) / original.width

        val scaledBitmap = original.scale(scaledWidth, scaledHeight)

        val rotationAngle = 180f

        val matrix = Matrix()
        matrix.postRotate(rotationAngle)
        bitmap = Bitmap.createBitmap(
            scaledBitmap,
            0,
            0,
            scaledBitmap.width,
            scaledBitmap.height,
            matrix,
            true
        )

        x = Random.nextInt(0, screenWidth - bitmap.width).toFloat()
        y = -bitmap.height.toFloat()

        // speed in pixels per second
        speed = Random.nextInt(300, 720).toFloat()

        width = bitmap.width
        height = bitmap.height
    }

    fun update(dt: Float) {
        y += speed * dt
    }

    fun health(): Boolean {
        enemyHealth--
        return enemyHealth == 0
    }

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, x, y, null)
    }

    fun getRect(): RectF {
        return RectF(x, y, x + bitmap.width, y + bitmap.height)
    }

    fun getWidth(): Int {
        return width
    }

    fun getHeight(): Int {
        return height
    }
}
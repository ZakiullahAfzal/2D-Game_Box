package com.example.a2dgamebox.air_shooter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import androidx.core.graphics.scale

class Player(context: Context, bitmapP: Int) {
    val bitmap : Bitmap
    var x: Float
    var y: Float
    val width: Int
    val height: Int


    init {
        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels


        val original = BitmapFactory.decodeResource(context.resources, bitmapP)
        val scaleFactor = 0.2f // 20% of screen width
        val scaledWidth = (screenWidth * scaleFactor).toInt()
        val scaledHeight = (original.height * scaledWidth) / original.width

        bitmap = original.scale(scaledWidth, scaledHeight)
        width = bitmap.width
        height = bitmap.height

        x = (screenWidth / 2 - width / 2).toFloat()
        y = (screenHeight - height - 100).toFloat()
    }



    fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, x, y, null)
    }

    //  Add this method to allow collision detection
    fun getRect(): RectF {
        return RectF(x, y, x + width, y + height)
    }

}

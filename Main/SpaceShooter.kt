package com.example.a2dgamebox

import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.a2dgamebox.air_shooter.GameView

class SpaceShooter : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var gameView: GameView


    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("gameBox2d",MODE_PRIVATE)
        val state = sharedPreferences.getBoolean("music", true)
        val bitmap = intent.getIntExtra("space_skin_res", 0)
        val type =  intent.getIntExtra("space_skin_type", 0)

        gameView = GameView(this, bitmap, type,state, sharedPreferences)

        setContentView(gameView)

        hideSystemBars()

    }

    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or 4096
    }

    override fun onResume() {
        super.onResume()
        gameView.resume()
    }

    override fun onPause() {
        super.onPause()
        gameView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        gameView.destroy()
    }


}
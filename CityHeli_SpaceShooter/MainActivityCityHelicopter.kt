package com.example.a2dgamebox.city_helicopter

import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.a2dgamebox.R


class MainActivityCityHelicopter : AppCompatActivity() {

    private lateinit var game: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main2)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        volumeControlStream = AudioManager.STREAM_MUSIC
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val (w, h) = getScreenSize()
        game = GameView(this, w, h)
        setContentView(game)
        hideSystemBars()
    }

    private fun getScreenSize(): Pair<Int, Int>{
        return  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds =  this.windowManager.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            val matrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            this.windowManager.defaultDisplay.getRealMetrics(matrics)
            matrics.widthPixels to matrics.heightPixels
        }
    }
    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or 4096
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (::game.isInitialized) game.onHostFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }
    override fun onResume() { super.onResume(); game.onHostResume() }
    override fun onPause() { game.onHostPause(); super.onPause() }
    override fun onDestroy() { game.release(); super.onDestroy() }
}
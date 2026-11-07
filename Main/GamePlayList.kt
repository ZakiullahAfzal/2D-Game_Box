package com.example.a2dgamebox

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.a2dgamebox.city_helicopter.MainActivityCityHelicopter
import com.example.a2dgamebox.databinding.ActivityGamePlayListBinding
import kotlin.system.exitProcess

class GamePlayList : AppCompatActivity() {

    companion object {
        private const val PREFS = "gameBox2d"
        private const val KEY_MUSIC_ENABLED = "music"

    }

    private lateinit var binding: ActivityGamePlayListBinding
    private lateinit var sharedPreferences: SharedPreferences

    private var backgroundMusic: MediaPlayer? = null
    private var musicEnabled: Boolean = true


    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityGamePlayListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        sharedPreferences = getSharedPreferences(PREFS, MODE_PRIVATE)


        musicEnabled = sharedPreferences.getBoolean(KEY_MUSIC_ENABLED, true)

        ensureMusicPlayer()
        applyMusicState()

        binding.imageViewSetting.setOnClickListener {
            musicEnabled = !musicEnabled
            sharedPreferences.edit { putBoolean(KEY_MUSIC_ENABLED, musicEnabled) }
            applyMusicState()
        }

        binding.exitButton.setOnClickListener {
            sharedPreferences.edit { putBoolean("p1", false); putBoolean("p2", false) }
            exitProcess(0)
        }




        setButtonAction()
        hideSystemBars()


    }

    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or 4096
    }
    private fun setButtonAction(){
        val buttons = arrayOf(binding.btnMathQuiz, binding.button2, binding.button3, binding.button4, binding.button5)
        val activities = arrayOf(MathQuiz::class.java, MissingNumber::class.java,
            MainActivityCityHelicopter::class.java, Tic_Toc_Toe::class.java,
            SpaceShipSelectActivity::class.java)

        for (i in 0..4){
            addPressEffect(buttons[i], activities[i])
        }
    }

    private fun addPressEffect(button: View, nextActivity: Class<out Any>) {
        button.animate().cancel()
        button.animate().withEndAction(null)
        button.setOnTouchListener(null)

       // button.scaleX = 1f
       // button.scaleY = 1f

        val originalY = button.translationY
        val offset = 3f * resources.displayMetrics.density


        button.setOnClickListener {
            button.animate()
                .translationY(originalY + offset)
                .setDuration(70)
                .withEndAction {
                    button.animate()
                        .translationY(originalY)
                        .setDuration(100)
                        .withEndAction {
                            startActivity(Intent(this, nextActivity))

                        }
                        .start()
                }
                .start()
        }
    }

    private fun ensureMusicPlayer() {
        if (backgroundMusic == null) {
            backgroundMusic = MediaPlayer.create(this, R.raw.backgroundmusic).apply {
                isLooping = true
                setVolume(0.5f, 0.5f)
            }
        }
    }

    private fun applyMusicState() {
        ensureMusicPlayer()

        if (musicEnabled) {
            binding.imageViewSetting.setImageResource(R.drawable.music)
            try {
                if (backgroundMusic?.isPlaying != true) backgroundMusic?.start()
            } catch (_: Exception) {}
        } else {
            binding.imageViewSetting.setImageResource(R.drawable.mute_music)
            try {
                if (backgroundMusic?.isPlaying == true) backgroundMusic?.pause()
            } catch (_: Exception) {}
        }
    }

    override fun onResume() {
        super.onResume()
        updateChange()
        if (musicEnabled) applyMusicState()
    }

    override fun onPause() {
        super.onPause()
        try {
            if (backgroundMusic?.isPlaying == true) backgroundMusic?.pause()
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundMusic?.release()
        backgroundMusic = null
    }

    @SuppressLint("SetTextI18n")
    private fun updateChange() {

        var coin = sharedPreferences.getInt("coin", 0)
        if (coin < 20000) {
            coin = 20000
            sharedPreferences.edit { putInt("coin", coin).apply() }
        }
        binding.coinBalance.text = "$coin"
        val state = sharedPreferences.getBoolean("p1", false)
        val stateTow = sharedPreferences.getBoolean("p2", false)

        if (state) {
            binding.blueScore.text = "${binding.blueScore.text.toString().toInt() + 1}"
            sharedPreferences.edit { putBoolean("p1", false) }
        } else if (stateTow) {
            binding.orangeScore.text = "${binding.orangeScore.text.toString().toInt() + 1}"
            sharedPreferences.edit { putBoolean("p2", false) }
        }
    }
}
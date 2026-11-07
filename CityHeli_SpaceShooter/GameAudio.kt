package com.example.a2dgamebox.city_helicopter

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log


/** Optional local raw resources. Missing audio never prevents the game from starting.
 * SoundPool is used only for SHORT effects / a short rotor loop; music is streamed.
 */
@Suppress("DEPRECATION")
class GameAudio(context: Context, private val onFocusLost: () -> Unit) {
    companion object {
        const val MUSIC_VOLUME = .10f
        const val EFFECT_VOLUME = .80f
        const val ENGINE_VOLUME = .65f
    }
    private val app = context.applicationContext
    private val manager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val pool = SoundPool(12, AudioManager.STREAM_MUSIC, 0)
    private val ids = mutableMapOf<String, Int>()
    private val ready = mutableSetOf<Int>()
    private var music: MediaPlayer? = null
    private var musicReady = false
    private var engineStream = 0
    private var active = false
    private var focus = false
    private var released = false
    private var rotorWasOn = false
    var enabled = true
        private set
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (released) return@OnAudioFocusChangeListener
        if (change == AudioManager.AUDIOFOCUS_GAIN) focus = true
        else {
            focus = false
            silence()
            onFocusLost()
        }
    }
    init {
        pool.setOnLoadCompleteListener { _, sample, status ->
            if (!released && status == 0) ready.add(sample)
        }
        for (name in listOf("engine_loop", "engine_start", "gunshot", "explosion", "rescue", "victory", "game_over")) {
            val resource = resource(name)
            if (resource != 0) {
                try { ids[name] = pool.load(app, resource, 1) }
                catch (e: Exception) { Log.w("SkyRescueAudio", "Cannot load $name", e) }
            }
        }
        val musicId = resource("bg_helicopter")
        if (musicId != 0) {
            try {
                val player = MediaPlayer()
                music = player
                player.setAudioStreamType(AudioManager.STREAM_MUSIC)
                val fd = requireNotNull(app.resources.openRawResourceFd(musicId)) {
                    "Music must be an uncompressed raw audio resource"
                }
                fd.use { fd ->
                    player.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                }
                player.isLooping = true
                player.setVolume(MUSIC_VOLUME, MUSIC_VOLUME)
                player.setOnPreparedListener { if (!released) musicReady = true }
                player.setOnErrorListener { _, _, _ ->
                    musicReady = false; music?.release(); music = null; true
                }
                player.prepareAsync()
            } catch (e: Exception) {
                Log.w("SkyRescueAudio", "Cannot load background_music", e)
                music?.release(); music = null; musicReady = false
            }
        }
    }
    // Controls background music only; engine and effects keep their own master switch.
    private var musicEnabled = true

    fun setMusicEnabled(value: Boolean) {
        musicEnabled = value
        if (!value && !released) {
            try {
                if (musicReady && music?.isPlaying == true) music?.pause()
            } catch (_: IllegalStateException) { }
        }
    }

    private fun resource(name: String) = app.resources.getIdentifier(name, "raw", app.packageName)
    fun setEnabled(value: Boolean) {
        enabled = value
        if (!value) { silence(); abandonFocus() }
        else if (active) acquireFocus()
    }
    fun activate() {
        if (released) return
        active = true
        if (enabled && !focus) acquireFocus()
    }
    private fun acquireFocus() {
        if (released || !enabled || !active || (ids.isEmpty() && music == null)) return
        focus = manager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }


    private fun abandonFocus() { manager.abandonAudioFocus(focusListener); focus = false }
    fun suspend() {
        if (released) return
        active = false; silence(); abandonFocus()
    }
    private fun silence() {
        if (released) return
        pool.autoPause()
        if (engineStream != 0) { pool.stop(engineStream); engineStream = 0 }
        try { if (musicReady && music?.isPlaying == true) music?.pause() } catch (_: IllegalStateException) { }
    }
    private fun play(name: String, volume: Float = EFFECT_VOLUME) {
        if (!active || !enabled || !focus || released) return
        val sample = ids[name] ?: return
        if (sample !in ready) return
        pool.play(sample, volume, volume, 2, 0, 1f)
    }
    fun update(playing: Boolean, inMenu: Boolean, rpm: Float, cues: List<AudioCue>) {
        if (released) return
        if (!active || !enabled || !focus) return
        // Pause is silent; menus, result screens and active flight use quiet music.
        if (playing || inMenu) {
            try {
                if (musicReady) {
                    if (musicEnabled) {
                        if (music?.isPlaying == false) music?.start()
                    } else {
                        if (music?.isPlaying == true) music?.pause()
                    }
                }
            } catch (_: IllegalStateException) { }
        } else { silence(); return }
        if (playing) {
            val rotorOn = rpm > .035f
            if (rotorOn && !rotorWasOn) play("engine_start", .65f)
            rotorWasOn = rotorOn
            val sample = ids["engine_loop"]
            if (rotorOn && sample != null && sample in ready) {
                val volume = (ENGINE_VOLUME * rpm).coerceIn(.03f, ENGINE_VOLUME)
                val rate = .65f + rpm * .75f
                if (engineStream == 0) engineStream = pool.play(sample, volume, volume, 10, -1, rate)
                else { pool.setVolume(engineStream, volume, volume); pool.setRate(engineStream, rate) }
            } else if (engineStream != 0) { pool.stop(engineStream); engineStream = 0 }
        } else {
            if (engineStream != 0) { pool.stop(engineStream); engineStream = 0 }
            rotorWasOn = false
        }
        for (cue in cues) when (cue) {
            AudioCue.SHOT -> play("gunshot")
            AudioCue.EXPLOSION -> play("explosion")
            AudioCue.RESCUE -> play("rescue", .75f)
            AudioCue.WIN -> play("victory", .75f)
            AudioCue.LOSE -> play("game_over", .75f)
        }
    }
    fun release() {
        if (released) return
        suspend(); released = true
        pool.release(); music?.release(); music = null; ready.clear()
    }
}

package com.example.a2dgamebox.city_helicopter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View

import kotlin.math.min

/** Coordinates existing flight simulation with the campaign, menus and optional audio. */
@SuppressLint("ViewConstructor")
class GameView(context: Context, width: Int, height: Int) : View(context),
    Choreographer.FrameCallback {

    companion object {
        private const val MUSIC_PREFS_NAME = "gameBox2d"
        private const val MUSIC_NAME_KEY = "music"
    }

    private val musicPrefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(
            MUSIC_PREFS_NAME, Context.MODE_PRIVATE
        )


    private fun syncMusicSetting() {
        audio.setMusicEnabled(musicPrefs.getBoolean(MUSIC_NAME_KEY, true))
    }

    val engine = GameEngine()
    private val controls = TouchControls(engine, width.toFloat(), height.toFloat())
    private val store = CampaignStore(context)
    private val progress = store.load()
    private val ui = CampaignUi()
    private val assetLoad = runCatching { GameAssets(context.assets) }
    private val renderer = assetLoad.getOrNull()?.let { GameRenderer(engine, controls, it) }
    private val menus = assetLoad.getOrNull()?.let {
        CampaignRenderer(
            context,
            it,
            progress,
            ui,
            width.toFloat(),
            height.toFloat()
        )
    }
    private val errorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val audio = GameAudio(context) { engine.pause(); controls.reset() }
    private var running = false
    private var previousNanos = 0L
    private var accumulator = 0f
    private var renderDt = 1f / 60f
    private var uiScale = 1f
    private var offsetX = 0f
    private var offsetY = 0f
    private var recordedWin = false
    private val fixedStep = 1f / 60f

    private var backStatus : Boolean = false

    init {
        isFocusable = true; keepScreenOn = true
        contentDescription = "City helicopter — Rescue operation"
        audio.setEnabled(store.soundEnabled)
        syncMusicSetting()
    }

    fun onHostResume() {
        syncMusicSetting()
        if (renderer == null) {
            invalidate(); return
        }
        audio.activate()
        if (running) return
        running = true; previousNanos = 0L; accumulator = 0f
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun onHostPause() {
        engine.pause(); controls.reset(); audio.suspend()
        running = false; previousNanos = 0L; accumulator = 0f
        Choreographer.getInstance().removeFrameCallback(this)
    }

    fun onHostFocusChanged(focused: Boolean) {
        if (focused) {
            if (running) audio.activate()
        } else {
            engine.pause(); controls.reset(); audio.suspend()
        }
    }

    fun release() {
        audio.release()
    }

    override fun onDetachedFromWindow() {
        onHostPause(); release(); super.onDetachedFromWindow()
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        renderDt = if (previousNanos == 0L) fixedStep else ((frameTimeNanos - previousNanos) / 1_000_000_000f).coerceIn(0f, .10f)
        previousNanos = frameTimeNanos
        accumulator += renderDt
        while (accumulator >= fixedStep) {
            if (ui.screen == GameScreen.FLIGHT)
                engine.update(fixedStep)
            accumulator -= fixedStep
        }
        if (ui.screen == GameScreen.FLIGHT && engine.phase == Phase.WON && !recordedWin) {
            val reward = progress.recordVictory(engine.mission.level)
            renderer?.earnedCoins = reward
            store.save(progress); recordedWin = true
        }
        val inMenu =
            ui.screen != GameScreen.FLIGHT || engine.phase == Phase.WON || engine.phase == Phase.LOST
        audio.update(
            ui.screen == GameScreen.FLIGHT && engine.phase == Phase.PLAYING,
            inMenu, engine.helicopter.rpm, engine.drainAudioEvents()
        )
        renderer?.soundEnabled = audio.enabled; menus?.soundEnabled = audio.enabled
        invalidate()
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val logicalWidth = maxOf(1100f, w.toFloat() / maxOf(h, 1) * 720f)
        uiScale = min(w / logicalWidth, h / 720f).coerceAtLeast(.01f)
        offsetX = (w - logicalWidth * uiScale) / 2; offsetY = (h - 720 * uiScale) / 2
        controls.width = logicalWidth; controls.height = 720f
        renderer?.let { it.width = logicalWidth; it.height = 720f }
        menus?.let { it.width = logicalWidth; it.height = 720f }
        controls.reset()
    }

    @SuppressLint("UseKtx")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(9, 29, 27))
        canvas.save()
        canvas.translate(offsetX, offsetY); canvas.scale(uiScale, uiScale)
        val drawing = renderer
        if (drawing != null) {
            if (ui.screen == GameScreen.FLIGHT) drawing.draw(canvas, renderDt)
            else menus?.draw(canvas)
        } else {
            errorPaint.color = Color.rgb(255, 205, 162); errorPaint.textSize = 27f
            canvas.drawText("Unable to load images", 50f, 80f, errorPaint)
            errorPaint.color = Color.WHITE; errorPaint.textSize = 18f
            canvas.drawText("Check app/src/main/assets/game/", 50f, 125f, errorPaint)
            (assetLoad.exceptionOrNull()?.message ?: "Asset loading failed").lines().take(15)
                .forEachIndexed { i, line ->
                    canvas.drawText(
                        line,
                        50f,
                        175f + i * 27f,
                        errorPaint
                    )
                }
        }
        canvas.restore()
    }

    private fun toggleSound() {
        audio.setEnabled(!audio.enabled); store.soundEnabled = audio.enabled
        renderer?.soundEnabled = audio.enabled; menus?.soundEnabled = audio.enabled
    }

    private fun launchMission(aircraft: Int, mission: Int) {
        if (!progress.canPlay(aircraft, mission)) return
        ui.selectedAircraft = aircraft
        engine.prepare(Campaign.level(aircraft, mission))
        renderer?.let { it.selectedPlayer = aircraft; it.earnedCoins = 0; it.resetCamera() }
        controls.reset()
        recordedWin = false
        accumulator = 0f
        ui.screen = GameScreen.FLIGHT
        engine.start()
        audio.activate()
    }


    private fun openHangar() {
        engine.pause(); controls.reset(); ui.purchaseDialog = false; ui.screen = GameScreen.HANGAR
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val drawing = renderer ?: return true
        val menu = menus ?: return true
        val x = (event.getX(event.actionIndex) - offsetX) / uiScale
        val y = (event.getY(event.actionIndex) - offsetY) / uiScale
        if (event.actionMasked == MotionEvent.ACTION_DOWN) audio.activate()
        if (ui.screen == GameScreen.FLIGHT && engine.phase == Phase.PLAYING) {
            if (event.actionMasked == MotionEvent.ACTION_DOWN && controls.soundRect.contains(x, y)) {
                toggleSound(); performClick()
            } else controls.handle(event, uiScale, offsetX, offsetY)
            return true
        }
        if (event.actionMasked != MotionEvent.ACTION_UP) return true
        if (ui.screen == GameScreen.FLIGHT) {
            if (drawing.primaryRect.contains(x, y)) when (engine.phase) {
                Phase.PAUSED -> {
                    engine.resume(); controls.reset(); audio.activate()
                }

                Phase.LOST -> launchMission(
                    engine.mission.aircraftIndex,
                    engine.mission.missionNumber - 1
                )

                Phase.WON -> {
                    controls.reset()
                    ui.screen =
                        if (engine.mission.missionNumber == 5) GameScreen.HANGAR else GameScreen.MISSIONS
                }

                else -> Unit
            } else if (drawing.secondaryRect.contains(x, y)) openHangar()
        } else if (ui.purchaseDialog) {
            if (menu.cancelRect.contains(x, y)) ui.purchaseDialog = false
            else if (menu.buyRect.contains(x, y)) {
                if (progress.buy(ui.selectedAircraft) == PurchaseResult.PURCHASED) {
                    store.save(progress); ui.purchaseDialog = false; ui.screen = GameScreen.MISSIONS
                }
            }
        } //else //if (menu.soundRect.contains(x, y)) toggleSound()
        //ui.screen != GameScreen.HUB &&

        else if (menu.backRect.contains(x, y)){
            if (!backStatus)
                (context as Activity).finish()
            else {
                backStatus = false
                openHangar()
            }
        }// onBack()
        else when (ui.screen) {
            // GameScreen.HUB -> if (menu.hubRect.contains(x, y)) ui.screen = GameScreen.HANGAR
            GameScreen.HANGAR -> for (i in 0..3) if (menu.aircraftRect(i).contains(x, y)) {
                backStatus = !backStatus
                ui.selectedAircraft = i
                if (progress.owns(i)) ui.screen = GameScreen.MISSIONS else ui.purchaseDialog = true
                break
            }

            GameScreen.MISSIONS -> for (i in 0..5) if (menu.missionRect(i).contains(x, y)) {
                launchMission(ui.selectedAircraft, i); break
            }

            GameScreen.FLIGHT -> Unit
        }

        performClick(); invalidate(); return true
    }


    override fun performClick(): Boolean {
        super.performClick(); return true
    }

}

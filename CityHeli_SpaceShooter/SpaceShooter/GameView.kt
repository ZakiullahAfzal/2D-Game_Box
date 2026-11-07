package com.example.a2dgamebox.air_shooter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.graphics.*
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.content.edit
import androidx.core.graphics.toRect
import com.example.a2dgamebox.R
import com.example.gamebox2d.air_shooter.Boss
import com.example.gamebox2d.air_shooter.Bullet
import com.example.gamebox2d.air_shooter.Enemy
import com.example.gamebox2d.air_shooter.Reward
import com.example.gamebox2d.air_shooter.RewardType
import java.util.Collections
import kotlin.math.min

@SuppressLint("ViewConstructor")
class GameView(context: Context,
               bitmap: Int,
               private val playerType: Int
               , play: Boolean, private val sharedPreferences: SharedPreferences
) : SurfaceView(context), Runnable {

    private val surfaceHolder: SurfaceHolder = holder
    private var gameThread: Thread? = null

    @Volatile
    private var isPlaying = false

    private enum class GameState { PLAYING, GAME_OVER, GAME_WON, START_SCREEN, PAUSED }

    @Volatile
    private var gameState = GameState.START_SCREEN

    private val typeList = arrayOf(0,1,2,3,4,5)
    private var enemyType : Int = 0
    // Bitmaps
    private val background by lazy { BitmapFactory.decodeResource(resources, R.drawable.bg_space_shooter) }
    private val healthBitmap by lazy { BitmapFactory.decodeResource(resources, R.drawable.health) }
    private val scoreBitmap by lazy { BitmapFactory.decodeResource(resources, R.drawable.score) }
    private val timerBitmap by lazy { BitmapFactory.decodeResource(resources, R.drawable.timer) }
    private val bossHealth by lazy { BitmapFactory.decodeResource(resources, R.drawable.bosshealth) }
    private val pauseBitmap by lazy { BitmapFactory.decodeResource(resources, R.drawable.pause_button) }

    // Game objects
    private val player = Player(context, bitmap)
    private val bullets = Collections.synchronizedList(mutableListOf<Bullet>())
    private val enemies = Collections.synchronizedList(mutableListOf<Enemy>())
    private val enemyBullets = Collections.synchronizedList(mutableListOf<Bullet>())
    private val explosions = Collections.synchronizedList(mutableListOf<Explosion>())
    private val rewards = Collections.synchronizedList(mutableListOf<Reward>())

    // Boss
    private var isBossLevel = false
    private var boss: Boss? = null
    private var bossIntroShown = false
    private var bossStartTime: Long = 0L

    // Gameplay time advances only during PLAYING, including all cooldowns.
    private var gameElapsedMillis = 0.0

    // Timers
    private var lastBulletTime = 0L
    private var lastEnemySpawnTime = 0L
    private var lastEnemyShootTime = 0L
    private var lastRewardSpawnTime = 0L
    private var lastBossBulletTime = 0L
    private var gameStartTime = 0L

    // UI metrics
    private val healthSize = 64
    // private val scoreSize = 74
    private val timerSize = 124

    // Audio
    private val soundPool: SoundPool
    private val explosionSoundId: Int
    private var backgroundMusic: MediaPlayer? = null
    private val playMusic: Boolean = play

    // Stats
    private var score = 0
    private var playerHealth = 5

    // Player shoot style
    private var doubleShoot = false
    private var counter = 5
    private var shoot = true

    private var money = 0
    private var coinFrame = 0

    // UI Paint
    private val paint = Paint().apply {
        color = Color.WHITE
        textSize = 60f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    // Buttons
    private val restartRect = RectF()
    private val exitRect = RectF()
    private val resumeReact = RectF()
    private val releaseRect = RectF()
    private var releaseUsed = false

    // Pause button
    private val pauseRect = RectF()


    private val hudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
    }
    private val hudRect = RectF()

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        explosionSoundId = soundPool.load(context, R.raw.explosion, 1)

        backgroundMusic = MediaPlayer.create(context, R.raw.background_music)
        if (playMusic) {
            backgroundMusic?.isLooping = true
            backgroundMusic?.setVolume(0.5f, 0.5f)
        }

        if (enemyType == playerType){
            enemyType = playerType + typeList.size + 1
        }

        Reward.loadBitmaps(context)
        money = sharedPreferences.getInt("coin", 0)
    }

    @Synchronized
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        layoutUiReacts()
    }

    private fun layoutUiReacts() {
        if (width <= 0 || height <= 0) return

        val cx = width * 0.5f
        val cy = height * 0.5f

        val btnW = (width * 0.55f).coerceIn(380f, 550f)
        val btnH = (height * 0.065f).coerceIn(90f, 120f)
        val gap = (height * 0.02f).coerceIn(18f, 34f)

        val left = cx - btnW / 2f
        val right = cx + btnW / 2f

        val top1 = cy - btnH * 0.2f
        restartRect.set(left, top1, right, top1 + btnH)
        resumeReact.set(left, top1, right, top1 + btnH)

        val top2 = top1 + btnH + gap
        exitRect.set(left, top2, right, top2 + btnH)

        val top3 = top2 + btnH + gap
        releaseRect.set(left, top3, right, top3 + btnH)

        val pSize = (min(width, height) * 0.085f).coerceIn(72f, 120f)
        pauseRect.set(
            width - pSize - 24f,
            height - pSize - 115f,
            width - 24f,
            height - 105f
        )
    }

    override fun run() {
        val frameDurationNanos = 1_000_000_000L / 60L
        var lastTime = System.nanoTime()

        while (isPlaying) {
            val now = System.nanoTime()
            val dt = ((now - lastTime) / 1_000_000_000f).coerceAtMost(0.05f)
            lastTime = now

            synchronized(this) {
                if (gameState == GameState.PLAYING) {
                    update(dt)
                }
                drawFrame()
            }
            // Drawing/posting the surface may already wait for the display.
            // Only sleep for the unused part of our 60 FPS frame budget.
            val remainingNanos = frameDurationNanos - (System.nanoTime() - now)
            if (remainingNanos > 0L) {
                try {
                    Thread.sleep(
                        remainingNanos / 1_000_000L,
                        (remainingNanos % 1_000_000L).toInt()
                    )
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }
    }

    @Synchronized
    fun update(dt: Float) {
        if (gameState != GameState.PLAYING) return

        gameElapsedMillis += dt * 1000.0
        val currentTime = gameElapsedMillis.toLong()
        val elapsedSeconds = (currentTime - gameStartTime) / 1000

        if (elapsedSeconds >= 60 && !isBossLevel) {
            startBossLevel(currentTime)
        }

        bullets.forEach { it.update(dt) }
        enemies.forEach { it.update(dt) }
        enemyBullets.forEach { it.update(dt) }
        explosions.forEach { it.update(dt) }
        rewards.forEach { it.update(dt) }

        bullets.removeAll { it.y < -it.height || it.y > height }
        enemyBullets.removeAll { it.y > height }
        enemies.removeAll { it.y > height }
        explosions.removeAll { it.isFinished }
        rewards.removeAll { it.isOffScreen() }

        handleCollisions()
        if (gameState != GameState.PLAYING) return

        if (!isBossLevel) {
            handlePlayerShooting(currentTime)
            handleEnemySpawning(currentTime)
            handleRewardSpawning(currentTime)
        } else {
            if (shoot) handlePlayerShooting(currentTime)
            handleBossLogic(currentTime, dt)
        }
    }

    private fun startBossLevel(currentTime: Long) {
        isBossLevel = true
        bossStartTime = currentTime
        bossIntroShown = false
        bullets.clear()
        enemies.clear()
        enemyBullets.clear()
    }

    private fun handlePlayerShooting(currentTime: Long) {
        if (currentTime - lastBulletTime > 400) {
            if (doubleShoot) {
                counter--
                bullets.add(Bullet(context, (player.x + player.width / 2f) - 16f, player.y, true))
                bullets.add(Bullet(context, (player.x + player.width / 2f) + 16f, player.y, true))
                if (counter == 0) doubleShoot = false
            } else {
                bullets.add(Bullet(context, player.x + player.width / 2f, player.y, true))
            }
            lastBulletTime = currentTime
        }
    }

    private fun handleEnemySpawning(currentTime: Long) {
        if (currentTime - lastEnemySpawnTime > 1500) {
            enemies.add(Enemy(context, width, enemyType))
            lastEnemySpawnTime = currentTime
        }

        if (currentTime - lastEnemyShootTime > 1000) {
            enemies.forEach {
                enemyBullets.add(
                    Bullet(
                        context,
                        it.x + it.getWidth() / 2f,
                        it.y + it.getHeight(),
                        false
                    )
                )
            }
            lastEnemyShootTime = currentTime
        }
    }

    private fun handleRewardSpawning(currentTime: Long) {
        if (rewards.size < 3 && currentTime - lastRewardSpawnTime > 10000) {
            rewards.add(Reward(context, width, height))
            lastRewardSpawnTime = currentTime
            changeEnemy()
        }
    }


    private fun changeEnemy(){
        enemyType = enemyType % typeList.size + 1
        if (enemyType == playerType) changeEnemy()
    }

    private fun handleBossLogic(currentTime: Long, dt: Float) {
        val bossElapsed = currentTime - bossStartTime

        if (!bossIntroShown && bossElapsed >= 3000) {
            bossIntroShown = true
            boss = Boss(context)
        }

        boss?.let { b ->
            b.update(width, dt)

            if (currentTime - lastBossBulletTime > 700 && bossElapsed % 6000 < 5000) {
                lastBossBulletTime = currentTime
                enemyBullets.addAll(b.shoot())
            }

            if (b.health <= 0) {
                gameState = GameState.GAME_WON
            }
        }
    }

    private fun handleCollisions() {
        bullets.removeAll { bullet ->
            var bulletHit = false

            enemies.removeAll { enemy ->
                if (RectF.intersects(bullet.getRect(), enemy.getRect())) {
                    bulletHit = true
                    soundPool.play(explosionSoundId, 1f, 1f, 0, 0, 1f)

                    if (enemy.health()) {
                        explosions.add(
                            Explosion(
                                context,
                                enemy.x,
                                enemy.y,
                                enemy.getWidth(),
                                enemy.getHeight()
                            )
                        )
                        score += 10

                        if (score % 10 == 0) {
                            coinFrame++
                            money++
                        }

                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            }

            bulletHit
        }

        enemyBullets.removeAll { bullet ->
            if (RectF.intersects(bullet.getRect(), player.getRect())) {
                playerHealth--
                if (playerHealth <= 0) {
                    gameState = GameState.GAME_OVER
                }
                true
            } else {
                false
            }
        }

        rewards.removeAll { reward ->
            if (RectF.intersects(reward.getRect(), player.getRect())) {
                when (reward.type) {
                    RewardType.HEALTH -> playerHealth = min(playerHealth + 1, 5)
                    RewardType.SCORE -> score += 50
                    RewardType.ENERGY -> {
                        doubleShoot = true
                        counter = 10
                    }
                }
                true
            } else {
                false
            }
        }

        boss?.let { b ->
            bullets.removeAll { bullet ->
                if (RectF.intersects(bullet.getRect(), b.getRect())) {
                    b.health--
                    score += 10
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun drawFrame() {
        if (!surfaceHolder.surface.isValid) return

        val canvas = surfaceHolder.lockCanvas() ?: return
        try {
            canvas.drawBitmap(background, null, Rect(0, 0, width, height), null)

            when (gameState) {
                GameState.START_SCREEN, GameState.PAUSED -> drawStartScreen(canvas)
                GameState.PLAYING -> drawGame(canvas)
                GameState.GAME_OVER, GameState.GAME_WON -> drawGameOver(canvas)
            }
        } finally {
            surfaceHolder.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawGame(canvas: Canvas) {
        player.draw(canvas)
        bullets.forEach { it.draw(canvas) }
        enemies.forEach { it.draw(canvas) }
        enemyBullets.forEach { it.draw(canvas) }
        explosions.forEach { it.draw(canvas) }
        rewards.forEach { it.draw(canvas) }

        if (isBossLevel) {
            if (!bossIntroShown) {
                paint.textAlign = Paint.Align.CENTER
                paint.color = Color.WHITE
                paint.textSize = 80f
                canvas.drawText("Boss Level", width / 2f, height / 2f, paint)
                shoot = false
            } else {
                shoot = true
                boss?.draw(canvas)
            }
        }

        drawHUD(canvas)
        canvas.drawBitmap(pauseBitmap, null, pauseRect.toRect(), null)
    }

    private fun drawHUD(canvas: Canvas) {

        hudRect.set(20f, 25f, 980f, 105f)
        hudPaint.style = Paint.Style.FILL
        hudPaint.color = Color.argb(210, 8, 17, 40)
        canvas.drawRoundRect(hudRect, 28f, 28f, hudPaint)

        hudPaint.style = Paint.Style.STROKE
        hudPaint.strokeWidth = 3f
        hudPaint.color = Color.rgb(65, 80, 110)
        canvas.drawRoundRect(hudRect, 28f, 28f, hudPaint)
        hudPaint.style = Paint.Style.FILL


        for (i in 0 until playerHealth) {
            canvas.drawBitmap(
                healthBitmap,
                null,
                Rect(
                    20 + i * (healthSize + 10),
                    30,
                    20 + i * (healthSize + 10) + healthSize,
                    20 + healthSize
                ),
                null
            )
        }

        hudPaint.alpha = 255

        // سکه   470 left  400f
        hudRect.set(470f, 40f, 530f, 87f)
        canvas.drawBitmap(scoreBitmap, null, hudRect, hudPaint)

        hudPaint.color = Color.WHITE
        hudPaint.textSize = 40f
        canvas.drawText(score.toString(), 670f, 75f, hudPaint)

        paint.color = Color.WHITE
        paint.textSize = 50f

        //  canvas.drawBitmap(scoreBitmap, null, Rect(30, 100, 30 + scoreSize, 100 + scoreSize), null)
        //canvas.drawText(" $score", (30 + scoreSize + 30).toFloat(), 110f + scoreSize / 1.5f, paint)

        if (!isBossLevel) {
            val timeLeft = (60 - ((gameElapsedMillis.toLong() - gameStartTime) / 1000).toInt()).coerceAtLeast(0)
            canvas.drawText(
                ": $timeLeft",
                (width - 30 - timerSize - 90).toFloat(),
                100f + timerSize / 1.5f,
                paint
            )
            canvas.drawBitmap(
                timerBitmap,
                null,
                Rect(width - 30 - timerSize, 100, width - 10, 100 + timerSize),
                null
            )
        } else {
            canvas.drawText(
                "${boss?.health ?: 30} : ",
                (width - 30 - timerSize - 90).toFloat(),
                100f + timerSize / 1.5f,
                paint
            )
            canvas.drawBitmap(
                bossHealth,
                null,
                Rect(width - 30 - timerSize, 100, width - 10, 100 + timerSize),
                null
            )
        }
    }

    private fun drawButton(canvas: Canvas, rect: RectF, text: String) {
        paint.color = Color.LTGRAY
        canvas.drawRoundRect(rect, 20f, 20f, paint)

        paint.color = Color.BLACK
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = rect.height() * 0.55f
        val y = rect.centerY() - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(text, rect.centerX(), y, paint)
    }

    private fun drawStartScreen(canvas: Canvas) {
        val cx = width * 0.5f
        val cy = height * 0.5f

        paint.color = Color.WHITE
        paint.textSize = height * 0.06f
        paint.textAlign = Paint.Align.CENTER

        if (gameState == GameState.PAUSED) {
            canvas.drawText("Game Paused", cx, cy - height * 0.15f, paint)
            drawButton(canvas, resumeReact, "RESUME")
            drawButton(canvas, exitRect, "EXIT")
        } else {
            canvas.drawText("Space Shooter", cx, cy - height * 0.15f, paint)
            drawButton(canvas, restartRect, "START GAME")
            drawButton(canvas, exitRect, "EXIT")
        }
    }

    private fun drawGameOver(canvas: Canvas) {
        val cx = width * 0.5f
        val cy = height * 0.5f

        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 100f
        paint.color = Color.WHITE

        val msg = if (gameState == GameState.GAME_WON) {
            "You Win! Coin : $coinFrame"
        } else {
            "Game Over Coin : $coinFrame"
        }

        canvas.drawText(msg, cx, cy - height * 0.25f, paint)

        drawButton(canvas, restartRect, "RESTART")
        drawButton(canvas, exitRect, "EXIT")

        if (!releaseUsed && gameState == GameState.GAME_OVER && money >= 50) {
            drawButton(canvas, releaseRect, "RELEASE")
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Synchronized
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                when (gameState) {
                    GameState.PLAYING -> {
                        if (pauseRect.contains(event.x, event.y)) {
                            gameState = GameState.PAUSED
                            return true
                        }
                    }

                    GameState.PAUSED -> {
                        if (resumeReact.contains(event.x, event.y)) {
                            gameState = GameState.PLAYING
                            return true
                        }
                        if (exitRect.contains(event.x, event.y)) {
                            sharedPreferences.edit { putInt("coin", money) }
                            exitGame()
                            return true
                        }
                    }

                    GameState.GAME_OVER, GameState.GAME_WON, GameState.START_SCREEN -> {
                        if (restartRect.contains(event.x, event.y)) {
                            sharedPreferences.edit { putInt("coin", money) }
                            restartGame()
                            return true
                        } else if (exitRect.contains(event.x, event.y)) {
                            sharedPreferences.edit { putInt("coin", money) }
                            exitGame()
                            return true
                        } else if (
                            !releaseUsed && money >= 50 &&
                            gameState == GameState.GAME_OVER &&
                            releaseRect.contains(event.x, event.y)
                        ) {
                            money -= 50
                            sharedPreferences.edit { putInt("coin", money) }
                            releaseUsed = true
                            // Remove old shots so reviving does not cause immediate death.
                            enemyBullets.clear()
                            playerHealth = 3
                            player.x = (width / 2f - player.width / 2f)
                            player.y = (height - player.height - 50).toFloat()
                            gameState = GameState.PLAYING
                            return true
                        }
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (gameState == GameState.PLAYING) {
                    player.x = event.x - player.width / 2f
                    player.y = event.y - player.height / 2f

                    if (player.x < 0f) player.x = 0f
                    if (player.x + player.width > width) player.x = (width - player.width).toFloat()
                    if (player.y < 0f) player.y = 0f
                    if (player.y + player.height > height) player.y = (height - player.height).toFloat()
                }
            }
        }
        return true
    }

    private fun exitGame() {
        isPlaying = false
        var owner: Context = context
        while (owner is ContextWrapper) {
            if (owner is Activity) {
                owner.finish()
                return
            }
            val base = owner.baseContext
            if (base === owner) break
            owner = base
        }
    }

    fun pause() {
        synchronized(this) {
            if (gameState == GameState.PLAYING) gameState = GameState.PAUSED
        }
        isPlaying = false
        try {
            gameThread?.join()
        } catch (_: InterruptedException) {
        }
        if (playMusic && backgroundMusic?.isPlaying == true) {
            backgroundMusic?.pause()
        }
    }

    fun resume() {
        if (isPlaying || destroyed) return

        isPlaying = true

        gameThread = Thread(this)
        gameThread?.start()

        if (playMusic && backgroundMusic?.isPlaying != true) {
            backgroundMusic?.start()
        }
    }

    private var destroyed = false

    fun destroy() {
        if (destroyed) return
        destroyed = true
        isPlaying = false
        try {
            gameThread?.join()
        } catch (_: InterruptedException) {
        }

        soundPool.release()
        backgroundMusic?.stop()
        backgroundMusic?.release()
        backgroundMusic = null
    }

    private fun restartGame() {
        score = 0
        playerHealth = 5
        bullets.clear()
        enemies.clear()
        enemyBullets.clear()
        explosions.clear()
        rewards.clear()
        isBossLevel = false
        boss = null
        bossIntroShown = false
        releaseUsed = false
        doubleShoot = false
        counter = 5
        shoot = true
        coinFrame = 0
        gameElapsedMillis = 0.0
        gameStartTime = 0L
        bossStartTime = 0L
        lastBulletTime = gameStartTime
        lastEnemySpawnTime = gameStartTime
        lastEnemyShootTime = gameStartTime
        lastRewardSpawnTime = gameStartTime
        lastBossBulletTime = gameStartTime

        player.x = (width / 2f - player.width / 2f)
        player.y = (height - player.height - 50).toFloat()
        gameState = GameState.PLAYING
        enemyType = 0

        // The loop stays alive on the start, pause, and game-over screens.
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        destroy()
    }
}

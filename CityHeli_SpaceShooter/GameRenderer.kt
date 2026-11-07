package com.example.a2dgamebox.city_helicopter

import android.annotation.SuppressLint
import android.graphics.*
import kotlin.math.*
import java.util.IdentityHashMap

/** Draws the supplied PNG artwork while retaining the existing simulation and HUD. */
class GameRenderer(private val engine: GameEngine,
                   private val controls: TouchControls,
                   private val assets: GameAssets,
    ) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val path = Path()
    private val ink = Color.rgb(15, 43, 43)
    private val mint = Color.rgb(184, 225, 177)
    private val cream = Color.rgb(238, 237, 213)
    private val orange = Color.rgb(245, 159, 112)
    private val muted = Color.rgb(148, 180, 158)
    private val enemyVariants = IdentityHashMap<Enemy, Int>()
    private var nextEnemyVariant = 0
    private val visibleWorld = RectF()
    var earnedCoins = 0
    var soundEnabled = true
    var selectedPlayer = 0
        set(value) { field = ((value % SpriteCatalog.players.size) + SpriteCatalog.players.size) % SpriteCatalog.players.size }
    val playerChoiceRect get() = RectF(width / 2 - 230f, height / 2 + 64f, width / 2 + 230f, height / 2 + 103f)
    private val camera = engine.map.base.copy()
    private var zoom = .85f

    var width = 1280f
    var height = 720f
    val primaryRect get() = RectF(width / 2 - 215f, height / 2 + 118f, width / 2 + 215f, height / 2 + 178f)
    val secondaryRect get() = RectF(width / 2 - 215f, height / 2 + 190f, width / 2 + 215f, height / 2 + 230f)

    fun resetCamera() { camera.set(engine.helicopter.pos.x, engine.helicopter.pos.y); zoom = .85f }
    private fun fill(color: Int) { p.color = color; p.style = Paint.Style.FILL; p.shader = null; p.pathEffect = null }
    private fun box(c: Canvas, x: Float, y: Float, w: Float, h: Float, color: Int, radius: Float = 0f) {
        fill(color); rect.set(x, y, x + w, y + h)
        if (radius > 0f) c.drawRoundRect(rect, radius, radius, p) else c.drawRect(rect, p)
    }
    private fun circle(c: Canvas, x: Float, y: Float, radius: Float, color: Int) {
        fill(color); c.drawCircle(x, y, radius, p)
    }
    private fun line(c: Canvas, x: Float, y: Float, x2: Float, y2: Float, color: Int, stroke: Float = 1f) {
        fill(color); p.strokeWidth = stroke; c.drawLine(x, y, x2, y2, p)
    }
    private fun ring(c: Canvas, x: Float, y: Float, radius: Float, color: Int, stroke: Float = 1f) {
        fill(color); p.style = Paint.Style.STROKE; p.strokeWidth = stroke
        c.drawCircle(x, y, radius, p); p.style = Paint.Style.FILL
    }
    private fun text(c: Canvas, value: String, x: Float, y: Float, size: Float, color: Int = cream,
                     align: Paint.Align = Paint.Align.LEFT, bold: Boolean = false) {
        fill(color); p.textSize = size; p.textAlign = align
        p.typeface = if (bold) Typeface.create("sans-serif", Typeface.BOLD) else Typeface.create("sans-serif", Typeface.NORMAL)
        c.drawText(value, x, y, p)
    }
    private fun polygon(c: Canvas, color: Int, vararg points: Float) {
        fill(color); path.reset(); path.moveTo(points[0], points[1])
        for (i in 2 until points.size step 2) path.lineTo(points[i], points[i + 1])
        path.close(); c.drawPath(path, p)
    }
    private fun panel(c: Canvas, x: Float, y: Float, w: Float, h: Float) {
        box(c, x, y, w, h, Color.argb(232, 15, 43, 43), 10f)
        fill(Color.argb(75, 204, 225, 187)); p.style = Paint.Style.STROKE; p.strokeWidth = 1f
        rect.set(x, y, x + w, y + h); c.drawRoundRect(rect, 10f, 10f, p); p.style = Paint.Style.FILL
    }
    @SuppressLint("UseKtx")
    fun draw(c: Canvas, dt: Float) {
        c.drawColor(ink)
        val h = engine.helicopter
        val zoomTarget = .97f - h.altitude / 180f * .43f
        val halfW = width / (2f * zoomTarget)
        val halfH = height / (2f * zoomTarget)
        //================================================
        fun clampCamera(value: Float, half: Float, size: Float): Float = if (half * 2 >= size) size / 2 else value.coerceIn(half, size - half)
        //====================================
        camera.x = approach(camera.x, clampCamera(h.pos.x, halfW, CityMap.WIDTH), 3.4f, dt)
        camera.y = approach(camera.y, clampCamera(h.pos.y, halfH, CityMap.HEIGHT), 3.4f, dt)
        // The world shrinks as the aircraft climbs; zoom increases again while descending.
        zoom = approach(zoom, .97f - h.altitude / 180f * .43f, 2f, dt)
        val worldScale = zoom
        val centerX = width * .5f
        val centerY = height * .5f
        c.save()
        c.clipRect(0f, 0f, width, height)
        c.translate(centerX, centerY)
        c.scale(worldScale, worldScale)
        c.translate(-camera.x, -camera.y)
        visibleWorld.set(camera.x - width / (2 * zoom), camera.y - height / (2 * zoom), camera.x + width / (2 * zoom), camera.y + height / (2 * zoom))
        assets.drawMap(c, visibleWorld)
        enemyVariants.keys.removeAll { old -> engine.enemies.none { it === old } }
        drawSurvivors(c)
        for (enemy in engine.enemies) {
            drawEnemy(c, enemy)
        //    box(c, enemy.pos.x - 21, enemy.pos.y + 52, 42f, 3f, 0x77423128)
            box(c, enemy.pos.x - 21, enemy.pos.y + 52, 42f * (enemy.health / enemy.maxHealth).coerceIn(0f, 1f), 3f, orange)
        }
        for (b in engine.bullets) {
            val dir = b.velocity.length()
            line(c, b.pos.x, b.pos.y, b.pos.x - b.velocity.x / dir * 13f, b.pos.y - b.velocity.y / dir * 13f,
                if (b.friendly) 0xfffbe9a4.toInt() else 0xffff8663.toInt(), if (b.friendly) 3f else 5f)
        }
        // Shadow uses the actual body silhouette and separates as altitude increases.
        val shadowOffset = 5f + h.altitude * .27f
        val playerArt = SpriteCatalog.players[selectedPlayer]
        assets.drawSprite(c, playerArt.body, h.pos.x + shadowOffset, h.pos.y + shadowOffset,
            h.heading * 180f / PI.toFloat(), 1f, (95 - h.altitude * .22f).toInt(), true)
        if (h.rpm > .35f && h.altitude < 55f) {
            val radius = 54f + engine.elapsed * 50f % 70f
            ring(c, h.pos.x, h.pos.y, radius, 0x30e2dfb0, 3f)
        }
        drawPlayer(c)
        for (part in engine.particles) {
            val alpha = (255 * part.life / part.total).toInt().coerceIn(0, 255)
            circle(c, part.pos.x, part.pos.y, 2f + 6f * part.life / part.total,
                if (part.warm) Color.argb(alpha, 255, 169, 85) else Color.argb(alpha, 183, 240, 178))
        }
        c.restore()
        drawHud(c)
        if (engine.phase != Phase.BRIEFING) drawControls(c)
        when (engine.phase) {
            Phase.BRIEFING -> drawModal(c, "SKY RESCUE", "Urban rescue operation", "Start operation", true)
            Phase.PAUSED -> drawModal(c, "PAUSED", "The Game is stopped", "Continue to fly", false)
            Phase.WON -> drawModal(c, "MISSION COMPLETE", "Successful operation", "Missions list", false)
            Phase.LOST -> drawModal(c, "GAME OVER", "Successful operation", "Try Again", false)
            else -> Unit
        }
    }
    private fun drawSurvivors(c: Canvas) {
        for (s in engine.survivors) {
            if (s.rescued) continue
            ring(c, s.pos.x, s.pos.y, 43f + sin(engine.elapsed * 3f) * 5f, 0x66c5f5a5, 2f)
            circle(c, s.pos.x + 4, s.pos.y + 5, 12f, 0x55375438)
            line(c, s.pos.x, s.pos.y - 5, s.pos.x, s.pos.y + 14, 0xfff4d58c.toInt(), 7f)
            circle(c, s.pos.x, s.pos.y - 12, 7f, 0xfff2cf9b.toInt())
            val wave = sin(engine.elapsed * 5) * 5
            line(c, s.pos.x, s.pos.y, s.pos.x - 17, s.pos.y - 9 - wave, cream, 4f)
            line(c, s.pos.x, s.pos.y, s.pos.x + 17, s.pos.y - 9 + wave, cream, 4f)
            box(c, s.pos.x - 36, s.pos.y - 71, 72f, 30f, mint, 5f)
            polygon(c, mint, s.pos.x - 6, s.pos.y - 42, s.pos.x + 6, s.pos.y - 42, s.pos.x, s.pos.y - 34)
            text(c, "HELP", s.pos.x, s.pos.y - 50, 18f, ink, Paint.Align.CENTER, true)
            if (s.progress > 0f) {
                fill(mint); p.style = Paint.Style.STROKE; p.strokeWidth = 6f
                c.drawArc(RectF(s.pos.x - 39, s.pos.y - 39, s.pos.x + 39, s.pos.y + 39), -90f, s.progress * 360f, false, p)
                p.style = Paint.Style.FILL
            }
        }
    }
    @SuppressLint("UseKtx")
    private fun drawPlayer(c: Canvas) {
        val h = engine.helicopter
        val art = SpriteCatalog.players[selectedPlayer]
        val degrees = h.heading * 180f / PI.toFloat()
        val scale = 1f + h.altitude * .001f
        c.save(); c.translate(h.pos.x, h.pos.y); c.rotate(degrees)
        c.scale(1f - abs(h.bank), 1f)
        assets.drawSprite(c, art.body, 0f, 0f, 0f, scale)
        val spin = h.rotorAngle * 180f / PI.toFloat()

        if (h.rpm > .58f) {
            assets.drawSprite(c, art.rotor, 0f, 0f, spin - 18f, scale, 45)
            assets.drawSprite(c, art.rotor, 0f, 0f, spin - 36f, scale, 23)
        }
        assets.drawSprite(c, art.rotor, 0f, 0f, spin, scale, if (h.rpm > .75f) 215 else 255)
        c.restore()
    }
    private fun drawEnemy(c: Canvas, enemy: Enemy) {
        val style = enemyVariants.getOrPut(enemy) { nextEnemyVariant++ % 3 }
        val spec = if (enemy.kind == EnemyKind.DRONE) SpriteCatalog.drones[style] else SpriteCatalog.enemyHelicopters[style]
        val angle = enemy.heading * 180f / PI.toFloat()
        assets.drawSprite(c, spec, enemy.pos.x + 14, enemy.pos.y + 16, angle, 1f, 70, true)
        assets.drawSprite(c, spec, enemy.pos.x, enemy.pos.y, angle)
        /* Enemy PNGs already contain rotor blur. Do not spin the whole helicopter or
         overlay a second four-blade rotor on the baked two-blade enemy artwork. */
    }
    private fun drawHud(c: Canvas) {
        val pause = controls.pauseRect
        panel(c, pause.left, pause.top, pause.width(), pause.height())
        text(c, "Ⅱ", pause.centerX(), pause.centerY() + 8, 23f, cream, Paint.Align.CENTER)
        val sound = controls.soundRect
        panel(c, sound.left, sound.top, sound.width(), sound.height())
        text(c, if (soundEnabled) "Sound ✓" else "Sound ×", sound.centerX(), sound.centerY() + 5, 11f, mint, Paint.Align.CENTER)
        panel(c, 22f, 24f, 243f, 204f)
        text(c, "Mission ${engine.mission.missionNumber} / ۵", 38f, 47f, 10f, muted, bold = true)
        val time = ceil(engine.remaining).toInt()
        text(c, "%02d:%02d".format(time / 60, time % 60), 39f, 88f, 33f, if (time < 30) orange else cream, bold = true)
        text(c, "Time", 249f, 80f, 11f, muted, Paint.Align.RIGHT)
        line(c, 39f, 102f, 249f, 102f, 0x447b9b82)
        text(c, "citizens", 248f, 127f, 14f, mint, Paint.Align.RIGHT)
        text(c, "${engine.rescued} / ${engine.mission.rescueGoal}", 39f, 127f, 20f, mint)
        text(c, "Enemy", 248f, 158f, 14f, orange, Paint.Align.RIGHT)
        text(c, "${engine.kills} / ${engine.mission.enemyGoal}", 39f, 158f, 20f, orange)
        text(c, "Health", 248f, 186f, 11f, muted, Paint.Align.RIGHT)
        text(c, "${engine.helicopter.health.toInt()}/${engine.mission.upgrade.maxHealth.toInt()}", 39f, 186f, 12f, cream)
        box(c, 39f, 204f, 208f, 4f, 0xff415f50.toInt())
        box(c, 39f, 204f, 208f * engine.helicopter.health / engine.mission.upgrade.maxHealth, 4f, if (engine.helicopter.health < 30) orange else mint)
        drawRadar(c)
        panel(c, width / 2 - 160, height - 139, 320f, 69f)
        val h = engine.helicopter
        val labels = listOf("Height", "Speed", "Engine")
        val values = listOf("${h.altitude.toInt()} m", "${(h.velocity.length() * .23f).toInt()} km/h", "${(h.rpm * 100).toInt()} %")
        for (i in 0..2) {
            val x = width / 2 - 105 + i * 105
            text(c, labels[i], x, height - 118, 10f, muted, Paint.Align.CENTER)
            text(c, values[i], x, height - 89, 22f, cream, Paint.Align.CENTER)
        }
    }
    @SuppressLint("UseKtx")
    private fun drawRadar(c: Canvas) {
        val x = width - 240; val y = 24f; val rw = 216f
        panel(c, x, y, rw, 180f)
        text(c, "LIVE RADAR", x + 15, y + 22, 10f, mint, bold = true)
        val mapX = x + 12; val mapY = y + 34; val mapW = 192f; val mapH = 108f
        c.save(); c.translate(mapX, mapY); c.scale(mapW / CityMap.WIDTH, mapH / CityMap.HEIGHT)
        assets.drawMap(c); c.restore()
        box(c, mapX, mapY, mapW, mapH, 0x60112a25)
        fun rx(px: Float) = mapX + px / CityMap.WIDTH * mapW
        fun ry(py: Float) = mapY + py / CityMap.HEIGHT * mapH
        text(c, "H", rx(engine.map.base.x), ry(engine.map.base.y) + 4, 10f, muted, Paint.Align.CENTER)
        for (s in engine.survivors) if (!s.rescued) {
            circle(c, rx(s.pos.x), ry(s.pos.y), 4f, mint)
            ring(c, rx(s.pos.x), ry(s.pos.y), 7f, 0x779adfa4)
        }
        for (e in engine.enemies) circle(c, rx(e.pos.x), ry(e.pos.y), 3.5f, orange)
        val h = engine.helicopter
        c.save(); c.translate(rx(h.pos.x), ry(h.pos.y)); c.rotate(h.heading * 180f / PI.toFloat())
        polygon(c, cream, 0f, -7f, 5f, 6f, 0f, 3f, -5f, 6f); c.restore()
        text(c, "Rescue ●", x + 43, y + 165, 15f, mint, Paint.Align.CENTER)
        text(c, "Enemy ●", x + 110, y + 165, 15f, orange, Paint.Align.CENTER)
        text(c, "You ▲", x + 179, y + 165, 15f, cream, Paint.Align.CENTER)
        text(c, "16 × 12 GRID  /  4 × 4 Map ", x + rw / 2, y + 200, 9f, cream, Paint.Align.CENTER)
    }
    private fun drawControls(c: Canvas) {
        val x = controls.stickX; val y = controls.stickY; val r = controls.stickRadius
        text(c, "FLIGHT CONTROL / 360°", x, y - r - 20, 15f, mint, Paint.Align.CENTER)
        assets.drawInto(c, "ui/joystick/Controler.png", RectF(x - r, y - r, x + r, y + r))
        val knobX = x + controls.knobX * .58f; val knobY = y + controls.knobY * .58f
        assets.drawInto(c, "ui/joystick/button.png", RectF(knobX - 31, knobY - 31, knobX + 31, knobY + 31))
        text(c, "Movement & Rotation", x, height - 63, 15f, muted, Paint.Align.CENTER)
        val tr = controls.throttleRect
        text(c, "COLLECTIVE", tr.centerX(), tr.top - 18, 15f, mint, Paint.Align.CENTER)
        assets.drawInto(c, "ui/throttle/Range.png", tr)
        val knob = controls.throttleTrackBottom - engine.throttle * (controls.throttleTrackBottom - controls.throttleTrackTop)
        val trackX = controls.throttleTrackX
        assets.drawInto(c, "ui/throttle/button.png", RectF(trackX - 24, knob - 8, trackX + 24, knob + 8))
        text(c, "${(engine.throttle * 100).toInt()}%", tr.centerX(), tr.bottom + 12, 18f, cream, Paint.Align.CENTER, true)
        val fire = controls.fireRect
        box(c, fire.left, fire.top, fire.width(), fire.height(), if (engine.firing) 0xdd966440.toInt() else 0xee284037.toInt(), 12f)
        text(c, if (engine.mission.upgrade.barrels == 2) "⌖  ⌖" else "⌖", fire.centerX(), fire.top + 42, 34f, orange, Paint.Align.CENTER)
        text(c, if (engine.mission.upgrade.barrels == 2) "Double Shot" else "Fire", fire.centerX(), fire.bottom - 21, 17f, orange, Paint.Align.CENTER)
    }
    private fun drawModal(c: Canvas, title: String, subtitle: String, action: String, briefing: Boolean) {
        box(c, 0f, 0f, width, height, 0xb507211f.toInt())
        val cx = width / 2; val cy = height / 2
        panel(c, cx - 280, cy - 230, 560f, 480f)
        text(c, "AIR RESCUE UNIT  /  SECTOR 07", cx, cy - 195, 17f, mint, Paint.Align.CENTER)
        text(c, title, cx, cy - 144, if (title.length > 15) 29f else 42f, cream, Paint.Align.CENTER, true)
        text(c, subtitle, cx, cy - 105, 25f, mint, Paint.Align.CENTER, true)
        if (briefing) {
            text(c, "${engine.mission.rescueGoal} Citizen  •  ${engine.mission.enemyGoal} Enemy  •  ${engine.mission.seconds.toInt()} Second", cx, cy - 63, 17f, cream, Paint.Align.CENTER)
            text(c, "1.Raise The right slider to speed up", cx, cy - 16, 15f, cream, Paint.Align.CENTER)
            val choice = playerChoiceRect
            box(c, choice.left, choice.top, choice.width(), choice.height(), 0xff294a42.toInt(), 6f)
            text(c, "Heli: ${SpriteCatalog.players[selectedPlayer].title}  ‹ Touch for change ›", cx, cy + 89, 14f, mint, Paint.Align.CENTER)
        } else {
            text(c, engine.outcome, cx, cy - 34, 15f, cream, Paint.Align.CENTER)
            text(c, "Rescue: ${engine.rescued}/${engine.mission.rescueGoal} " + " Enemy: ${engine.kills}/${engine.mission.enemyGoal}", cx, cy + 17, 20f, mint, Paint.Align.CENTER)
            text(c, if (engine.phase == Phase.WON) "Reward: $earnedCoins Coin" else "Mission ${engine.mission.missionNumber} / 5", cx, cy + 65, 25f, mint, Paint.Align.CENTER)
        }
        val r = primaryRect
        box(c, r.left, r.top, r.width(), r.height(), mint, 7f)
        text(c, action, cx, r.centerY() + 7, 20f, ink, Paint.Align.CENTER, true)
        if (!briefing) {
            val secondary = secondaryRect
            text(c, "RETURN TO HANGAR", cx, secondary.centerY() + 6, 24f, muted, Paint.Align.CENTER)
        } else text(c, "Helicopter SR–07  •  Ready for takeoff", cx, cy + 220, 22f, muted, Paint.Align.CENTER)
    }
}

package com.example.a2dgamebox.city_helicopter


import kotlin.math.*
import kotlin.random.Random

/** Fixed-step simulation, independent of Canvas, input devices and Android lifecycle. */
class GameEngine(seed: Int = 7) {
    val map = CityMap()
    private val random = Random(seed)
    var mission = Mission.forLevel(1); private set
    var phase = Phase.BRIEFING; private set
    var helicopter = Helicopter(); private set
    val survivors = mutableListOf<Survivor>()
    val enemies = mutableListOf<Enemy>()
    val bullets = mutableListOf<Bullet>()
    val particles = mutableListOf<Particle>()
    val movement = Vec2()
    var throttle = 0f
    var firing = false
    private var shotRequested = false
    private val audioEvents = mutableListOf<AudioCue>()
    fun drainAudioEvents(): List<AudioCue> = audioEvents.toList().also { audioEvents.clear() }
    fun requestShot() { if (phase == Phase.PLAYING) shotRequested = true }
    private fun sound(cue: AudioCue) { if (audioEvents.size < 48) audioEvents.add(cue) }
    var remaining = 240f; private set
    var kills = 0; private set
    val rescued get() = survivors.count { it.rescued }
    var elapsed = 0f; private set
    var notification = "Power Up the engine"; private set
    var notificationTime = 0f; private set
    var outcome = ""; private set
    private var spawned = 0
    private var spawnDelay = 4f
    private var shotDelay = 0f
    private var tookOff = false

    init { prepare(1) }

    fun prepare(level: Int) {
        mission = Mission.forLevel(level)
        helicopter = Helicopter(pos = map.base.copy(), health = mission.upgrade.maxHealth)
        survivors.clear(); survivors.addAll(map.rescueLocations(mission.rescueGoal, level).map { Survivor(it) })
        enemies.clear()
        bullets.clear()
        particles.clear()
        remaining = mission.seconds
        kills = 0
        elapsed = 0f; spawned = 0
        spawnDelay = 4f
        shotDelay = 0f
        tookOff = false
        throttle = 0f
        movement.set(0f, 0f)
        firing = false
        phase = Phase.BRIEFING
        outcome = ""
        shotRequested = false
        audioEvents.clear()
        notify("Power up the engine to 75%", 7f)
    }

    fun start() { if (phase == Phase.BRIEFING) phase = Phase.PLAYING }
    fun pause() { if (phase == Phase.PLAYING) { phase = Phase.PAUSED; clearInput() } }
    fun resume() { if (phase == Phase.PAUSED) phase = Phase.PLAYING }
    fun clearInput() { movement.set(0f, 0f); firing = false; shotRequested = false }
    fun notify(text: String, seconds: Float = 3f) { notification = text; notificationTime = seconds }
    fun update(dt: Float) {
        if (phase != Phase.PLAYING || dt <= 0f) return
        val step = dt.coerceAtMost(.05f)
        remaining = max(0f, remaining - step); elapsed += step
        notificationTime = max(0f, notificationTime - step)
        updateFlight(step)
        updateRescue(step)
        updateEnemies(step)
        updateWeapons(step)
        updateParticles(step)
        when {
            helicopter.health <= 0f -> finish(false, "Try Again.")
            remaining <= 0f -> finish(false, "Time up.")
            rescued >= mission.rescueGoal && kills >= mission.enemyGoal -> finish(true, "Everything is clear.")
        }
    }
    private fun finish(won: Boolean, message: String) {
        phase = if (won) Phase.WON else Phase.LOST; outcome = message; clearInput()
        sound(if (won) AudioCue.WIN else AudioCue.LOSE)
    }
    private fun updateFlight(dt: Float) {
        val h = helicopter
        throttle = throttle.coerceIn(0f, 1f)
        // First-order rotor inertia: a lever change never instantly changes rotor RPM.
        h.rpm = approach(h.rpm, throttle, if (throttle > h.rpm) .75f else .58f, dt)
        h.rotorAngle = (h.rotorAngle + h.rpm * 49f * dt) % (2f * PI.toFloat())
        val targetAltitude = max(0f, (h.rpm - .30f) / .70f) * 180f
        h.altitude = approach(h.altitude, targetAltitude, .9f, dt)
        val magnitude = movement.length()
        val dx = if (magnitude > 1f) movement.x / magnitude else movement.x
        val dy = if (magnitude > 1f) movement.y / magnitude else movement.y
        val maxSpeed = 290f * mission.upgrade.speedFactor * (.4f + .6f * h.rpm)
        val canMove = h.altitude > 7f
        h.velocity.x = approach(h.velocity.x, if (canMove) dx * maxSpeed else 0f, 2.4f, dt)
        h.velocity.y = approach(h.velocity.y, if (canMove) dy * maxSpeed else 0f, 2.4f, dt)
        val speed = h.velocity.length()
        if (speed > 5f) {
            val desired = atan2(h.velocity.y, h.velocity.x) + PI.toFloat() / 2f
            val turn = angleDifference(desired, h.heading)
            h.heading += turn * min(1f, dt * 3.3f)
            h.bank = approach(h.bank, (turn * .26f).coerceIn(-.23f, .23f), 4f, dt)
        } else h.bank = approach(h.bank, 0f, 3f, dt)
        h.pos.x = (h.pos.x + h.velocity.x * dt).coerceIn(40f, CityMap.WIDTH - 40f)
        h.pos.y = (h.pos.y + h.velocity.y * dt).coerceIn(40f, CityMap.HEIGHT - 40f)
        if (!tookOff && h.altitude > 30f) {
            tookOff = true //notify("Go to the HELP sides", 5f)
        }
        if (h.pos.distance(map.base) < 80f && h.altitude < 18f){
            h.health = min(mission.upgrade.maxHealth, h.health + dt * 7f)
        }
    }
    private fun updateRescue(dt: Float) {
        val h = helicopter
        for (person in survivors) {
            if (person.rescued) continue
            val close = h.pos.distance(person.pos) < 66f
            val heightOK = h.altitude in 18f..60f
            val steady = h.velocity.length() < 65f
            if (close && heightOK && steady) {
                person.progress = min(1f, person.progress + dt / 2.6f)
                if (person.progress >= 1f) {
                    person.rescued = true
                    burst(person.pos, false, 18)
                    sound(AudioCue.RESCUE)
                    notify("Citizen saved!  $rescued / ${mission.rescueGoal}", 4f)
                }
            } else {
                person.progress = max(0f, person.progress - dt * .6f)
                if (close && notificationTime <= 0f) notify(
                    if (!heightOK) "To Recue people power up to 45%" else "Leave the analog and pause on buildings", 1.8f)
            }
        }
    }
    private fun spawnEnemy() {
        val edge = spawned % 4
        val p = when (edge) {
            0 -> Vec2(CityMap.WIDTH - 30f, random.nextFloat() * CityMap.HEIGHT)
            1 -> Vec2(random.nextFloat() * CityMap.WIDTH, 30f)
            2 -> Vec2(30f, random.nextFloat() * CityMap.HEIGHT)
            else -> Vec2(random.nextFloat() * CityMap.WIDTH, CityMap.HEIGHT - 30f)
        }
        val kind = if (spawned % 3 == 2) EnemyKind.HELICOPTER else EnemyKind.DRONE
        enemies.add(
            Enemy(
                p,
                kind,
                if (kind == EnemyKind.DRONE) mission.droneHits.toFloat() else mission.helicopterHits.toFloat(),
                2f + random.nextFloat(),
                orbit = if (spawned % 2 == 0) 1f else -1f
            )
        )
        spawned++
    }
    private fun updateEnemies(dt: Float) {
        if (!tookOff) return
        spawnDelay -= dt
        val activeLimit = mission.simultaneousEnemies
        if (spawned < mission.enemyGoal && enemies.size < activeLimit && spawnDelay <= 0f) {
            spawnEnemy()
            spawnDelay = max(3f, 7f - mission.level * .25f)
        }
        for (enemy in enemies) {
            val dx = helicopter.pos.x - enemy.pos.x
            val dy = helicopter.pos.y - enemy.pos.y
            val distance = max(1f, hypot(dx, dy))
            val forward = if (distance > 310f) 1f else if (distance < 210f) -.6f else 0f
            val tangent = if (distance < 500f) .65f * enemy.orbit else .15f * enemy.orbit
            val speed = if (enemy.kind == EnemyKind.DRONE) 97f else 76f
            enemy.velocity.set((dx / distance * forward - dy / distance * tangent) * speed,
                (dy / distance * forward + dx / distance * tangent) * speed)
            enemy.pos.x = (enemy.pos.x + enemy.velocity.x * dt).coerceIn(20f, CityMap.WIDTH - 20f)
            enemy.pos.y = (enemy.pos.y + enemy.velocity.y * dt).coerceIn(20f, CityMap.HEIGHT - 20f)
            enemy.heading = atan2(dy, dx) + PI.toFloat() / 2f
            enemy.cooldown -= dt
            if (distance < 700f && enemy.cooldown <= 0f) {
                // Slightly imperfect aim keeps manoeuvring useful and combat fair.
                val angle = atan2(dy, dx) + (random.nextFloat() - .5f) * .16f
                bullets.add(
                    Bullet(
                        enemy.pos.copy(),
                        Vec2(cos(angle) * 330f, sin(angle) * 330f),
                        false,
                        2.7f
                    )
                )
                enemy.cooldown = (if (enemy.kind == EnemyKind.DRONE) 2.1f else 1.45f) / (1f + min(mission.level, 8) * .035f)
            }
        }
    }
    private fun updateWeapons(dt: Float) {
        shotDelay -= dt
        val h = helicopter
        val target = enemies.minByOrNull { h.pos.distance(it.pos) }
        val inRange = target != null && h.pos.distance(target.pos) < 720f
        if (h.altitude > 8f && shotDelay <= 0f && (firing || shotRequested)) {
            // Lead a moving target, otherwise circling drones could evade every aimed shot.
            val angle = if (inRange) {
                val travel = h.pos.distance(target.pos) / 720f
                atan2(target.pos.y + target.velocity.y * travel - h.pos.y, target.pos.x + target.velocity.x * travel - h.pos.x)
            } else h.heading - PI.toFloat() / 2
            val dx = cos(angle); val dy = sin(angle)
            val barrels = mission.upgrade.barrels
            repeat(barrels) { barrel ->
                val offset = if (barrels == 1) 0f else if (barrel == 0) -9f else 9f
                bullets.add(
                    Bullet(
                        Vec2(h.pos.x + dx * 36 - dy * offset, h.pos.y + dy * 36 + dx * offset),
                        Vec2(dx * 720, dy * 720), true, 1.15f
                    )
                )
            }
            sound(AudioCue.SHOT); shotRequested = false
            shotDelay = mission.upgrade.shotInterval
        }
        val iterator = bullets.iterator()
        while (iterator.hasNext()) {
            val bullet = iterator.next()
            val old = bullet.pos.copy()
            bullet.pos.x += bullet.velocity.x * dt
            bullet.pos.y += bullet.velocity.y * dt
            bullet.life -= dt
            var hit = false
            if (bullet.friendly) {
                val enemy = enemies.firstOrNull { segmentDistance(it.pos, old, bullet.pos) < if (it.kind == EnemyKind.DRONE) 24f else 33f }
                if (enemy != null) {
                    enemy.health -= 1f
                    hit = true
                    burst(bullet.pos, true, 3)
                    if (enemy.health <= 0f) {
                        burst(enemy.pos, true, 27)
                        enemies.remove(enemy)
                        kills++
                        sound(AudioCue.EXPLOSION)
                        if (kills == mission.enemyGoal) notify("All clear!", 4f)
                    }
                }
            } else if (segmentDistance(h.pos, old, bullet.pos) < 24f) {
                h.health = max(0f, h.health - 6f)
                hit = true
                burst(bullet.pos, true, 6)
            }
            if (hit || bullet.life <= 0f) iterator.remove()
        }
    }
    private fun burst(pos: Vec2, warm: Boolean, count: Int) {
        repeat(count) {
            val angle = random.nextFloat() * 2f * PI.toFloat()
            val speed = 25f + random.nextFloat() * 130f
            val life = .3f + random.nextFloat() * .7f
            particles.add(
                Particle(
                    pos.copy(),
                    Vec2(cos(angle) * speed, sin(angle) * speed),
                    life,
                    life,
                    warm
                )
            )
        }
    }
    private fun updateParticles(dt: Float) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.life -= dt
            p.pos.x += p.velocity.x * dt
            p.pos.y += p.velocity.y * dt
            if (p.life <= 0f) iterator.remove()
        }
    }
    companion object {
        /** Swept collision avoids bullets crossing a target between two simulation frames. */
        fun segmentDistance(p: Vec2, a: Vec2, b: Vec2): Float {
            val dx = b.x - a.x
            val dy = b.y - a.y
            val length = dx * dx + dy * dy
            if (length <= .0001f) return p.distance(a)
            val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / length).coerceIn(0f, 1f)
            return hypot(p.x - a.x - t * dx, p.y - a.y - t * dy)
        }
    }
}

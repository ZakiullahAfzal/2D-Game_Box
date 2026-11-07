package com.example.a2dgamebox.city_helicopter

import kotlin.math.*

/** Pure Kotlin models: no Android dependency, so the simulation is unit-testable. */
data class Vec2(var x: Float = 0f, var y: Float = 0f) {
    fun length() = hypot(x, y)
    fun distance(other: Vec2) = hypot(x - other.x, y - other.y)
    fun set(a: Float, b: Float) { x = a; y = b }
}
enum class Phase { BRIEFING, PLAYING, PAUSED, WON, LOST }
enum class EnemyKind { DRONE, HELICOPTER }
data class Helicopter(
    val pos: Vec2 = Vec2(), val velocity: Vec2 = Vec2(), var heading: Float = 0f,
    var bank: Float = 0f, var altitude: Float = 0f, var rpm: Float = 0f,
    var rotorAngle: Float = 0f, var health: Float = 100f
)
data class Survivor(val pos: Vec2, var progress: Float = 0f, var rescued: Boolean = false)
data class Enemy(val pos: Vec2, val kind: EnemyKind, var health: Float,
                 var cooldown: Float, var heading: Float = 0f,
                 val orbit: Float = 1f, val velocity: Vec2 = Vec2(),
                 val maxHealth: Float = health)
data class Bullet(val pos: Vec2, val velocity: Vec2, val friendly: Boolean, var life: Float = 2.2f)
data class Particle(val pos: Vec2, val velocity: Vec2, var life: Float, val total: Float, val warm: Boolean)

data class Mission(val level: Int, val rescueGoal: Int, val enemyGoal: Int, val seconds: Float,
                   val droneHits: Int, val helicopterHits: Int, val simultaneousEnemies: Int) {
    val aircraftIndex get() = (level - 1) / 5
    val missionNumber get() = (level - 1) % 5 + 1
    val upgrade get() = Campaign.aircraft[aircraftIndex]
    companion object {
        fun forLevel(level: Int): Mission {
            val n = level.coerceIn(1, Campaign.TOTAL_MISSIONS)
            val aircraft = (n - 1) / 5 // for aircrafts
            val local = (n - 1) % 5  // for missions
            val droneHits = when { n <= 2 -> 2; n <= 5 -> 3; n <= 10 -> 4; else -> 5 }
            val helicopterHits = when { n <= 2 -> 3; n <= 5 -> 4; else -> 5 }
            return Mission(n, (2 + aircraft + local / 2).coerceAtMost(8),
                6 + (n - 1) * 2, 300f + aircraft * 40f - local * 10f,
                droneHits, helicopterHits, (2 + (n - 1) / 4).coerceAtMost(6))
        }
    }
}
enum class AudioCue { SHOT, EXPLOSION, RESCUE, WIN, LOSE }

// Start range 30% : 70% maximum range because we start from 30 it means 100 - 30 = 70
// we used exp e^x approach(0.20f, 0.80f, 0.75f, 0.01667f)
// نحوه و سرعت رسیدن نرم به قدرت انتخاب‌شده را تعیین می‌کند. exp
// rate × dt = 0.75 × 0.01667 ≈ 0.0125
//
//exp(-0.0125) ≈ 0.98758
//
//سهم تغییر = 1 - 0.98758 ≈ 0.01242
fun approach(value: Float, target: Float, rate: Float, dt: Float) = value + (target - value) * (1f - exp(-rate * dt))
fun angleDifference(target: Float, current: Float): Float = atan2(sin(target - current), cos(target - current))

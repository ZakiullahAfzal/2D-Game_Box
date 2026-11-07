package com.example.a2dgamebox.city_helicopter

import com.example.a2dgamebox.city_helicopter.CityMap

/** Art calibration is kept out of the simulation. Coordinates are ORIGINAL PNG pixels.
 * The engine's heading=0 faces up; right-facing source art needs -90 degrees.
 * The body's rotor mast and the rotor's hub both map to the aircraft world position.
 */
data class SpriteSpec(
    val path: String,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val pivotX: Float,
    val pivotY: Float,
    val correctionDegrees: Float,
    val worldLength: Float,
    val lengthUsesWidth: Boolean = true
)
data class AircraftArt(val title: String, val body: SpriteSpec, val rotor: SpriteSpec)

object SpriteCatalog {
    private fun rotor(file: String, w: Int, h: Int, x: Float, y: Float, size: Float = 145f) =
        SpriteSpec("rotors/$file", w, h, x, y, 0f, size)
    val players = listOf(
        AircraftArt("RESCUE", SpriteSpec("players/plaery00.png", 1672, 941, 986f, 445f, -90f, 162f),
            rotor("player01_fan.png", 1338, 1175, 668f, 588f)),
        AircraftArt("APACHE", SpriteSpec("players/player03.png", 644, 1280, 323f, 452f, 0f, 172f, false),
            rotor("player03_fan.png", 762, 722, 385f, 365f, 145f)),
        AircraftArt("NIGHTHAWK", SpriteSpec("players/player01.png", 1672, 941, 974f, 466f, -90f, 168f),
            rotor("player01_fan.png", 1338, 1175, 668f, 588f)),
        AircraftArt("ORANGE", SpriteSpec("players/plyaer02.png", 1672, 941, 995f, 446f, -90f, 168f),
            rotor("player02_fan.png", 1362, 1155, 687f, 562f)),
        // No separate fan named player00 was supplied: the first supplied rotor is reused.

    )
    val enemyHelicopters = listOf(
        SpriteSpec("enemies/eh1.png", 937, 505, 540f, 255f, -90f, 147f),
        SpriteSpec("enemies/eh2.png", 937, 485, 546f, 249f, -90f, 147f),
        SpriteSpec("enemies/eh3.png", 936, 502, 548f, 256f, -90f, 147f)
    )
    // Drones in these source files face DOWN; normalize to the engine's up-facing convention.
    val drones = listOf(
        SpriteSpec("drones/e1.png", 665, 528, 334f, 269f, 180f, 92f),
        SpriteSpec("drones/e2.png", 666, 518, 335f, 263f, 180f, 92f),
        SpriteSpec("drones/e3.png", 640, 494, 321f, 253f, 180f, 92f)
    )
    val uiFiles = listOf("ui/joystick/Controler.png", "ui/joystick/button.png", "ui/throttle/Range.png", "ui/throttle/button.png")
    val mapFiles = (0 until CityMap.CHUNK_ROWS).flatMap { row ->
        (0 until CityMap.CHUNK_COLS).map { col -> "Map/city_r${row}_c${col}.png" }
    }
    val allSpecs = (players.flatMap { listOf(it.body, it.rotor) } + enemyHelicopters + drones).distinctBy { it.path }
    val requiredFiles = mapFiles + uiFiles + allSpecs.map { it.path }
}

package com.example.a2dgamebox.city_helicopter



/** Logical cells stay 16 x 12; the supplied background is 4 x 4 image chunks.
 * Rectangular logical cells preserve the original map's 16:9 aspect ratio. */
class CityMap {
    companion object {
        const val COLS = 16
        const val ROWS = 12
        const val TILE = 180f // Legacy model compatibility: logical cell width.
        const val TILE_WIDTH = TILE
        const val TILE_HEIGHT = 135f
        const val WIDTH = COLS * TILE_WIDTH
        const val HEIGHT = ROWS * TILE_HEIGHT
        const val IMAGE_WIDTH = 1536f
        const val IMAGE_HEIGHT = 864f
        const val CHUNK_COLS = 4
        const val CHUNK_ROWS = 4
        const val CHUNK_PIXEL_WIDTH = 384
        const val CHUNK_PIXEL_HEIGHT = 216
        fun imageToWorld(x: Float, y: Float) =
            Vec2(x / IMAGE_WIDTH * WIDTH, y / IMAGE_HEIGHT * HEIGHT)
    }
    // Center of the H actually painted in the supplied city image.
    val base = imageToWorld(893f, 386f)

    // Hand-positioned on visible rooftops in the supplied map, NOT the former procedural city.
    val rooftops = listOf(
        imageToWorld(753f, 451f),
        imageToWorld(1172f, 647f),
        imageToWorld(738f, 174f),
        imageToWorld(151f, 143f),
        imageToWorld(1080f, 170f),
        imageToWorld(64f, 676f),
        imageToWorld(492f, 687f),
        imageToWorld(1437f, 303f)
    )
    fun rescueLocations(count: Int, level: Int): List<Vec2> {
        require(count in 0..rooftops.size)
        val shift = (level.coerceAtLeast(1) - 1) % rooftops.size
        return (0 until count).map { rooftops[(it + shift) % rooftops.size].copy() }
    }
}

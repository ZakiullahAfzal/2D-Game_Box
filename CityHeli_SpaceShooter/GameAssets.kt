package com.example.a2dgamebox.city_helicopter

import android.content.res.AssetManager
import android.graphics.*
import java.io.IOException
import kotlin.math.max

/** Loads once, never in onDraw. Files live in app/src/main/assets/game/.
 * Bitmap bounds exclude transparent padding at drawing time; originals are not rewritten.
 */
class GameAssets(private val manager: AssetManager) {
    data class Sprite(val bitmap: Bitmap, val source: Rect, val pivotX: Float = 0f, val pivotY: Float = 0f)
    private val images = mutableMapOf<String, Sprite>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val dst = RectF()
    private val shadowFilter = PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)

    init {
        val missing = SpriteCatalog.requiredFiles.filter { path ->
            try { manager.open("game/$path").use { }; false } catch (_: IOException) { true }
        }
        require(missing.isEmpty()) { "Missing assets in app/src/main/assets/game/:\n" + missing.joinToString("\n") }
        for (file in SpriteCatalog.mapFiles) {
            val bmp = load(file, 512)
            require(bmp.width == CityMap.CHUNK_PIXEL_WIDTH && bmp.height == CityMap.CHUNK_PIXEL_HEIGHT) {
                "Wrong map chunk dimensions: $file (expected 384 x 216)"
            }
            images[file] = Sprite(bmp, Rect(0, 0, bmp.width, bmp.height))
        }
        for (file in SpriteCatalog.uiFiles) {
            val bmp = load(file, 512)
            images[file] = Sprite(bmp, opaqueBounds(bmp))
        }
        for (spec in SpriteCatalog.allSpecs) {
            val bmp = load(spec.path, 768)
            images[spec.path] = Sprite(bmp, opaqueBounds(bmp),
                spec.pivotX / spec.sourceWidth * bmp.width, spec.pivotY / spec.sourceHeight * bmp.height)
        }
    }
    private fun load(path: String, maxSize: Int): Bitmap {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true; inScaled = false }
        manager.open("game/$path").use { BitmapFactory.decodeStream(it, null, options) }
        require(options.outWidth > 0 && options.outHeight > 0) { "Invalid PNG: $path" }
        var sample = 1
        while (max(options.outWidth, options.outHeight) / sample > maxSize) sample *= 2
        options.inJustDecodeBounds = false; options.inSampleSize = sample
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        return manager.open("game/$path").use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Cannot decode PNG: $path")
    }
    private fun opaqueBounds(b: Bitmap): Rect {
        val pixels = IntArray(b.width * b.height)
        b.getPixels(pixels, 0, b.width, 0, 0, b.width, b.height)
        var left = b.width; var top = b.height; var right = -1; var bottom = -1
        for (y in 0 until b.height) for (x in 0 until b.width) {
            if ((pixels[y * b.width + x] ushr 24) > 24) {
                if (x < left) left = x; if (x > right) right = x
                if (y < top) top = y; if (y > bottom) bottom = y
            }
        }
        require(right >= left && bottom >= top) { "A supplied sprite is entirely transparent" }
        return Rect(left, top, right + 1, bottom + 1)
    }
    fun drawInto(c: Canvas, path: String, target: RectF, alpha: Int = 255) {
        val sprite = images.getValue(path)
        paint.colorFilter = null; paint.alpha = alpha
        c.drawBitmap(sprite.bitmap, sprite.source, target, paint)
        paint.alpha = 255
    }
    fun drawSprite(c: Canvas, spec: SpriteSpec, x: Float, y: Float, angleDegrees: Float,
                   extraScale: Float = 1f, alpha: Int = 255, shadow: Boolean = false) {
        val sprite = images.getValue(spec.path)
        val visibleLength = if (spec.lengthUsesWidth) sprite.source.width() else sprite.source.height()
        val factor = spec.worldLength / visibleLength * extraScale
        dst.set((sprite.source.left - sprite.pivotX) * factor, (sprite.source.top - sprite.pivotY) * factor,
            (sprite.source.right - sprite.pivotX) * factor, (sprite.source.bottom - sprite.pivotY) * factor)
        paint.alpha = alpha.coerceIn(0, 255); paint.colorFilter = if (shadow) shadowFilter else null
        c.save(); c.translate(x, y); c.rotate(angleDegrees + spec.correctionDegrees)
        c.drawBitmap(sprite.bitmap, sprite.source, dst, paint)
        c.restore(); paint.alpha = 255; paint.colorFilter = null
    }
    fun drawMap(c: Canvas, visible: RectF? = null) {
        val w = CityMap.WIDTH / CityMap.CHUNK_COLS
        val h = CityMap.HEIGHT / CityMap.CHUNK_ROWS
        // All chunks share one canvas transform; integer shared edges prevent layout gaps.
        for (row in 0 until CityMap.CHUNK_ROWS) for (col in 0 until CityMap.CHUNK_COLS) {
            dst.set(col * w, row * h, (col + 1) * w, (row + 1) * h)
            if (visible == null || RectF.intersects(dst, visible)) {
                drawInto(c, "Map/city_r${row}_c${col}.png", dst)
            }
        }
    }
}

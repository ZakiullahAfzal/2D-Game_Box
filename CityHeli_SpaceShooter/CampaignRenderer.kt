package com.example.a2dgamebox.city_helicopter

import android.content.Context
import android.graphics.*


/** Game box -> four-aircraft hangar -> five missions. Uses existing art unchanged. */
class CampaignRenderer(
    context: Context,
    private val assets: GameAssets,
    private val progress: CampaignProgress,
    private val ui: CampaignUi,
    Cwidth: Float,
    Cheight: Float
) {
    var width = Cwidth//1280f
    var height = Cheight//720f
    var soundEnabled = true
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val navy = 0xff0c2029.toInt()
    private val panel = 0xee142f3a.toInt()
    private val white = 0xffe3ece8.toInt()
    private val muted = 0xff93aab3.toInt()
    private val green = 0xffb7dfb8.toInt()
    private val gold = 0xffffcc82.toInt()
    val backRect get() = RectF(28f, 24f, 135f, 74f)

    //  val soundRect get() = RectF(width - 92f, 24f, width - 28f, 74f)
    val hubRect get() = RectF(width / 2 - 265f, 164f, width / 2 + 265f, 581f)
    val buyRect get() = RectF(width / 2 - 210f, 433f, width / 2 + 210f, 492f)
    val cancelRect get() = RectF(width / 2 - 210f, 505f, width / 2 + 210f, 551f)
    fun aircraftRect(index: Int): RectF {
        val gap = 16f;
        val card = (width - 64f - gap * 3) / 4
        val x = 32f + index * (card + gap)
        return RectF(x, 163f, x + card, 597f)
    }

    fun missionRect(index: Int) =
        RectF(width / 2 - 400f, 155f + index * 89f, width / 2 + 400f, 230f + index * 89f)

    private fun box(c: Canvas, r: RectF, color: Int, radius: Float = 12f) {
        p.color = color; p.style = Paint.Style.FILL
        c.drawRoundRect(r, radius, radius, p)
    }

    private fun label(
        c: Canvas,
        s: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int = white,
        bold: Boolean = false,
        align: Paint.Align = Paint.Align.CENTER
    ) {
        p.color = color; p.textSize = size; p.style = Paint.Style.FILL; p.textAlign = align
        p.typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        c.drawText(s, x, y, p)
    }

    private fun title(c: Canvas, value: String, subtitle: String) {
        label(c, value, width / 2, 61f, 29f, white, true)
        label(c, subtitle, width / 2, 105f, 15f, muted)
        // if (ui.screen != GameScreen.HUB) {
        box(c, backRect, panel); label(c, "Back", backRect.centerX(), 57f, 25f)
        // }
//        box(c, soundRect, panel)
//        label(
//            c,
//            if (soundEnabled) "Sound Off" else "Sound On",
//            soundRect.centerX(),
//            56f,
//            13f,
//            green
//        )
        label(
            c, "Coin ${progress.coins}", width - 115f, 120f, 18f, gold
        )
    }

    fun draw(c: Canvas) {
        c.drawColor(navy)
        c.save(); c.scale(width / CityMap.WIDTH, height / CityMap.HEIGHT)
        assets.drawMap(c); c.restore()
        box(c, RectF(0f, 0f, width, height), 0xea0b202a.toInt(), 0f)
        when (ui.screen) {
            // GameScreen.HUB -> drawHub(c)
            GameScreen.HANGAR -> drawHangar(c)
            GameScreen.MISSIONS -> drawMissions(c)
            GameScreen.FLIGHT -> Unit
        }
        if (ui.purchaseDialog) drawPurchase(c)
    }

    /*
    private fun drawHub(c: Canvas) {
        title(c, "City Helicopter", "Select")
        val r = hubRect
        box(c, r, panel)
        label(c, "SKY RESCUE", r.centerX(), 217f, 33f, white, true)
        val art = SpriteCatalog.players[0]
        assets.drawSprite(c, art.body, r.centerX(), 328f, 90f, 2f)
        assets.drawSprite(c, art.rotor, r.centerX(), 328f, 25f, 2f)
        label(c, "Rescue operation with Heli", r.centerX(), 452f, 23f, green, true)
        label(c, "۴ چرخ‌بال  •  ۲۰ مأموریت  •  خرید با سکه", r.centerX(), 491f, 25f, muted)
        box(c, RectF(r.left + 25, 519f, r.right - 25, 564f), green)
        label(c, "ورود به آشیانه  ←", r.centerX(), 548f, 18f, navy, true)
    }

    */

    private fun drawHangar(c: Canvas) {
        title(c, "Helicopters Hangar", "Click to play")
        for (i in 0..3) {
            val r = aircraftRect(i);
            val spec = Campaign.aircraft[i];
            val art = SpriteCatalog.players[i]
            val owned = progress.owns(i)
            box(c, r, if (owned) panel else 0xee152731.toInt())
            label(c, "0${i + 1}", r.left + 23, 199f, 16f, muted, align = Paint.Align.LEFT)
            label(
                c,
                if (owned) "Accessible" else "Lock",
                r.right - 17,
                199f,
                14f,
                if (owned) green else gold,
                align = Paint.Align.RIGHT
            )
            val scale = ((r.width() - 34) / 185f).coerceAtMost(1.7f)
            assets.drawSprite(c, art.body, r.centerX(), 292f, 90f, scale, if (owned) 255 else 150)
            assets.drawSprite(c, art.rotor, r.centerX(), 292f, 25f, scale, if (owned) 255 else 130)
            label(c, spec.name, r.centerX(), 393f, 18f, white, true)
            label(
                c,
                "Health ${spec.maxHealth.toInt()}  •  ${if (spec.barrels == 2) "Double" else "Single"}",
                r.centerX(),
                434f,
                13f,
                muted
            )
            label(
                c,
                if (owned) "Mission: ${progress.completedCount(i)} / 5" else "Coin ${spec.price}",
                r.centerX(),
                471f,
                20f,
                gold
            )
            if (!owned) label(c, "Need: The previous helicopter mission 5", r.centerX(), 503f, 11f, muted)
            val button = RectF(r.left + 14, 532f, r.right - 14, 578f)
            box(c, button, if (owned) green else 0xff385264.toInt())
            label(
                c,
                if (owned) "Mission Selection" else "View Purchase",
                r.centerX(),
                562f,
                15f,
                if (owned) navy else white,
                true
            )
        }

    }

    private fun drawMissions(c: Canvas) {
        val aircraft = ui.selectedAircraft
        title(
            c,
            "${Campaign.aircraft[aircraft].name} Five Mission",
               "Click to play"
        )
        for (i in 0..4) {
            val r = missionRect(i);
            val m = Mission.forLevel(Campaign.level(aircraft, i))
            val unlocked = progress.canPlay(aircraft, i);
            val done = progress.completed(aircraft, i)
            box(c, r, if (unlocked) panel else 0xdd14252d.toInt())
            label(c, "Mission ${i + 1}", r.right - 25, r.top + 30, 20f, if (unlocked) white else muted, true,
                Paint.Align.RIGHT
            )
            label(
                c,
                "${m.rescueGoal} Rescue • ${m.enemyGoal} Enemy • Health ${m.droneHits}/${m.helicopterHits} Bullet",
                r.right - 25,
                r.top + 57,
                13f,
                muted,
                align = Paint.Align.RIGHT
            )
            label(
                c,
                if (done) "Completed — Again" else if (unlocked) "Start + Coin ${m.upgrade.reward}" else "Lock",
                r.left + 25,
                r.centerY() + 6,
                16f,
                if (unlocked) green else muted,
                align = Paint.Align.LEFT
            )
        }

    }

    private fun drawPurchase(c: Canvas) {
        box(c, RectF(0f, 0f, width, height), 0xce051219.toInt(), 0f)
        val a = ui.selectedAircraft;
        val spec = Campaign.aircraft[a]
        box(c, RectF(width / 2 - 270, 166f, width / 2 + 270, 581f), panel)
        label(c, "Purchase ${spec.name}", width / 2, 224f, 25f, white, true)
        label(c, "Price: ${spec.price} Coin", width / 2, 281f, 24f, gold)
        label(c, "Your balance: ${progress.coins} Coin", width / 2, 323f, 18f, white)
        val status = progress.purchaseEligibility(a)
        val eligible = status == PurchaseResult.PURCHASED
        label(
            c, when (status) {
                PurchaseResult.PREVIOUS_MISSIONS_REQUIRED -> "First complete 5 previous missions."
                PurchaseResult.INSUFFICIENT_COINS -> "No enough coin."
                else -> "After purchase the missions will be opened."
            }, width / 2, 385f, 15f, muted
        )
        box(c, buyRect, if (eligible) green else 0xff344b55.toInt())
        label(
            c,
            if (eligible) "Purchase with ${spec.price} Coin" else "It's lock for now",
            width / 2,
            470f,
            19f,
            if (eligible) navy else muted,
            true
        )
        box(c, cancelRect, 0xff1e3742.toInt()); label(c, "Return", width / 2, 536f, 16f)
    }
}

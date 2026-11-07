package com.example.a2dgamebox

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.edit

class SpaceShipSelectActivity : AppCompatActivity() {

    private data class Ship(val key: String, @param:DrawableRes val drawableRes: Int, val price: Int, val type: Int)
    private val ships = listOf(
        Ship("default", R.drawable.player, 0, 0),
        Ship("star_breaker", R.drawable.start_breaker, 1800,1),
        Ship("star_war", R.drawable.star_war, 2400,2) ,
        Ship("star_ship", R.drawable.star_ship, 4300, 3),
        Ship("star_galaxy", R.drawable.start_galaxy, 6000, 4)


    )

    private val prefs by lazy { getSharedPreferences("gameBox2d", MODE_PRIVATE) }


    private lateinit var carousel: HorizontalScrollView
    private lateinit var cards: LinearLayout
    private val cardViews = mutableListOf<View>()
    private var selected = 0
    // Set this to the full class name of your EXISTING gameplay Activity.
    private var step = 1
    private var side = 0
    private var coins = 0
    private val snap = Runnable {
        if (cardViews.isNotEmpty()) {
            select(((carousel.scrollX.toFloat() / step).toIntRound()).coerceIn(0, ships.lastIndex), true)
        }
    }

    private fun Float.toIntRound() = kotlin.math.round(this).toInt()
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun owned(ship: Ship) = ship.price == 0 || prefs.getBoolean("owned_${ship.key}", false)


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        selected = (savedInstanceState?.getInt("selected") ?: prefs.getInt("selected", 1)).coerceIn(0, ships.lastIndex)
        coins = prefs.getInt("coin", 0)
        setContentView(R.layout.activity_space_ship_select)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.ssRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        carousel = findViewById(R.id.ssCarousel)
        cards = findViewById(R.id.ssCards)
        findViewById<Button>(R.id.ssBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.ssBuy).setOnClickListener { buySelected() }
        findViewById<Button>(R.id.ssPlay).setOnClickListener {
            val ship = ships[selected]
            if (owned(ship)) {
                onPlaySelected(ship.key)
            }
        }
        carousel.addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
            if (right - left > 0 && (cardViews.isEmpty() || right - left != oldRight - oldLeft)) {
                buildCards(right - left)
            }
        }
        // Debounce selection until horizontal scrolling settles. Requires API 23+.
        carousel.setOnScrollChangeListener { _, _, _, _, _ ->
            carousel.removeCallbacks(snap)
            carousel.postDelayed(snap, 200)
        }
        updateControls()
    }

    private fun buildCards(width: Int) {
        carousel.removeCallbacks(snap)
        cards.removeAllViews()
        cardViews.clear()
        val cardWidth = (width * 0.78f).toInt()
        val gap = dp(12)
        side = (width - cardWidth) / 2
        step = cardWidth + gap
        cards.setPadding(side, dp(4), side, dp(4))
        ships.forEachIndexed { index, ship ->
            val card = layoutInflater.inflate(R.layout.item_space_ship, cards, false)
            val params = LinearLayout.LayoutParams(cardWidth, LinearLayout.LayoutParams.MATCH_PARENT)
            if (index < ships.lastIndex) params.rightMargin = gap
            card.layoutParams = params
            card.findViewById<ImageView>(R.id.ssShipImage).setImageResource(ship.drawableRes)
            card.findViewById<TextView>(R.id.ssShipPrice).text = getString(R.string.ss_price, ship.price)
            card.setOnClickListener { select(index, true) }
            cards.addView(card)
            cardViews.add(card)
        }
        cards.post {
            carousel.scrollTo(selected * step, 0)
            updateControls()
        }
    }

    private fun select(index: Int, scroll: Boolean) {
        selected = index.coerceIn(0, ships.lastIndex)
        prefs.edit { putInt("selected", selected).apply() }
        updateControls()
        if (scroll && carousel.scrollX != selected * step) carousel.smoothScrollTo(selected * step, 0)
    }

    private fun updateControls() {
        val ship = ships[selected]
        val isOwned = owned(ship)

        findViewById<TextView>(R.id.ssBalance).text = getString(R.string.ss_balance, coins)
        val buy = findViewById<Button>(R.id.ssBuy)
        buy.text = getString(R.string.ss_buy, ship.price)
        buy.visibility = if (isOwned) View.GONE else View.VISIBLE
        val play = findViewById<Button>(R.id.ssPlay)
        play.isEnabled = isOwned
        play.alpha = if (isOwned) 1f else 0.55f
        findViewById<TextView>(R.id.ssHint).setText(if (isOwned) R.string.ss_ready else R.string.ss_unlock)
        findViewById<TextView>(R.id.ssSwipeHint).text = getString(R.string.ss_dots, selected + 1, ships.size)
        cardViews.forEachIndexed { index, card ->
            val unlocked = owned(ships[index])
            card.findViewById<TextView>(R.id.ssShipPrice).visibility =
                if (unlocked) View.INVISIBLE else View.VISIBLE
            card.isSelected = index == selected
            card.setBackgroundResource(if (index == selected) R.drawable.ss_card_selected else R.drawable.ss_card)
            card.findViewById<TextView>(R.id.ssShipState).apply {
                setText(if (unlocked) R.string.ss_owned else R.string.ss_locked)
                setTextColor(if (unlocked) 0xFF63DFB0.toInt() else 0xFFFF688F.toInt())
            }
        }
    }

    private fun buySelected() {
        val ship = ships[selected]
        if (owned(ship)) return
        if (coins < ship.price) {
            Toast.makeText(this, R.string.ss_insufficient, Toast.LENGTH_SHORT).show()
            return
        }
        coins -= ship.price
        prefs.edit {  putInt("coin", coins).putBoolean("owned_${ship.key}", true).apply() }
        updateControls()
    }

    private fun onPlaySelected(shipKey: String) {
        prefs.edit { putString("selected_ship", shipKey).apply() }
        val intent = Intent(this, SpaceShooter::class.java)
        intent.putExtra("space_skin_type", ships[selected].type)
        intent.putExtra("space_skin_res", ships[selected].drawableRes)

        try {
            startActivity(intent)
        } catch (error: android.content.ActivityNotFoundException) {
            Toast.makeText(this, "Game Activity not found. Check its name and Manifest entry.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("selected", selected)
        super.onSaveInstanceState(outState)
    }
    override fun onDestroy() {
        carousel.removeCallbacks(snap)
        super.onDestroy()
    }


}
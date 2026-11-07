package com.example.a2dgamebox.city_helicopter

/** Pure Kotlin progression. Prices use earned game coins, NOT real-money billing. */
data class AircraftUpgrade(val name: String, val price: Int, val reward: Int,
    val maxHealth: Float, val speedFactor: Float, val barrels: Int, val shotInterval: Float)

object Campaign {
  //  const val AIRCRAFT_COUNT = 4
    const val MISSIONS_PER_AIRCRAFT = 5
    const val TOTAL_MISSIONS = 20
    val aircraft = listOf(
        AircraftUpgrade("RESCUE ELITE ", 0, 100, 100f, 1f, 1, .25f),
        AircraftUpgrade("APACHE", 500, 180, 120f, 1.06f, 1, .23f),
        AircraftUpgrade("NIGHTHAWK", 900, 280, 140f, 1.10f, 2, .23f),
        AircraftUpgrade("ORANGE", 1400, 400, 170f, 1.16f, 2, .19f)
    )
    fun level(aircraft: Int, mission: Int): Int {
        require(aircraft in 0..3 && mission in 0..5)
        return aircraft * MISSIONS_PER_AIRCRAFT + mission + 1
    }
}
enum class PurchaseResult { PURCHASED, ALREADY_OWNED, PREVIOUS_MISSIONS_REQUIRED, INSUFFICIENT_COINS, INVALID }

class CampaignProgress(coins: Int = 0, ownedMask: Int = 1, completedMask: Int = 0) {
    var coins = coins.coerceAtLeast(0); private set
    var ownedMask = (ownedMask and 15) or 1; private set
    var completedMask = completedMask and ((1 shl Campaign.TOTAL_MISSIONS) - 1); private set
    fun owns(aircraft: Int) = aircraft in 0..3 && ownedMask and (1 shl aircraft) != 0
    fun completed(aircraft: Int, mission: Int): Boolean =
        aircraft in 0..3 && mission in 0..5 && completedMask and (1 shl (aircraft * 5 + mission)) != 0
    fun completedCount(aircraft: Int) = (0..4).count { completed(aircraft, it) }
    fun canPlay(aircraft: Int, mission: Int) = owns(aircraft) && mission in 0..5 && (mission == 0 || completed(aircraft, mission - 1))




    fun purchaseEligibility(aircraft: Int): PurchaseResult = when {
        aircraft !in 0..3 -> PurchaseResult.INVALID
        owns(aircraft) -> PurchaseResult.ALREADY_OWNED
        !owns(aircraft - 1) || completedCount(aircraft - 1) != 5 -> PurchaseResult.PREVIOUS_MISSIONS_REQUIRED
        coins < Campaign.aircraft[aircraft].price -> PurchaseResult.INSUFFICIENT_COINS
        else -> PurchaseResult.PURCHASED
    }


    fun buy(aircraft: Int): PurchaseResult {
        val result = purchaseEligibility(aircraft)
        if (result == PurchaseResult.PURCHASED) {
            coins -= Campaign.aircraft[aircraft].price
            ownedMask = ownedMask or (1 shl aircraft)
        }
        return result
    }

    fun recordVictory(level: Int): Int {
        if (level !in 1..20) return 0
        val aircraft = (level - 1) / 5
        val mission = (level - 1) % 5
        if (!canPlay(aircraft, mission) || completed(aircraft, mission)) return 0
        val reward = Campaign.aircraft[aircraft].reward
        completedMask = completedMask or (1 shl (level - 1))
        coins += reward
        return reward
    }
}

//HUB,
enum class GameScreen {  HANGAR, MISSIONS, FLIGHT }
class CampaignUi {
    var screen = GameScreen.HANGAR
    var selectedAircraft = 0
    var purchaseDialog = false
}

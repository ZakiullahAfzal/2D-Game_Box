package com.example.a2dgamebox.city_helicopter

import android.content.Context
import androidx.core.content.edit


class CampaignStore(context: Context) {
    private val prefs = context.getSharedPreferences("gameBox2d", Context.MODE_PRIVATE)
    fun load() = CampaignProgress(
        prefs.getInt("coin", 0),
        prefs.getInt("owned", 1),
        prefs.getInt("completed", 0)
    )
    fun save(progress: CampaignProgress) {
        // One editor transaction prevents a reward flag and its coins being saved separately.
        prefs.edit {
            putInt("coin", progress.coins).putInt("owned", progress.ownedMask)
                .putInt("completed", progress.completedMask).apply()
        }
    }
    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound", true)
        set(value) { prefs.edit { putBoolean("sound", value).apply() } }
}

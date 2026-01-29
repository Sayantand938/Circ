package com.example.circ.data

import androidx.compose.ui.graphics.Color
import com.example.circ.ui.theme.*

data class RankThreshold(val name: String, val color: Color, val minPoints: Long)

object TimerConstants {
    const val ROUTE_TIMER = "timer"
    const val ROUTE_DASHBOARD = "dashboard"
    const val ROUTE_HISTORY = "history"
    const val ROUTE_SETTINGS = "settings"

    // SYNCED WITH SETTINGS UI
    const val DEFAULT_WORK_MINS = 30L
    const val DEFAULT_BREAK_MINS = 30L
    const val DEFAULT_DAILY_GOAL_MINS = 480L
    const val DEFAULT_HOURLY_GOAL_MINS = 30L

    val RANKS = listOf(
        RankThreshold("CELESTIAL", RankCelestial, 3840),
        RankThreshold("LEGENDARY", RankLegendary, 3360),
        RankThreshold("GUARDIAN", RankGuardian, 2880),
        RankThreshold("ACOLYTE", RankAcolyte, 1000),
        RankThreshold("INITIATE", Color.Gray, 0),
        RankThreshold("OUTCAST", RankOutcast, -9999)
    )
}
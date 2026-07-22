package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class QosPriority(val label: String, val badgeColor: Long) {
    HIGH("High Priority", 0xFF00E676),    // Green
    NORMAL("Normal", 0xFF00B0FF),          // Cyan/Blue
    LOW("Low Priority", 0xFFFFAB00),      // Amber
    GUEST("Guest / Throttled", 0xFFFF5252); // Red/Coral

    fun getColor(): Color = Color(badgeColor)
}

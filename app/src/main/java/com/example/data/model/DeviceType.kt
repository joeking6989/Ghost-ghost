package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.ui.graphics.vector.ImageVector

enum class DeviceType(val displayName: String, val defaultIconName: String) {
    SMART_TV("Smart TV", "Tv"),
    LAPTOP("Laptop / PC", "Computer"),
    SMARTPHONE("Smartphone", "Smartphone"),
    TABLET("Tablet", "Tablet"),
    GAMING_CONSOLE("Gaming Console", "Gamepad"),
    SMART_SPEAKER("Smart Speaker", "Headsets"),
    IP_CAMERA("Security Camera", "Videocam"),
    SMART_HOME("Smart Home / IoT", "DeveloperBoard"),
    OTHER("Other Network Device", "Router");

    fun getIcon(): ImageVector {
        return when (this) {
            SMART_TV -> Icons.Default.Tv
            LAPTOP -> Icons.Default.Computer
            SMARTPHONE -> Icons.Default.Smartphone
            TABLET -> Icons.Default.Tablet
            GAMING_CONSOLE -> Icons.Default.Gamepad
            SMART_SPEAKER -> Icons.Default.VolumeUp
            IP_CAMERA -> Icons.Default.Videocam
            SMART_HOME -> Icons.Default.DeveloperBoard
            OTHER -> Icons.Default.Router
        }
    }
}

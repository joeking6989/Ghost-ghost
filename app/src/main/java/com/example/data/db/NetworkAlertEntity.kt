package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "network_alerts")
data class NetworkAlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val title: String,
    val message: String,
    val alertType: String, // "HIGH_USAGE", "SECURITY", "NEW_DEVICE"
    val deviceMac: String? = null,
    val isRead: Boolean = false
)

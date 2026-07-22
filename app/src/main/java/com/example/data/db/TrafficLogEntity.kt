package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "traffic_logs")
data class TrafficLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val deviceMac: String,
    val deviceName: String = "Device",
    val destinationHost: String = "cloud.service.com",
    val protocol: String = "HTTPS",
    val port: Int = 443,
    val rxBytes: Long = 0L,
    val txBytes: Long = 0L,
    val bytesTransferred: Long = 0L,
    val durationSeconds: Int = 2,
    val timestamp: Long = System.currentTimeMillis()
)

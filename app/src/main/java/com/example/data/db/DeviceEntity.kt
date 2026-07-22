package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.DeviceType
import com.example.data.model.QosPriority

@Entity(tableName = "connected_devices")
data class DeviceEntity(
    @PrimaryKey
    val macAddress: String,
    val ipAddress: String,
    val hostname: String,
    val customName: String,
    val deviceType: DeviceType,
    val roomName: String = "Living Room",
    val vendor: String = "Unknown Vendor",
    val priority: QosPriority = QosPriority.NORMAL,
    val isOnline: Boolean = true,
    val isBlocked: Boolean = false,
    val speedLimitLimitKbps: Long = 0L, // 0 = unlimited
    val totalDownloadBytes: Long = 0L,
    val totalUploadBytes: Long = 0L,
    val currentDownloadSpeedBps: Long = 0L,
    val currentUploadSpeedBps: Long = 0L,
    val firstSeenTimestamp: Long = System.currentTimeMillis(),
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val mirroredFromMac: String? = null
)

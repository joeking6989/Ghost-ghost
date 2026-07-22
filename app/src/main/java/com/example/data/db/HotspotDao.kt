package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HotspotDao {
    @Query("SELECT * FROM connected_devices ORDER BY totalDownloadBytes + totalUploadBytes DESC")
    fun getAllDevices(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM connected_devices WHERE macAddress = :mac LIMIT 1")
    suspend fun getDeviceByMac(mac: String): DeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDevice(device: DeviceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevices(devices: List<DeviceEntity>)

    @Update
    suspend fun updateDevice(device: DeviceEntity)

    @Query("UPDATE connected_devices SET isBlocked = :blocked WHERE macAddress = :mac")
    suspend fun setDeviceBlocked(mac: String, blocked: Boolean)

    @Query("DELETE FROM connected_devices WHERE macAddress = :mac")
    suspend fun deleteDevice(mac: String)

    @Query("UPDATE connected_devices SET totalDownloadBytes = 0, totalUploadBytes = 0")
    suspend fun resetAllDeviceUsage()

    @Query("UPDATE connected_devices SET totalDownloadBytes = 0, totalUploadBytes = 0 WHERE macAddress = :mac")
    suspend fun resetDeviceUsage(mac: String)

    @Query("DELETE FROM connected_devices")
    suspend fun deleteAllDevices()

    // Alerts
    @Query("SELECT * FROM network_alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<NetworkAlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: NetworkAlertEntity)

    @Query("UPDATE network_alerts SET isRead = 1 WHERE id = :alertId")
    suspend fun markAlertAsRead(alertId: Long)

    @Query("DELETE FROM network_alerts")
    suspend fun clearAllAlerts()

    // Traffic Logs
    @Query("SELECT * FROM traffic_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentTrafficLogs(): Flow<List<TrafficLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrafficLog(log: TrafficLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrafficLogs(logs: List<TrafficLogEntity>)

    @Query("DELETE FROM traffic_logs")
    suspend fun clearTrafficLogs()
}

package com.example.data.repository

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.db.DeviceEntity
import com.example.data.db.HotspotDao
import com.example.data.db.NetworkAlertEntity
import com.example.data.db.TrafficLogEntity
import com.example.data.model.DeviceType
import com.example.data.model.QosPriority
import com.example.data.network.NetworkScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

data class HotspotSettings(
    val monthlyCapGb: Float = 50.0f,
    val dailyCapGb: Float = 5.0f,
    val hotspotSsid: String = "Home_5G_Hotspot",
    val isHotspotActive: Boolean = true,
    val autoBlockOverLimit: Boolean = false
)

class HotspotRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao: HotspotDao = db.hotspotDao()
    private val scanner = NetworkScanner(context)

    val allDevices: Flow<List<DeviceEntity>> = dao.getAllDevices()
    val allAlerts: Flow<List<NetworkAlertEntity>> = dao.getAllAlerts()

    private val _settings = MutableStateFlow(HotspotSettings())
    val settings: StateFlow<HotspotSettings> = _settings.asStateFlow()

    private val _currentRxSpeed = MutableStateFlow(11_500_000L) // Bps
    val currentRxSpeed: StateFlow<Long> = _currentRxSpeed.asStateFlow()

    private val _currentTxSpeed = MutableStateFlow(1_850_000L) // Bps
    val currentTxSpeed: StateFlow<Long> = _currentTxSpeed.asStateFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.IO)
    private var simulationJob: Job? = null

    init {
        repositoryScope.launch {
            // Seed default devices if DB is empty
            val existing = dao.getAllDevices().first()
            if (existing.isEmpty()) {
                val defaults = scanner.getInitialDefaultDevices()
                dao.insertDevices(defaults)
                seedInitialAlerts()
            }
            startLiveMonitoringLoop()
        }
    }

    private fun seedInitialAlerts() {
        repositoryScope.launch {
            val now = System.currentTimeMillis()
            dao.insertAlert(
                NetworkAlertEntity(
                    timestamp = now - 3600000 * 2,
                    title = "High Data Rate Detected",
                    message = "PS5 Gaming Console downloaded 4.8 GB in 30 minutes.",
                    alertType = "HIGH_USAGE",
                    deviceMac = "DC:A6:32:8F:4C:D8"
                )
            )
            dao.insertAlert(
                NetworkAlertEntity(
                    timestamp = now - 3600000 * 5,
                    title = "New Household Device Connected",
                    message = "Front Porch Camera (Ring) joined the hotspot network.",
                    alertType = "NEW_DEVICE",
                    deviceMac = "24:FD:52:11:78:09"
                )
            )
        }
    }

    private fun startLiveMonitoringLoop() {
        simulationJob?.cancel()
        simulationJob = repositoryScope.launch {
            while (true) {
                delay(2000) // update stats every 2 seconds
                val currentDevices = dao.getAllDevices().first()
                if (currentDevices.isNotEmpty()) {
                    val newLogsList = mutableListOf<TrafficLogEntity>()
                    var totalRxSum = 0L
                    var totalTxSum = 0L

                    val nonMirrored = currentDevices.filter { it.mirroredFromMac == null }
                    val mirrored = currentDevices.filter { it.mirroredFromMac != null }

                    val processedMap = mutableMapOf<String, DeviceEntity>()

                    // First, process non-mirrored devices normally
                    nonMirrored.forEach { device ->
                        if (device.isOnline && !device.isBlocked) {
                            val baseRxSpeed = when (device.deviceType) {
                                DeviceType.SMART_TV -> Random.nextLong(2_000_000, 6_000_000)
                                DeviceType.GAMING_CONSOLE -> Random.nextLong(3_000_000, 9_000_000)
                                DeviceType.LAPTOP -> Random.nextLong(800_000, 3_000_000)
                                DeviceType.SMARTPHONE -> Random.nextLong(100_000, 1_200_000)
                                DeviceType.IP_CAMERA -> Random.nextLong(20_000, 80_000)
                                else -> Random.nextLong(10_000, 300_000)
                            }

                            val baseTxSpeed = when (device.deviceType) {
                                DeviceType.IP_CAMERA -> Random.nextLong(500_000, 1_500_000)
                                DeviceType.LAPTOP -> Random.nextLong(200_000, 800_000)
                                else -> Random.nextLong(5_000, 80_000)
                            }

                            val finalRx = if (device.speedLimitLimitKbps > 0) {
                                val capBps = device.speedLimitLimitKbps * 125L
                                baseRxSpeed.coerceAtMost(capBps)
                            } else baseRxSpeed

                            val addedRx = (finalRx * 2)
                            val addedTx = (baseTxSpeed * 2)

                            totalRxSum += finalRx
                            totalTxSum += baseTxSpeed

                            val updatedDevice = device.copy(
                                totalDownloadBytes = device.totalDownloadBytes + addedRx,
                                totalUploadBytes = device.totalUploadBytes + addedTx,
                                currentDownloadSpeedBps = finalRx,
                                currentUploadSpeedBps = baseTxSpeed,
                                lastSeenTimestamp = System.currentTimeMillis()
                            )
                            processedMap[device.macAddress] = updatedDevice

                            newLogsList.add(
                                TrafficLogEntity(
                                    deviceMac = device.macAddress,
                                    rxBytes = addedRx,
                                    txBytes = addedTx,
                                    durationSeconds = 2
                                )
                            )
                        } else {
                            processedMap[device.macAddress] = device.copy(
                                currentDownloadSpeedBps = 0L,
                                currentUploadSpeedBps = 0L
                            )
                        }
                    }

                    // Process mirrored devices using their active source device
                    mirrored.forEach { device ->
                        val sourceDev = processedMap[device.mirroredFromMac] ?: currentDevices.find { it.macAddress == device.mirroredFromMac }
                        if (sourceDev != null) {
                            val updatedDevice = device.copy(
                                roomName = sourceDev.roomName,
                                deviceType = sourceDev.deviceType,
                                priority = sourceDev.priority,
                                speedLimitLimitKbps = sourceDev.speedLimitLimitKbps,
                                isBlocked = sourceDev.isBlocked,
                                isOnline = sourceDev.isOnline,
                                totalDownloadBytes = sourceDev.totalDownloadBytes,
                                totalUploadBytes = sourceDev.totalUploadBytes,
                                currentDownloadSpeedBps = sourceDev.currentDownloadSpeedBps,
                                currentUploadSpeedBps = sourceDev.currentUploadSpeedBps,
                                lastSeenTimestamp = System.currentTimeMillis()
                            )
                            processedMap[device.macAddress] = updatedDevice

                            if (updatedDevice.isOnline && !updatedDevice.isBlocked) {
                                val addedRx = (updatedDevice.currentDownloadSpeedBps * 2)
                                val addedTx = (updatedDevice.currentUploadSpeedBps * 2)
                                totalRxSum += updatedDevice.currentDownloadSpeedBps
                                totalTxSum += updatedDevice.currentUploadSpeedBps

                                newLogsList.add(
                                    TrafficLogEntity(
                                        deviceMac = device.macAddress,
                                        rxBytes = addedRx,
                                        txBytes = addedTx,
                                        durationSeconds = 2
                                    )
                                )
                            }
                        } else {
                            processedMap[device.macAddress] = device.copy(
                                currentDownloadSpeedBps = 0L,
                                currentUploadSpeedBps = 0L
                            )
                        }
                    }

                    val updatedList = processedMap.values.toList()
                    _currentRxSpeed.value = totalRxSum
                    _currentTxSpeed.value = totalTxSum
                    if (newLogsList.isNotEmpty()) {
                        dao.insertTrafficLogs(newLogsList)
                    }
                    dao.insertDevices(updatedList)
                }
            }
        }
    }

    private suspend fun syncMirroredDevices(sourceDev: DeviceEntity) {
        val all = dao.getAllDevices().first()
        val targetsToUpdate = all.filter { it.mirroredFromMac == sourceDev.macAddress }
        if (targetsToUpdate.isNotEmpty()) {
            val updatedTargets = targetsToUpdate.map { target ->
                target.copy(
                    roomName = sourceDev.roomName,
                    deviceType = sourceDev.deviceType,
                    priority = sourceDev.priority,
                    speedLimitLimitKbps = sourceDev.speedLimitLimitKbps,
                    isBlocked = sourceDev.isBlocked,
                    isOnline = sourceDev.isOnline,
                    currentDownloadSpeedBps = sourceDev.currentDownloadSpeedBps,
                    currentUploadSpeedBps = sourceDev.currentUploadSpeedBps,
                    totalDownloadBytes = sourceDev.totalDownloadBytes,
                    totalUploadBytes = sourceDev.totalUploadBytes
                )
            }
            dao.insertDevices(updatedTargets)
        }
    }

    suspend fun addDevice(
        name: String,
        mac: String,
        ip: String,
        room: String,
        type: DeviceType,
        vendor: String
    ) {
        val newDevice = DeviceEntity(
            macAddress = mac.ifEmpty { generateRandomMac() },
            ipAddress = ip.ifEmpty { "192.168.43.${Random.nextInt(160, 250)}" },
            hostname = name.replace(" ", "-"),
            customName = name,
            deviceType = type,
            roomName = room.ifEmpty { "Main House" },
            vendor = vendor.ifEmpty { "Generic Vendor" },
            isOnline = true
        )
        dao.insertOrUpdateDevice(newDevice)
        dao.insertAlert(
            NetworkAlertEntity(
                title = "Device Added",
                message = "$name ($room) was added to monitored hotspot devices.",
                alertType = "NEW_DEVICE",
                deviceMac = newDevice.macAddress
            )
        )
    }

    suspend fun updateDevice(device: DeviceEntity) {
        dao.insertOrUpdateDevice(device)
        syncMirroredDevices(device)
    }

    suspend fun toggleBlockDevice(mac: String, blocked: Boolean) {
        dao.setDeviceBlocked(mac, blocked)
        val dev = dao.getDeviceByMac(mac)
        val devName = dev?.customName ?: mac
        dao.insertAlert(
            NetworkAlertEntity(
                title = if (blocked) "Device Access Blocked" else "Device Access Unblocked",
                message = "$devName has been ${if (blocked) "blocked from" else "restored access to"} the hotspot.",
                alertType = "SECURITY",
                deviceMac = mac
            )
        )
        if (dev != null) {
            syncMirroredDevices(dev.copy(isBlocked = blocked))
        }
    }

    suspend fun deleteDevice(mac: String) {
        dao.deleteDevice(mac)
    }

    suspend fun mirrorDeviceSettings(sourceMac: String, targetMac: String) {
        val sourceDev = dao.getDeviceByMac(sourceMac) ?: return
        val targetDev = dao.getDeviceByMac(targetMac) ?: return

        val mirroredDevice = targetDev.copy(
            roomName = sourceDev.roomName,
            deviceType = sourceDev.deviceType,
            priority = sourceDev.priority,
            speedLimitLimitKbps = sourceDev.speedLimitLimitKbps,
            isBlocked = sourceDev.isBlocked,
            mirroredFromMac = sourceMac
        )
        dao.insertOrUpdateDevice(mirroredDevice)

        dao.insertAlert(
            NetworkAlertEntity(
                title = "Device Mirroring Linked",
                message = "Settings and activity from ${sourceDev.customName} are now mirrored to ${targetDev.customName} in real time.",
                alertType = "CONFIGURATION",
                deviceMac = targetMac
            )
        )
    }

    suspend fun resetSingleDeviceUsage(mac: String) {
        dao.resetDeviceUsage(mac)
        val dev = dao.getDeviceByMac(mac)
        dao.insertAlert(
            NetworkAlertEntity(
                title = "Device Usage Reset",
                message = "Data usage counters reset for ${dev?.customName ?: mac}.",
                alertType = "SYSTEM",
                deviceMac = mac
            )
        )
    }

    suspend fun resetAllUsage() {
        dao.resetAllDeviceUsage()
        dao.clearTrafficLogs()
        _currentRxSpeed.value = 0L
        _currentTxSpeed.value = 0L
        dao.insertAlert(
            NetworkAlertEntity(
                title = "All Devices Usage Reset",
                message = "Network data bandwidth counters and traffic history logs have been cleared.",
                alertType = "SYSTEM"
            )
        )
    }

    suspend fun resetToFactoryDefaults() {
        dao.deleteAllDevices()
        dao.clearTrafficLogs()
        dao.clearAllAlerts()
        _settings.value = HotspotSettings()
        _currentRxSpeed.value = 0L
        _currentTxSpeed.value = 0L

        // Re-seed initial default devices
        val defaults = scanner.getInitialDefaultDevices()
        dao.insertDevices(defaults)
        seedInitialAlerts()
    }

    fun updateSettings(
        monthlyCapGb: Float,
        dailyCapGb: Float,
        ssid: String,
        active: Boolean,
        autoBlock: Boolean
    ) {
        _settings.value = HotspotSettings(
            monthlyCapGb = monthlyCapGb,
            dailyCapGb = dailyCapGb,
            hotspotSsid = ssid,
            isHotspotActive = active,
            autoBlockOverLimit = autoBlock
        )
    }

    suspend fun markAlertRead(alertId: Long) {
        dao.markAlertAsRead(alertId)
    }

    suspend fun clearAlerts() {
        dao.clearAllAlerts()
    }

    private fun generateRandomMac(): String {
        return (1..6).joinToString(":") { "%02X".format(Random.nextInt(0, 256)) }
    }
}

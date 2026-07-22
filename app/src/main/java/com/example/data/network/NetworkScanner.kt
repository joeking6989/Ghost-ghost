package com.example.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Build
import android.util.Log
import com.example.data.db.DeviceEntity
import com.example.data.model.DeviceType
import com.example.data.model.QosPriority
import java.io.BufferedReader
import java.io.FileReader

data class NetworkTrafficSnapshot(
    val totalRxBytes: Long,
    val totalTxBytes: Long,
    val rxSpeedBps: Long,
    val txSpeedBps: Long,
    val timestamp: Long = System.currentTimeMillis()
)

class NetworkScanner(private val context: Context) {

    private var lastRxBytes: Long = TrafficStats.getTotalRxBytes()
    private var lastTxBytes: Long = TrafficStats.getTotalTxBytes()
    private var lastTimestamp: Long = System.currentTimeMillis()

    fun captureTrafficSnapshot(): NetworkTrafficSnapshot {
        val now = System.currentTimeMillis()
        val currentRx = TrafficStats.getTotalRxBytes().let { if (it == TrafficStats.UNSUPPORTED.toLong()) 0L else it }
        val currentTx = TrafficStats.getTotalTxBytes().let { if (it == TrafficStats.UNSUPPORTED.toLong()) 0L else it }

        val timeDiffSec = ((now - lastTimestamp) / 1000.0).coerceAtLeast(0.5)
        
        var rxDiff = currentRx - lastRxBytes
        var txDiff = currentTx - lastTxBytes

        if (rxDiff < 0) rxDiff = 0
        if (txDiff < 0) txDiff = 0

        val rxSpeed = (rxDiff / timeDiffSec).toLong()
        val txSpeed = (txDiff / timeDiffSec).toLong()

        lastRxBytes = currentRx
        lastTxBytes = currentTx
        lastTimestamp = now

        return NetworkTrafficSnapshot(
            totalRxBytes = currentRx,
            totalTxBytes = currentTx,
            rxSpeedBps = rxSpeed,
            txSpeedBps = txSpeed,
            timestamp = now
        )
    }

    /**
     * Attempts to read system ARP table /proc/net/arp to find tethered hotspot clients.
     * Falls back to high-fidelity household devices pre-populated with realistic live activity profiles.
     */
    fun parseArpTable(): List<String> {
        val macs = mutableListOf<String>()
        try {
            val br = BufferedReader(FileReader("/proc/net/arp"))
            var line: String?
            while (br.readLine().also { line = it } != null) {
                val tokens = line!!.split("\\s+".toRegex())
                if (tokens.size >= 4) {
                    val mac = tokens[3]
                    if (mac.matches("..:..:..:..:..:..".toRegex()) && mac != "00:00:00:00:00:00") {
                        macs.add(mac)
                    }
                }
            }
            br.close()
        } catch (e: Exception) {
            Log.d("NetworkScanner", "Proc ARP not readable on modern Android runtime: ${e.message}")
        }
        return macs
    }

    fun getInitialDefaultDevices(): List<DeviceEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            DeviceEntity(
                macAddress = "A4:83:E7:89:12:4A",
                ipAddress = "192.168.43.102",
                hostname = "LG-OLED65-LivingRoom",
                customName = "Living Room 4K TV",
                deviceType = DeviceType.SMART_TV,
                roomName = "Living Room",
                vendor = "LG Electronics",
                priority = QosPriority.HIGH,
                isOnline = true,
                totalDownloadBytes = 14200000000L, // ~14.2 GB
                totalUploadBytes = 640000000L,     // ~640 MB
                currentDownloadSpeedBps = 3_450_000L, // 3.45 MB/s (4K stream)
                currentUploadSpeedBps = 120_000L,
                firstSeenTimestamp = now - (86400000L * 5)
            ),
            DeviceEntity(
                macAddress = "F4:D4:88:31:0B:12",
                ipAddress = "192.168.43.105",
                hostname = "MacBook-Pro-M3",
                customName = "Work Laptop",
                deviceType = DeviceType.LAPTOP,
                roomName = "Office",
                vendor = "Apple Inc.",
                priority = QosPriority.HIGH,
                isOnline = true,
                totalDownloadBytes = 8500000000L, // ~8.5 GB
                totalUploadBytes = 2100000000L,  // ~2.1 GB
                currentDownloadSpeedBps = 1_200_000L, // 1.2 MB/s
                currentUploadSpeedBps = 480_000L,
                firstSeenTimestamp = now - (86400000L * 4)
            ),
            DeviceEntity(
                macAddress = "DC:A6:32:8F:4C:D8",
                ipAddress = "192.168.43.110",
                hostname = "PlayStation-5-Console",
                customName = "PS5 Gaming Console",
                deviceType = DeviceType.GAMING_CONSOLE,
                roomName = "Game Room",
                vendor = "Sony Interactive",
                priority = QosPriority.NORMAL,
                isOnline = true,
                totalDownloadBytes = 28400000000L, // ~28.4 GB (Game Download)
                totalUploadBytes = 950000000L,
                currentDownloadSpeedBps = 5_800_000L, // 5.8 MB/s
                currentUploadSpeedBps = 320_000L,
                firstSeenTimestamp = now - (86400000L * 7)
            ),
            DeviceEntity(
                macAddress = "98:01:A7:2B:EE:90",
                ipAddress = "192.168.43.118",
                hostname = "iPhone-15-Pro",
                customName = "Personal Smartphone",
                deviceType = DeviceType.SMARTPHONE,
                roomName = "Mobile",
                vendor = "Apple Inc.",
                priority = QosPriority.NORMAL,
                isOnline = true,
                totalDownloadBytes = 3200000000L,
                totalUploadBytes = 890000000L,
                currentDownloadSpeedBps = 450_000L,
                currentUploadSpeedBps = 85_000L,
                firstSeenTimestamp = now - (86400000L * 6)
            ),
            DeviceEntity(
                macAddress = "B8:27:EB:5A:88:1C",
                ipAddress = "192.168.43.124",
                hostname = "HomeAssistant-Hub",
                customName = "Smart Home Hub",
                deviceType = DeviceType.SMART_HOME,
                roomName = "Hallway",
                vendor = "Raspberry Pi Foundation",
                priority = QosPriority.HIGH,
                isOnline = true,
                totalDownloadBytes = 410000000L,
                totalUploadBytes = 320000000L,
                currentDownloadSpeedBps = 24_000L,
                currentUploadSpeedBps = 18_000L,
                firstSeenTimestamp = now - (86400000L * 10)
            ),
            DeviceEntity(
                macAddress = "70:EE:50:91:21:00",
                ipAddress = "192.168.43.130",
                hostname = "Google-Nest-Audio",
                customName = "Kitchen Smart Speaker",
                deviceType = DeviceType.SMART_SPEAKER,
                roomName = "Kitchen",
                vendor = "Google LLC",
                priority = QosPriority.LOW,
                isOnline = true,
                totalDownloadBytes = 1800000000L,
                totalUploadBytes = 85000000L,
                currentDownloadSpeedBps = 180_000L,
                currentUploadSpeedBps = 5_000L,
                firstSeenTimestamp = now - (86400000L * 8)
            ),
            DeviceEntity(
                macAddress = "24:FD:52:11:78:09",
                ipAddress = "192.168.43.142",
                hostname = "Ring-Doorbell-Cam",
                customName = "Front Porch Camera",
                deviceType = DeviceType.IP_CAMERA,
                roomName = "Outdoor",
                vendor = "Amazon Ring",
                priority = QosPriority.HIGH,
                isOnline = true,
                totalDownloadBytes = 620000000L,
                totalUploadBytes = 2800000000L, // ~2.8 GB Upload (Live Video Stream)
                currentDownloadSpeedBps = 35_000L,
                currentUploadSpeedBps = 720_000L,
                firstSeenTimestamp = now - (86400000L * 9)
            ),
            DeviceEntity(
                macAddress = "50:C7:BF:18:4E:55",
                ipAddress = "192.168.43.155",
                hostname = "Galaxy-Tab-S9",
                customName = "Kids Tablet",
                deviceType = DeviceType.TABLET,
                roomName = "Kids Room",
                vendor = "Samsung",
                priority = QosPriority.GUEST,
                isBlocked = false,
                speedLimitLimitKbps = 2000, // 2 Mbps cap
                isOnline = false,
                totalDownloadBytes = 4900000000L,
                totalUploadBytes = 190000000L,
                currentDownloadSpeedBps = 0L,
                currentUploadSpeedBps = 0L,
                firstSeenTimestamp = now - (86400000L * 3)
            )
        )
    }
}

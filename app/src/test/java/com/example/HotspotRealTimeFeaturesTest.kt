package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.db.DeviceEntity
import com.example.data.model.DeviceType
import com.example.data.model.QosPriority
import com.example.util.CsvExportUtil
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HotspotRealTimeFeaturesTest {

    @Test
    fun testReportGenerators() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val device1 = DeviceEntity(
            macAddress = "AA:BB:CC:DD:EE:01",
            ipAddress = "192.168.43.10",
            hostname = "Device-1",
            customName = "My Gaming Console",
            deviceType = DeviceType.GAMING_CONSOLE,
            roomName = "Bedroom",
            priority = QosPriority.HIGH,
            isOnline = true,
            totalDownloadBytes = 5000000L,
            totalUploadBytes = 800000L
        )

        val device2 = DeviceEntity(
            macAddress = "AA:BB:CC:DD:EE:02",
            ipAddress = "192.168.43.20",
            hostname = "Device-2",
            customName = "Smart TV",
            deviceType = DeviceType.SMART_TV,
            roomName = "Living Room",
            priority = QosPriority.NORMAL,
            isOnline = true,
            totalDownloadBytes = 12000000L,
            totalUploadBytes = 1500000L,
            mirroredFromMac = "AA:BB:CC:DD:EE:01" // Mirroring Device-1
        )

        val devices = listOf(device1, device2)

        // 1. Verify Document/CSV Generation
        val csv = CsvExportUtil.generateDevicesUsageCsv(devices)
        assertTrue(csv.contains("My Gaming Console"))
        assertTrue(csv.contains("Smart TV"))
        assertTrue(csv.contains("AA:BB:CC:DD:EE:01"))
        assertTrue(csv.contains("AA:BB:CC:DD:EE:02"))

        // 2. Verify Visual PNG Image Generation
        val imgFile = CsvExportUtil.generateImageReport(context, devices, "Test Filters")
        assertNotNull(imgFile)
        assertTrue(imgFile.exists())
        assertTrue(imgFile.length() > 0)
        imgFile.delete()

        // 3. Verify Simulated Video MP4 Generation
        val videoFile = CsvExportUtil.generateVideoReport(context, devices, "Test Filters")
        assertNotNull(videoFile)
        assertTrue(videoFile.exists())
        assertTrue(videoFile.length() > 0)
        videoFile.delete()
    }
}

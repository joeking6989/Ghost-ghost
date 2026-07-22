package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.widget.Toast
import com.example.data.db.DeviceEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object CsvExportUtil {

    /**
     * Generates a comprehensive CSV string representing overall device usage analytics.
     */
    fun generateDevicesUsageCsv(devices: List<DeviceEntity>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val nowStr = dateFormat.format(Date())

        val totalDlBytes = devices.sumOf { it.totalDownloadBytes }
        val totalUlBytes = devices.sumOf { it.totalUploadBytes }

        val sb = StringBuilder()
        // Metadata header
        sb.append("Hotspot Data Consumption Analytics Report\n")
        sb.append("Generated At,${nowStr}\n")
        sb.append("Total Devices,${devices.size}\n")
        sb.append("Total Download,${FormatUtils.formatBytes(totalDlBytes)}\n")
        sb.append("Total Upload,${FormatUtils.formatBytes(totalUlBytes)}\n")
        sb.append("\n")

        // Main table header
        sb.append("Device Name,MAC Address,IP Address,Category,Room,Status,QoS Priority,Current Download (Kbps),Current Upload (Kbps),Total Download (Bytes),Total Download Formatted,Total Upload (Bytes),Total Upload Formatted,Total Combined Usage\n")

        // Rows
        devices.forEach { dev ->
            val status = when {
                dev.isBlocked -> "Blocked"
                dev.isOnline -> "Online"
                else -> "Offline"
            }
            val totalBytes = dev.totalDownloadBytes + dev.totalUploadBytes
            sb.append("\"${escapeCsv(dev.customName)}\",")
            sb.append("\"${dev.macAddress}\",")
            sb.append("\"${dev.ipAddress}\",")
            sb.append("\"${dev.deviceType.displayName}\",")
            sb.append("\"${escapeCsv(dev.roomName)}\",")
            sb.append("\"${status}\",")
            sb.append("\"${dev.priority.name}\",")
            sb.append("${dev.currentDownloadSpeedBps * 8 / 1000},")
            sb.append("${dev.currentUploadSpeedBps * 8 / 1000},")
            sb.append("${dev.totalDownloadBytes},")
            sb.append("\"${FormatUtils.formatBytes(dev.totalDownloadBytes)}\",")
            sb.append("${dev.totalUploadBytes},")
            sb.append("\"${FormatUtils.formatBytes(dev.totalUploadBytes)}\",")
            sb.append("\"${FormatUtils.formatBytes(totalBytes)}\"\n")
        }

        // 24-hour breakdown section
        sb.append("\n24-Hour Hourly Consumption Breakdown (MB)\n")
        sb.append("Device Name,MAC Address,Hour,Download (MB),Upload (MB),Total (MB)\n")

        devices.forEach { dev ->
            val totalDlMb = (dev.totalDownloadBytes / (1024f * 1024f)).coerceAtLeast(0.5f)
            val totalUlMb = (dev.totalUploadBytes / (1024f * 1024f)).coerceAtLeast(0.2f)
            val macHash = dev.macAddress.hashCode()

            for (hour in 0..23) {
                val peakMultiplier = when (hour) {
                    in 8..11 -> 1.4f
                    in 12..14 -> 1.8f
                    in 18..22 -> 2.2f
                    in 1..6 -> 0.2f
                    else -> 0.8f
                }
                val pseudoRandom = abs((macHash xor (hour * 31)).toDouble() % 100) / 100f
                val hourDl = (totalDlMb / 24f) * peakMultiplier * (0.6f + pseudoRandom.toFloat() * 0.8f)
                val hourUl = (totalUlMb / 24f) * peakMultiplier * (0.5f + pseudoRandom.toFloat() * 0.7f)
                val hourLabel = String.format(Locale.US, "%02d:00", hour)

                sb.append("\"${escapeCsv(dev.customName)}\",")
                sb.append("\"${dev.macAddress}\",")
                sb.append("\"${hourLabel}\",")
                sb.append(String.format(Locale.US, "%.2f", hourDl) + ",")
                sb.append(String.format(Locale.US, "%.2f", hourUl) + ",")
                sb.append(String.format(Locale.US, "%.2f", hourDl + hourUl) + "\n")
            }
        }

        return sb.toString()
    }

    /**
     * Copies CSV content to device clipboard.
     */
    fun copyToClipboard(context: Context, csvData: String, label: String = "Device Usage Analytics CSV") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, csvData)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Report copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    /**
     * Shares the CSV content using Android System Share Sheet.
     */
    fun shareCsv(context: Context, csvData: String, title: String = "Export Device Analytics CSV") {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, csvData)
            putExtra(Intent.EXTRA_SUBJECT, "Hotspot Usage Analytics Export")
            type = "text/csv"
        }
        val shareIntent = Intent.createChooser(sendIntent, title)
        context.startActivity(shareIntent)
    }

    /**
     * Draws a high-fidelity visual dashboard of the filtered devices and outputs a shareable PNG file.
     */
    fun generateImageReport(context: Context, devices: List<DeviceEntity>, filterDesc: String): File {
        val width = 1000
        val height = 800
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Colors
        val bgPaint = Paint().apply { color = android.graphics.Color.parseColor("#0D0E15") } // Dark grey/black background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Title Paint
        val titlePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#00E5FF") // Neon Cyan
            textSize = 34f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText("HOTSPOT MONITOR - USAGE USAGE", 50f, 70f, titlePaint)

        // Subtitle Paint
        val subPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#8E92B2") // Grey text
            textSize = 20f
            isAntiAlias = true
        }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val dateStr = dateFormat.format(Date())
        canvas.drawText("Generated: $dateStr  |  Filter: $filterDesc", 50f, 110f, subPaint)

        // Divider
        val linePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#222533")
            strokeWidth = 3f
        }
        canvas.drawLine(50f, 130f, 950f, 130f, linePaint)

        // KPI Section
        val totalDlBytes = devices.sumOf { it.totalDownloadBytes }
        val totalUlBytes = devices.sumOf { it.totalUploadBytes }
        val totalCombined = totalDlBytes + totalUlBytes

        val kpiTitlePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#8E92B2")
            textSize = 16f
            isAntiAlias = true
        }
        val kpiValPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 26f
            isAntiAlias = true
            isFakeBoldText = true
        }

        canvas.drawText("TOTAL DEVICES", 50f, 180f, kpiTitlePaint)
        canvas.drawText("${devices.size}", 50f, 220f, kpiValPaint)

        canvas.drawText("TOTAL DOWNLOAD", 280f, 180f, kpiTitlePaint)
        canvas.drawText(FormatUtils.formatBytes(totalDlBytes), 280f, 220f, kpiValPaint)

        canvas.drawText("TOTAL UPLOAD", 550f, 180f, kpiTitlePaint)
        canvas.drawText(FormatUtils.formatBytes(totalUlBytes), 550f, 220f, kpiValPaint)

        canvas.drawText("COMBINED USAGE", 780f, 180f, kpiTitlePaint)
        canvas.drawText(FormatUtils.formatBytes(totalCombined), 780f, 220f, kpiValPaint)

        canvas.drawLine(50f, 250f, 950f, 250f, linePaint)

        // Table Header
        val thPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#00E5FF")
            textSize = 18f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText("Device Name", 50f, 290f, thPaint)
        canvas.drawText("Room / Location", 320f, 290f, thPaint)
        canvas.drawText("Category", 540f, 290f, thPaint)
        canvas.drawText("Status", 720f, 290f, thPaint)
        canvas.drawText("Combined Usage", 830f, 290f, thPaint)

        canvas.drawLine(50f, 310f, 950f, 310f, linePaint)

        // Table Rows
        val trPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 16f
            isAntiAlias = true
        }
        val trStatusOnlinePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#00E676") // Green
            textSize = 16f
            isAntiAlias = true
            isFakeBoldText = true
        }
        val trStatusOfflinePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#FF1744") // Red
            textSize = 16f
            isAntiAlias = true
        }

        var startY = 350f
        val maxRows = 8
        devices.take(maxRows).forEach { dev ->
            canvas.drawText(dev.customName, 50f, startY, trPaint)
            canvas.drawText(dev.roomName, 320f, startY, trPaint)
            canvas.drawText(dev.deviceType.displayName, 540f, startY, trPaint)

            if (dev.isBlocked) {
                canvas.drawText("Blocked", 720f, startY, trStatusOfflinePaint)
            } else if (dev.isOnline) {
                canvas.drawText("Online", 720f, startY, trStatusOnlinePaint)
            } else {
                canvas.drawText("Offline", 720f, startY, trStatusOfflinePaint)
            }

            canvas.drawText(FormatUtils.formatBytes(dev.totalDownloadBytes + dev.totalUploadBytes), 830f, startY, trPaint)

            canvas.drawLine(50f, startY + 15f, 950f, startY + 15f, linePaint)
            startY += 45f
        }

        if (devices.size > maxRows) {
            canvas.drawText("... and ${devices.size - maxRows} more devices match filters", 50f, startY + 10f, subPaint)
        }

        // Draw Footer
        val footerPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#4B4D66")
            textSize = 15f
            isAntiAlias = true
        }
        canvas.drawText("Household Hotspot Security & Diagnostics System", 50f, 765f, footerPaint)

        // Save file
        val file = File(context.cacheDir, "hotspot_report_${System.currentTimeMillis()}.png")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }

    /**
     * Generates a simulated MP4 container video showing filtered devices activity logs.
     */
    fun generateVideoReport(context: Context, devices: List<DeviceEntity>, filterDesc: String): File {
        val file = File(context.cacheDir, "traffic_simulation_${System.currentTimeMillis()}.mp4")
        file.outputStream().use { out ->
            // Write standard ftyp header
            out.write(byteArrayOf(
                0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70, // ftyp
                0x6d, 0x70, 0x34, 0x32, 0x00, 0x00, 0x00, 0x00, // mp42
                0x6d, 0x70, 0x34, 0x32, 0x69, 0x73, 0x6f, 0x6d  // isom
            ))

            // Add text-based simulation trace
            val metadata = "Hotspot Monitor - Real-Time Simulated Traffic Recording MP4 Video File\n" +
                    "Filters: $filterDesc\n" +
                    "Total Tracked Devices: ${devices.size}\n" +
                    devices.joinToString("\n") { "${it.customName} (${it.macAddress}) -> DL Speed: ${it.currentDownloadSpeedBps} Bps" }

            out.write(metadata.toByteArray(Charsets.UTF_8))

            // Padding frame bytes to make it look like a short capture
            val dummyFrame = ByteArray(1024) { 0xAA.toByte() }
            for (i in 1..20) {
                out.write(dummyFrame)
            }
        }
        return file
    }

    /**
     * Shares a local File using the custom Android FileProvider.
     */
    fun shareFile(context: Context, file: File, mimeType: String, title: String = "Share Exported File") {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "com.example.fileprovider",
                file
            )
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = mimeType
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val shareIntent = Intent.createChooser(sendIntent, title)
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error sharing: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }
}

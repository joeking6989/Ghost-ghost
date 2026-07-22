package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.db.DeviceEntity
import com.example.data.model.DeviceType
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DangerRed
import com.example.ui.theme.NeonGreen
import com.example.util.CsvExportUtil
import com.example.util.FormatUtils
import kotlin.random.Random

enum class ExportFormat {
    DOCUMENT,
    IMAGE,
    VIDEO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportCsvDialog(
    devices: List<DeviceEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Local filters state
    var selectedRoomFilter by remember { mutableStateOf("All") }
    var selectedCategoryFilter by remember { mutableStateOf<DeviceType?>(null) }
    var selectedStatusFilter by remember { mutableStateOf("All") }

    var roomDropdownExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var statusDropdownExpanded by remember { mutableStateOf(false) }

    // Selected Tab
    var selectedFormat by remember { mutableStateOf(ExportFormat.DOCUMENT) }

    // Dynamic Rooms list from devices
    val availableRooms = remember(devices) {
        listOf("All") + devices.map { it.roomName }.distinct().filter { it.isNotBlank() }
    }

    // Filter devices in real-time
    val filteredDevices = remember(devices, selectedRoomFilter, selectedCategoryFilter, selectedStatusFilter) {
        devices.filter { dev ->
            val matchesRoom = selectedRoomFilter == "All" || dev.roomName.equals(selectedRoomFilter, ignoreCase = true)
            val matchesCategory = selectedCategoryFilter == null || dev.deviceType == selectedCategoryFilter
            val matchesStatus = when (selectedStatusFilter) {
                "Online" -> dev.isOnline && !dev.isBlocked
                "Offline" -> !dev.isOnline || dev.isBlocked
                else -> true
            }
            matchesRoom && matchesCategory && matchesStatus
        }
    }

    // Report descriptor
    val filterDescription = remember(selectedRoomFilter, selectedCategoryFilter, selectedStatusFilter) {
        val r = "Room: $selectedRoomFilter"
        val c = "Cat: ${selectedCategoryFilter?.displayName ?: "All"}"
        val s = "Status: $selectedStatusFilter"
        "[$r | $c | $s]"
    }

    // Formatted data content
    val csvContent = remember(filteredDevices) {
        CsvExportUtil.generateDevicesUsageCsv(filteredDevices)
    }

    val previewLines = remember(csvContent) {
        csvContent.lines().take(15).joinToString("\n") +
                if (csvContent.lines().size > 15) "\n... [${csvContent.lines().size - 15} more lines]" else ""
    }

    // Video Recording simulation animations
    val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    val loadingProgress by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "simulation_progress"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("export_csv_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyanPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Export Report",
                                tint = CyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Filter & Automated Export",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${filteredDevices.size} filtered records of ${devices.size} devices",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_csv_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- DATA FILTERING INTERFACE ---
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Filter Data Sources",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Room Dropdown
                            Box(modifier = Modifier.weight(1.1f)) {
                                OutlinedButton(
                                    onClick = { roomDropdownExpanded = true },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.fillMaxWidth().height(36.dp).testTag("filter_room_btn")
                                ) {
                                    Text(selectedRoomFilter, fontSize = 10.sp, maxLines = 1)
                                }
                                DropdownMenu(
                                    expanded = roomDropdownExpanded,
                                    onDismissRequest = { roomDropdownExpanded = false }
                                ) {
                                    availableRooms.forEach { room ->
                                        DropdownMenuItem(
                                            text = { Text(room, fontSize = 11.sp) },
                                            onClick = {
                                                selectedRoomFilter = room
                                                roomDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Category Dropdown
                            Box(modifier = Modifier.weight(1.1f)) {
                                OutlinedButton(
                                    onClick = { categoryDropdownExpanded = true },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.fillMaxWidth().height(36.dp).testTag("filter_category_btn")
                                ) {
                                    Text(selectedCategoryFilter?.displayName ?: "All Cats", fontSize = 10.sp, maxLines = 1)
                                }
                                DropdownMenu(
                                    expanded = categoryDropdownExpanded,
                                    onDismissRequest = { categoryDropdownExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All Categories", fontSize = 11.sp) },
                                        onClick = {
                                            selectedCategoryFilter = null
                                            categoryDropdownExpanded = false
                                        }
                                    )
                                    DeviceType.values().forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat.displayName, fontSize = 11.sp) },
                                            onClick = {
                                                selectedCategoryFilter = cat
                                                categoryDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Status Dropdown
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { statusDropdownExpanded = true },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.fillMaxWidth().height(36.dp).testTag("filter_status_btn")
                                ) {
                                    Text(selectedStatusFilter, fontSize = 10.sp, maxLines = 1)
                                }
                                DropdownMenu(
                                    expanded = statusDropdownExpanded,
                                    onDismissRequest = { statusDropdownExpanded = false }
                                ) {
                                    listOf("All", "Online", "Offline").forEach { status ->
                                        DropdownMenuItem(
                                            text = { Text(status, fontSize = 11.sp) },
                                            onClick = {
                                                selectedStatusFilter = status
                                                statusDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // --- FORMAT SELECTION TABS ---
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ExportFormat.values().forEach { format ->
                        val isSelected = selectedFormat == format
                        val title = when (format) {
                            ExportFormat.DOCUMENT -> "Document"
                            ExportFormat.IMAGE -> "Visual Image"
                            ExportFormat.VIDEO -> "Video Simulation"
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedFormat = format }
                                .background(if (isSelected) CyanPrimary.copy(alpha = 0.25f) else Color.Transparent)
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) CyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // --- INTERACTIVE PREVIEW PANEL ---
                Text(
                    text = "Live Automated Preview",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (filteredDevices.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No data matches your selected filters.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    when (selectedFormat) {
                        ExportFormat.DOCUMENT -> {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth().height(220.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp)
                                        .verticalScroll(rememberScrollState())
                                        .horizontalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = previewLines,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }

                        ExportFormat.IMAGE -> {
                            // Beautiful UI visualization of the generated PNG report
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0E15)),
                                modifier = Modifier.fillMaxWidth().height(220.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "HOTSPOT MONITOR REPORT",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyanPrimary
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(CyanPrimary.copy(alpha = 0.2f))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text("PNG Image", fontSize = 8.sp, color = CyanPrimary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text("Metadata: ${filterDescription}", fontSize = 9.sp, color = Color.Gray)

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // KPI Indicators
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("DEVICES", fontSize = 8.sp, color = Color.LightGray)
                                            Text("${filteredDevices.size}", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Column {
                                            Text("DOWNLOAD", fontSize = 8.sp, color = Color.LightGray)
                                            Text(FormatUtils.formatBytes(filteredDevices.sumOf { it.totalDownloadBytes }), fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Column {
                                            Text("UPLOAD", fontSize = 8.sp, color = Color.LightGray)
                                            Text(FormatUtils.formatBytes(filteredDevices.sumOf { it.totalUploadBytes }), fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("DATA MATRIX TABLE PREVIEW", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyanPrimary)

                                    // Matrix row preview
                                    filteredDevices.take(4).forEach { dev ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(dev.customName, fontSize = 10.sp, color = Color.White, maxLines = 1)
                                            Text(dev.roomName, fontSize = 10.sp, color = Color.LightGray)
                                            Text(FormatUtils.formatBytes(dev.totalDownloadBytes + dev.totalUploadBytes), fontSize = 10.sp, color = CyanPrimary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (filteredDevices.size > 4) {
                                        Text("... and ${filteredDevices.size - 4} more rows rendered in PNG file.", fontSize = 9.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }

                        ExportFormat.VIDEO -> {
                            // Animated Simulation Player Preview
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Black),
                                modifier = Modifier.fillMaxWidth().height(220.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(14.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(DangerRed.copy(alpha = pulseAlpha))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "SIMULATING LIVE TRAFFIC CAPTURE...",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DangerRed
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Moving wave/bar simulations
                                    Row(
                                        modifier = Modifier.fillMaxWidth(0.8f).height(60.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        val heights = listOf(0.4f, 0.8f, 0.5f, 0.9f, 0.3f, 0.7f, 0.6f, 0.95f)
                                        heights.forEachIndexed { idx, h ->
                                            val currentHeight = remember { Animatable(0.1f) }
                                            LaunchedEffect(loadingProgress) {
                                                currentHeight.animateTo(
                                                    targetValue = h * (0.6f + Random.nextFloat() * 0.4f),
                                                    animationSpec = tween(600, easing = LinearEasing)
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .width(14.dp)
                                                    .fillMaxHeight(currentHeight.value)
                                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                    .background(if (idx % 2 == 0) CyanPrimary else NeonGreen)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = "Recording Simulated Traffic MP4 Video report...",
                                        fontSize = 10.sp,
                                        color = Color.LightGray
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { loadingProgress },
                                        modifier = Modifier.fillMaxWidth(0.8f).height(4.dp).clip(RoundedCornerShape(2.dp)),
                                        color = CyanPrimary,
                                        trackColor = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Left helper action: Copy or share report
                    if (selectedFormat == ExportFormat.DOCUMENT) {
                        OutlinedButton(
                            onClick = {
                                CsvExportUtil.copyToClipboard(context, csvContent)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("copy_csv_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Copy Text", fontSize = 12.sp)
                        }
                    } else {
                        // Cancel button
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", fontSize = 12.sp)
                        }
                    }

                    // Primary Automated Share Button
                    Button(
                        onClick = {
                            when (selectedFormat) {
                                ExportFormat.DOCUMENT -> {
                                    CsvExportUtil.shareCsv(context, csvContent)
                                }
                                ExportFormat.IMAGE -> {
                                    val imgFile = CsvExportUtil.generateImageReport(context, filteredDevices, filterDescription)
                                    CsvExportUtil.shareFile(context, imgFile, "image/png", "Share Visual Image Report")
                                }
                                ExportFormat.VIDEO -> {
                                    val videoFile = CsvExportUtil.generateVideoReport(context, filteredDevices, filterDescription)
                                    CsvExportUtil.shareFile(context, videoFile, "video/mp4", "Share Video Simulation Report")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                        shape = RoundedCornerShape(12.dp),
                        enabled = filteredDevices.isNotEmpty(),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("share_csv_button")
                    ) {
                        Icon(
                            imageVector = when (selectedFormat) {
                                ExportFormat.DOCUMENT -> Icons.Default.Description
                                ExportFormat.IMAGE -> Icons.Default.Image
                                ExportFormat.VIDEO -> Icons.Default.Videocam
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val btnLabel = when (selectedFormat) {
                            ExportFormat.DOCUMENT -> "Export Doc"
                            ExportFormat.IMAGE -> "Export Image"
                            ExportFormat.VIDEO -> "Export Video"
                        }
                        Text(
                            text = btnLabel,
                            color = MaterialTheme.colorScheme.surface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

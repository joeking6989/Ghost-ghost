package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.db.DeviceEntity
import com.example.data.model.DeviceType
import com.example.data.model.QosPriority
import com.example.ui.components.Device24hDataChart
import com.example.ui.components.ExportCsvDialog
import com.example.ui.components.MirrorDeviceDialog
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DangerRed
import com.example.util.CsvExportUtil
import com.example.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailDialog(
    device: DeviceEntity,
    allDevices: List<DeviceEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSaveDevice: (DeviceEntity) -> Unit,
    onToggleBlock: (Boolean) -> Unit,
    onDeleteDevice: () -> Unit,
    onMirrorSettings: ((sourceMac: String, targetMac: String) -> Unit)? = null,
    onResetUsage: (() -> Unit)? = null
) {
    var nickname by remember(device) { mutableStateOf(device.customName) }
    var roomName by remember(device) { mutableStateOf(device.roomName) }
    var selectedPriority by remember(device) { mutableStateOf(device.priority) }
    var selectedCategory by remember(device) { mutableStateOf(device.deviceType) }
    var speedLimitKbps by remember(device) { mutableFloatStateOf(device.speedLimitLimitKbps.toFloat()) }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var showExportCsvDialog by remember { mutableStateOf(false) }
    var showMirrorDialog by remember { mutableStateOf(false) }

    if (showExportCsvDialog) {
        ExportCsvDialog(
            devices = listOf(device),
            onDismiss = { showExportCsvDialog = false }
        )
    }

    if (showMirrorDialog && onMirrorSettings != null) {
        MirrorDeviceDialog(
            devices = allDevices,
            initialTargetMac = device.macAddress,
            onMirrorApplied = { srcMac, tgtMac ->
                onMirrorSettings(srcMac, tgtMac)
            },
            onDismiss = { showMirrorDialog = false }
        )
    }

    Dialog(onDismissRequest = { onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("device_detail_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyanPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = selectedCategory.getIcon(),
                                contentDescription = null,
                                tint = CyanPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Device Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = device.macAddress,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = { onDismiss() },
                        modifier = Modifier.testTag("close_detail_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Nickname Field
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Device Custom Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("device_nickname_field")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Room Field
                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it },
                    label = { Text("Room / Location") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("device_room_field")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Device Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Device Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("device_category_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        DeviceType.values().forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.displayName) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // QoS Priority Selector
                Text(
                    text = "QoS Bandwidth Priority",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QosPriority.values().forEach { prio ->
                        val isSelected = selectedPriority == prio
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) prio.getColor()
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedPriority = prio }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = prio.label.split(" ").first(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Speed Limit Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Speed Limit Cap",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (speedLimitKbps <= 0) "Unlimited" else "${speedLimitKbps.toInt()} Kbps",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyanPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = speedLimitKbps,
                    onValueChange = { speedLimitKbps = it },
                    valueRange = 0f..10000f,
                    steps = 20,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // --- REAL-TIME PROFILE MIRRORING SECTION ---
                Text(
                    text = "Real-time Profile Mirroring",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Continuously mirror settings & activity from another device in real-time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                var mirrorEnabled by remember(device) { mutableStateOf(device.mirroredFromMac != null) }
                var selectedSourceMac by remember(device) { mutableStateOf(device.mirroredFromMac ?: "") }
                var mirrorDropdownExpanded by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Real-time Mirroring Link", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = mirrorEnabled,
                        onCheckedChange = {
                            mirrorEnabled = it
                            if (!it) selectedSourceMac = ""
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanPrimary),
                        modifier = Modifier.testTag("mirror_link_switch")
                    )
                }

                if (mirrorEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val availableSources = allDevices.filter { it.macAddress != device.macAddress }
                    val currentSourceDev = availableSources.find { it.macAddress == selectedSourceMac }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            shape = RoundedCornerShape(12.dp),
                            onClick = { mirrorDropdownExpanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("select_mirror_source_picker")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentSourceDev?.customName ?: "Select Source Device to Mirror",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (currentSourceDev != null) {
                                        Text(
                                            text = "${currentSourceDev.deviceType.displayName} • Priority: ${currentSourceDev.priority.name}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = mirrorDropdownExpanded,
                            onDismissRequest = { mirrorDropdownExpanded = false }
                        ) {
                            availableSources.forEach { dev ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(dev.customName, fontWeight = FontWeight.Bold)
                                            Text(
                                                "${dev.deviceType.displayName} • ${dev.roomName}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedSourceMac = dev.macAddress
                                        mirrorDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Real-time 24h consumption chart for individual device
                Device24hDataChart(device = device)

                Spacer(modifier = Modifier.height(16.dp))

                // Network Technical Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Technical Identifiers",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "IP: ${device.ipAddress} • Host: ${device.hostname}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Vendor: ${device.vendor}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Total Usage: ${FormatUtils.formatBytes(device.totalDownloadBytes + device.totalUploadBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = CyanPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Export CSV, Mirror Profile & Reset Usage Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showExportCsvDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("device_export_csv_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Export Doc", fontSize = 12.sp)
                    }

                    if (onMirrorSettings != null && allDevices.size > 1) {
                        OutlinedButton(
                            onClick = { showMirrorDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("device_mirror_profile_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CopyAll,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Mirror Device", fontSize = 12.sp)
                        }
                    }

                    if (onResetUsage != null) {
                        OutlinedButton(
                            onClick = { onResetUsage() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("device_reset_usage_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Reset Usage", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Block Access Button
                    OutlinedButton(
                        onClick = { onToggleBlock(!device.isBlocked) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (device.isBlocked) CyanPrimary else DangerRed
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("toggle_block_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = if (device.isBlocked) "Unblock" else "Block Access")
                    }

                    // Save Button
                    Button(
                        onClick = {
                            val updated = device.copy(
                                customName = nickname,
                                roomName = roomName,
                                deviceType = selectedCategory,
                                priority = selectedPriority,
                                speedLimitLimitKbps = speedLimitKbps.toLong(),
                                mirroredFromMac = if (mirrorEnabled && selectedSourceMac.isNotEmpty()) selectedSourceMac else null
                            )
                            onSaveDevice(updated)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_device_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Save Changes")
                    }
                }
            }
        }
    }
}

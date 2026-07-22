package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.AddDeviceDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DeviceDetailDialog
import com.example.ui.screens.DevicesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.HotspotMonitorTheme
import com.example.ui.viewmodel.HotspotViewModel

enum class NavigationTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.WifiTethering),
    DEVICES("Devices", Icons.Default.Devices),
    ANALYTICS("Analytics", Icons.Default.Analytics),
    SETTINGS("Settings & Logs", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: HotspotViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HotspotMonitorTheme {
                MainAppLayout(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppLayout(viewModel: HotspotViewModel) {
    var selectedTab by remember { mutableStateOf(NavigationTab.DASHBOARD) }

    val selectedDeviceForDetail by viewModel.selectedDeviceForDetail.collectAsStateWithLifecycle()
    val showAddDeviceDialog by viewModel.showAddDeviceDialog.collectAsStateWithLifecycle()
    val allDevices by viewModel.devices.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = selectedTab.title,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_navigation")
            ) {
                NavigationTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = CyanPrimary,
                            indicatorColor = CyanPrimary
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                NavigationTab.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToDevices = { selectedTab = NavigationTab.DEVICES }
                )

                NavigationTab.DEVICES -> DevicesScreen(
                    viewModel = viewModel
                )

                NavigationTab.ANALYTICS -> AnalyticsScreen(
                    viewModel = viewModel
                )

                NavigationTab.SETTINGS -> SettingsScreen(
                    viewModel = viewModel
                )
            }
        }

        // Selected Device Detail Dialog
        selectedDeviceForDetail?.let { device ->
            DeviceDetailDialog(
                device = device,
                allDevices = allDevices,
                onDismiss = { viewModel.selectDeviceForDetail(null) },
                onSaveDevice = { updated -> viewModel.updateDevice(updated) },
                onToggleBlock = { blocked -> viewModel.toggleBlockDevice(device.macAddress, blocked) },
                onDeleteDevice = { viewModel.deleteDevice(device.macAddress) },
                onMirrorSettings = { srcMac, tgtMac -> viewModel.mirrorDeviceSettings(srcMac, tgtMac) },
                onResetUsage = { viewModel.resetSingleDeviceUsage(device.macAddress) }
            )
        }

        // Add Device Dialog
        if (showAddDeviceDialog) {
            AddDeviceDialog(
                onDismiss = { viewModel.toggleAddDeviceDialog(false) },
                onAddDevice = { name, mac, ip, room, type, vendor ->
                    viewModel.addDevice(name, mac, ip, room, type, vendor)
                }
            )
        }
    }
}

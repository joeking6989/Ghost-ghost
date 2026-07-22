package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.DeviceEntity
import com.example.data.db.NetworkAlertEntity
import com.example.data.model.DeviceType
import com.example.data.model.QosPriority
import com.example.data.repository.HotspotRepository
import com.example.data.repository.HotspotSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SpeedTestResult(
    val isRunning: Boolean = false,
    val progressPercent: Float = 0f,
    val pingMs: Int = 0,
    val downloadMbps: Float = 0f,
    val uploadMbps: Float = 0f,
    val testFinished: Boolean = false
)

data class CategoryUsage(
    val category: DeviceType,
    val totalBytes: Long,
    val percentage: Float
)

data class DailyUsageTrend(
    val dayLabel: String,
    val downloadBytes: Long,
    val uploadBytes: Long
)

class HotspotViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HotspotRepository(application.applicationContext)

    val devices: StateFlow<List<DeviceEntity>> = repository.allDevices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alerts: StateFlow<List<NetworkAlertEntity>> = repository.allAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<HotspotSettings> = repository.settings

    val currentRxSpeed: StateFlow<Long> = repository.currentRxSpeed
    val currentTxSpeed: StateFlow<Long> = repository.currentTxSpeed

    // Search and Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedRoomFilter = MutableStateFlow("All")
    val selectedRoomFilter: StateFlow<String> = _selectedRoomFilter.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<DeviceType?>(null)
    val selectedCategoryFilter: StateFlow<DeviceType?> = _selectedCategoryFilter.asStateFlow()

    // Real-time Traffic Graph History (last 30 seconds rx & tx points)
    private val _rxHistory = MutableStateFlow<List<Float>>(List(30) { 0f })
    val rxHistory: StateFlow<List<Float>> = _rxHistory.asStateFlow()

    private val _txHistory = MutableStateFlow<List<Float>>(List(30) { 0f })
    val txHistory: StateFlow<List<Float>> = _txHistory.asStateFlow()

    // Speed Test State
    private val _speedTest = MutableStateFlow(SpeedTestResult())
    val speedTest: StateFlow<SpeedTestResult> = _speedTest.asStateFlow()

    // Dialog & UI State
    private val _selectedDeviceForDetail = MutableStateFlow<DeviceEntity?>(null)
    val selectedDeviceForDetail: StateFlow<DeviceEntity?> = _selectedDeviceForDetail.asStateFlow()

    private val _showAddDeviceDialog = MutableStateFlow(false)
    val showAddDeviceDialog: StateFlow<Boolean> = _showAddDeviceDialog.asStateFlow()

    // Filtered Device List
    val filteredDevices: StateFlow<List<DeviceEntity>> = combine(
        devices,
        _searchQuery,
        _selectedRoomFilter,
        _selectedCategoryFilter
    ) { deviceList, query, room, category ->
        deviceList.filter { dev ->
            val matchesQuery = query.isEmpty() ||
                    dev.customName.contains(query, ignoreCase = true) ||
                    dev.hostname.contains(query, ignoreCase = true) ||
                    dev.ipAddress.contains(query, ignoreCase = true) ||
                    dev.macAddress.contains(query, ignoreCase = true)

            val matchesRoom = room == "All" || dev.roomName.equals(room, ignoreCase = true)
            val matchesCategory = category == null || dev.deviceType == category

            matchesQuery && matchesRoom && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            combine(currentRxSpeed, currentTxSpeed) { rx, tx -> Pair(rx, tx) }
                .collect { (rx, tx) ->
                    val rxMb = rx / 1_000_000f
                    val txMb = tx / 1_000_000f

                    _rxHistory.value = (_rxHistory.value.drop(1) + rxMb)
                    _txHistory.value = (_txHistory.value.drop(1) + txMb)
                }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setRoomFilter(room: String) {
        _selectedRoomFilter.value = room
    }

    fun setCategoryFilter(category: DeviceType?) {
        _selectedCategoryFilter.value = category
    }

    fun selectDeviceForDetail(device: DeviceEntity?) {
        _selectedDeviceForDetail.value = device
    }

    fun toggleAddDeviceDialog(show: Boolean) {
        _showAddDeviceDialog.value = show
    }

    fun addDevice(name: String, mac: String, ip: String, room: String, type: DeviceType, vendor: String) {
        viewModelScope.launch {
            repository.addDevice(name, mac, ip, room, type, vendor)
            _showAddDeviceDialog.value = false
        }
    }

    fun updateDevice(device: DeviceEntity) {
        viewModelScope.launch {
            repository.updateDevice(device)
            _selectedDeviceForDetail.value = device
        }
    }

    fun toggleBlockDevice(mac: String, blocked: Boolean) {
        viewModelScope.launch {
            repository.toggleBlockDevice(mac, blocked)
            val currentDetail = _selectedDeviceForDetail.value
            if (currentDetail != null && currentDetail.macAddress == mac) {
                _selectedDeviceForDetail.value = currentDetail.copy(isBlocked = blocked)
            }
        }
    }

    fun deleteDevice(mac: String) {
        viewModelScope.launch {
            repository.deleteDevice(mac)
            if (_selectedDeviceForDetail.value?.macAddress == mac) {
                _selectedDeviceForDetail.value = null
            }
        }
    }

    fun mirrorDeviceSettings(sourceMac: String, targetMac: String) {
        viewModelScope.launch {
            repository.mirrorDeviceSettings(sourceMac, targetMac)
        }
    }

    fun resetSingleDeviceUsage(mac: String) {
        viewModelScope.launch {
            repository.resetSingleDeviceUsage(mac)
            val current = _selectedDeviceForDetail.value
            if (current != null && current.macAddress == mac) {
                _selectedDeviceForDetail.value = current.copy(
                    totalDownloadBytes = 0L,
                    totalUploadBytes = 0L
                )
            }
        }
    }

    fun resetAllUsage() {
        viewModelScope.launch {
            repository.resetAllUsage()
        }
    }

    fun resetToFactoryDefaults() {
        viewModelScope.launch {
            repository.resetToFactoryDefaults()
            _selectedDeviceForDetail.value = null
        }
    }

    fun saveSettings(monthlyGb: Float, dailyGb: Float, ssid: String, active: Boolean, autoBlock: Boolean) {
        repository.updateSettings(monthlyGb, dailyGb, ssid, active, autoBlock)
    }

    fun markAlertRead(alertId: Long) {
        viewModelScope.launch {
            repository.markAlertRead(alertId)
        }
    }

    fun clearAlerts() {
        viewModelScope.launch {
            repository.clearAlerts()
        }
    }

    fun runSpeedTest() {
        viewModelScope.launch {
            _speedTest.value = SpeedTestResult(isRunning = true, progressPercent = 0f, pingMs = 0)
            
            // Ping phase
            for (p in 1..20) {
                delay(50)
                _speedTest.value = _speedTest.value.copy(
                    progressPercent = p / 100f,
                    pingMs = (12..28).random()
                )
            }

            // Download phase
            for (p in 21..60) {
                delay(60)
                val currentDl = (18f + (p - 20) * 1.8f) + (-2..3).random()
                _speedTest.value = _speedTest.value.copy(
                    progressPercent = p / 100f,
                    downloadMbps = currentDl
                )
            }

            // Upload phase
            for (p in 61..100) {
                delay(60)
                val currentUl = (8f + (p - 60) * 0.6f) + (-1..2).random()
                _speedTest.value = _speedTest.value.copy(
                    progressPercent = p / 100f,
                    uploadMbps = currentUl
                )
            }

            _speedTest.value = _speedTest.value.copy(
                isRunning = false,
                progressPercent = 1f,
                testFinished = true
            )
        }
    }

    // Helper functions for analytics
    fun getCategoryUsages(deviceList: List<DeviceEntity>): List<CategoryUsage> {
        val totalAll = deviceList.sumOf { it.totalDownloadBytes + it.totalUploadBytes }.coerceAtLeast(1L)
        return DeviceType.values().mapNotNull { type ->
            val categoryTotal = deviceList
                .filter { it.deviceType == type }
                .sumOf { it.totalDownloadBytes + it.totalUploadBytes }

            if (categoryTotal > 0) {
                CategoryUsage(
                    category = type,
                    totalBytes = categoryTotal,
                    percentage = (categoryTotal.toFloat() / totalAll.toFloat())
                )
            } else null
        }.sortedByDescending { it.totalBytes }
    }

    fun getWeeklyTrends(): List<DailyUsageTrend> {
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val rxValues = listOf(4.2, 5.1, 3.8, 6.4, 8.9, 12.4, 9.8) // in GB
        val txValues = listOf(0.8, 0.9, 0.7, 1.2, 1.5, 2.1, 1.8)  // in GB

        return days.mapIndexed { index, day ->
            DailyUsageTrend(
                dayLabel = day,
                downloadBytes = (rxValues[index] * 1_073_741_824).toLong(),
                uploadBytes = (txValues[index] * 1_073_741_824).toLong()
            )
        }
    }
}

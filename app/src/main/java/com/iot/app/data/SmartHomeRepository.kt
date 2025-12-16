package com.iot.app.data

import android.content.Context
import android.util.Log
import com.iot.app.data.gateway.DeviceCommandGateway
import com.iot.app.data.local.SmartHomeDatabase
import com.iot.app.data.local.dao.DeviceDao
import com.iot.app.data.local.dao.HomeDao
import com.iot.app.data.local.dao.RoomDao
import com.iot.app.data.mapper.toDomain
import com.iot.app.data.mapper.toEntity
import com.iot.app.data.mqtt.MqttDeviceGateway
import com.iot.app.model.Device
import com.iot.app.model.DeviceCommand
import com.iot.app.model.DeviceState
import com.iot.app.model.DeviceType
import com.iot.app.model.Home
import com.iot.app.model.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * SmartHomeRepository - SINGLE SOURCE OF TRUTH
 * 
 * This repository coordinates between:
 * 1. Room Database (persistent storage)
 * 2. MQTT Gateway (device communication)
 * 
 * CRITICAL ARCHITECTURE RULES:
 * - ViewModels NEVER talk to MQTT directly
 * - All device commands go through this repository
 * - Optimistic UI updates (update DB first, then send MQTT)
 * - MQTT state updates automatically sync to DB
 * - Public API remains unchanged for backward compatibility
 * 
 * FLOW:
 * UI Toggle → ViewModel → Repository.toggleDevice() →
 *   1. Update Room DB immediately (optimistic)
 *   2. Send MQTT command to device
 *   3. When device responds, update DB again (confirmation)
 */
class SmartHomeRepository private constructor(
    context: Context,
    private val gateway: DeviceCommandGateway? = null  // Optional for testing
) {
    companion object {
        private const val TAG = "SmartHomeRepository"
        
        @Volatile
        private var instance: SmartHomeRepository? = null

        fun getInstance(context: Context): SmartHomeRepository =
            instance ?: synchronized(this) {
                instance ?: SmartHomeRepository(
                    context.applicationContext,
                    //null  // Temporarily disable MQTT for testing
                    MqttDeviceGateway.getInstance(context.applicationContext)
                ).also { 
                    instance = it
                    // Initialize MQTT connection
                    it.initializeMqtt()
                }
            }

        // Legacy method for backward compatibility
        fun getInstance(): SmartHomeRepository {
            return instance ?: throw IllegalStateException(
                "SmartHomeRepository must be initialized with context first"
            )
        }
    }
    
    private val database: SmartHomeDatabase = SmartHomeDatabase.getDatabase(context)
    private val homeDao: HomeDao = database.homeDao()
    private val roomDao: RoomDao = database.roomDao()
    private val deviceDao: DeviceDao = database.deviceDao()

    private val scope = CoroutineScope(Dispatchers.IO)

    // Expose data as StateFlow for ViewModels
    val homes: StateFlow<List<Home>> = homeDao.getAllHomes()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val rooms: StateFlow<List<Room>> = roomDao.getAllRooms()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val devices: StateFlow<List<Device>> = deviceDao.getAllDevices()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
    
    /**
     * Initialize MQTT connection and start listening for device updates
     */
    private fun initializeMqtt() {
        scope.launch {
            try {
                gateway?.connect()
                Log.i(TAG, "MQTT gateway connected - starting global state observers")
                
                // Observe ALL state updates globally (not per-device)
                // This ensures we catch states for devices added later or from external sources
                observeAllDeviceStateUpdates()
                observeAllDeviceStatusUpdates()
            } catch (e: Exception) {
                Log.e(TAG, "MQTT initialization failed: ${e.message}", e)
                // App continues to work with local DB only
            }
        }
    }
    
    /**
     * Observe ALL MQTT state updates globally and sync to DB
     * This catches state updates for any device, even if app didn't initiate the action
     */
    private fun observeAllDeviceStateUpdates() {
        gateway?.let { gw ->
            scope.launch {
                // Collect from the gateway's global state flow
                // MqttDeviceGateway emits (deviceId, DeviceState) pairs
                devices.value.forEach { device ->
                    launch {
                        gw.observeDeviceState(device.id).collect { newState ->
                            Log.i(TAG, "📥 MQTT state received for device ${device.id}: $newState")
                            
                            // Update Room DB - this will trigger UI update via Flow
                            val (stateString, stateValue) = when (newState) {
                                is DeviceState.On -> "ON" to null
                                is DeviceState.Off -> "OFF" to null
                                is DeviceState.Value -> "VALUE" to newState.value
                            }
                            deviceDao.updateDeviceState(device.id, stateString, stateValue)
                            Log.i(TAG, "💾 DB updated: device ${device.id} -> $stateString${stateValue?.let { " ($it)" } ?: ""}")
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Observe ALL MQTT status updates globally and update online/offline
     */
    private fun observeAllDeviceStatusUpdates() {
        gateway?.let { gw ->
            scope.launch {
                devices.value.forEach { device ->
                    launch {
                        gw.observeDeviceStatus(device.id).collect { status ->
                            Log.i(TAG, "📡 MQTT status received for device ${device.id}: online=${status.online}")
                            deviceDao.updateDeviceOnlineStatus(device.id, status.online)
                            Log.i(TAG, "💾 DB updated: device ${device.id} online status -> ${status.online}")
                        }
                    }
                }
            }
        }
    }

    // ===== HOME OPERATIONS =====
    fun addHome(name: String) {
        scope.launch {
            val newHome = Home(name = name)
            homeDao.insertHome(newHome.toEntity())
        }
    }

    fun removeHome(homeId: String) {
        scope.launch {
            homeDao.deleteHomeById(homeId)
            // Cascade delete handles rooms and devices automatically
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun getHome(homeId: String): Home? {
        // This is a blocking call - consider making it suspend if needed
        // For now, keeping the same signature as original
        return null // ViewModels should use Flow instead
    }

    // ===== ROOM OPERATIONS =====
    fun addRoom(homeId: String, name: String) {
        scope.launch {
            val newRoom = Room(name = name, homeId = homeId)
            roomDao.insertRoom(newRoom.toEntity())
        }
    }

    fun removeRoom(roomId: String) {
        scope.launch {
            roomDao.deleteRoomById(roomId)
            // Cascade delete handles devices automatically
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun getRoomsByHome(homeId: String): List<Room> {
        // This is a blocking call - keeping same signature as original
        return emptyList() // ViewModels should use Flow instead
    }

    @Suppress("UNUSED_PARAMETER")
    fun getRoom(roomId: String): Room? {
        // This is a blocking call - keeping same signature as original
        return null // ViewModels should use Flow instead
    }

    // ===== DEVICE OPERATIONS =====
    fun addDevice(name: String, type: DeviceType, roomId: String) {
        scope.launch {
            val newDevice = Device(name = name, type = type, roomId = roomId)
            deviceDao.insertDevice(newDevice.toEntity())
            
            // Start observing this new device's state and status
            gateway?.let { gw ->
                launch {
                    gw.observeDeviceState(newDevice.id).collect { newState ->
                        Log.i(TAG, "📥 MQTT state received for NEW device ${newDevice.id}: $newState")
                        val (stateString, stateValue) = when (newState) {
                            is DeviceState.On -> "ON" to null
                            is DeviceState.Off -> "OFF" to null
                            is DeviceState.Value -> "VALUE" to newState.value
                        }
                        deviceDao.updateDeviceState(newDevice.id, stateString, stateValue)
                        Log.i(TAG, "💾 DB updated: new device ${newDevice.id} -> $stateString")
                    }
                }
                
                launch {
                    gw.observeDeviceStatus(newDevice.id).collect { status ->
                        Log.i(TAG, "📡 MQTT status received for NEW device ${newDevice.id}: online=${status.online}")
                        deviceDao.updateDeviceOnlineStatus(newDevice.id, status.online)
                    }
                }
                
                // Send initial state request to device if connected
                val room = roomDao.getRoomById(roomId)
                if (room != null && gw.isConnected()) {
                    sendCommandToDevice(room.homeOwnerId, roomId, newDevice.id, DeviceCommand.RequestState)
                }
            }
        }
    }

    fun removeDevice(deviceId: String) {
        scope.launch {
            deviceDao.deleteDeviceById(deviceId)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun getDevicesByRoom(roomId: String): List<Device> {
        // This is a blocking call - keeping same signature as original
        return emptyList() // ViewModels should use Flow instead
    }

    @Suppress("UNUSED_PARAMETER")
    fun getDevice(deviceId: String): Device? {
        // This is a blocking call - keeping same signature as original
        return null // ViewModels should use Flow instead
    }

    fun toggleDevice(deviceId: String) {
        scope.launch {
            val device = deviceDao.getDeviceById(deviceId)
            if (device != null) {
                // Determine new state
                val newState = when (device.state) {
                    "OFF" -> DeviceState.On
                    "ON" -> DeviceState.Off
                    else -> DeviceState.Off
                }
                
                // OPTIMISTIC UI UPDATE: Update DB immediately
                val (stateString, stateValue) = when (newState) {
                    is DeviceState.On -> "ON" to null
                    is DeviceState.Off -> "OFF" to null
                    is DeviceState.Value -> "VALUE" to newState.value
                }
                deviceDao.updateDeviceState(deviceId, stateString, stateValue)
                Log.i(TAG, "🔄 Optimistic update: device $deviceId -> $stateString")
                
                // Send MQTT command to physical device
                val command = if (newState is DeviceState.On) {
                    DeviceCommand.TurnOn
                } else {
                    DeviceCommand.TurnOff
                }
                
                val room = roomDao.getRoomById(device.roomOwnerId)
                if (room != null) {
                    sendCommandToDevice(room.homeOwnerId, device.roomOwnerId, deviceId, command)
                    Log.i(TAG, "📤 MQTT command sent: $command to device $deviceId")
                }
            }
        }
    }

    fun updateDeviceState(deviceId: String, state: DeviceState) {
        scope.launch {
            val (stateString, stateValue) = when (state) {
                is DeviceState.On -> "ON" to null
                is DeviceState.Off -> "OFF" to null
                is DeviceState.Value -> "VALUE" to state.value
            }
            deviceDao.updateDeviceState(deviceId, stateString, stateValue)
            
            // If this is a user-initiated change (not from MQTT), send command
            // For now, we only send commands from toggleDevice()
            // State updates from MQTT are already handled in observeDeviceStateUpdates()
        }
    }
    
    /**
     * Send command to device via MQTT gateway
     * This is internal - only called by repository methods
     */
    private suspend fun sendCommandToDevice(
        homeId: String,
        roomId: String,
        deviceId: String,
        command: DeviceCommand
    ) {
        gateway?.let { gw ->
            if (gw.isConnected()) {
                try {
                    gw.sendCommand(homeId, roomId, deviceId, command)
                    Log.d(TAG, "Sent MQTT command: $command to device $deviceId")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send MQTT command: ${e.message}", e)
                }
            } else {
                Log.w(TAG, "MQTT not connected - command not sent")
            }
        }
    }

    fun renameDevice(deviceId: String, newName: String) {
        scope.launch {
            deviceDao.updateDeviceName(deviceId, newName)
        }
    }

    fun moveDeviceToRoom(deviceId: String, newRoomId: String) {
        scope.launch {
            deviceDao.updateDeviceRoom(deviceId, newRoomId)
        }
    }
}

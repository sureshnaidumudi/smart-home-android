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
                    null  // Temporarily disable MQTT for testing
                    //MqttDeviceGateway.getInstance(context.applicationContext)
                ).also { 
                    instance = it
                    // Initialize MQTT connection
                    //it.initializeMqtt()
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
                Log.i(TAG, "MQTT gateway connected")
                
                // Observe state updates from all devices and sync to DB
                devices.value.forEach { device ->
                    observeDeviceStateUpdates(device.id)
                    observeDeviceStatusUpdates(device.id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "MQTT initialization failed: ${e.message}", e)
                // App continues to work with local DB only
            }
        }
    }
    
    /**
     * Observe MQTT state updates for a device and sync to DB
     */
    private fun observeDeviceStateUpdates(deviceId: String) {
        gateway?.let { gw ->
            scope.launch {
                gw.observeDeviceState(deviceId).collect { newState ->
                    Log.d(TAG, "MQTT state update for $deviceId: $newState")
                    updateDeviceState(deviceId, newState)
                }
            }
        }
    }
    
    /**
     * Observe MQTT status updates for a device and update online/offline
     */
    private fun observeDeviceStatusUpdates(deviceId: String) {
        gateway?.let { gw ->
            scope.launch {
                gw.observeDeviceStatus(deviceId).collect { status ->
                    Log.d(TAG, "MQTT status update for $deviceId: online=${status.online}")
                    deviceDao.updateDeviceOnlineStatus(deviceId, status.online)
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
            
            // Start observing this new device
            observeDeviceStateUpdates(newDevice.id)
            observeDeviceStatusUpdates(newDevice.id)
            
            // Send initial state request to device if gateway is connected
            gateway?.let { gw ->
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
                
                // Send MQTT command to physical device
                val command = if (newState is DeviceState.On) {
                    DeviceCommand.TurnOn
                } else {
                    DeviceCommand.TurnOff
                }
                
                val room = roomDao.getRoomById(device.roomOwnerId)
                if (room != null) {
                    sendCommandToDevice(room.homeOwnerId, device.roomOwnerId, deviceId, command)
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

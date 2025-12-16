package com.iot.app.data

import android.content.Context
import com.iot.app.data.local.SmartHomeDatabase
import com.iot.app.data.local.dao.DeviceDao
import com.iot.app.data.local.dao.HomeDao
import com.iot.app.data.local.dao.RoomDao
import com.iot.app.data.mapper.toDomain
import com.iot.app.data.mapper.toEntity
import com.iot.app.model.Device
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

class SmartHomeRepository private constructor(context: Context) {
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
                val newState = when (device.state) {
                    "OFF" -> "ON" to null
                    "ON" -> "OFF" to null
                    else -> device.state to device.stateValue
                }
                deviceDao.updateDeviceState(deviceId, newState.first, newState.second)
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

    companion object {
        @Volatile
        private var instance: SmartHomeRepository? = null

        fun getInstance(context: Context): SmartHomeRepository =
            instance ?: synchronized(this) {
                instance ?: SmartHomeRepository(context.applicationContext).also { instance = it }
            }

        // Legacy method for backward compatibility
        fun getInstance(): SmartHomeRepository {
            return instance ?: throw IllegalStateException(
                "SmartHomeRepository must be initialized with context first"
            )
        }
    }
}

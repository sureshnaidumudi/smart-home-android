package com.iot.app.data.local.dao

import androidx.room.*
import com.iot.app.data.local.entity.DeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices")
    fun getAllDevices(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE roomOwnerId = :roomId")
    fun getDevicesByRoomId(roomId: String): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE deviceId = :deviceId")
    suspend fun getDeviceById(deviceId: String): DeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevices(devices: List<DeviceEntity>)

    @Update
    suspend fun updateDevice(device: DeviceEntity)

    @Delete
    suspend fun deleteDevice(device: DeviceEntity)

    @Query("DELETE FROM devices WHERE deviceId = :deviceId")
    suspend fun deleteDeviceById(deviceId: String)

    @Query("UPDATE devices SET state = :state, stateValue = :stateValue WHERE deviceId = :deviceId")
    suspend fun updateDeviceState(deviceId: String, state: String, stateValue: Float?)

    @Query("UPDATE devices SET state = :state, stateValue = :stateValue, responseMessage = :responseMessage, responseStatus = :responseStatus WHERE deviceId = :deviceId")
    suspend fun updateDeviceStateWithResponse(
        deviceId: String, 
        state: String, 
        stateValue: Float?,
        responseMessage: String?,
        responseStatus: String
    )
    
    @Query("UPDATE devices SET responseMessage = :message, responseStatus = :status WHERE deviceId = :deviceId")
    suspend fun updateDeviceResponse(deviceId: String, message: String?, status: String)

    @Query("UPDATE devices SET name = :newName WHERE deviceId = :deviceId")
    suspend fun updateDeviceName(deviceId: String, newName: String)

    @Query("UPDATE devices SET roomOwnerId = :newRoomId WHERE deviceId = :deviceId")
    suspend fun updateDeviceRoom(deviceId: String, newRoomId: String)
    
    @Query("UPDATE devices SET isOnline = :online WHERE deviceId = :deviceId")
    suspend fun updateDeviceOnlineStatus(deviceId: String, online: Boolean)
}

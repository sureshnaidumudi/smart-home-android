package com.iot.app.data.mapper

import com.iot.app.data.local.entity.DeviceEntity
import com.iot.app.data.local.entity.HomeEntity
import com.iot.app.data.local.entity.RoomEntity
import com.iot.app.model.Device
import com.iot.app.model.DeviceState
import com.iot.app.model.DeviceType
import com.iot.app.model.Home
import com.iot.app.model.Room

// Home Mappers
fun HomeEntity.toDomain(): Home {
    return Home(
        id = this.homeId,
        name = this.name
    )
}

fun Home.toEntity(): HomeEntity {
    return HomeEntity(
        homeId = this.id,
        name = this.name
    )
}

// Room Mappers
fun RoomEntity.toDomain(): Room {
    return Room(
        id = this.roomId,
        name = this.name,
        homeId = this.homeOwnerId
    )
}

fun Room.toEntity(): RoomEntity {
    return RoomEntity(
        roomId = this.id,
        homeOwnerId = this.homeId,
        name = this.name
    )
}

// Device Mappers
fun DeviceEntity.toDomain(): Device {
    return Device(
        id = this.deviceId,
        name = this.name,
        type = DeviceType.valueOf(this.type),
        roomId = this.roomOwnerId,
        state = when (this.state) {
            "ON" -> DeviceState.On
            "OFF" -> DeviceState.Off
            "VALUE" -> DeviceState.Value(this.stateValue ?: 0f)
            else -> DeviceState.Off
        },
        isOnline = this.isOnline
    )
}

fun Device.toEntity(): DeviceEntity {
    val (stateString, stateValue) = when (this.state) {
        is DeviceState.On -> "ON" to null
        is DeviceState.Off -> "OFF" to null
        is DeviceState.Value -> "VALUE" to this.state.value
    }
    
    return DeviceEntity(
        deviceId = this.id,
        roomOwnerId = this.roomId,
        name = this.name,
        type = this.type.name,
        state = stateString,
        stateValue = stateValue,
        isOnline = this.isOnline
    )
}

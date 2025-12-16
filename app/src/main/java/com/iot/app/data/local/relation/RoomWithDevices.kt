package com.iot.app.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.iot.app.data.local.entity.DeviceEntity
import com.iot.app.data.local.entity.RoomEntity

data class RoomWithDevices(
    @Embedded val room: RoomEntity,
    @Relation(
        parentColumn = "roomId",
        entityColumn = "roomOwnerId"
    )
    val devices: List<DeviceEntity>
)

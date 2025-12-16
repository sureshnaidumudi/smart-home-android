package com.iot.app.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.iot.app.data.local.entity.HomeEntity
import com.iot.app.data.local.entity.RoomEntity

data class HomeWithRoomsAndDevices(
    @Embedded val home: HomeEntity,
    @Relation(
        entity = RoomEntity::class,
        parentColumn = "homeId",
        entityColumn = "homeOwnerId"
    )
    val roomsWithDevices: List<RoomWithDevices>
)

package com.iot.app.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.iot.app.data.local.entity.HomeEntity
import com.iot.app.data.local.entity.RoomEntity

data class HomeWithRooms(
    @Embedded val home: HomeEntity,
    @Relation(
        parentColumn = "homeId",
        entityColumn = "homeOwnerId"
    )
    val rooms: List<RoomEntity>
)

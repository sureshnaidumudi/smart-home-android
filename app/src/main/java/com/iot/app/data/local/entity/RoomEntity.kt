package com.iot.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rooms",
    foreignKeys = [
        ForeignKey(
            entity = HomeEntity::class,
            parentColumns = ["homeId"],
            childColumns = ["homeOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["homeOwnerId"])]
)
data class RoomEntity(
    @PrimaryKey
    val roomId: String,
    val homeOwnerId: String,
    val name: String
)

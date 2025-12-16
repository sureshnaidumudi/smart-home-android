package com.iot.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "devices",
    foreignKeys = [
        ForeignKey(
            entity = RoomEntity::class,
            parentColumns = ["roomId"],
            childColumns = ["roomOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["roomOwnerId"])]
)
data class DeviceEntity(
    @PrimaryKey
    val deviceId: String,
    val roomOwnerId: String,
    val name: String,
    val type: String, // DeviceType as String
    val state: String, // DeviceState serialized as String
    val stateValue: Float? = null, // For DeviceState.Value
    val isOnline: Boolean = true
)

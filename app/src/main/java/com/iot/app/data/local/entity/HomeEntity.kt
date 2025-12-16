package com.iot.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "homes")
data class HomeEntity(
    @PrimaryKey
    val homeId: String,
    val name: String
)

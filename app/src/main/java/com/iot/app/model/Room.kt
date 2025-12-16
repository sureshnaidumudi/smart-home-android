package com.iot.app.model

import java.util.UUID

data class Room(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val homeId: String
)

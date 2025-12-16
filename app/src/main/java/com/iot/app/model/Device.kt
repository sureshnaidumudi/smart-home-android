package com.iot.app.model

import java.util.UUID

data class Device(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: DeviceType,
    val roomId: String,
    val state: DeviceState = DeviceState.Off,
    val isOnline: Boolean = true,
    val responseMessage: String? = null,
    val responseStatus: DeviceResponseStatus = DeviceResponseStatus.IDLE
)

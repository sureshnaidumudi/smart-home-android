package com.iot.app.model

sealed class DeviceState {
    data object Off : DeviceState()
    data object On : DeviceState()
    data class Value(val value: Float) : DeviceState()
}

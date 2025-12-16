package com.iot.app.model

/**
 * Represents the online/offline status of a device.
 * Used to track connectivity with physical hardware (ESP32, Raspberry Pi, etc.)
 */
data class DeviceStatus(
    val deviceId: String,
    val online: Boolean,
    val lastSeen: Long = System.currentTimeMillis()
)

package com.iot.app.model

/**
 * Response status for device commands
 * 
 * IDLE - No pending command
 * WAITING - Command sent, waiting for hardware response
 * CONFIRMED - Device responded successfully
 */
enum class DeviceResponseStatus {
    IDLE,
    WAITING,
    CONFIRMED
}

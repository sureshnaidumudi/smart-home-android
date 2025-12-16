package com.iot.app.data.mqtt

/**
 * MQTT Topic Structure (FINAL - DO NOT CHANGE)
 * 
 * All topics follow this pattern:
 * smarthome/{homeId}/{roomId}/{deviceId}/{suffix}
 * 
 * Suffixes:
 * - /cmd    : Commands sent TO device (app → ESP32/RPi)
 * - /state  : State updates FROM device (ESP32/RPi → app)
 * - /status : Online/offline status (ESP32/RPi → app, Last Will)
 * 
 * Example:
 * smarthome/home-123/room-456/device-789/cmd
 * smarthome/home-123/room-456/device-789/state
 * smarthome/home-123/room-456/device-789/status
 */
object MqttTopicBuilder {
    
    private const val BASE = "smarthome"
    
    /**
     * Build command topic for sending commands to device
     */
    fun buildCommandTopic(homeId: String, roomId: String, deviceId: String): String {
        return "$BASE/$homeId/$roomId/$deviceId/cmd"
    }
    
    /**
     * Build state topic for receiving state updates from device
     */
    fun buildStateTopic(homeId: String, roomId: String, deviceId: String): String {
        return "$BASE/$homeId/$roomId/$deviceId/state"
    }
    
    /**
     * Build status topic for receiving online/offline updates from device
     */
    fun buildStatusTopic(homeId: String, roomId: String, deviceId: String): String {
        return "$BASE/$homeId/$roomId/$deviceId/status"
    }
    
    /**
     * Subscribe to all state topics for all devices
     */
    fun buildAllStatesWildcard(): String {
        return "$BASE/+/+/+/state"
    }
    
    /**
     * Subscribe to all status topics for all devices
     */
    fun buildAllStatusWildcard(): String {
        return "$BASE/+/+/+/status"
    }
    
    /**
     * Extract device ID from topic
     * Example: "smarthome/home-1/room-2/device-3/state" → "device-3"
     */
    fun extractDeviceId(topic: String): String? {
        val parts = topic.split("/")
        return if (parts.size >= 4) parts[3] else null
    }
    
    /**
     * Extract home ID from topic
     */
    fun extractHomeId(topic: String): String? {
        val parts = topic.split("/")
        return if (parts.size >= 2) parts[1] else null
    }
    
    /**
     * Extract room ID from topic
     */
    fun extractRoomId(topic: String): String? {
        val parts = topic.split("/")
        return if (parts.size >= 3) parts[2] else null
    }
}

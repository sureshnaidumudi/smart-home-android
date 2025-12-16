package com.iot.app.data.mqtt

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

/**
 * JSON payloads for MQTT messages (FINAL CONTRACT)
 * 
 * These data classes define the wire format for communication
 * between the Android app and ESP32/Raspberry Pi devices.
 * 
 * DO NOT CHANGE without updating ESP32/RPi firmware accordingly.
 */

/**
 * Command payload sent TO device on /cmd topic
 * 
 * Examples:
 * { "action": "ON" }
 * { "action": "OFF" }
 * { "action": "SET_VALUE", "value": 75.0 }
 */
data class MqttCommandPayload(
    val action: String,
    val value: Float? = null
) {
    companion object {
        const val ACTION_ON = "ON"
        const val ACTION_OFF = "OFF"
        const val ACTION_SET_VALUE = "SET_VALUE"
        const val ACTION_REQUEST_STATE = "REQUEST_STATE"
    }
}

/**
 * State payload received FROM device on /state topic
 * 
 * Examples:
 * { "isOn": true }
 * { "isOn": false }
 * { "value": 42.5 }
 * { "isOn": true, "msg": "Turned ON successfully" }
 */
data class MqttStatePayload(
    val isOn: Boolean? = null,
    val value: Float? = null,
    val msg: String? = null  // Optional response message from hardware
)

/**
 * Status payload received FROM device on /status topic
 * Used for Last Will Testament and heartbeat.
 * 
 * Examples:
 * { "online": true }
 * { "online": false }
 */
data class MqttStatusPayload(
    val online: Boolean
)
// to check the git comment is updating
/**
 * JSON serializer using Gson
 */
object MqttJson {
    private val gson = Gson()
    
    // Encoder helpers
    fun encodeCommand(payload: MqttCommandPayload): String = gson.toJson(payload)
    fun encodeState(payload: MqttStatePayload): String = gson.toJson(payload)
    fun encodeStatus(payload: MqttStatusPayload): String = gson.toJson(payload)
    
    // Decoder helpers with error handling
    fun decodeCommand(jsonString: String): MqttCommandPayload? = try {
        gson.fromJson(jsonString, MqttCommandPayload::class.java)
    } catch (e: JsonSyntaxException) {
        null
    }
    
    fun decodeState(jsonString: String): MqttStatePayload? = try {
        gson.fromJson(jsonString, MqttStatePayload::class.java)
    } catch (e: JsonSyntaxException) {
        null
    }
    
    fun decodeStatus(jsonString: String): MqttStatusPayload? = try {
        gson.fromJson(jsonString, MqttStatusPayload::class.java)
    } catch (e: JsonSyntaxException) {
        null
    }
}

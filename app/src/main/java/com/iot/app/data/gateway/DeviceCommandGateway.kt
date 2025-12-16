package com.iot.app.data.gateway

import com.iot.app.model.DeviceCommand
import com.iot.app.model.DeviceState
import com.iot.app.model.DeviceStatus
import kotlinx.coroutines.flow.Flow

/**
 * ABSTRACTION LAYER for device communication.
 * 
 * This interface decouples the app from any specific communication protocol.
 * 
 * TODAY: Can use MQTT for testing with simulated devices
 * TOMORROW: Same interface works with ESP32/Raspberry Pi
 * FUTURE: Could add REST, WebSocket, BLE, or other protocols
 * 
 * CRITICAL RULES:
 * - UI and ViewModels NEVER talk to this directly
 * - Only SmartHomeRepository uses this interface
 * - No Android Context leaks
 * - All operations are async/Flow-based
 * - Thread-safe by design
 */
interface DeviceCommandGateway {
    
    /**
     * Send a command to a device.
     * 
     * This is fire-and-forget. The actual state change will arrive
     * via observeDeviceState() when the device responds.
     * 
     * @param homeId The home containing the device
     * @param roomId The room containing the device
     * @param deviceId The target device ID
     * @param command The command to send (ON, OFF, SetValue, etc.)
     */
    suspend fun sendCommand(
        homeId: String,
        roomId: String,
        deviceId: String,
        command: DeviceCommand
    )
    
    /**
     * Observe real-time state changes from a device.
     * 
     * Emits new DeviceState whenever the device reports a change.
     * Used to keep the UI in sync with physical hardware.
     * 
     * @param deviceId The device to observe
     * @return Flow that emits state updates
     */
    fun observeDeviceState(deviceId: String): Flow<DeviceState>
    
    /**
     * Observe online/offline status of a device.
     * 
     * Emits DeviceStatus when device connects, disconnects, or heartbeat received.
     * 
     * @param deviceId The device to observe
     * @return Flow that emits status updates
     */
    fun observeDeviceStatus(deviceId: String): Flow<DeviceStatus>
    
    /**
     * Connect to the communication backend (MQTT broker, REST server, etc.)
     * Should be called once at app startup.
     */
    suspend fun connect()
    
    /**
     * Disconnect from the communication backend.
     * Should be called on app shutdown or when switching networks.
     */
    suspend fun disconnect()
    
    /**
     * Check if gateway is currently connected and operational.
     */
    fun isConnected(): Boolean
}

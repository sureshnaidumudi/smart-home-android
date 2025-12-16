package com.iot.app.model

/**
 * Represents commands that can be sent to a physical device.
 * This abstraction allows the app to work with simulated devices today
 * and real ESP32/Raspberry Pi devices tomorrow without UI changes.
 */
sealed class DeviceCommand {
    /**
     * Turn device ON
     */
    data object TurnOn : DeviceCommand()
    
    /**
     * Turn device OFF
     */
    data object TurnOff : DeviceCommand()
    
    /**
     * Set device value (for dimmers, thermostats, etc.)
     * @param value The value to set (0.0 - 100.0 for percentage-based devices)
     */
    data class SetValue(val value: Float) : DeviceCommand()
    
    /**
     * Request current state from device
     */
    data object RequestState : DeviceCommand()
}

package com.iot.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.iot.app.data.SmartHomeRepository
import com.iot.app.model.Device
import kotlinx.coroutines.flow.StateFlow

class DeviceListViewModel(
    private val repository: SmartHomeRepository = SmartHomeRepository.getInstance()
) : ViewModel() {
    val devices: StateFlow<List<Device>> = repository.devices

    fun getDevicesByRoom(roomId: String): List<Device> {
        return repository.getDevicesByRoom(roomId)
    }

    fun toggleDevice(deviceId: String) {
        repository.toggleDevice(deviceId)
    }

    fun removeDevice(deviceId: String) {
        repository.removeDevice(deviceId)
    }

    fun addDevice(name: String, type: com.iot.app.model.DeviceType, roomId: String) {
        repository.addDevice(name, type, roomId)
    }
}

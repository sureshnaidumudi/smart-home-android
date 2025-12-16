package com.iot.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.iot.app.data.SmartHomeRepository
import com.iot.app.model.Device
import kotlinx.coroutines.flow.StateFlow

class DeviceDetailViewModel(
    private val repository: SmartHomeRepository = SmartHomeRepository.getInstance()
) : ViewModel() {
    val devices: StateFlow<List<Device>> = repository.devices
    val rooms: StateFlow<List<com.iot.app.model.Room>> = repository.rooms

    fun getDevice(deviceId: String): Device? {
        return repository.getDevice(deviceId)
    }

    fun toggleDevice(deviceId: String) {
        repository.toggleDevice(deviceId)
    }

    fun renameDevice(deviceId: String, newName: String) {
        repository.renameDevice(deviceId, newName)
    }

    fun moveDeviceToRoom(deviceId: String, newRoomId: String) {
        repository.moveDeviceToRoom(deviceId, newRoomId)
    }

    fun removeDevice(deviceId: String) {
        repository.removeDevice(deviceId)
    }
}

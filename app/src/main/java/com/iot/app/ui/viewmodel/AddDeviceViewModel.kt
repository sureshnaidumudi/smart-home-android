package com.iot.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.iot.app.data.SmartHomeRepository
import com.iot.app.model.DeviceType
import com.iot.app.model.Room
import kotlinx.coroutines.flow.StateFlow

class AddDeviceViewModel(
    private val repository: SmartHomeRepository = SmartHomeRepository.getInstance()
) : ViewModel() {
    val rooms: StateFlow<List<Room>> = repository.rooms

    fun getRoomsByHome(homeId: String): List<Room> {
        return repository.getRoomsByHome(homeId)
    }

    fun addDevice(name: String, type: DeviceType, roomId: String) {
        repository.addDevice(name, type, roomId)
    }
}

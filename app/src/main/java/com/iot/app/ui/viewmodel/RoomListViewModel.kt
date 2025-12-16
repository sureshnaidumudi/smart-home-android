package com.iot.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.iot.app.data.SmartHomeRepository
import com.iot.app.model.Room
import kotlinx.coroutines.flow.StateFlow

class RoomListViewModel(
    private val repository: SmartHomeRepository = SmartHomeRepository.getInstance()
) : ViewModel() {
    val rooms: StateFlow<List<Room>> = repository.rooms

    fun getRoomsByHome(homeId: String): List<Room> {
        return repository.getRoomsByHome(homeId)
    }

    fun addRoom(homeId: String, name: String) {
        repository.addRoom(homeId, name)
    }

    fun removeRoom(roomId: String) {
        repository.removeRoom(roomId)
    }
}

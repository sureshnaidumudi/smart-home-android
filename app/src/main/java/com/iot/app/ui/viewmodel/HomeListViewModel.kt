package com.iot.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.iot.app.data.SmartHomeRepository
import com.iot.app.model.Home
import kotlinx.coroutines.flow.StateFlow

class HomeListViewModel(
    private val repository: SmartHomeRepository = SmartHomeRepository.getInstance()
) : ViewModel() {
    val homes: StateFlow<List<Home>> = repository.homes

    fun addHome(name: String) {
        repository.addHome(name)
    }

    fun removeHome(homeId: String) {
        repository.removeHome(homeId)
    }
}

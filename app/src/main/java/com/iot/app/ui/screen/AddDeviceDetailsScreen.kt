package com.iot.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iot.app.model.DeviceType
import com.iot.app.model.Room
import com.iot.app.ui.viewmodel.AddDeviceViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceDetailsScreen(
    viewModel: AddDeviceViewModel,
    deviceType: DeviceType,
    roomId: String,
    onDeviceAdded: () -> Unit,
    onBackPressed: () -> Unit
) {
    val allRooms by viewModel.rooms.collectAsState()
    val rooms = allRooms.filter { it.id == roomId } + allRooms.filter { it.id != roomId }

    var deviceName by remember { mutableStateOf("") }
    var selectedRoomId by remember { mutableStateOf(roomId) }
    var showRoomDropdown by remember { mutableStateOf(false) }

    val selectedRoom = rooms.find { it.id == selectedRoomId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Device Details", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Device Type Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Device Type", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        deviceType.displayName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // Device Name Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Device Name", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = deviceName,
                        onValueChange = { deviceName = it },
                        label = { Text("Name") },
                        placeholder = { Text("e.g., Living Room Lamp") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    )
                }
            }

            // Select Room Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Room", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.padding(top = 12.dp)) {
                        Button(onClick = { showRoomDropdown = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedRoom?.name ?: "Select Room")
                        }
                        DropdownMenu(
                            expanded = showRoomDropdown,
                            onDismissRequest = { showRoomDropdown = false }
                        ) {
                            rooms.forEach { room ->
                                DropdownMenuItem(
                                    text = { Text(room.name) },
                                    onClick = {
                                        selectedRoomId = room.id
                                        showRoomDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onBackPressed,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (deviceName.isNotBlank()) {
                            viewModel.addDevice(deviceName, deviceType, selectedRoomId)
                            onDeviceAdded()
                        }
                    },
                    enabled = deviceName.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add Device")
                }
            }
        }
    }
}

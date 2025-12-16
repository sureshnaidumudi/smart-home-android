package com.iot.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import com.iot.app.model.Device
import com.iot.app.model.DeviceState
import com.iot.app.ui.viewmodel.DeviceDetailViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    viewModel: DeviceDetailViewModel,
    deviceId: String,
    onBackPressed: () -> Unit
) {
    val allDevices by viewModel.devices.collectAsState()
    val device = allDevices.find { it.id == deviceId }
    val allRooms by viewModel.rooms.collectAsState()

    var isRenaming by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(device?.name ?: "") }
    var showRoomDropdown by remember { mutableStateOf(false) }

    if (device == null) {
        Text("Device not found")
        return
    }

    val isOn = device.state == DeviceState.On
    val currentRoom = allRooms.find { it.id == device.roomId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(device.name, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.removeDevice(device.id); onBackPressed() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Device")
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
            // Device Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Device Information", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Type:", fontWeight = FontWeight.SemiBold)
                        Text(device.type.displayName)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Status:", fontWeight = FontWeight.SemiBold)
                        Text(if (device.isOnline) "Online" else "Offline")
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Room:", fontWeight = FontWeight.SemiBold)
                        Text(currentRoom?.name ?: "Unknown")
                    }
                }
            }

            // Control Card
            if (device.type.name != "SENSOR") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Control", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Power", fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = isOn,
                                onCheckedChange = { viewModel.toggleDevice(device.id) }
                            )
                        }
                    }
                }
            }

            // Rename Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Rename Device", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    if (isRenaming) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Device Name") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.renameDevice(device.id, newName)
                                    isRenaming = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save")
                            }
                            Button(
                                onClick = {
                                    isRenaming = false
                                    newName = device.name
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                        }
                    } else {
                        Button(
                            onClick = { isRenaming = true; newName = device.name },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Text("Edit Name")
                        }
                    }
                }
            }

            // Move to Room Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Move to Room", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.padding(top = 12.dp)) {
                        Button(onClick = { showRoomDropdown = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(currentRoom?.name ?: "Select Room")
                        }
                        DropdownMenu(
                            expanded = showRoomDropdown,
                            onDismissRequest = { showRoomDropdown = false }
                        ) {
                            allRooms.forEach { room ->
                                DropdownMenuItem(
                                    text = { Text(room.name) },
                                    onClick = {
                                        viewModel.moveDeviceToRoom(device.id, room.id)
                                        showRoomDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

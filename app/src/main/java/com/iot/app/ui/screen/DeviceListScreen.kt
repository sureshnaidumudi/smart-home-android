package com.iot.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iot.app.model.Device
import com.iot.app.model.DeviceResponseStatus
import com.iot.app.model.DeviceState
import com.iot.app.model.DeviceType
import com.iot.app.ui.viewmodel.DeviceListViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(
    viewModel: DeviceListViewModel,
    roomId: String,
    roomName: String,
    onDeviceSelected: (String) -> Unit,
    onAddDeviceClick: () -> Unit,
    onBackPressed: () -> Unit
) {
    val allDevices by viewModel.devices.collectAsState()
    val devices = allDevices.filter { it.roomId == roomId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(roomName, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddDeviceClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Device")
            }
        }
    ) { paddingValues ->
        if (devices.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("No devices yet. Add your first device!", fontSize = 16.sp)
                Button(onClick = onAddDeviceClick, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Add Device")
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(devices) { device ->
                    DeviceCard(
                        device = device,
                        onSelected = { onDeviceSelected(device.id) },
                        onToggle = { viewModel.toggleDevice(device.id) },
                        onDelete = { viewModel.removeDevice(device.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceCard(
    device: Device,
    onSelected: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val isOn = device.state == DeviceState.On

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        device.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                    Text(
                        device.type.displayName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    
                    // Response status indicator
                    if (device.responseStatus != DeviceResponseStatus.IDLE && 
                        device.responseMessage != null) {
                        val responseColor = when (device.responseStatus) {
                            DeviceResponseStatus.WAITING -> Color(0xFFFF9800)  // Orange
                            DeviceResponseStatus.CONFIRMED -> Color(0xFF4CAF50)  // Green
                            DeviceResponseStatus.IDLE -> Color.Transparent
                        }
                        
                        Text(
                            text = device.responseMessage,
                            fontSize = 11.sp,
                            color = responseColor,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Status indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusText = if (device.isOnline) "Online" else "Offline"
                val statusColor = if (device.isOnline) {
                    MaterialTheme.colorScheme.onTertiary
                } else {
                    MaterialTheme.colorScheme.error
                }
                Text(statusText, fontSize = 12.sp, color = statusColor, fontWeight = FontWeight.SemiBold)

                if (device.type != DeviceType.SENSOR) {
                    Switch(
                        checked = isOn,
                        onCheckedChange = { onToggle() },
                        modifier = Modifier.size(width = 48.dp, height = 24.dp)
                    )
                }
            }

            // Detail button
            Button(
                onClick = onSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Details", modifier = Modifier.size(18.dp))
                Text("Details", modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

package com.iot.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.iot.app.data.SmartHomeRepository
import com.iot.app.model.DeviceType
import com.iot.app.ui.screen.AddDeviceDetailsScreen
import com.iot.app.ui.screen.AddDeviceTypeScreen
import com.iot.app.ui.screen.DeviceDetailScreen
import com.iot.app.ui.screen.DeviceListScreen
import com.iot.app.ui.screen.HomeListScreen
import com.iot.app.ui.screen.RoomListScreen
import com.iot.app.ui.viewmodel.AddDeviceViewModel
import com.iot.app.ui.viewmodel.DeviceDetailViewModel
import com.iot.app.ui.viewmodel.DeviceListViewModel
import com.iot.app.ui.viewmodel.HomeListViewModel
import com.iot.app.ui.viewmodel.RoomListViewModel

@Composable
fun SmartHomeNavHost() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val repository = remember { SmartHomeRepository.getInstance(context) }
    val homeListViewModel = remember { HomeListViewModel(repository) }
    val roomListViewModel = remember { RoomListViewModel(repository) }
    val deviceListViewModel = remember { DeviceListViewModel(repository) }
    val deviceDetailViewModel = remember { DeviceDetailViewModel(repository) }
    val addDeviceViewModel = remember { AddDeviceViewModel(repository) }

    NavHost(navController, startDestination = "home_list") {
        composable("home_list") {
            HomeListScreen(
                viewModel = homeListViewModel,
                onHomeSelected = { homeId ->
                    val homeName = repository.getHome(homeId)?.name ?: "Home"
                    navController.navigate("room_list/$homeId/${homeName.replace("/", "_")}")
                }
            )
        }

        composable(
            "room_list/{homeId}/{homeName}",
            arguments = listOf(
                navArgument("homeId") { type = NavType.StringType },
                navArgument("homeName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val homeId = backStackEntry.arguments?.getString("homeId") ?: return@composable
            val homeName = backStackEntry.arguments?.getString("homeName")?.replace("_", "/") ?: "Home"

            RoomListScreen(
                viewModel = roomListViewModel,
                homeId = homeId,
                homeName = homeName,
                onRoomSelected = { roomId, roomName ->
                    navController.navigate("device_list/$roomId/${roomName.replace("/", "_")}")
                },
                onBackPressed = { navController.popBackStack() }
            )
        }

        composable(
            "device_list/{roomId}/{roomName}",
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType },
                navArgument("roomName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
            val roomName = backStackEntry.arguments?.getString("roomName")?.replace("_", "/") ?: "Room"

            DeviceListScreen(
                viewModel = deviceListViewModel,
                roomId = roomId,
                roomName = roomName,
                onDeviceSelected = { deviceId ->
                    navController.navigate("device_detail/$deviceId")
                },
                onAddDeviceClick = {
                    navController.navigate("add_device_type/$roomId")
                },
                onBackPressed = { navController.popBackStack() }
            )
        }

        composable(
            "device_detail/{deviceId}",
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: return@composable
            DeviceDetailScreen(
                viewModel = deviceDetailViewModel,
                deviceId = deviceId,
                onBackPressed = { navController.popBackStack() }
            )
        }

        composable(
            "add_device_type/{roomId}",
            arguments = listOf(navArgument("roomId") { type = NavType.StringType })
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
            AddDeviceTypeScreen(
                onDeviceTypeSelected = { deviceType ->
                    navController.navigate("add_device_details/$roomId/${deviceType.name}")
                },
                onBackPressed = { navController.popBackStack() }
            )
        }

        composable(
            "add_device_details/{roomId}/{deviceType}",
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType },
                navArgument("deviceType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
            val deviceTypeStr = backStackEntry.arguments?.getString("deviceType") ?: return@composable
            val deviceType = try {
                DeviceType.valueOf(deviceTypeStr)
            } catch (e: Exception) {
                DeviceType.BULB
            }

            AddDeviceDetailsScreen(
                viewModel = addDeviceViewModel,
                deviceType = deviceType,
                roomId = roomId,
                onDeviceAdded = {
                    navController.popBackStack("device_list/{roomId}/{roomName}", false)
                },
                onBackPressed = { navController.popBackStack() }
            )
        }
    }
}

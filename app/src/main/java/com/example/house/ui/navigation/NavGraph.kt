
package com.example.house.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.house.di.AppContainer
import com.example.house.ui.meter.MeterReadingScreen
import com.example.house.ui.room.RoomDetailScreen
import com.example.house.ui.room.RoomListScreen
import com.example.house.ui.settlement.SettlementScreen
import com.example.house.ui.statistics.StatisticsScreen
import com.example.house.ui.tenant.TenantListScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Rooms : Screen("rooms", "房间", Icons.Default.Home)
    data object Meter : Screen("meter", "抄表", Icons.Default.Speed)
    data object Settlement : Screen("settlement", "结算", Icons.Default.Receipt)
    data object Tenants : Screen("tenants", "租客", Icons.Default.People)
    data object Stats : Screen("stats", "统计", Icons.Default.BarChart)
}

@Composable
fun HouseNavGraph(container: AppContainer) {
    val navController = rememberNavController()
    val items = listOf(Screen.Rooms, Screen.Meter, Screen.Settlement, Screen.Tenants, Screen.Stats)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Column(Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = Screen.Rooms.route, modifier = Modifier.weight(1f)) {
            composable(Screen.Rooms.route) { RoomListScreen(container = container, onRoomClick = { navController.navigate("room_detail/$it") }) }
            composable(Screen.Meter.route) { MeterReadingScreen(container) }
            composable(Screen.Settlement.route) { SettlementScreen(container) }
            composable(Screen.Tenants.route) { TenantListScreen(container) }
            composable(Screen.Stats.route) { StatisticsScreen(container) }
            composable("room_detail/{roomId}", arguments = listOf(navArgument("roomId") { type = NavType.LongType })) { entry ->
                RoomDetailScreen(roomId = entry.arguments?.getLong("roomId") ?: 0L, container = container, onBack = { navController.popBackStack() })
            }
        }
        NavigationBar {
            items.forEach { screen ->
                NavigationBarItem(
                    icon = { Icon(screen.icon, screen.label) },
                    label = { Text(screen.label) },
                    selected = currentRoute == screen.route,
                    onClick = { navController.navigate(screen.route) { popUpTo(Screen.Rooms.route) { saveState = true }; launchSingleTop = true; restoreState = true } }
                )
            }
        }
    }
}

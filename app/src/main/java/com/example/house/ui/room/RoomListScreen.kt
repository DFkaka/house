
package com.example.house.ui.room

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.house.data.local.entity.RoomEntity
import com.example.house.data.local.entity.RoomStatus
import com.example.house.di.AppContainer
import com.example.house.ui.theme.*
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomListScreen(
    container: AppContainer,
    onRoomClick: (Long) -> Unit,
    onAddRoom: () -> Unit
) {
    val vm: RoomViewModel = viewModel(
        factory = RoomViewModel.Factory(container.roomRepository, container.tenantRepository)
    )
    val state by vm.state.collectAsState()
    val fmt = DecimalFormat("#0.00")

    var showAddDialog by remember { mutableStateOf(false) }
    var showStatusFilter by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("房间总览", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { showStatusFilter = true }) {
                        Icon(Icons.Default.FilterList, "筛选", tint = Color.White)
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "添加", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "添加房间", tint = Color.White)
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            // Search bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { vm.onSearch(it) },
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                placeholder = { Text("搜索房间号或名称...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty())
                        IconButton(onClick = { vm.onSearch("") }) {
                            Icon(Icons.Default.Clear, "清除")
                        }
                },
                singleLine = true
            )

            // Status filter chips
            if (state.statusFilter != null) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    AssistChip(
                        onClick = { vm.onFilterStatus(null) },
                        label = { Text(when (state.statusFilter) {
                            RoomStatus.OCCUPIED -> "已出租"
                            RoomStatus.VACANT -> "空置"
                            RoomStatus.MAINTENANCE -> "维修中"
                            null -> "全部"
                        }) },
                        trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                    )
                }
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.rooms, key = { it.room.roomId }) { item ->
                        RoomCard(item, fmt, onClick = { onRoomClick(item.room.roomId) })
                    }
                }
            }
        }
    }

    // Status filter dialog
    if (showStatusFilter) {
        AlertDialog(
            onDismissRequest = { showStatusFilter = false },
            title = { Text("按状态筛选") },
            text = {
                Column {
                    listOf(null to "全部", RoomStatus.OCCUPIED to "已出租", RoomStatus.VACANT to "空置", RoomStatus.MAINTENANCE to "维修中").forEach { (status, label) ->
                        TextButton(
                            onClick = { vm.onFilterStatus(status); showStatusFilter = false },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(label) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showStatusFilter = false }) { Text("取消") } }
        )
    }

    // Add room dialog
    if (showAddDialog) {
        AddRoomDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { code, name ->
                vm.addRoom(
                    RoomEntity(roomCode = code, roomName = name)
                ) { showAddDialog = false }
            }
        )
    }
}

@Composable
fun RoomCard(item: RoomWithTenant, fmt: DecimalFormat, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            // Header row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.room.roomCode, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    StatusBadge(item.room.status)
                }
                Text(item.tenantName.ifEmpty { "无租客" }, color = Gray600, fontSize = 14.sp)
            }

            if (item.room.roomName.isNotEmpty()) {
                Text(item.room.roomName, color = Gray400, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
            }

            Divider(Modifier.padding(vertical = 6.dp))

            // Meter readings & fees
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("上次水表", color = Gray400, fontSize = 11.sp)
                    Text(item.room.waterMeterLast.toString(), fontWeight = FontWeight.Medium, fontSize = 15.sp)
                }
                Column {
                    Text("上次电表", color = Gray400, fontSize = 11.sp)
                    Text(item.room.electricMeterLast.toString(), fontWeight = FontWeight.Medium, fontSize = 15.sp)
                }
                Column {
                    Text("上次水费", color = Gray400, fontSize = 11.sp)
                    Text("¥${fmt.format(item.room.lastWaterFee)}", color = Blue700, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                }
                Column {
                    Text("上次电费", color = Gray400, fontSize = 11.sp)
                    Text("¥${fmt.format(item.room.lastElectricFee)}", color = Orange600, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                }
                Column {
                    Text("上次合计", color = Gray400, fontSize = 11.sp)
                    Text("¥${fmt.format(item.room.lastTotalFee)}", color = Red600, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            if (item.room.lastSettleDate != null) {
                Spacer(Modifier.height(4.dp))
                Text("结算日: ${item.room.lastSettleDate}", color = Gray400, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun StatusBadge(status: RoomStatus) {
    val (color, text) = when (status) {
        RoomStatus.OCCUPIED -> Green600 to "已出租"
        RoomStatus.VACANT -> Gray600 to "空置"
        RoomStatus.MAINTENANCE -> Orange600 to "维修中"
    }
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun AddRoomDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加房间") },
        text = {
            Column {
                OutlinedTextField(code, { code = it }, label = { Text("房间号") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(name, { name = it }, label = { Text("房间名称") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = { if (code.isNotBlank()) onConfirm(code, name) }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

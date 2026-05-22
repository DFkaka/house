
package com.example.house.ui.meter

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.house.data.local.entity.RoomEntity
import com.example.house.di.AppContainer
import com.example.house.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeterReadingScreen(container: AppContainer) {
    val vm: MeterReadingViewModel = viewModel(
        factory = MeterReadingViewModel.Factory(
            container.meterReadingRepository,
            container.roomRepository
        )
    )
    val state by vm.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedRoomForAdd by remember { mutableStateOf<RoomEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("抄表记录", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "添加抄表")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Room filter dropdown
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.padding(12.dp)
            ) {
                OutlinedTextField(
                    value = state.selectedRoomId?.let { id ->
                        state.rooms.find { it.roomId == id }?.roomCode ?: ""
                    } ?: "全部房间",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    label = { Text("筛选房间") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("全部房间") },
                        onClick = { vm.selectRoom(null); expanded = false }
                    )
                    state.rooms.forEach { room ->
                        DropdownMenuItem(
                            text = { Text("${room.roomCode} ${room.roomName}".trim()) },
                            onClick = { vm.selectRoom(room.roomId); expanded = false }
                        )
                    }
                }
            }

            // Reading list
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(state.readings) { reading ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        state.rooms.find { it.roomId == reading.roomId }?.roomCode ?: "房间${reading.roomId}",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(reading.recordDate, color = Gray400, fontSize = 12.sp)
                                }
                                Column {
                                    Text("水: ${reading.waterReading}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text("电: ${reading.electricReading}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add reading dialog
    if (showAddDialog) {
        AddReadingDialog(
            rooms = state.rooms,
            onDismiss = { showAddDialog = false },
            onConfirm = { roomId, water, electric ->
                vm.addReading(roomId, water, electric) { showAddDialog = false }
            }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AddReadingDialog(
    rooms: List<RoomEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Double, Double) -> Unit
) {
    var selectedRoomId by remember { mutableStateOf<Long?>(null) }
    var waterReading by remember { mutableStateOf("") }
    var electricReading by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增抄表") },
        text = {
            Column {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = rooms.find { it.roomId == selectedRoomId }?.roomCode ?: "选择房间",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        label = { Text("房间") }
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        rooms.forEach { room ->
                            DropdownMenuItem(
                                text = { Text(room.roomCode) },
                                onClick = { selectedRoomId = room.roomId; expanded = false }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(waterReading, { waterReading = it }, label = { Text("水表读数") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(electricReading, { electricReading = it }, label = { Text("电表读数") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val w = waterReading.toDoubleOrNull() ?: return@TextButton
                val e = electricReading.toDoubleOrNull() ?: return@TextButton
                val id = selectedRoomId ?: return@TextButton
                onConfirm(id, w, e)
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

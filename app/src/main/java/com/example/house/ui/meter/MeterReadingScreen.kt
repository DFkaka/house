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
import com.example.house.data.local.model.MeterReading
import com.example.house.data.local.model.Room
import com.example.house.di.AppContainer
import com.example.house.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeterReadingScreen(container: AppContainer) {
    var readings by remember { mutableStateOf<List<MeterReading>>(emptyList()) }
    var rooms by remember { mutableStateOf<List<Room>>(emptyList()) }
    var selectedRoomId by remember { mutableStateOf<Long?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Edit state
    var editingReading by remember { mutableStateOf<MeterReading?>(null) }
    // Delete confirm
    var deletingReading by remember { mutableStateOf<MeterReading?>(null) }

    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch(Dispatchers.IO) {
            rooms = container.roomRepository.getAll()
            readings = if (selectedRoomId != null) container.meterReadingRepository.getByRoom(selectedRoomId!!)
            else container.meterReadingRepository.getAll()
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("抄表记录", fontWeight = FontWeight.Bold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White)) },
        floatingActionButton = { FloatingActionButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, null) } }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = Modifier.padding(12.dp)) {
                OutlinedTextField(
                    value = selectedRoomId?.let { id -> rooms.find { r -> r.roomId == id }?.roomCode ?: "" } ?: "全部房间",
                    onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(), label = { Text("筛选房间") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("全部房间") }, onClick = { selectedRoomId = null; expanded = false; load() })
                    rooms.forEach { r -> DropdownMenuItem(text = { Text(r.roomCode) }, onClick = { selectedRoomId = r.roomId; expanded = false; load() }) }
                }
            }
            if (loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(readings, key = { it.recordId }) { r ->
                    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(rooms.find { it.roomId == r.roomId }?.roomCode ?: "", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(r.recordDate, color = Gray400, fontSize = 12.sp)
                                }
                                Spacer(Modifier.height(2.dp))
                                Row {
                                    Text("水: ${r.waterReading}  ", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Blue700)
                                    Text("电: ${r.electricReading}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Orange600)
                                }
                            }
                            IconButton(onClick = { editingReading = r }) {
                                Icon(Icons.Default.Edit, "编辑", tint = Gray600, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { deletingReading = r }) {
                                Icon(Icons.Default.Delete, "删除", tint = Red600, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Add dialog
    if (showAddDialog) {
        AddEditDialog(
            rooms = rooms,
            title = "新增抄表",
            initialRoomId = null,
            initialDate = java.time.LocalDate.now().toString(),
            initialWater = "",
            initialElectric = "",
            onDismiss = { showAddDialog = false },
            onConfirm = { roomId, date, water, electric ->
                scope.launch(Dispatchers.IO) {
                    container.meterReadingRepository.insert(
                        MeterReading(roomId = roomId, recordDate = date, waterReading = water, electricReading = electric)
                    )
                    load()
                }
                showAddDialog = false
            }
        )
    }

    // Edit dialog
    editingReading?.let { reading ->
        AddEditDialog(
            rooms = rooms,
            title = "编辑抄表",
            initialRoomId = reading.roomId,
            initialDate = reading.recordDate,
            initialWater = reading.waterReading.toString(),
            initialElectric = reading.electricReading.toString(),
            onDismiss = { editingReading = null },
            onConfirm = { roomId, date, water, electric ->
                scope.launch(Dispatchers.IO) {
                    container.meterReadingRepository.update(
                        reading.copy(roomId = roomId, recordDate = date, waterReading = water, electricReading = electric)
                    )
                    load()
                }
                editingReading = null
            }
        )
    }

    // Delete confirm dialog
    deletingReading?.let { reading ->
        AlertDialog(
            onDismissRequest = { deletingReading = null },
            title = { Text("确认删除") },
            text = { Text("删除 ${rooms.find { it.roomId == reading.roomId }?.roomCode ?: ""} 的 ${reading.recordDate} 抄表记录？") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch(Dispatchers.IO) {
                        container.meterReadingRepository.delete(reading.recordId)
                        load()
                    }
                    deletingReading = null
                }) { Text("删除", color = Red600) }
            },
            dismissButton = { TextButton(onClick = { deletingReading = null }) { Text("取消") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDialog(
    rooms: List<Room>,
    title: String,
    initialRoomId: Long?,
    initialDate: String,
    initialWater: String,
    initialElectric: String,
    onDismiss: () -> Unit,
    onConfirm: (roomId: Long, date: String, water: Double, electric: Double) -> Unit
) {
    var selRoomId by remember { mutableStateOf(initialRoomId) }
    var date by remember { mutableStateOf(initialDate) }
    var water by remember { mutableStateOf(initialWater) }
    var electric by remember { mutableStateOf(initialElectric) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = rooms.find { it.roomId == selRoomId }?.roomCode ?: "选择房间",
                        onValueChange = {}, readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        label = { Text("房间") }
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        rooms.forEach { r ->
                            DropdownMenuItem(
                                text = { Text("${r.roomCode} ${r.roomName}".trim()) },
                                onClick = { selRoomId = r.roomId; expanded = false }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(date, { date = it }, label = { Text("抄表日期") }, singleLine = true, placeholder = { Text("yyyy-MM-dd") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(water, { water = it }, label = { Text("水表读数") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(electric, { electric = it }, label = { Text("电表读数") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val w = water.toDoubleOrNull() ?: return@TextButton
                val e = electric.toDoubleOrNull() ?: return@TextButton
                val id = selRoomId ?: return@TextButton
                if (date.isBlank()) return@TextButton
                onConfirm(id, date, w, e)
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

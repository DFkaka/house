
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeterReadingScreen(container: AppContainer) {
    var readings by remember { mutableStateOf<List<MeterReading>>(emptyList()) }
    var rooms by remember { mutableStateOf<List<Room>>(emptyList()) }
    var selectedRoomId by remember { mutableStateOf<Long?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
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
                items(readings) { r ->
                    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column { Text(rooms.find { it.roomId == r.roomId }?.roomCode ?: "", fontWeight = FontWeight.Bold); Text(r.recordDate, color = Gray400, fontSize = 12.sp) }
                            Column { Text("水: ${r.waterReading}", fontSize = 14.sp, fontWeight = FontWeight.Medium); Text("电: ${r.electricReading}", fontSize = 14.sp, fontWeight = FontWeight.Medium) }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var selRoomId by remember { mutableStateOf<Long?>(null) }; var water by remember { mutableStateOf("") }; var electric by remember { mutableStateOf("") }
        var expanded by remember { mutableStateOf(false) }
        AlertDialog(onDismissRequest = { showAddDialog = false }, title = { Text("新增抄表") },
            text = {
                Column {
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(value = rooms.find { it.roomId == selRoomId }?.roomCode ?: "选择房间", onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(), label = { Text("房间") })
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { rooms.forEach { r -> DropdownMenuItem(text = { Text(r.roomCode) }, onClick = { selRoomId = r.roomId; expanded = false }) } }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(water, { water = it }, label = { Text("水表读数") }, singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(electric, { electric = it }, label = { Text("电表读数") }, singleLine = true)
                }
            },
            confirmButton = { TextButton(onClick = {
                val w = water.toDoubleOrNull(); val e = electric.toDoubleOrNull(); val id = selRoomId
                if (w != null && e != null && id != null) scope.launch(Dispatchers.IO) {
                    container.meterReadingRepository.insert(MeterReading(roomId = id, recordDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE), waterReading = w, electricReading = e))
                    load()
                }
                showAddDialog = false
            }) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("取消") } })
    }
}

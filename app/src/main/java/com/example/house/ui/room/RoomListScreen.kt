
package com.example.house.ui.room

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.house.data.local.model.Room
import com.example.house.di.AppContainer
import com.example.house.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomListScreen(container: AppContainer, onRoomClick: (Long) -> Unit) {
    var rooms by remember { mutableStateOf<List<Room>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    val fmt = DecimalFormat("#0.00")
    val scope = rememberCoroutineScope()

    fun loadData() {
        scope.launch(Dispatchers.IO) {
            try {
                rooms = if (searchQuery.isNotBlank())
                    container.roomRepository.search(searchQuery)
                else container.roomRepository.getAll()
                loading = false
            } catch (e: Exception) {
                errorMsg = e.message
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("房间总览", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White),
                actions = { IconButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, null, tint = Color.White) } }
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, null, tint = Color.White) } }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                placeholder = { Text("搜索...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = ""; loadData() }) { Icon(Icons.Default.Clear, null) } },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { loadData() })
            )

            when {
                errorMsg != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Warning, null, Modifier.size(48.dp), tint = Red600)
                        Spacer(Modifier.height(12.dp))
                        Text("Error", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Red600)
                        Text(errorMsg!!, fontSize = 13.sp, color = Gray600)
                    }
                }
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                rooms.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("暂无房间", fontSize = 16.sp, color = Gray600)
                        TextButton(onClick = { showAddDialog = true }) { Text("+ 添加房间") }
                    }
                }
                else -> LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(rooms, key = { it.roomId }) { room ->
                        Card(Modifier.fillMaxWidth().clickable { onRoomClick(room.roomId) }, elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                            Column(Modifier.padding(14.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(room.roomCode, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text(when(room.status) { "OCCUPIED" -> "已出租"; "MAINTENANCE" -> "维修中"; else -> "空置" }, color = Gray600, fontSize = 13.sp)
                                }
                                if (room.roomName.isNotEmpty()) Text(room.roomName, color = Gray400, fontSize = 13.sp)
                                Divider(Modifier.padding(vertical = 6.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column { Text("水表", color = Gray400, fontSize = 11.sp); Text(room.waterMeterLast.toString(), fontWeight = FontWeight.Medium, fontSize = 15.sp) }
                                    Column { Text("电表", color = Gray400, fontSize = 11.sp); Text(room.electricMeterLast.toString(), fontWeight = FontWeight.Medium, fontSize = 15.sp) }
                                    Column { Text("水费", color = Gray400, fontSize = 11.sp); Text("￥${fmt.format(room.lastWaterFee)}", color = Blue700, fontWeight = FontWeight.Medium, fontSize = 15.sp) }
                                    Column { Text("电费", color = Gray400, fontSize = 11.sp); Text("￥${fmt.format(room.lastElectricFee)}", color = Orange600, fontWeight = FontWeight.Medium, fontSize = 15.sp) }
                                    Column { Text("合计", color = Gray400, fontSize = 11.sp); Text("￥${fmt.format(room.lastTotalFee)}", color = Red600, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var code by remember { mutableStateOf("") }; var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("添加房间") },
            text = { Column { OutlinedTextField(code, { code = it }, label = { Text("房间号") }, singleLine = true); Spacer(Modifier.height(8.dp)); OutlinedTextField(name, { name = it }, label = { Text("房间名称") }, singleLine = true) } },
            confirmButton = { TextButton(onClick = { if (code.isNotBlank()) { scope.launch(Dispatchers.IO) { container.roomRepository.insert(Room(roomCode = code, roomName = name)); scope.launch(Dispatchers.Main) { loadData() } } }; showAddDialog = false }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("取消") } }
        )
    }
}

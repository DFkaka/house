
package com.example.house.ui.tenant

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
import com.example.house.data.local.model.Tenant
import com.example.house.di.AppContainer
import com.example.house.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantListScreen(container: AppContainer) {
    var tenants by remember { mutableStateOf<List<Tenant>>(emptyList()) }
    var rooms by remember { mutableStateOf<List<Room>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch(Dispatchers.IO) {
            tenants = if (searchQuery.isNotBlank()) container.tenantRepository.search(searchQuery)
            else container.tenantRepository.getAll()
            rooms = container.roomRepository.getAll()
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("租客管理", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White))
        },
        floatingActionButton = { FloatingActionButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.PersonAdd, null) } }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(12.dp), placeholder = { Text("搜索...") },
                leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true,
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { load() }))
            if (loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(tenants) { t ->
                    val inRent = t.checkOutDate == null
                    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(t.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Surface(color = (if (inRent) Green600 else Gray400).copy(alpha = 0.12f), shape = MaterialTheme.shapes.small) {
                                        Text(if (inRent) "在租" else "已退", Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = if (inRent) Green600 else Gray400, fontSize = 11.sp)
                                    }
                                }
                                Text("${t.phone}  |  ${rooms.find { it.roomId == t.roomId }?.roomCode ?: ""}", color = Gray600, fontSize = 13.sp)
                                Text("入住: ${t.checkInDate}", color = Gray400, fontSize = 12.sp)
                            }
                            if (inRent) TextButton(onClick = { scope.launch(Dispatchers.IO) { container.tenantRepository.checkout(t.tenantId, LocalDate.now().toString()); container.roomRepository.updateTenantAndStatus(t.roomId, null, "VACANT"); load() } }) { Text("退租", color = Red600) }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }; var selRoomId by remember { mutableStateOf<Long?>(null) }
        var expanded by remember { mutableStateOf(false) }
        val vacantRooms = rooms.filter { it.status == "VACANT" }
        AlertDialog(onDismissRequest = { showAddDialog = false }, title = { Text("添加租客") },
            text = {
                Column {
                    OutlinedTextField(name, { name = it }, label = { Text("姓名") }, singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(phone, { phone = it }, label = { Text("手机号") }, singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(value = rooms.find { it.roomId == selRoomId }?.roomCode ?: "选择房间", onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(), label = { Text("入住房间") })
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            vacantRooms.forEach { r -> DropdownMenuItem(text = { Text("${r.roomCode} ${r.roomName}".trim()) }, onClick = { selRoomId = r.roomId; expanded = false }) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = {
                if (name.isNotBlank() && phone.isNotBlank() && selRoomId != null) {
                    scope.launch(Dispatchers.IO) {
                        container.tenantRepository.insert(Tenant(name = name, phone = phone, roomId = selRoomId!!, checkInDate = LocalDate.now().toString()))
                        container.roomRepository.updateTenantAndStatus(selRoomId!!, null, "OCCUPIED") // Room doesn't have tenantId yet
                        load()
                    }
                }
                showAddDialog = false
            }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("取消") } })
    }
}

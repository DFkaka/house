path = r"D:\codex-app\house\app\src\main\java\com\example\house\ui\tenant\TenantListScreen.kt"
with open(path, "r", encoding="utf-8") as f:
    c = f.read()

# Find and replace the old add dialog logic + checkout button with full edit/delete version
# We'll replace from "@Composable" to end with the new version

new_content = '''package com.example.house.ui.tenant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
    var editingTenant by remember { mutableStateOf<Tenant?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Tenant?>(null) }
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
                                Text("入住: ${t.checkInDate}  |  初水: ${t.initialWaterReading}  初电: ${t.initialElectricReading}", color = Gray400, fontSize = 12.sp)
                            }
                            Row {
                                if (inRent) {
                                    IconButton(onClick = { editingTenant = t }) { Icon(Icons.Default.Edit, "编辑", tint = Blue600, modifier = Modifier.size(20.dp)) }
                                    TextButton(onClick = { scope.launch(Dispatchers.IO) { container.tenantRepository.checkout(t.tenantId, LocalDate.now().toString()); container.roomRepository.updateTenantAndStatus(t.roomId, null, "VACANT"); load() } }) { Text("退租", color = Red600) }
                                } else {
                                    IconButton(onClick = { editingTenant = t }) { Icon(Icons.Default.Edit, "编辑", tint = Blue600, modifier = Modifier.size(20.dp)) }
                                    IconButton(onClick = { showDeleteConfirm = t }) { Icon(Icons.Default.Delete, "删除", tint = Red600, modifier = Modifier.size(20.dp)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Dialog
    if (showAddDialog) {
        TenantFormDialog(
            rooms = rooms,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, phone, roomId, initialWater, initialElectric ->
                scope.launch(Dispatchers.IO) {
                    val newTenant = Tenant(name = name, phone = phone, roomId = roomId, checkInDate = LocalDate.now().toString(), initialWaterReading = initialWater, initialElectricReading = initialElectric)
                    val newId = container.tenantRepository.insert(newTenant)
                    container.roomRepository.updateTenantAndStatus(roomId, newId, "OCCUPIED")
                    container.roomRepository.updateLastReadings(roomId, initialWater, initialElectric, LocalDate.now().toString(), 0.0, 0.0, 0.0)
                    load()
                }
                showAddDialog = false
            }
        )
    }

    // Edit Dialog
    if (editingTenant != null) {
        val t = editingTenant!!
        TenantFormDialog(
            rooms = rooms,
            initialName = t.name,
            initialPhone = t.phone,
            initialRoomId = t.roomId,
            initialWater = t.initialWaterReading,
            initialElectric = t.initialElectricReading,
            onDismiss = { editingTenant = null },
            onConfirm = { name, phone, roomId, initialWater, initialElectric ->
                scope.launch(Dispatchers.IO) {
                    val updated = t.copy(name = name, phone = phone, roomId = roomId, initialWaterReading = initialWater, initialElectricReading = initialElectric)
                    container.tenantRepository.update(updated)
                    if (updated.checkOutDate == null) {
                        container.roomRepository.updateTenantAndStatus(roomId, updated.tenantId, "OCCUPIED")
                    }
                    container.roomRepository.updateLastReadings(roomId, initialWater, initialElectric, updated.checkInDate, 0.0, 0.0, 0.0)
                    load()
                }
                editingTenant = null
            }
        )
    }

    // Delete Confirm Dialog
    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除租客「${showDeleteConfirm!!.name}」吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch(Dispatchers.IO) {
                        val t = showDeleteConfirm!!
                        container.tenantRepository.delete(t.tenantId)
                        if (t.checkOutDate == null) {
                            container.roomRepository.updateTenantAndStatus(t.roomId, null, "VACANT")
                        }
                        load()
                    }
                    showDeleteConfirm = null
                }) { Text("删除", color = Red600) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text("取消") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantFormDialog(
    rooms: List<Room>,
    initialName: String = "",
    initialPhone: String = "",
    initialRoomId: Long? = null,
    initialWater: Double = 0.0,
    initialElectric: Double = 0.0,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, roomId: Long, water: Double, electric: Double) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var selRoomId by remember { mutableStateOf(initialRoomId) }
    var waterStr by remember { mutableStateOf(if (initialWater == 0.0) "" else initialWater.toString()) }
    var electricStr by remember { mutableStateOf(if (initialElectric == 0.0) "" else initialElectric.toString()) }
    var expanded by remember { mutableStateOf(false) }
    val availableRooms = rooms.filter { it.status == "VACANT" || it.roomId == initialRoomId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialName.isEmpty()) "添加租客" else "编辑租客") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("姓名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(phone, { phone = it }, label = { Text("手机号") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(value = rooms.find { it.roomId == selRoomId }?.roomCode ?: "选择房间", onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(), label = { Text("入住房间") })
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        availableRooms.forEach { r -> DropdownMenuItem(text = { Text("${r.roomCode} ${r.roomName}".trim()) }, onClick = { selRoomId = r.roomId; expanded = false }) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("初始抄表读数（选填）", fontSize = 13.sp, color = Gray600)
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = waterStr,
                        onValueChange = { waterStr = it },
                        label = { Text("水表度数") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = electricStr,
                        onValueChange = { electricStr = it },
                        label = { Text("电表度数") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val w = waterStr.toDoubleOrNull() ?: 0.0
                val e = electricStr.toDoubleOrNull() ?: 0.0
                if (name.isNotBlank() && phone.isNotBlank() && selRoomId != null) {
                    onConfirm(name, phone, selRoomId!!, w, e)
                }
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
'''

with open(path, "w", encoding="utf-8") as f:
    f.write(new_content)
print("done")
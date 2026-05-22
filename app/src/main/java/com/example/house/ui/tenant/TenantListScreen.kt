
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.house.data.local.entity.TenantEntity
import com.example.house.di.AppContainer
import com.example.house.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantListScreen(container: AppContainer) {
    val vm: TenantViewModel = viewModel(
        factory = TenantViewModel.Factory(container.tenantRepository, container.roomRepository)
    )
    val state by vm.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("租客管理", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.PersonAdd, "添加租客")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { vm.onSearch(it) },
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                placeholder = { Text("搜索姓名或手机号...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty())
                        IconButton(onClick = { vm.onSearch("") }) {
                            Icon(Icons.Default.Clear, "清除")
                        }
                },
                singleLine = true
            )

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(state.tenants) { tenant ->
                        TenantCard(tenant, vm)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTenantDialog(
            container = container,
            onDismiss = { showAddDialog = false },
            onConfirm = { tenant ->
                vm.addTenant(tenant) { showAddDialog = false }
            }
        )
    }
}

@Composable
fun TenantCard(tenant: TenantEntity, vm: TenantViewModel) {
    val inRent = tenant.checkOutDate == null
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tenant.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = (if (inRent) Green600 else Gray400).copy(alpha = 0.12f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            if (inRent) "在租" else "已退租",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = if (inRent) Green600 else Gray400,
                            fontSize = 11.sp
                        )
                    }
                }
                Text(tenant.phone, color = Gray600, fontSize = 13.sp)
                Text("入住: ${tenant.checkInDate}", color = Gray400, fontSize = 12.sp)
                if (!inRent) Text("退租: ${tenant.checkOutDate}", color = Gray400, fontSize = 12.sp)
            }
            if (inRent) {
                TextButton(onClick = {
                    vm.checkoutTenant(tenant, java.time.LocalDate.now().toString()) {}
                }) {
                    Text("退租", color = Red600)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTenantDialog(
    container: AppContainer,
    onDismiss: () -> Unit,
    onConfirm: (TenantEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var rooms by remember { mutableStateOf<List<com.example.house.data.local.entity.RoomEntity>>(emptyList()) }
    var selectedRoomId by remember { mutableStateOf<Long?>(null) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        container.roomRepository.allRooms.collect { rooms = it }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加租客") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("姓名") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(phone, { phone = it }, label = { Text("手机号") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = rooms.find { it.roomId == selectedRoomId }?.roomCode ?: "选择房间",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        label = { Text("入住房间") }
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        rooms.filter { it.status == com.example.house.data.local.entity.RoomStatus.VACANT }.forEach { room ->
                            DropdownMenuItem(
                                text = { Text("${room.roomCode} ${room.roomName}".trim()) },
                                onClick = { selectedRoomId = room.roomId; expanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank() && phone.isNotBlank() && selectedRoomId != null) {
                    onConfirm(TenantEntity(
                        name = name, phone = phone, roomId = selectedRoomId!!,
                        checkInDate = java.time.LocalDate.now().toString()
                    ))
                }
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

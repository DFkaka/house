
package com.example.house.ui.room

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
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
import com.example.house.data.local.model.Settlement
import com.example.house.di.AppContainer
import com.example.house.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailScreen(roomId: Long, container: AppContainer, onBack: () -> Unit) {
    var room by remember { mutableStateOf<Room?>(null) }
    var tenant by remember { mutableStateOf<Tenant?>(null) }
    var settlements by remember { mutableStateOf<List<Settlement>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showInitReadingsDialog by remember { mutableStateOf(false) }
    val fmt = DecimalFormat("#0.00")
    val scope = rememberCoroutineScope()

    LaunchedEffect(roomId) {
        scope.launch(Dispatchers.IO) {
            room = container.roomRepository.getById(roomId)
            tenant = container.tenantRepository.getActiveByRoom(roomId)
            settlements = container.settlementRepository.getByRoom(roomId)
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(room?.roomCode ?: "", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        if (loading) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else room?.let { r ->
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Card {
                        Column(Modifier.padding(14.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(r.roomCode, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                            if (r.roomName.isNotEmpty()) Text(r.roomName, color = Gray600, fontSize = 14.sp)
                            if (tenant != null) {
                                Divider(Modifier.padding(vertical = 8.dp))
                                Text("当前租客: ${tenant!!.name}", fontWeight = FontWeight.Medium)
                                Text("电话: ${tenant!!.phone}", color = Gray600)
                            }
                        }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Blue50)) {
                        Column(Modifier.padding(14.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("上次结算", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Blue800)
                                IconButton(onClick = { showInitReadingsDialog = true }) {
                                    Icon(Icons.Default.Edit, "初始化读数", tint = Blue600, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column { Text("水表", color = Gray400, fontSize = 11.sp); Text(r.waterMeterLast.toString(), fontWeight = FontWeight.Medium, fontSize = 16.sp) }
                                Column { Text("电表", color = Gray400, fontSize = 11.sp); Text(r.electricMeterLast.toString(), fontWeight = FontWeight.Medium, fontSize = 16.sp) }
                                Column { Text("合计", color = Gray400, fontSize = 11.sp); Text("￥${fmt.format(r.lastTotalFee)}", color = Red600, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                            }
                        }
                    }
                }
                item { Text("结算历史", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                items(settlements) { s ->
                    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${s.settleDate ?: "-"}", color = Gray600, fontSize = 13.sp)
                                Text(if (s.status == "PAID") "已收取" else "未收取", color = if (s.status == "PAID") Green600 else Red600, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("水: ${fmt.format(s.waterUsage)}吨 ￥${fmt.format(s.waterFee)}", color = Blue700, fontSize = 13.sp)
                                Text("电: ${fmt.format(s.electricUsage)}度 ￥${fmt.format(s.electricFee)}", color = Orange600, fontSize = 13.sp)
                                Text("￥${fmt.format(s.totalFee)}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // 初始化水/电表读数 Dialog
    if (showInitReadingsDialog && room != null) {
        InitReadingsDialog(
            room = room!!,
            onDismiss = { showInitReadingsDialog = false },
            onConfirm = { water, electric ->
                scope.launch(Dispatchers.IO) {
                    container.roomRepository.updateLastReadings(
                        room!!.roomId, water, electric,
                        LocalDate.now().toString(), 0.0, 0.0, 0.0
                    )
                    room = container.roomRepository.getById(roomId)
                }
                showInitReadingsDialog = false
            }
        )
    }
}

@Composable
fun InitReadingsDialog(
    room: Room,
    onDismiss: () -> Unit,
    onConfirm: (water: Double, electric: Double) -> Unit
) {
    var waterStr by remember { mutableStateOf(room.waterMeterLast.toString()) }
    var electricStr by remember { mutableStateOf(room.electricMeterLast.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("初始化读数") },
        text = {
            Column {
                Text("房间: ${room.roomCode}", color = Gray600, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = waterStr,
                    onValueChange = { waterStr = it },
                    label = { Text("水表度数") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = electricStr,
                    onValueChange = { electricStr = it },
                    label = { Text("电表度数") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val w = waterStr.toDoubleOrNull() ?: 0.0
                val e = electricStr.toDoubleOrNull() ?: 0.0
                onConfirm(w, e)
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

package com.example.house.ui.settlement

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.house.data.local.model.Room
import com.example.house.data.local.model.Settlement
import com.example.house.di.AppContainer
import com.example.house.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementQueryScreen(container: AppContainer, onBack: () -> Unit) {
    var rooms by remember { mutableStateOf<List<Room>>(emptyList()) }
    var selectedRoomId by remember { mutableStateOf<Long?>(null) }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Settlement>>(emptyList()) }
    var queried by remember { mutableStateOf(false) }

    val fmt = DecimalFormat("#0.00")
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            rooms = container.roomRepository.getAll()
        }
    }

    fun doQuery() {
        scope.launch(Dispatchers.IO) {
            results = container.settlementRepository.query(
                roomId = selectedRoomId,
                startDate = startDate.ifBlank { null },
                endDate = endDate.ifBlank { null }
            )
            queried = true
        }
    }

    val totalWaterFee = results.sumOf { it.waterFee }
    val totalElectricFee = results.sumOf { it.electricFee }
    val totalFee = results.sumOf { it.totalFee }
    val paidCount = results.count { it.status == "PAID" }
    val unpaidCount = results.count { it.status == "UNPAID" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("结算查询", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // 筛选区
            Card(Modifier.fillMaxWidth().padding(12.dp)) {
                Column(Modifier.padding(12.dp)) {
                    var roomExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = roomExpanded, onExpandedChange = { roomExpanded = it }) {
                        OutlinedTextField(
                            value = selectedRoomId?.let { id -> rooms.find { r -> r.roomId == id }?.roomCode ?: "" } ?: "全部房间",
                            onValueChange = {}, readOnly = true,
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            label = { Text("房间") }
                        )
                        ExposedDropdownMenu(expanded = roomExpanded, onDismissRequest = { roomExpanded = false }) {
                            DropdownMenuItem(text = { Text("全部房间") }, onClick = { selectedRoomId = null; roomExpanded = false })
                            rooms.forEach { r -> DropdownMenuItem(text = { Text(r.roomCode) }, onClick = { selectedRoomId = r.roomId; roomExpanded = false }) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startDate, onValueChange = { startDate = it },
                            label = { Text("开始日期") }, singleLine = true,
                            placeholder = { Text("yyyy-MM-dd") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endDate, onValueChange = { endDate = it },
                            label = { Text("结束日期") }, singleLine = true,
                            placeholder = { Text("yyyy-MM-dd") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { doQuery() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Search, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("查询")
                    }
                }
            }

            // 汇总
            if (queried && results.isNotEmpty()) {
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Blue50)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("汇总", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Blue800)
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("共 ${results.size} 笔", fontSize = 13.sp, color = Gray600)
                            Text("已收 $paidCount  未收 $unpaidCount", fontSize = 13.sp, color = Gray600)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("水费合计: ¥${fmt.format(totalWaterFee)}", color = Blue700, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("电费合计: ¥${fmt.format(totalElectricFee)}", color = Orange600, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("总计: ¥${fmt.format(totalFee)}", color = Red600, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 结果列表
            if (queried) {
                if (results.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("无查询结果", color = Gray400, fontSize = 15.sp)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(results) { s ->
                            val room = rooms.find { it.roomId == s.roomId }
                            Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${room?.roomCode ?: "-"}  |  ${s.settleDate ?: "-"}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Surface(
                                            color = if (s.status == "PAID") Green600.copy(alpha = 0.12f) else Red600.copy(alpha = 0.12f),
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Text(
                                                if (s.status == "PAID") "已收" else "未收",
                                                Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                color = if (s.status == "PAID") Green600 else Red600,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("水: ${fmt.format(s.waterUsage)}吨  ¥${fmt.format(s.waterFee)}", color = Blue700, fontSize = 13.sp)
                                        Text("电: ${fmt.format(s.electricUsage)}度  ¥${fmt.format(s.electricFee)}", color = Orange600, fontSize = 13.sp)
                                        Text("¥${fmt.format(s.totalFee)}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Red600)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("选择条件后点击查询", color = Gray400, fontSize = 15.sp)
                }
            }
        }
    }
}


package com.example.house.ui.settlement

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
import com.example.house.data.local.model.*
import com.example.house.di.AppContainer
import com.example.house.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementScreen(container: AppContainer) {
    var rooms by remember { mutableStateOf<List<Room>>(emptyList()) }
    var selectedRoom by remember { mutableStateOf<Room?>(null) }
    var activeTenant by remember { mutableStateOf<Tenant?>(null) }
    var prevSettlement by remember { mutableStateOf<Settlement?>(null) }
    var settlementList by remember { mutableStateOf<List<Settlement>>(emptyList()) }

    var waterMode by remember { mutableStateOf("BY_METER") }
    var waterReading by remember { mutableStateOf("") }
    var electricReading by remember { mutableStateOf("") }
    var waterUnitPrice by remember { mutableStateOf("3.50") }
    var electricUnitPrice by remember { mutableStateOf("0.65") }
    var fixedWaterAmount by remember { mutableStateOf("30.00") }

    var showResult by remember { mutableStateOf(false) }
    var calcWaterUsage by remember { mutableStateOf(0.0) }
    var calcElectricUsage by remember { mutableStateOf(0.0) }
    var calcWaterFee by remember { mutableStateOf(0.0) }
    var calcElectricFee by remember { mutableStateOf(0.0) }
    var calcTotalFee by remember { mutableStateOf(0.0) }
    var calcMonths by remember { mutableStateOf(1) }

    val fmt = DecimalFormat("#0.00")
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            rooms = container.roomRepository.getAll()
        }
    }

    fun selectRoom(id: Long) {
        scope.launch(Dispatchers.IO) {
            selectedRoom = container.roomRepository.getById(id)
            activeTenant = container.tenantRepository.getActiveByRoom(id)
            prevSettlement = container.settlementRepository.getLatest(id)
            settlementList = container.settlementRepository.getByRoom(id)
            val latest = container.meterReadingRepository.getLatest(id)
            waterReading = latest?.waterReading?.toString() ?: ""
            electricReading = latest?.electricReading?.toString() ?: ""
            showResult = false
        }
    }

    fun calculate() {
        val room = selectedRoom ?: return
        val w = waterReading.toDoubleOrNull() ?: return
        val e = electricReading.toDoubleOrNull() ?: return
        val ePrice = electricUnitPrice.toDoubleOrNull() ?: 0.65
        val wPrice = waterUnitPrice.toDoubleOrNull() ?: 3.50

        calcElectricUsage = e - room.electricMeterLast
        calcElectricFee = calcElectricUsage * ePrice
        calcWaterUsage = w - room.waterMeterLast

        if (waterMode == "FIXED") {
            calcMonths = if (room.lastSettleDate != null)
                ChronoUnit.MONTHS.between(LocalDate.parse(room.lastSettleDate), LocalDate.now()).toInt().coerceAtLeast(1)
            else 1
            calcWaterFee = (fixedWaterAmount.toDoubleOrNull() ?: 30.00) * calcMonths
        } else {
            calcMonths = 0
            calcWaterFee = calcWaterUsage * wPrice
        }
        calcTotalFee = calcWaterFee + calcElectricFee
        showResult = true
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("水电结算", fontWeight = FontWeight.Bold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White)) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Room selector
            item {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(value = selectedRoom?.let { "${it.roomCode} ${it.roomName}".trim() } ?: "选择房间",
                        onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(), label = { Text("结算房间") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        rooms.forEach { room -> DropdownMenuItem(text = { Text("${room.roomCode} ${room.roomName}".trim()) }, onClick = { selectRoom(room.roomId); expanded = false }) }
                    }
                }
            }

            if (selectedRoom != null) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Blue50)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("上次结算摘要", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Blue800)
                            Spacer(Modifier.height(6.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("水表：${selectedRoom!!.waterMeterLast}", color = Gray600, fontSize = 13.sp); Text("电表：${selectedRoom!!.electricMeterLast}", color = Gray600, fontSize = 13.sp) }
                            Text("结算日：${selectedRoom!!.lastSettleDate ?: "-"}", color = Gray400, fontSize = 12.sp)
                        }
                    }
                }

                item { if (activeTenant != null) Text("租客: ${activeTenant!!.name}", color = Gray600) else Text("⚠ 该房间无当前租客", color = Red600) }

                item { Text("本次读数", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(waterReading, { waterReading = it; showResult = false }, label = { Text("水表 (吨)") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(electricReading, { electricReading = it; showResult = false }, label = { Text("电表 (度)") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                }

                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Gray100)) {
                        Column(Modifier.padding(10.dp)) {
                            Text("水费模式", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Spacer(Modifier.height(4.dp))
                            Row {
                                FilterChip(selected = waterMode == "BY_METER", onClick = { waterMode = "BY_METER"; showResult = false }, label = { Text("按度数") }, modifier = Modifier.padding(end = 8.dp))
                                FilterChip(selected = waterMode == "FIXED", onClick = { waterMode = "FIXED"; showResult = false }, label = { Text("固定水费") })
                            }
                        }
                    }
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (waterMode == "BY_METER") OutlinedTextField(waterUnitPrice, { waterUnitPrice = it; showResult = false }, label = { Text("水费单价 (元/吨)") }, modifier = Modifier.weight(1f), singleLine = true)
                        else OutlinedTextField(fixedWaterAmount, { fixedWaterAmount = it; showResult = false }, label = { Text("月固定水费 (元)") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(electricUnitPrice, { electricUnitPrice = it; showResult = false }, label = { Text("电费单价 (元/度)") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                }

                item {
                    Button(onClick = { calculate() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Blue700)) {
                        Icon(Icons.Default.Calculate, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("计算费用", fontSize = 16.sp)
                    }
                }

                if (showResult) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FFF0)), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                            Column(Modifier.padding(14.dp)) {
                                Text("结算明细", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Green600)
                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("用水度数:", color = Gray600); Text("${fmt.format(calcWaterUsage)} 吨", fontWeight = FontWeight.Medium) }
                                if (waterMode == "FIXED") Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("间隔月数:", color = Gray600); Text("$calcMonths 个月", fontWeight = FontWeight.Medium) }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("用电度数:", color = Gray600); Text("${fmt.format(calcElectricUsage)} 度", fontWeight = FontWeight.Medium) }
                                Divider(Modifier.padding(vertical = 6.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("水费:", color = Gray600); Text("￥${fmt.format(calcWaterFee)}", color = Blue700, fontWeight = FontWeight.Bold) }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("电费:", color = Gray600); Text("￥${fmt.format(calcElectricFee)}", color = Orange600, fontWeight = FontWeight.Bold) }
                                Divider(Modifier.padding(vertical = 6.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("合计:", fontWeight = FontWeight.Bold, fontSize = 16.sp); Text("￥${fmt.format(calcTotalFee)}", color = Red600, fontWeight = FontWeight.Bold, fontSize = 20.sp) }
                                Spacer(Modifier.height(10.dp))
                                Button(onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            val room = selectedRoom!!; val tenant = activeTenant!!; val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                                            container.settlementRepository.insert(Settlement(
                                                roomId = room.roomId, tenantId = tenant.tenantId,
                                                startDate = room.lastSettleDate, endDate = today,
                                                waterStart = room.waterMeterLast, waterEnd = waterReading.toDoubleOrNull() ?: 0.0,
                                                waterUsage = calcWaterUsage, waterUnitPrice = waterUnitPrice.toDoubleOrNull() ?: 0.0,
                                                waterFixedAmount = if (waterMode == "FIXED") fixedWaterAmount.toDoubleOrNull() else null,
                                                waterFee = calcWaterFee,
                                                electricStart = room.electricMeterLast, electricEnd = electricReading.toDoubleOrNull() ?: 0.0,
                                                electricUsage = calcElectricUsage, electricUnitPrice = electricUnitPrice.toDoubleOrNull() ?: 0.0,
                                                electricFee = calcElectricFee, totalFee = calcTotalFee, settleDate = today, status = "PAID"
                                            ))
                                            container.roomRepository.updateLastReadings(room.roomId, waterReading.toDoubleOrNull()?:0.0, electricReading.toDoubleOrNull()?:0.0, today, calcWaterFee, calcElectricFee, calcTotalFee)
                                            scope.launch(Dispatchers.Main) { snackbarHostState.showSnackbar("结算成功") }
                                            selectRoom(room.roomId)
                                        } catch (e: Exception) {
                                            scope.launch(Dispatchers.Main) { snackbarHostState.showSnackbar("结算失败") }
                                        }
                                    }
                                }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green600)) {
                                    Icon(Icons.Default.Check, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("确认收取")
                                }
                            }
                        }
                    }
                }

                item { Text("历史结算", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp)) }
                items(settlementList) { s ->
                    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("${s.settleDate ?: "-"}", color = Gray600, fontSize = 13.sp); Text(if (s.status == "PAID") "已收" else "未收", color = if (s.status == "PAID") Green600 else Red600, fontSize = 13.sp) }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("水: $calcWaterUsage ￥${fmt.format(s.waterFee)}", color = Blue700, fontSize = 13.sp); Text("电: ${fmt.format(s.electricUsage)} ￥${fmt.format(s.electricFee)}", color = Orange600, fontSize = 13.sp); Text("￥${fmt.format(s.totalFee)}", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                        }
                    }
                }
            }
        }
    }
}

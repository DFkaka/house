
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.house.data.local.entity.RoomEntity
import com.example.house.data.local.entity.SettlementEntity
import com.example.house.di.AppContainer
import com.example.house.ui.theme.*
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementScreen(container: AppContainer) {
    val vm: SettlementViewModel = viewModel(
        factory = SettlementViewModel.Factory(
            container.settlementRepository,
            container.roomRepository,
            container.tenantRepository,
            container.meterReadingRepository
        )
    )
    val state by vm.state.collectAsState()
    val fmt = DecimalFormat("#0.00")
    var showSnackbar by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("水电结算", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Room selector
            item {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = state.selectedRoom?.let { "${it.roomCode} ${it.roomName}".trim() } ?: "选择房间",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        label = { Text("结算房间") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        state.rooms.forEach { room ->
                            DropdownMenuItem(
                                text = { Text("${room.roomCode} ${room.roomName}".trim()) },
                                onClick = { vm.selectRoom(room.roomId); expanded = false }
                            )
                        }
                    }
                }
            }

            // Previous settlement summary
            if (state.selectedRoom != null) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Blue50)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("上次结算摘要", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Blue800)
                            Spacer(Modifier.height(6.dp))
                            RoomSummaryRow("上次水表", state.selectedRoom!!.waterMeterLast.toString())
                            RoomSummaryRow("上次电表", state.selectedRoom!!.electricMeterLast.toString())
                            RoomSummaryRow("上次水费", "¥${fmt.format(state.selectedRoom!!.lastWaterFee)}")
                            RoomSummaryRow("上次电费", "¥${fmt.format(state.selectedRoom!!.lastElectricFee)}")
                            RoomSummaryRow("上次合计", "¥${fmt.format(state.selectedRoom!!.lastTotalFee)}")
                            if (state.selectedRoom!!.lastSettleDate != null)
                                RoomSummaryRow("结算日期", state.selectedRoom!!.lastSettleDate!!)
                        }
                    }
                }

                // Tenant info
                item {
                    if (state.activeTenant != null) {
                        Text("租客: ${state.activeTenant!!.name}", color = Gray600, fontSize = 14.sp)
                    } else {
                        Text("⚠ 该房间无当前租客", color = Red600, fontSize = 14.sp)
                    }
                }

                // New readings input
                item {
                    Text("本次读数", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.currentWaterReading,
                            onValueChange = { vm.updateWaterReading(it) },
                            label = { Text("水表读数 (吨)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.currentElectricReading,
                            onValueChange = { vm.updateElectricReading(it) },
                            label = { Text("电表读数 (度)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                // Water mode switch
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Gray100)) {
                        Column(Modifier.padding(10.dp)) {
                            Text("水费模式", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Spacer(Modifier.height(4.dp))
                            Row {
                                FilterChip(
                                    selected = state.waterMode == WaterMode.BY_METER,
                                    onClick = { vm.setWaterMode(WaterMode.BY_METER) },
                                    label = { Text("按度数") },
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                FilterChip(
                                    selected = state.waterMode == WaterMode.FIXED,
                                    onClick = { vm.setWaterMode(WaterMode.FIXED) },
                                    label = { Text("固定水费") }
                                )
                            }
                        }
                    }
                }

                // Price inputs
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.waterMode == WaterMode.BY_METER) {
                            OutlinedTextField(
                                value = state.waterUnitPrice,
                                onValueChange = { vm.updateWaterUnitPrice(it) },
                                label = { Text("水费单价 (元/吨)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        } else {
                            OutlinedTextField(
                                value = state.fixedWaterAmount,
                                onValueChange = { vm.updateFixedWaterAmount(it) },
                                label = { Text("月固定水费 (元)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        OutlinedTextField(
                            value = state.electricUnitPrice,
                            onValueChange = { vm.updateElectricUnitPrice(it) },
                            label = { Text("电费单价 (元/度)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                // Calculate button
                item {
                    Button(
                        onClick = { vm.calculate() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Blue700)
                    ) {
                        Icon(Icons.Default.Calculate, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("计算费用", fontSize = 16.sp)
                    }
                }

                // Result
                if (state.showResult) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FFF0)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text("结算明细", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Green600)
                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("用水度数:", color = Gray600)
                                    Text("${fmt.format(state.waterUsage)} 吨", fontWeight = FontWeight.Medium)
                                }
                                if (state.waterMode == WaterMode.FIXED) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("间隔月数:", color = Gray600)
                                        Text("${state.monthsBetween} 个月", fontWeight = FontWeight.Medium)
                                    }
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("用电度数:", color = Gray600)
                                    Text("${fmt.format(state.electricUsage)} 度", fontWeight = FontWeight.Medium)
                                }
                                Divider(Modifier.padding(vertical = 6.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("水费:", color = Gray600)
                                    Text("¥${fmt.format(state.waterFee)}", color = Blue700, fontWeight = FontWeight.Bold)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("电费:", color = Gray600)
                                    Text("¥${fmt.format(state.electricFee)}", color = Orange600, fontWeight = FontWeight.Bold)
                                }
                                Divider(Modifier.padding(vertical = 6.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("合计:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("¥${fmt.format(state.totalFee)}", color = Red600, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }

                                Spacer(Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        vm.confirmSettlement { success ->
                                            showSnackbar = if (success) "结算成功!" else "结算失败"
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Green600)
                                ) {
                                    Icon(Icons.Default.Check, null, Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("确认收取")
                                }
                            }
                        }
                    }
                }

                // History
                item {
                    Text("历史结算", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
                }
                items(state.settlements) { settlement ->
                    SettlementHistoryCard(settlement, fmt)
                }
            }
        }
    }

    showSnackbar?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(2000)
            showSnackbar = null
        }
    }
}

@Composable
fun RoomSummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Gray600, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    }
}

@Composable
fun SettlementHistoryCard(s: SettlementEntity, fmt: DecimalFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("结算日: ${s.settleDate ?: "-"}", color = Gray600, fontSize = 13.sp)
                Text(
                    if (s.status == com.example.house.data.local.entity.SettleStatus.PAID) "已收取" else "未收取",
                    color = if (s.status == com.example.house.data.local.entity.SettleStatus.PAID) Green600 else Red600,
                    fontSize = 13.sp, fontWeight = FontWeight.Medium
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("水: ${fmt.format(s.waterUsage)}吨 ¥${fmt.format(s.waterFee)}", color = Blue700, fontSize = 13.sp)
                Text("电: ${fmt.format(s.electricUsage)}度 ¥${fmt.format(s.electricFee)}", color = Orange600, fontSize = 13.sp)
                Text("合计 ¥${fmt.format(s.totalFee)}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

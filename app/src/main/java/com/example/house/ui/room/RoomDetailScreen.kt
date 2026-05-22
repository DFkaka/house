
package com.example.house.ui.room

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
import com.example.house.data.local.entity.RoomEntity
import com.example.house.data.local.entity.SettlementEntity
import com.example.house.di.AppContainer
import com.example.house.ui.theme.*
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailScreen(
    roomId: Long,
    container: AppContainer,
    onBack: () -> Unit
) {
    var room by remember { mutableStateOf<RoomEntity?>(null) }
    var tenant by remember { mutableStateOf<com.example.house.data.local.entity.TenantEntity?>(null) }
    var settlements by remember { mutableStateOf<List<SettlementEntity>>(emptyList()) }
    val fmt = DecimalFormat("#0.00")

    LaunchedEffect(roomId) {
        room = container.roomRepository.getRoomById(roomId)
        tenant = container.tenantRepository.getActiveTenantByRoom(roomId)
        container.settlementRepository.getSettlementsByRoom(roomId).collect {
            settlements = it
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(room?.roomCode ?: "房间详情", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        room?.let { r ->
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Room info
                item {
                    Card {
                        Column(Modifier.padding(14.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(r.roomCode, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                StatusBadge(r.status)
                            }
                            if (r.roomName.isNotEmpty())
                                Text(r.roomName, color = Gray600, fontSize = 14.sp)
                            if (r.area != null)
                                Text("面积: ${r.area} m²", color = Gray400, fontSize = 13.sp)
                            if (tenant != null) {
                                Divider(Modifier.padding(vertical = 8.dp))
                                Text("当前租客: ${tenant!!.name}", fontWeight = FontWeight.Medium)
                                Text("电话: ${tenant!!.phone}", color = Gray600, fontSize = 13.sp)
                                Text("入住: ${tenant!!.checkInDate}", color = Gray400, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Last readings & fees
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Blue50)) {
                        Column(Modifier.padding(14.dp)) {
                            Text("上次结算信息", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Blue800)
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column { Text("水表", color = Gray400, fontSize = 11.sp); Text(r.waterMeterLast.toString(), fontWeight = FontWeight.Medium, fontSize = 16.sp) }
                                Column { Text("电表", color = Gray400, fontSize = 11.sp); Text(r.electricMeterLast.toString(), fontWeight = FontWeight.Medium, fontSize = 16.sp) }
                                Column { Text("水费", color = Gray400, fontSize = 11.sp); Text("¥${fmt.format(r.lastWaterFee)}", color = Blue700, fontWeight = FontWeight.Medium, fontSize = 16.sp) }
                                Column { Text("电费", color = Gray400, fontSize = 11.sp); Text("¥${fmt.format(r.lastElectricFee)}", color = Orange600, fontWeight = FontWeight.Medium, fontSize = 16.sp) }
                                Column { Text("合计", color = Gray400, fontSize = 11.sp); Text("¥${fmt.format(r.lastTotalFee)}", color = Red600, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                            }
                            if (r.lastSettleDate != null)
                                Text("结算日: ${r.lastSettleDate}", color = Gray400, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }

                // Settlement history
                item {
                    Text("结算历史", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                items(settlements) { s ->
                    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("结算日: ${s.settleDate ?: "-"}", color = Gray600, fontSize = 13.sp)
                                Text(
                                    if (s.status == com.example.house.data.local.entity.SettleStatus.PAID) "已收取" else "未收取",
                                    color = if (s.status == com.example.house.data.local.entity.SettleStatus.PAID) Green600 else Red600,
                                    fontWeight = FontWeight.Medium, fontSize = 13.sp
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
            }
        } ?: Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

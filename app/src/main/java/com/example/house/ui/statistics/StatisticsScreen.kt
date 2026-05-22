
package com.example.house.ui.statistics

import androidx.compose.foundation.layout.*
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
import com.example.house.di.AppContainer
import com.example.house.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(container: AppContainer) {
    var totalUnpaid by remember { mutableStateOf(0.0) }
    var unpaidCount by remember { mutableStateOf(0) }
    var monthlyTotal by remember { mutableStateOf(0.0) }
    var quarterlyTotal by remember { mutableStateOf(0.0) }
    var yearlyTotal by remember { mutableStateOf(0.0) }
    var loading by remember { mutableStateOf(true) }
    val fmt = DecimalFormat("#0.00")
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            totalUnpaid = container.settlementRepository.getTotalUnpaid()
            unpaidCount = container.settlementRepository.getUnpaid().size
            val today = LocalDate.now(); val f = DateTimeFormatter.ISO_LOCAL_DATE
            monthlyTotal = container.settlementRepository.getTotalBetween(today.withDayOfMonth(1).format(f), today.format(f))
            val qm = ((today.monthValue - 1) / 3) * 3 + 1
            quarterlyTotal = container.settlementRepository.getTotalBetween(LocalDate.of(today.year, qm, 1).format(f), today.format(f))
            yearlyTotal = container.settlementRepository.getTotalBetween(LocalDate.of(today.year, 1, 1).format(f), today.format(f))
            loading = false
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("统计报表", fontWeight = FontWeight.Bold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White)) }
    ) { padding ->
        if (loading) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else Column(Modifier.fillMaxSize().padding(padding).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (unpaidCount > 0) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = Orange600, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("未收取: ${unpaidCount} 笔", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("未收金额: ￥${fmt.format(totalUnpaid)}", color = Red600, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("本月收入", "￥${fmt.format(monthlyTotal)}", Blue700, Modifier.weight(1f))
                StatCard("本季收入", "￥${fmt.format(quarterlyTotal)}", Green600, Modifier.weight(1f))
            }
            StatCard("本年收入", "￥${fmt.format(yearlyTotal)}", Blue800, Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun StatCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = Gray600, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }
    }
}

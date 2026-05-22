
package com.example.house.ui.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.house.di.AppContainer
import com.example.house.ui.theme.*
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(container: AppContainer) {
    val vm: StatisticsViewModel = viewModel(
        factory = StatisticsViewModel.Factory(container.settlementRepository)
    )
    val state by vm.state.collectAsState()
    val fmt = DecimalFormat("#0.00")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("统计报表", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Unpaid alert card
                if (state.unpaidCount > 0) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                        Row(Modifier.padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = Orange600, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("未收取结算单: ${state.unpaidCount} 笔", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("未收总金额: ¥${fmt.format(state.totalUnpaid)}", color = Red600, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                        }
                    }
                }

                // Revenue cards
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("本月收入", "¥${fmt.format(state.monthlyTotal)}", Blue700, Modifier.weight(1f))
                    StatCard("本季收入", "¥${fmt.format(state.quarterlyTotal)}", Green600, Modifier.weight(1f))
                }
                StatCard("本年收入", "¥${fmt.format(state.yearlyTotal)}", Blue800, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = Gray600, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }
    }
}

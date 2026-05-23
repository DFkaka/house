package com.example.house.ui.meter

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
import com.example.house.data.local.model.MeterReading
import com.example.house.data.local.model.Room
import com.example.house.data.repository.SettlementRepository
import com.example.house.di.AppContainer
import com.example.house.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeterReadingScreen(container: AppContainer) {
    var readings by remember { mutableStateOf<List<MeterReading>>(emptyList()) }
    var rooms by remember { mutableStateOf<List<Room>>(emptyList()) }
    var selectedRoomId by remember { mutableStateOf<Long?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Edit state
    var editingReading by remember { mutableStateOf<MeterReading?>(null) }
    // Delete confirm
    var deletingReading by remember { mutableStateOf<MeterReading?>(null) }

    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch(Dispatchers.IO) {
            rooms = container.roomRepository.getAll()
            readings = if (selectedRoomId != null) container.meterReadingRepository.getByRoom(selectedRoomId!!)
            else container.meterReadingRepository.getAll()
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("抄表记录", fontWeight = FontWeight.Bold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White)) },
        floatingActionButton = { FloatingActionButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, null) } }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = Modifier.padding(12.dp)) {
                OutlinedTextField(
                    value = selectedRoomId?.let { id -> rooms.find { r -> r.roomId == id }?.roomCode ?: "" } ?: "全部房间",
                    onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(), label = { Text("筛选房间") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("全部房间") }, onClick = { selectedRoomId = null; expanded = false; load() })
                    rooms.forEach { r -> DropdownMenuItem(text = { Text(r.roomCode) }, onClick = { selectedRoomId = r.roomId; expanded = false; load() }) }
                }
            }
            if (loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(readings, key = { it.recordId }) { r ->
                    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(rooms.find { it.roomId == r.roomId }?.roomCode ?: "", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(r.recordDate, color = Gray400, fontSize = 12.sp)
                                }
                                Spacer(Modifier.height(2.dp))
                                Row {
                                    Text("水: ${r.waterReading}  ", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Blue700)
                                    Text("电: ${r.electricReading}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Orange600)
                                }
                            }
                            Row {
                                val isFuture = try { LocalDate.parse(r.recordDate).isAfter(LocalDate.now()) } catch (_: Exception) { false }
                                if (!isFuture) {
                                    IconButton(onClick = { editingReading = r }) {
                                        Icon(Icons.Default.Edit, "编辑", tint = Blue600, modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(onClick = { deletingReading = r }) {
                                        Icon(Icons.Default.Delete, "删除", tint = Red600, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add dialog
    if (showAddDialog) {
        val addRoomId = selectedRoomId
        val prevRoom = addRoomId?.let { rooms.find { r -> r.roomId == it } }
        AddEditDialog(
            rooms = rooms,
            title = "新增抄表",
            initialRoomId = addRoomId,
            initialDate = LocalDate.now().toString(),
            initialWater = "0",
            initialElectric = "0",
            previousWater = prevRoom?.waterMeterLast,
            previousElectric = prevRoom?.electricMeterLast,
            settlementRepo = container.settlementRepository,
            onDismiss = { showAddDialog = false },
            onConfirm = { roomId, date, water, electric ->
                scope.launch(Dispatchers.IO) {
                    container.meterReadingRepository.insert(
                        MeterReading(roomId = roomId, recordDate = date, waterReading = water, electricReading = electric)
                    )
                    load()
                }
                showAddDialog = false
            }
        )
    }

    // Edit dialog
    editingReading?.let { reading ->
        val prevReadings = readings
            .filter { it.roomId == reading.roomId && it.recordId != reading.recordId }
            .sortedByDescending { it.recordDate }
        val prevWater = prevReadings.firstOrNull()?.waterReading
            ?: rooms.find { it.roomId == reading.roomId }?.waterMeterLast
        val prevElectric = prevReadings.firstOrNull()?.electricReading
            ?: rooms.find { it.roomId == reading.roomId }?.electricMeterLast

        AddEditDialog(
            rooms = rooms,
            title = "编辑抄表",
            initialRoomId = reading.roomId,
            initialDate = reading.recordDate,
            initialWater = reading.waterReading.toString(),
            initialElectric = reading.electricReading.toString(),
            previousWater = prevWater,
            previousElectric = prevElectric,
            settlementRepo = container.settlementRepository,
            onDismiss = { editingReading = null },
            onConfirm = { roomId, date, water, electric ->
                scope.launch(Dispatchers.IO) {
                    container.meterReadingRepository.update(
                        reading.copy(roomId = roomId, recordDate = date, waterReading = water, electricReading = electric)
                    )
                    load()
                }
                editingReading = null
            }
        )
    }

    // Delete confirm dialog
    deletingReading?.let { reading ->
        AlertDialog(
            onDismissRequest = { deletingReading = null },
            title = { Text("确认删除") },
            text = { Text("删除 ${rooms.find { it.roomId == reading.roomId }?.roomCode ?: ""} 的 ${reading.recordDate} 抄表记录？") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch(Dispatchers.IO) {
                        container.meterReadingRepository.delete(reading.recordId)
                        load()
                    }
                    deletingReading = null
                }) { Text("删除", color = Red600) }
            },
            dismissButton = { TextButton(onClick = { deletingReading = null }) { Text("取消") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDialog(
    rooms: List<Room>,
    title: String,
    initialRoomId: Long?,
    initialDate: String,
    initialWater: String,
    initialElectric: String,
    previousWater: Double? = null,
    previousElectric: Double? = null,
    settlementRepo: SettlementRepository? = null,
    onDismiss: () -> Unit,
    onConfirm: (roomId: Long, date: String, water: Double, electric: Double) -> Unit
) {
    var selRoomId by remember { mutableStateOf(initialRoomId) }
    var date by remember { mutableStateOf(initialDate) }
    var water by remember { mutableStateOf(initialWater) }
    var electric by remember { mutableStateOf(initialElectric) }
    var expanded by remember { mutableStateOf(false) }
    var showLowWarning by remember { mutableStateOf(false) }
    var isFixedWater by remember { mutableStateOf(false) }
    var currentPrevWater by remember { mutableStateOf(previousWater) }
    var currentPrevElectric by remember { mutableStateOf(previousElectric) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(initialRoomId) {
        initialRoomId?.let { rid ->
            settlementRepo?.let { repo ->
                scope.launch(Dispatchers.IO) {
                    val s = repo.getLatest(rid)
                    isFixedWater = s?.waterFixedAmount != null
                }
            }
        }
    }

    fun doSave() {
        val w = water.toDoubleOrNull() ?: return
        val e = electric.toDoubleOrNull() ?: return
        val id = selRoomId ?: return
        if (date.isBlank()) return
        onConfirm(id, date, w, e)
    }

    fun checkAndSave() {
        val w = water.toDoubleOrNull()
        val e = electric.toDoubleOrNull()
        val pw = currentPrevWater
        val pe = currentPrevElectric
        if ((pw != null && w != null && w < pw) || (pe != null && e != null && e < pe)) {
            showLowWarning = true
        } else {
            doSave()
        }
    }

    // 读数过低警告弹窗
    if (showLowWarning) {
        AlertDialog(
            onDismissRequest = { showLowWarning = false },
            title = { Text("无法保存") },
            text = {
                Column {
                    Text("当前输入的读数小于上次读数，不能保存：")
                    Spacer(Modifier.height(4.dp))
                    currentPrevWater?.let { pw ->
                        val w = water.toDoubleOrNull()
                        if (w != null && w < pw) {
                            Text("· 水表: 上次 $pw → 本次 $w")
                        }
                    }
                    currentPrevElectric?.let { pe ->
                        val e = electric.toDoubleOrNull()
                        if (e != null && e < pe) {
                            Text("· 电表: 上次 $pe → 本次 $e")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("如因换新表导致读数归零，请在房间详情中初始化读数后再抄表", color = Gray400, fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showLowWarning = false }) {
                    Text("返回修改")
                }
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = rooms.find { it.roomId == selRoomId }?.roomCode ?: "选择房间",
                        onValueChange = {}, readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        label = { Text("房间") }
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    rooms.forEach { r ->
                        DropdownMenuItem(
                            text = { Text("${r.roomCode} ${r.roomName}".trim()) },
                            onClick = {
                                selRoomId = r.roomId; expanded = false
                                currentPrevWater = r.waterMeterLast
                                currentPrevElectric = r.electricMeterLast
                                settlementRepo?.let { repo ->
                                    scope.launch(Dispatchers.IO) {
                                        val s = repo.getLatest(r.roomId)
                                        isFixedWater = s?.waterFixedAmount != null
                                    }
                                }
                            }
                        )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(date, { date = it }, label = { Text("抄表日期") }, singleLine = true, placeholder = { Text("yyyy-MM-dd") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(water, { water = it }, label = { Text("水表读数") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(electric, { electric = it }, label = { Text("电表读数") }, singleLine = true)

                // 上次读数参考
                if (currentPrevWater != null || currentPrevElectric != null) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = Gray200)
                    Spacer(Modifier.height(8.dp))
                    Text("上次抄表读数", fontSize = 12.sp, color = Gray400, fontWeight = FontWeight.Medium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        currentPrevWater?.let {
                            Text("水表: $it", fontSize = 13.sp, color = Gray600)
                        }
                        currentPrevElectric?.let {
                            Text("电表: $it", fontSize = 13.sp, color = Gray600)
                        }
                    }
                }
                if (isFixedWater) {
                    Spacer(Modifier.height(6.dp))
                    Text("水费按月收费房间", color = Red600, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { checkAndSave() }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

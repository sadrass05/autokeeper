package com.example.autobookkeeper.ui.screen

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.autobookkeeper.data.entity.FinancePosition
import com.example.autobookkeeper.ui.components.GlassCard
import com.example.autobookkeeper.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.example.autobookkeeper.data.entity.FinanceExpense
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.ui.platform.LocalLocale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FinanceScreen(viewModel: MainViewModel = hiltViewModel()) {
    val positions by viewModel.positions.collectAsStateWithLifecycle()
    val totalProfit by viewModel.totalProfit.collectAsStateWithLifecycle()
    val financeExpense by viewModel.financeExpense.collectAsStateWithLifecycle()
    val netFinanceProfit by viewModel.netFinanceProfit.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var showActionSheet by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showDeleteSheet by remember { mutableStateOf(false) }
    var positionToDelete by remember { mutableStateOf<FinancePosition?>(null) }
    var showImportPreview by remember { mutableStateOf(false) }
    var importPreviewData by remember { mutableStateOf<List<FinancePosition>>(emptyList()) }
    var importPreviewTotal by remember { mutableStateOf(0) }
    var importPreviewSkipped by remember { mutableStateOf(0) }

    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuPosition by remember { mutableStateOf<FinancePosition?>(null) }
    var showAddAmountDialog by remember { mutableStateOf(false) }
    var showReduceAmountDialog by remember { mutableStateOf(false) }
    var addReduceAmount by remember { mutableStateOf("") }
    var isAddOperation by remember { mutableStateOf(true) }

    val financeExpenses by viewModel.financeExpenses.collectAsStateWithLifecycle()
    var showAddExpenseSheet by remember { mutableStateOf(false) }
    var showAllExpenses by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<FinanceExpense?>(null) }
    var showDeleteExpenseConfirm by remember { mutableStateOf(false) }

    val accumulatedProfit by viewModel.accumulatedProfit.collectAsStateWithLifecycle()
    val financeExpenseRecords by viewModel.financeExpenseRecords.collectAsStateWithLifecycle()
    var showEditAccumulatedProfit by remember { mutableStateOf(false) }
    var accumulatedProfitEditText by remember { mutableStateOf("") }

    val context = LocalContext.current

    val fileImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            coroutineScope.launch(Dispatchers.IO) {
                val result = parseFinanceFile(context, uri)
                withContext(Dispatchers.Main) {
                    if (result.positions.isEmpty()) {
                        Toast.makeText(context, "未能解析到有效数据", Toast.LENGTH_SHORT).show()
                    } else {
                        importPreviewData = result.positions
                        importPreviewTotal = result.positions.size
                        importPreviewSkipped = result.skipped
                        showImportPreview = true
                    }
                }
            }
        }
    }

    val totalCurrentValue = positions.sumOf { it.currentValue }

    val navBarHeight = WindowInsets.navigationBars
        .asPaddingValues().calculateBottomPadding()
    val fabBottomPadding = navBarHeight + 76.dp

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(
                    bottom = navBarHeight + 140.dp
                ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                GlassCard(contentPadding = PaddingValues(0.dp)) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatCell(
                                label = "持仓收益",
                                value = formatFinanceAmount(totalProfit),
                                valueColor = if (totalProfit > 0) MaterialTheme.colorScheme.tertiary
                                             else if (totalProfit < 0) MaterialTheme.colorScheme.error
                                             else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                alignment = Alignment.CenterHorizontally
                            )
                            Box(
                                modifier = Modifier
                                    .width(0.5.dp)
                                    .height(40.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                            Box(
                                modifier = Modifier.weight(1f).clickable(
                                    interactionSource = MutableInteractionSource(),
                                    indication = null
                                ) {
                                    accumulatedProfitEditText = if (accumulatedProfit == 0.0) "" else accumulatedProfit.toString()
                                    showEditAccumulatedProfit = true
                                },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "累计收益",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Icon(
                                            Icons.Default.Edit, contentDescription = "编辑",
                                            modifier = Modifier.size(12.dp).padding(start = 2.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        formatFinanceAmount(accumulatedProfit),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1, softWrap = false
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (netFinanceProfit >= 0) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                                            else MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                                )
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "净收益 = 累计收益 - 总收益支出 = ${formatFinanceAmount(netFinanceProfit)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (netFinanceProfit > 0) MaterialTheme.colorScheme.tertiary
                                        else if (netFinanceProfit < 0) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // ===== 收益支出记录 =====
            item {
                GlassCard(contentPadding = PaddingValues(0.dp)) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp, start = 14.dp, end = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "收益支出记录",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            TextButton(onClick = { showAddExpenseSheet = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("记录支出")
                            }
                        }
                        if (financeExpenses.isEmpty()) {
                            Text(
                                "暂无收益支出记录",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        } else {
                            val displayList = if (showAllExpenses) financeExpenses else financeExpenses.take(5)
                            displayList.forEachIndexed { index, expense ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            expense.description.ifEmpty { "未命名" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            expense.fromProduct,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "-¥${"%.2f".format(expense.amount)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            java.text.SimpleDateFormat("MM-dd HH:mm", LocalLocale.current.platformLocale).format(java.util.Date(expense.recordedAt)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    if (expense.id != 0) {
                                        IconButton(
                                            onClick = { expenseToDelete = expense; showDeleteExpenseConfirm = true },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "删除",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                if (index < displayList.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 14.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        thickness = 0.5.dp
                                    )
                                }
                            }
                            if (financeExpenses.size > 5 && !showAllExpenses) {
                                TextButton(
                                    onClick = { showAllExpenses = true },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Text("查看全部 (${financeExpenses.size}条)")
                                }
                            }
                            if (showAllExpenses && financeExpenses.size > 5) {
                                TextButton(
                                    onClick = { showAllExpenses = false },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Text("收起")
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // ===== 理财支出排行 =====
            item {
                GlassCard(contentPadding = PaddingValues(0.dp)) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp, start = 14.dp, end = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "理财支出排行",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (financeExpenseRecords.isEmpty()) {
                            Text(
                                "暂无理财支出记录",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        } else {
                            val displayRecords = financeExpenseRecords.take(10)
                            displayRecords.forEachIndexed { index, record ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RankBadge(rank = index + 1)
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            record.merchant.ifEmpty { record.category.ifEmpty { "未分类" } },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "${record.platform} · ${record.paymentChannel}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "-¥${"%.2f".format(record.amount)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            java.text.SimpleDateFormat("MM-dd", LocalLocale.current.platformLocale).format(java.util.Date(record.recordedAt)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (index < displayRecords.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 14.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        thickness = 0.5.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
                if (isMultiSelectMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("已选 ${selectedIds.size} 项", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { isMultiSelectMode = false; selectedIds = emptySet() }, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)) { Text("取消") }
                            val allSelected = positions.isNotEmpty() && selectedIds.size == positions.size
                            OutlinedButton(onClick = { selectedIds = if (allSelected) emptySet() else positions.map { it.id.toLong() }.toSet() }, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)) { Text(if (allSelected) "取消全选" else "全选") }
                            Button(onClick = { if (selectedIds.isNotEmpty()) showBatchDeleteConfirm = true }, enabled = selectedIds.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)) { Text("删除") }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("持仓明细", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Button(
                            onClick = { isMultiSelectMode = true; selectedIds = emptySet() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.primary)
                        ) { Text("多选") }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            items(positions, key = { it.id }) { position ->
                Column {
                    Box {
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 10.dp)
                                .combinedClickable(onClick = {}, onLongClick = { if (!isMultiSelectMode) { contextMenuPosition = position; showContextMenu = true } }),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isMultiSelectMode) {
                                Checkbox(checked = position.id.toLong() in selectedIds, onCheckedChange = { checked -> selectedIds = if (checked) selectedIds + position.id.toLong() else selectedIds - position.id.toLong() })
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(position.productName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text(position.platform, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("¥${"%.2f".format(position.profit)}", style = MaterialTheme.typography.headlineMedium, color = if (position.profit >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
                                Text("${"%.2f".format(position.profitRate)}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        DropdownMenu(expanded = showContextMenu && contextMenuPosition == position, onDismissRequest = { showContextMenu = false }) {
                            DropdownMenuItem(text = { Text("加仓") }, onClick = { showContextMenu = false; isAddOperation = true; addReduceAmount = ""; showAddAmountDialog = true })
                            DropdownMenuItem(text = { Text("减仓") }, onClick = { showContextMenu = false; isAddOperation = false; addReduceAmount = ""; showReduceAmountDialog = true })
                            DropdownMenuItem(text = { Text("删除") }, onClick = { showContextMenu = false; positionToDelete = position; showDeleteSheet = true })
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            item { Spacer(modifier = Modifier.height(160.dp)) }
        }

            FloatingActionButton(
                onClick = { showActionSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = fabBottomPadding),
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加持仓")
            }
        }
    }

    // ---- ActionSheet (选择手动添加或文件导入) ----
    if (showActionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showActionSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("添加理财持仓", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)

                OutlinedButton(
                    onClick = { showActionSheet = false; showAddSheet = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("手动添加", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text("填写表单添加单条持仓", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                OutlinedButton(
                    onClick = { showActionSheet = false; fileImportLauncher.launch(arrayOf("text/csv", "text/plain", "*/*")) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("文件导入", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text("从 Excel/CSV/TXT 批量导入", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    // ---- ImportPreviewSheet ----
    if (showImportPreview) {
        ModalBottomSheet(
            onDismissRequest = { showImportPreview = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("导入预览", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("检测到 $importPreviewTotal 条记录", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (importPreviewSkipped > 0) {
                        Text("，跳过 $importPreviewSkipped 条", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                }

                importPreviewData.take(5).forEachIndexed { index, position ->
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(position.productName.ifEmpty { "未命名" }, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                Text(position.platform.ifEmpty { "未知平台" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("¥${"%.2f".format(position.currentValue)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                        if (index < importPreviewData.take(5).size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                        }
                    }
                }

                if (importPreviewData.size > 5) {
                    Text("... 还有 ${importPreviewData.size - 5} 条", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showImportPreview = false },
                        modifier = Modifier.weight(1f)
                    ) { Text("取消") }
                    Button(
                        onClick = {
                            showImportPreview = false
                            coroutineScope.launch {
                                viewModel.addFinancePositionsFromList(importPreviewData)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "成功导入 $importPreviewTotal 条", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("确认导入 ${importPreviewData.size} 条") }
                }
            }
        }
    }

    // ---- 原有 BottomSheets (保持完全不变) ----
    if (showAddSheet) {
        AddFinanceSheet(
            onDismiss = { showAddSheet = false },
            onAdd = { position -> viewModel.addFinancePosition(position); showAddSheet = false }
        )
    }

    if (showDeleteSheet && positionToDelete != null) {
        val targetPos = positionToDelete
        DeleteFinanceSheet(
            positionName = targetPos?.productName ?: "未知持仓",
            onDismiss = { showDeleteSheet = false; positionToDelete = null },
            onDelete = {
                coroutineScope.launch {
                    targetPos?.let { viewModel.deleteFinancePosition(it.id.toLong()) }
                    showDeleteSheet = false; positionToDelete = null
                }
            }
        )
    }

    if (showAddAmountDialog && contextMenuPosition != null) {
        val targetPos = contextMenuPosition
        AlertDialog(
            onDismissRequest = { showAddAmountDialog = false },
            title = { Text("加仓") },
            text = { OutlinedTextField(value = addReduceAmount, onValueChange = { addReduceAmount = it }, label = { Text("加仓金额") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { Button(onClick = { val amount = addReduceAmount.toDoubleOrNull() ?: 0.0; if (amount > 0) { targetPos?.let { pos -> val updated = pos.copy(buyAmount = pos.buyAmount + amount, currentValue = pos.currentValue + amount); coroutineScope.launch { viewModel.updateFinancePosition(updated) } } }; showAddAmountDialog = false }) { Text("确定") } },
            dismissButton = { Button(onClick = { showAddAmountDialog = false }) { Text("取消") } }
        )
    }

    if (showReduceAmountDialog && contextMenuPosition != null) {
        val targetPos = contextMenuPosition
        AlertDialog(
            onDismissRequest = { showReduceAmountDialog = false },
            title = { Text("减仓") },
            text = { OutlinedTextField(value = addReduceAmount, onValueChange = { addReduceAmount = it }, label = { Text("减仓金额") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { Button(onClick = { val amount = addReduceAmount.toDoubleOrNull() ?: 0.0; if (amount > 0) { targetPos?.let { pos -> val updated = pos.copy(buyAmount = (pos.buyAmount - amount).coerceAtLeast(0.0), currentValue = (pos.currentValue - amount).coerceAtLeast(0.0)); coroutineScope.launch { viewModel.updateFinancePosition(updated) } } }; showReduceAmountDialog = false }) { Text("确定") } },
            dismissButton = { Button(onClick = { showReduceAmountDialog = false }) { Text("取消") } }
        )
    }

    if (showBatchDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            title = { Text("批量删除确认") },
            text = { Text("确定要删除选中的 ${selectedIds.size} 项持仓吗？此操作不可撤销。") },
            confirmButton = { Button(onClick = { viewModel.deleteFinancePositions(selectedIds.toList()); selectedIds = emptySet(); isMultiSelectMode = false; showBatchDeleteConfirm = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)) { Text("删除") } },
            dismissButton = { Button(onClick = { showBatchDeleteConfirm = false }) { Text("取消") } }
        )
    }

    if (showAddExpenseSheet) {
        AddFinanceExpenseSheet(
            positions = positions,
            onDismiss = { showAddExpenseSheet = false },
            onAdd = { expense ->
                viewModel.addFinanceExpense(expense)
                showAddExpenseSheet = false
            }
        )
    }

    if (showDeleteExpenseConfirm && expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteExpenseConfirm = false; expenseToDelete = null },
            title = { Text("删除支出记录") },
            text = { Text("确定要删除「${expenseToDelete?.description}」吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        expenseToDelete?.let { viewModel.deleteFinanceExpense(it) }
                        showDeleteExpenseConfirm = false
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
                ) { Text("删除") }
            },
            dismissButton = { Button(onClick = { showDeleteExpenseConfirm = false; expenseToDelete = null }) { Text("取消") } }
        )
    }

    if (showEditAccumulatedProfit) {
        AlertDialog(
            onDismissRequest = { showEditAccumulatedProfit = false },
            title = { Text("设置累计收益") },
            text = {
                OutlinedTextField(
                    value = accumulatedProfitEditText,
                    onValueChange = { accumulatedProfitEditText = it },
                    label = { Text("累计收益金额") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val value = accumulatedProfitEditText.toDoubleOrNull() ?: return@Button
                    viewModel.setAccumulatedProfit(value)
                    showEditAccumulatedProfit = false
                }) { Text("确定") }
            },
            dismissButton = { Button(onClick = { showEditAccumulatedProfit = false }) { Text("取消") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFinanceExpenseSheet(
    positions: List<FinancePosition>,
    onDismiss: () -> Unit,
    onAdd: (FinanceExpense) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf(if (positions.isNotEmpty()) positions[0].productName else "") }
    var expanded by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTime by remember { mutableStateOf(LocalTime.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("记录理财支出", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("支出金额") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("用途描述") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectedProduct.ifEmpty { "选择来源产品" })
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    positions.forEach { position ->
                        DropdownMenuItem(
                            text = { Text(position.productName) },
                            onClick = { selectedProduct = position.productName; expanded = false }
                        )
                    }
                }
            }

            Text("日期时间", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${selectedDate.monthValue}月${selectedDate.dayOfMonth}日", fontSize = 13.sp)
                }
                OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(String.format("%02d:%02d", selectedTime.hour, selectedTime.minute), fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val amountVal = amount.toDoubleOrNull() ?: return@Button
                    val recordedAt = selectedDate.atTime(selectedTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val expense = FinanceExpense(amount = amountVal, description = description, fromProduct = selectedProduct, recordedAt = recordedAt)
                    onAdd(expense)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text("确认添加")
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { millis -> selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate() }; showDatePicker = false }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = selectedTime.hour, initialMinute = selectedTime.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = { TextButton(onClick = { selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute); showTimePicker = false }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("取消") } },
            text = { TimePicker(state = timePickerState) }
        )
    }
}

@Composable
private fun RankBadge(rank: Int) {
    val (bgColor, textColor) = when (rank) {
        1 -> Pair(Color(0xFFFFD700), Color.White)
        2 -> Pair(Color(0xFFC0C0C0), Color.White)
        3 -> Pair(Color(0xFFCD7F32), Color.White)
        else -> Pair(Color.Transparent, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Box(
        modifier = Modifier.size(28.dp).clip(CircleShape).background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "$rank",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = textColor,
            fontSize = 13.sp
        )
    }
}

private data class FinanceParseResult(
    val positions: List<FinancePosition>,
    val skipped: Int = 0
)

private fun parseAmount(text: String): Double {
    return text.trim().replace("¥", "").replace("￥", "").replace(",", "").toDoubleOrNull() ?: 0.0
}

private fun getFileName(context: Context, uri: Uri): String {
    var name = "unknown"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) {
            name = cursor.getString(index) ?: "unknown"
        }
    }
    return name
}

private fun parseCsv(context: Context, uri: Uri): FinanceParseResult {
    val positions = mutableListOf<FinancePosition>()
    var skipped = 0
    try {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
            var isFirstLine = true
            reader.forEachLine { line ->
                if (isFirstLine) { isFirstLine = false; return@forEachLine }
                val trimmed = line.trim()
                if (trimmed.isEmpty()) return@forEachLine
                try {
                    val cols = trimmed.split(",").map { it.trim() }
                    if (cols.size < 6) { skipped++; return@forEachLine }
                    val productName = cols[0]
                    val platform = cols[1]
                    val buyAmount = parseAmount(cols[2])
                    val currentValue = parseAmount(cols[3])
                    val profit = parseAmount(cols[4])
                    val profitRate = parseAmount(cols[5])
                    if (productName.isBlank()) { skipped++; return@forEachLine }
                    positions.add(FinancePosition(productName = productName, platform = platform, buyAmount = buyAmount, currentValue = currentValue, profit = profit, profitRate = profitRate, screenshotPath = "", updatedAt = System.currentTimeMillis()))
                } catch (e: Exception) { skipped++ }
            }
        }
    } catch (e: Exception) { }
    return FinanceParseResult(positions, skipped)
}

private fun shouldSkipLine(line: String): Boolean {
    if (line.isEmpty()) return true
    val lower = line.lowercase()
    return lower.contains("产品名称") || lower.contains("产品") && lower.contains("名称") || lower.contains("日期")
}

private fun parseTxt(context: Context, uri: Uri): FinanceParseResult {
    val positions = mutableListOf<FinancePosition>()
    var skipped = 0
    try {
        val lines = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readLines() } ?: return FinanceParseResult(emptyList(), 0)

        val firstNonEmpty = lines.firstOrNull { it.trim().isNotEmpty() } ?: return FinanceParseResult(emptyList(), 0)
        val isKeyValue = firstNonEmpty.contains(":")

        if (isKeyValue) {
            val blocks = mutableListOf<MutableList<String>>()
            var currentBlock = mutableListOf<String>()
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) {
                    if (currentBlock.isNotEmpty()) { blocks.add(currentBlock); currentBlock = mutableListOf() }
                } else {
                    currentBlock.add(trimmed)
                }
            }
            if (currentBlock.isNotEmpty()) blocks.add(currentBlock)

            for (block in blocks) {
                try {
                    val map = mutableMapOf<String, String>()
                    for (line in block) {
                        val parts = line.split(":", limit = 2)
                        if (parts.size == 2) map[parts[0].trim()] = parts[1].trim()
                    }
                    val productName = map["产品名称"] ?: map["产品"] ?: ""
                    if (productName.isBlank()) { skipped++; continue }
                    val platform = map["平台"] ?: "未知"
                    val buyAmount = map["买入金额"]?.let { parseAmount(it) } ?: 0.0
                    val currentValue = map["当前市值"]?.let { parseAmount(it) } ?: 0.0
                    val profit = map["收益"]?.let { parseAmount(it) } ?: 0.0
                    val profitRate = map["收益率"]?.let { parseAmount(it) } ?: 0.0
                    positions.add(FinancePosition(productName = productName, platform = platform, buyAmount = buyAmount, currentValue = currentValue, profit = profit, profitRate = profitRate, screenshotPath = "", updatedAt = System.currentTimeMillis()))
                } catch (e: Exception) { skipped++ }
            }
        } else {
            for (line in lines) {
                val trimmed = line.trim()
                if (shouldSkipLine(trimmed)) continue
                val parts = if (trimmed.contains("\t")) trimmed.split("\t") else trimmed.split(",")
                val cols = parts.map { it.trim() }.filter { it.isNotEmpty() }
                if (cols.size < 6) { skipped++; continue }
                try {
                    val productName = cols[0]
                    val platform = cols[1]
                    val buyAmount = parseAmount(cols[2])
                    val currentValue = parseAmount(cols[3])
                    val profit = parseAmount(cols[4])
                    val profitRate = parseAmount(cols[5])
                    if (productName.isBlank()) { skipped++; continue }
                    positions.add(FinancePosition(productName = productName, platform = platform, buyAmount = buyAmount, currentValue = currentValue, profit = profit, profitRate = profitRate, screenshotPath = "", updatedAt = System.currentTimeMillis()))
                } catch (e: Exception) { skipped++ }
            }
        }
    } catch (e: Exception) { }
    return FinanceParseResult(positions, skipped)
}

private suspend fun parseFinanceFile(context: Context, uri: Uri): FinanceParseResult {
    val fileName = getFileName(context, uri).lowercase()
    return when {
        fileName.endsWith(".csv") -> parseCsv(context, uri)
        fileName.endsWith(".txt") -> parseTxt(context, uri)
        else -> parseCsv(context, uri)
    }
}

private fun formatFinanceAmount(value: Double): String {
    val abs = kotlin.math.abs(value)
    val sign = if (value < 0) "-" else ""
    return when {
        abs >= 100_000_000 -> "${sign}¥${"%.2f".format(abs / 100_000_000)}亿"
        abs >= 10_000 -> "${sign}¥${"%.2f".format(abs / 10_000)}万"
        else -> "${sign}¥${"%.2f".format(abs)}"
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier,
    alignment: Alignment.Horizontal = Alignment.CenterHorizontally
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            softWrap = false
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFinanceSheet(
    onDismiss: () -> Unit,
    onAdd: (FinancePosition) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var productName by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("") }
    var buyAmount by remember { mutableStateOf("") }
    var currentValue by remember { mutableStateOf("") }
    var profit by remember { mutableStateOf("") }
    var profitRate by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "添加理财持仓",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = productName,
                onValueChange = { productName = it },
                label = { Text("产品名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            OutlinedTextField(
                value = platform,
                onValueChange = { platform = it },
                label = { Text("平台") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            OutlinedTextField(
                value = buyAmount,
                onValueChange = { buyAmount = it },
                label = { Text("买入金额") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            OutlinedTextField(
                value = currentValue,
                onValueChange = { currentValue = it },
                label = { Text("当前市值") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            OutlinedTextField(
                value = profit,
                onValueChange = { profit = it },
                label = { Text("收益") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            OutlinedTextField(
                value = profitRate,
                onValueChange = { profitRate = it },
                label = { Text("收益率(%)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val position = FinancePosition(
                        productName = productName,
                        platform = platform,
                        buyAmount = buyAmount.toDoubleOrNull() ?: 0.0,
                        currentValue = currentValue.toDoubleOrNull() ?: 0.0,
                        profit = profit.toDoubleOrNull() ?: 0.0,
                        profitRate = profitRate.toDoubleOrNull() ?: 0.0,
                        screenshotPath = "",
                        updatedAt = System.currentTimeMillis()
                    )
                    onAdd(position)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("保存")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteFinanceSheet(
    positionName: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "确认删除",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "确定要删除「$positionName」吗？",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("取消")
                }
                Button(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("删除")
                }
            }
        }
    }
}

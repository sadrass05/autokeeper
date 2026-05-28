package com.example.autobookkeeper.ui.screen

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.autobookkeeper.BuildConfig
import com.example.autobookkeeper.backup.BackupFile
import com.example.autobookkeeper.backup.BackupManager
import com.example.autobookkeeper.backup.BackupResult
import com.example.autobookkeeper.backup.RestoreResult
import com.example.autobookkeeper.backup.WeeklyBackupWorker
import com.example.autobookkeeper.data.SyncPrefs
import com.example.autobookkeeper.data.repository.ExpenseRepository
import com.example.autobookkeeper.network.SyncService
import com.example.autobookkeeper.network.DiagnosticsStep
import com.example.autobookkeeper.ui.export.CsvExporter
import com.example.autobookkeeper.ui.export.ExportResult
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.EntryPoints
import com.example.autobookkeeper.ui.importdata.ImportManager
import com.example.autobookkeeper.ui.importdata.ImportResult
import com.example.autobookkeeper.ui.components.GlassCard
import com.example.autobookkeeper.ui.theme.ThemePrefs
import com.example.autobookkeeper.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    var showSyncSheet by remember { mutableStateOf(false) }

    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf("") }
    var syncSuccess by remember { mutableStateOf<Boolean?>(null) }
    var pingMessage by remember { mutableStateOf("") }
    var pingSuccess by remember { mutableStateOf<Boolean?>(null) }
    var isPinging by remember { mutableStateOf(false) }
    var isDiagnosing by remember { mutableStateOf(false) }
    var diagnosticsSteps by remember { mutableStateOf<List<DiagnosticsStep>>(emptyList()) }
    var showCleanupDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: MainViewModel = hiltViewModel()
    val syncPrefs = runBlocking {
        EntryPoints.get(context.applicationContext, SyncPrefsEntryPoint::class.java).syncPrefs()
    }
    val syncService = runBlocking {
        EntryPoints.get(context.applicationContext, SyncServiceEntryPoint::class.java).syncService()
    }
    val csvExporter = runBlocking {
        EntryPoints.get(context.applicationContext, CsvExporterEntryPoint::class.java).csvExporter()
    }
    val importManager = runBlocking {
        EntryPoints.get(context.applicationContext, ImportManagerEntryPoint::class.java).importManager()
    }
    val expenseRepository = runBlocking {
        EntryPoints.get(context.applicationContext, ExpenseRepoEntryPoint::class.java).expenseRepository()
    }
    var importResult by remember { mutableStateOf<ImportResult?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    var showTrash by remember { mutableStateOf(false) }
    var serverIp by remember { mutableStateOf(syncPrefs.serverIp) }
    var serverPortText by remember { mutableStateOf(syncPrefs.serverPort.toString()) }
    val lastSyncFormatted = if (syncPrefs.lastSyncTime > 0L) {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", LocalLocale.current.platformLocale)
            .format(Date(syncPrefs.lastSyncTime))
    } else "从未同步"
    val initialDarkTheme = remember {
        runBlocking { ThemePrefs.isDarkTheme(context).first() }
    }
    var isDarkTheme by remember { mutableStateOf(initialDarkTheme) }

    val backupManager = runBlocking {
        EntryPoints.get(context.applicationContext, BackupManagerEntryPoint::class.java).backupManager()
    }
    var showBackupSheet by remember { mutableStateOf(false) }
    var backupList by remember { mutableStateOf<List<BackupFile>>(emptyList()) }
    var isBackingUp by remember { mutableStateOf(false) }
    var backupMessage by remember { mutableStateOf("") }
    var restoreTargetFile by remember { mutableStateOf<BackupFile?>(null) }
    var restoreResult by remember { mutableStateOf<RestoreResult?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        ThemePrefs.isDarkTheme(context).collect { dark ->
            isDarkTheme = dark
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            isImporting = true
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    importManager.importFile(uri)
                }
                importResult = result
                isImporting = false
            }
        }
    }

    if (showTrash) {
        TrashScreen(onBack = { showTrash = false })
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Text(
                text = "设置",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(
                    bottom = 88.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                )
                .verticalScroll(rememberScrollState())
        ) {
            SectionHeader(title = "通知")
            GlassCard(contentPadding = PaddingValues(0.dp)) {
                SettingsRow(
                    icon = Icons.Default.Notifications,
                    title = "通知权限设置",
                    subtitle = "开启后自动读取支付通知",
                    onClick = {
                        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                        context.startActivity(intent)
                    }
                )
            }
            Spacer(Modifier.height(12.dp))

            SectionHeader(title = "数据")

            if (BuildConfig.IS_PRO) {
                GlassCard(contentPadding = PaddingValues(0.dp)) {
                    SettingsRow(
                        icon = Icons.Default.Refresh,
                        title = "数据同步",
                        subtitle = if (syncPrefs.lastSyncTime > 0L)
                            "上次同步: $lastSyncFormatted"
                        else
                            "点击配置同步服务器",
                        onClick = { showSyncSheet = true }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            GlassCard(contentPadding = PaddingValues(0.dp)) {
                SettingsRow(
                    icon = Icons.Default.Share,
                    title = "导入数据",
                    subtitle = "从CSV或TXT文件导入支出/理财记录",
                    onClick = { filePicker.launch(arrayOf("*/*")) }
                )
                SettingsRow(
                    icon = Icons.Default.Delete,
                    title = "清理错误导入数据",
                    subtitle = "删除今天通过导入/手动添加的误操作数据",
                    onClick = { showCleanupDialog = true }
                )
                if (isImporting) {
                    Text(
                        text = "正在导入...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 56.dp, vertical = 4.dp)
                    )
                }
                var isExportingExpenses by remember { mutableStateOf(false) }
                var exportExpensesMessage by remember { mutableStateOf("") }
                var exportExpensesError by remember { mutableStateOf(false) }
                var exportedCsvUri by remember { mutableStateOf<Uri?>(null) }
                var exportedCsvFileName by remember { mutableStateOf("") }

                SettingsRow(
                    icon = Icons.Default.ArrowDropDown,
                    title = "导出支出记录(CSV)",
                    subtitle = if (isExportingExpenses) "正在导出..." 
                               else if (exportExpensesMessage.isNotBlank()) exportExpensesMessage
                               else "导出为CSV，可用Excel打开",
                    onClick = {
                        if (isExportingExpenses) return@SettingsRow
                        isExportingExpenses = true
                        exportExpensesMessage = ""
                        exportExpensesError = false
                        exportedCsvUri = null
                        exportedCsvFileName = ""
                        scope.launch {
                            try {
                                val result = withContext(Dispatchers.IO) {
                                    csvExporter.exportExpenses(context)
                                }
                                when (result) {
                                    is ExportResult.Success -> {
                                        exportedCsvUri = result.uri
                                        exportedCsvFileName = result.fileName
                                        exportExpensesMessage = "已导出 ${result.count} 条记录到下载文件夹：${result.fileName}"
                                        exportExpensesError = false
                                    }
                                    is ExportResult.Failure -> {
                                        exportExpensesMessage = "导出失败：${result.error}"
                                        exportExpensesError = true
                                    }
                                }
                            } catch (e: Exception) {
                                exportExpensesMessage = "导出失败：${e.message}"
                                exportExpensesError = true
                            } finally {
                                isExportingExpenses = false
                            }
                        }
                    }
                )

                if (exportExpensesMessage.isNotBlank()) {
                    val msgColor = if (exportExpensesError) MaterialTheme.colorScheme.error 
                                   else Color(0xFF4CAF50)
                    Text(
                        text = exportExpensesMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = msgColor,
                        modifier = Modifier.padding(horizontal = 56.dp)
                    )
                    if (!exportExpensesError && exportedCsvUri != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 56.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(Intent.EXTRA_STREAM, exportedCsvUri)
                                        putExtra(Intent.EXTRA_SUBJECT, exportedCsvFileName)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "分享 $exportedCsvFileName"))
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "分享",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("分享", fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (BuildConfig.IS_PRO) {
                    var isExportingPositions by remember { mutableStateOf(false) }
                    var exportPositionsMessage by remember { mutableStateOf("") }
                    var exportPositionsError by remember { mutableStateOf(false) }
                    var exportedPositionsUri by remember { mutableStateOf<Uri?>(null) }
                    var exportedPositionsFileName by remember { mutableStateOf("") }

                    SettingsRow(
                        icon = Icons.Default.ArrowDropDown,
                        title = "导出理财持仓(CSV)",
                        subtitle = if (isExportingPositions) "正在导出..."
                                   else if (exportPositionsMessage.isNotBlank()) exportPositionsMessage
                                   else "导出为CSV，可用Excel打开",
                        onClick = {
                            if (isExportingPositions) return@SettingsRow
                            isExportingPositions = true
                            exportPositionsMessage = ""
                            exportPositionsError = false
                            exportedPositionsUri = null
                            exportedPositionsFileName = ""
                            scope.launch {
                                try {
                                    val result = withContext(Dispatchers.IO) {
                                        csvExporter.exportPositions(context)
                                    }
                                    when (result) {
                                        is ExportResult.Success -> {
                                            exportedPositionsUri = result.uri
                                            exportedPositionsFileName = result.fileName
                                            exportPositionsMessage = "已导出 ${result.count} 条记录到下载文件夹：${result.fileName}"
                                            exportPositionsError = false
                                        }
                                        is ExportResult.Failure -> {
                                            exportPositionsMessage = "导出失败：${result.error}"
                                            exportPositionsError = true
                                        }
                                    }
                                } catch (e: Exception) {
                                    exportPositionsMessage = "导出失败：${e.message}"
                                    exportPositionsError = true
                                } finally {
                                    isExportingPositions = false
                                }
                            }
                        }
                    )

                    if (exportPositionsMessage.isNotBlank()) {
                        val msgColor = if (exportPositionsError) MaterialTheme.colorScheme.error
                                       else Color(0xFF4CAF50)
                        Text(
                            text = exportPositionsMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = msgColor,
                            modifier = Modifier.padding(horizontal = 56.dp)
                        )
                        if (!exportPositionsError && exportedPositionsUri != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 56.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/csv"
                                            putExtra(Intent.EXTRA_STREAM, exportedPositionsUri)
                                            putExtra(Intent.EXTRA_SUBJECT, exportedPositionsFileName)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "分享 $exportedPositionsFileName"))
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "分享",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("分享", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                SettingsRow(
                    icon = Icons.Default.CloudUpload,
                    title = "自动备份管理",
                    subtitle = "查看备份文件、手动备份或恢复数据",
                    onClick = {
                        backupList = backupManager.getBackupList()
                        showBackupSheet = true
                    }
                )
                SettingsRow(
                    icon = Icons.Default.Delete,
                    title = "回收站",
                    subtitle = "查看和恢复已删除记录",
                    onClick = { showTrash = true }
                )
            }
            Spacer(Modifier.height(12.dp))

            SectionHeader(title = "外观")
            GlassCard(contentPadding = PaddingValues(0.dp)) {
                SettingsRow(
                    icon = Icons.Default.Refresh,
                    title = "深色模式",
                    subtitle = if (isDarkTheme) "当前：深色主题" else "当前：浅色主题",
                    onClick = {
                        scope.launch {
                            ThemePrefs.setDarkTheme(context, !isDarkTheme)
                        }
                    }
                )
            }
            Spacer(Modifier.height(12.dp))

            SectionHeader(title = "关于")
            GlassCard(contentPadding = PaddingValues(0.dp)) {
                InfoRow(label = "版本", value = "v1.0")
                InfoRow(label = "开发者", value = "自动记账助手")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "自动记账助手 · 让每一笔消费都有迹可循",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }

    if (BuildConfig.IS_PRO && showSyncSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSyncSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "数据同步配置",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "上次同步: $lastSyncFormatted",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "服务器设置",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = serverIp,
                        onValueChange = {
                            serverIp = it
                            syncPrefs.serverIp = it
                        },
                        label = { Text("IP地址") },
                        placeholder = { Text("例如: 192.168.1.100") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = serverPortText,
                        onValueChange = {
                            serverPortText = it
                            val port = it.toIntOrNull()
                            if (port != null && port in 1..65535) {
                                syncPrefs.serverPort = port
                            }
                        },
                        label = { Text("端口") },
                        placeholder = { Text("5000") },
                        modifier = Modifier.width(100.dp),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        isPinging = true
                        pingMessage = ""
                        pingSuccess = null
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                syncService.ping(serverIp, syncPrefs.serverPort)
                            }
                            withContext(Dispatchers.Main) {
                                isPinging = false
                                if (result == "ok") {
                                    pingMessage = "连接成功"
                                    pingSuccess = true
                                } else {
                                    pingMessage = "连接失败: $result"
                                    pingSuccess = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = serverIp.isNotBlank() && !isPinging
                ) {
                    if (isPinging) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("测试连接")
                }

                if (pingMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = pingMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (pingSuccess == true) Color(0xFF4CAF50)
                                else MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        isDiagnosing = true
                        diagnosticsSteps = emptyList()
                        scope.launch {
                            val steps = withContext(Dispatchers.IO) {
                                syncService.runDiagnostics(serverIp, syncPrefs.serverPort)
                            }
                            withContext(Dispatchers.Main) {
                                isDiagnosing = false
                                diagnosticsSteps = steps
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = serverIp.isNotBlank() && !isDiagnosing
                ) {
                    if (isDiagnosing) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("详细诊断")
                }

                if (diagnosticsSteps.isNotEmpty() && !isDiagnosing) {
                    Spacer(modifier = Modifier.height(8.dp))
                    diagnosticsSteps.forEach { step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (step.success) "\u2705" else "\u274C",
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = step.stepName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (step.success) Color(0xFF4CAF50)
                                            else MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = step.detail,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            isSyncing = true
                            syncMessage = ""
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    syncService.fullSync(
                                        ip = serverIp,
                                        port = syncPrefs.serverPort,
                                        expenses = viewModel.expenses.value.filter { !it.isDeleted },
                                        positions = if (BuildConfig.IS_PRO) viewModel.positions.value else emptyList(),
                                        onProgress = { msg ->
                                            scope.launch(Dispatchers.Main) {
                                                syncMessage = msg
                                            }
                                        }
                                    )
                                }
                                withContext(Dispatchers.Main) {
                                    isSyncing = false
                                    syncMessage = result.message
                                    syncSuccess = result.success
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = serverIp.isNotBlank() && !isSyncing && pingSuccess == true
                    ) {
                        Text("全量同步")
                    }
                    OutlinedButton(
                        onClick = {
                            isSyncing = true
                            syncMessage = ""
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    syncService.incrementalSync(
                                        ip = serverIp,
                                        port = syncPrefs.serverPort,
                                        expenses = viewModel.expenses.value.filter { !it.isDeleted },
                                        positions = if (BuildConfig.IS_PRO) viewModel.positions.value else emptyList(),
                                        onProgress = { msg ->
                                            scope.launch(Dispatchers.Main) {
                                                syncMessage = msg
                                            }
                                        }
                                    )
                                }
                                withContext(Dispatchers.Main) {
                                    isSyncing = false
                                    syncMessage = result.message
                                    syncSuccess = result.success
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = serverIp.isNotBlank() && !isSyncing && pingSuccess == true
                    ) {
                        Text("增量同步")
                    }
                }

                if (isSyncing) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                if (syncMessage.isNotBlank() && !isSyncing) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = syncMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (syncSuccess == true) Color(0xFF4CAF50)
                                else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showBackupSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showBackupSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "自动备份管理",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))

                val lastBackup = backupList.firstOrNull()
                Text(
                    text = if (lastBackup != null)
                        "上次备份: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(lastBackup.date)}"
                    else "暂无备份",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (backupList.isEmpty()) {
                    Text(
                        text = "暂无备份文件，点击下方按钮立即创建",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    Text(
                        text = "备份文件列表",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    backupList.forEach { backup ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = backup.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${formatFileSize(backup.size)} · ${backup.recordCount} 条记录",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = { restoreTargetFile = backup },
                                modifier = Modifier.padding(end = 4.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("恢复", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    backupManager.deleteBackup(backup.file)
                                    backupList = backupManager.getBackupList()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("删除", fontSize = 12.sp)
                            }
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            thickness = 0.5.dp
                        )
                    }
                }

                if (backupMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = backupMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                FilledTonalButton(
                    onClick = {
                        isBackingUp = true
                        backupMessage = ""
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                backupManager.performManualBackup()
                            }
                            withContext(Dispatchers.Main) {
                                isBackingUp = false
                                when (result) {
                                    is BackupResult.Success -> {
                                        backupMessage = "备份成功：${result.fileName}（${result.count}条记录）"
                                        backupList = backupManager.getBackupList()
                                    }
                                    is BackupResult.Failure -> {
                                        backupMessage = "备份失败：${result.error}"
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isBackingUp
                ) {
                    if (isBackingUp) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isBackingUp) "正在备份..." else "立即备份")
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "每周自动备份一次，保留最近两周的备份数据",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    restoreTargetFile?.let { target ->
        AlertDialog(
            onDismissRequest = { restoreTargetFile = null },
            title = { Text("从备份恢复") },
            text = {
                Text("将从备份文件 ${target.name} 恢复数据。\n\n仅恢复备份日期之前缺失的数据，不会覆盖已有记录或影响备份后的新数据。\n\n确认继续？")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val file = target.file
                        restoreTargetFile = null
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                backupManager.restoreFromBackup(file)
                            }
                            withContext(Dispatchers.Main) {
                                restoreResult = result
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("确认恢复")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { restoreTargetFile = null }) {
                    Text("取消")
                }
            }
        )
    }

    restoreResult?.let { result ->
        AlertDialog(
            onDismissRequest = { restoreResult = null },
            title = { Text(if (result is RestoreResult.Success) "恢复成功" else "恢复失败") },
            text = {
                Text(
                    if (result is RestoreResult.Success)
                        "成功恢复 ${result.restoredCount} 条记录"
                    else
                        (result as? RestoreResult.Failure)?.error ?: "恢复过程出错"
                )
            },
            confirmButton = {
                Button(onClick = { restoreResult = null }) {
                    Text("确定")
                }
            }
        )
    }

    if (showCleanupDialog) {
        AlertDialog(
            onDismissRequest = { showCleanupDialog = false },
            title = { Text("清理错误导入数据") },
            text = {
                Text("将删除今天通过CSV/TXT导入或手动添加的记录（通知自动记账的数据不受影响）。\n\n此操作不可撤销，确认继续？")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCleanupDialog = false
                        scope.launch {
                            val calendar = Calendar.getInstance()
                            calendar.set(Calendar.HOUR_OF_DAY, 0)
                            calendar.set(Calendar.MINUTE, 0)
                            calendar.set(Calendar.SECOND, 0)
                            calendar.set(Calendar.MILLISECOND, 0)
                            val startOfToday = calendar.timeInMillis
                            val endOfToday = startOfToday + 24 * 60 * 60 * 1000 - 1
                            val deleted = withContext(Dispatchers.IO) {
                                expenseRepository.deleteImportedDataOnDay(startOfToday, endOfToday)
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "已清理 $deleted 条错误导入记录，通知自动记账数据未受影响",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCleanupDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    importResult?.let { result ->
        AlertDialog(
            onDismissRequest = { importResult = null },
            title = { Text("导入完成") },
            text = {
                Column {
                    Text(
                        text = "成功导入: ${result.successCount} 条",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "失败/跳过: ${result.errorCount} 条",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    if (result.errors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        result.errors.take(3).forEach { error ->
                            Text(
                                text = "· $error",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { importResult = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("确定")
                }
            }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncPrefsEntryPoint {
    fun syncPrefs(): SyncPrefs
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncServiceEntryPoint {
    fun syncService(): SyncService
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CsvExporterEntryPoint {
    fun csvExporter(): CsvExporter
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ImportManagerEntryPoint {
    fun importManager(): ImportManager
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ExpenseRepoEntryPoint {
    fun expenseRepository(): ExpenseRepository
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BackupManagerEntryPoint {
    fun backupManager(): BackupManager
}

@Composable
fun InfoRow(label: String, value: String) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}

package com.example.autobookkeeper.ui.screen

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.autobookkeeper.App
import com.example.autobookkeeper.notification.Rom

/**
 * MIUI / HyperOS / ColorOS / EMUI 等国产 ROM 引导向导
 *
 * 3 步, 缺一不可:
 * 1. 允许自启动
 * 2. 关闭电池优化
 * 3. 锁定在最近任务 (MIUI 专属)
 *
 * 步骤带深度链接, 跳到 ROM 特定设置页
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RomSetupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val rom = App.currentRom
    val scrollState = rememberScrollState()

    val autostartOk = remember { mutableStateOf(false) }
    val batteryOk = remember {
        mutableStateOf(
            (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
                .isIgnoringBatteryOptimizations(context.packageName)
        )
    }
    val recentsLockOk = remember { mutableStateOf(false) }  // 用户需自查, 没法程序验证

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ROM 设置引导") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("返回") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 顶部: ROM 检测结果
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("检测到的 ROM", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("${rom.type} (Android ${rom.version})")
                    if (rom.needsRomSetup) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "此 ROM 有后台限制, 必须按以下 3 步设置, 否则 NLS 会被系统杀掉。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // 步骤 1: 自启动
            SetupStep(
                title = "1. 允许自启动",
                description = autostartDescription(rom),
                isDone = autostartOk.value,
                onClick = {
                    openAutostartSettings(context, rom)
                    autostartOk.value = true  // 乐观假设用户已设置, 回到主页时 NLS 重建会验证
                }
            )

            // 步骤 2: 电池优化
            SetupStep(
                title = "2. 关闭电池优化",
                description = "允许 App 在后台运行而不被电池优化杀掉。点击下方按钮跳到设置。",
                isDone = batteryOk.value,
                onClick = {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = Uri.parse("package:${context.packageName}")
                    runCatching { context.startActivity(intent) }
                        .onFailure {
                            // 部分 ROM 不支持, 跳到应用详情
                            runCatching {
                                val detail = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                detail.data = Uri.parse("package:${context.packageName}")
                                context.startActivity(detail)
                            }
                        }
                }
            )

            // 步骤 3: 锁定最近任务 (仅 MIUI/HyperOS)
            if (rom.needsRecentsLock) {
                SetupStep(
                    title = "3. 锁定最近任务",
                    description = "从屏幕底部上滑打开最近任务, 找到本 App 卡片, 下拉卡片显示锁定图标, 点击锁定。这样 NLS 不会被滑动清掉。",
                    isDone = recentsLockOk.value,
                    onClick = {
                        recentsLockOk.value = !recentsLockOk.value
                    },
                    showCheckHint = true
                )
            }

            // 底部: 完成提示
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("完成后", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("回到主页, NLS 应在 5-10 秒内自动恢复。如果没恢复, 重启手机一次。")
                }
            }

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("返回主页")
            }
        }
    }
}

@Composable
private fun SetupStep(
    title: String,
    description: String,
    isDone: Boolean,
    onClick: () -> Unit,
    showCheckHint: Boolean = false
) {
    Card {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isDone) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = if (isDone) "已完成" else "未完成",
                tint = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodySmall)
                if (showCheckHint && isDone) {
                    Spacer(Modifier.height(4.dp))
                    Text("(已确认, 你可以继续)", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onClick) {
                Text(if (isDone) "重设" else "去设置")
            }
        }
    }
}

private fun openAutostartSettings(context: Context, rom: Rom) {
    val intent = when (rom.type) {
        Rom.Type.MIUI, Rom.Type.HyperOS -> Intent().apply {
            component = ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        }
        Rom.Type.ColorOS -> Intent().apply {
            component = ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity"
            )
        }
        Rom.Type.EMUI -> Intent().apply {
            component = ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            )
        }
        Rom.Type.OriginOS -> Intent().apply {
            component = ComponentName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
            )
        }
        else -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
        .onFailure {
            // Fallback: 应用详情页
            val detail = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(detail) }
        }
}

private fun autostartDescription(rom: Rom): String = when (rom.type) {
    Rom.Type.MIUI, Rom.Type.HyperOS ->
        "在 MIUI「自启动」管理中找到本 App, 打开开关。否则 MIUI 会杀进程。"
    Rom.Type.ColorOS ->
        "在 ColorOS「自启动管理」中找到本 App, 打开开关。"
    Rom.Type.EMUI ->
        "在 EMUI「应用启动管理」中找到本 App, 设为「自动管理」。"
    Rom.Type.OriginOS ->
        "在 OriginOS「后台高耗电」中找到本 App, 设为允许。"
    else -> "允许 App 自启动, 否则系统可能杀掉后台进程。"
}

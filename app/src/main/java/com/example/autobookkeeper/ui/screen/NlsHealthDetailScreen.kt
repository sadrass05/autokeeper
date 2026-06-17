package com.example.autobookkeeper.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.autobookkeeper.App
import com.example.autobookkeeper.notification.NlsHealthState
import com.example.autobookkeeper.notification.NlsRestartWorker
import com.example.autobookkeeper.notification.KickOnceResult

/**
 * 设置板块跳转的 NLS 诊断详情页
 *
 * 内容: NlsHealthCard 的全部信息(状态/ROM/通知时间/kick 时间/重启次数)
 *       + 立即恢复按钮 + 跳转 RomSetup 按钮
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NlsHealthDetailScreen(
    onBack: () -> Unit,
    onNavigateToRomSetup: () -> Unit
) {
    val context = LocalContext.current

    var notificationAccessGranted by remember {
        mutableStateOf(
            android.provider.Settings.Secure.getString(
                context.contentResolver, "enabled_notification_listeners"
            )?.contains(context.packageName) == true
        )
    }
    val nlsRunning = remember { mutableStateOf(App.notificationListenerRunning) }
    val rom = App.currentRom
    val lastNotificationTime = remember {
        mutableStateOf(
            context.getSharedPreferences("nls_health", android.content.Context.MODE_PRIVATE)
                .getLong("last_notification_time", 0L)
        )
    }
    val lastKickTime = remember {
        mutableStateOf(
            context.getSharedPreferences("nls_kick", android.content.Context.MODE_PRIVATE)
                .getLong("last_kick_time", 0L)
        )
    }
    val restartAttempts = remember { mutableStateOf(App.nlsRestartAttempts) }

    LaunchedEffect(Unit) {
        while (true) {
            nlsRunning.value = App.notificationListenerRunning
            restartAttempts.value = App.nlsRestartAttempts
            lastNotificationTime.value = context.getSharedPreferences("nls_health", android.content.Context.MODE_PRIVATE)
                .getLong("last_notification_time", 0L)
            lastKickTime.value = context.getSharedPreferences("nls_kick", android.content.Context.MODE_PRIVATE)
                .getLong("last_kick_time", 0L)
            notificationAccessGranted = android.provider.Settings.Secure.getString(
                context.contentResolver, "enabled_notification_listeners"
            )?.contains(context.packageName) == true
            kotlinx.coroutines.delay(2000L)
        }
    }

    val state = NlsHealthState.compute(
        notificationAccessGranted = notificationAccessGranted,
        nlsRunning = nlsRunning.value,
        watchdogMissCount = 0,
        restartGiveUp = App.nlsRestartGiveUp
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("通知监听诊断") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("返回") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 状态卡
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.isHealthy)
                        MaterialTheme.colorScheme.surfaceVariant
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("状态: ${state.status.name}", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("ROM: ${rom.type} (Android ${rom.version})")
                    if (rom.needsRomSetup) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "⚠️ 此 ROM 有后台限制, 必须手动开启自启动 / 电池优化 / 最近任务锁",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // 时序数据卡
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("时序数据", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("上一条通知: ${formatRelative(lastNotificationTime.value)}")
                    Text("上次重启尝试: ${formatRelative(lastKickTime.value)}")
                    Text("重启尝试次数: ${restartAttempts.value}")
                }
            }

            // 操作区
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!state.isHealthy) {
                    Button(
                        onClick = {
                            try {
                                com.example.autobookkeeper.App.clearNlsRestartAttempts()
                                NlsRestartWorker.schedule(context)
                                val result = NlsRestartWorker.kickOnce(context)
                                val msg = when (result) {
                                    is KickOnceResult.SkippedAlreadyAlive -> "NLS 已经在运行"
                                    is KickOnceResult.Success -> "已恢复尝试 (耗时 ${result.durationMs}ms)"
                                    is KickOnceResult.Failed -> "恢复失败: ${result.reason}"
                                }
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                            } catch (e: Throwable) {
                                android.widget.Toast.makeText(context, "恢复失败: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    ) { Text("立即恢复") }
                }
                if (rom.needsRomSetup) {
                    OutlinedButton(onClick = onNavigateToRomSetup) {
                        Text("ROM 设置引导")
                    }
                }
            }
        }
    }
}

private fun formatRelative(timestampMs: Long): String {
    if (timestampMs == 0L) return "从未"
    val diff = System.currentTimeMillis() - timestampMs
    return when {
        diff < 60_000 -> "${diff / 1000} 秒前"
        diff < 3_600_000 -> "${diff / 60_000} 分钟前"
        diff < 86_400_000 -> "${diff / 3_600_000} 小时前"
        else -> "${diff / 86_400_000} 天前"
    }
}

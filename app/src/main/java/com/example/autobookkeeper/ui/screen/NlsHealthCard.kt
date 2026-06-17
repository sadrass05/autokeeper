package com.example.autobookkeeper.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.autobookkeeper.App
import com.example.autobookkeeper.notification.NlsHealthState

/**
 * HomeScreen 上的 NLS 状态卡片
 *
 * 显示:
 * - NLS alive / died
 * - ROM 类型 + 是否需要引导
 * - 上一条通知时间
 * - 上次重启尝试
 */
@Composable
fun NlsHealthCard(
    onClickSetup: () -> Unit,
    onClickKick: () -> Unit
) {
    val context = LocalContext.current

    var notificationAccessGranted by remember {
        mutableStateOf(
            android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
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

    // 每 2 秒刷新一次
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
            Text(
                "NLS 状态: ${state.status.name}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text("ROM: ${rom.type} (Android ${rom.version})")
            if (rom.needsRomSetup) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "⚠️ 此 ROM 需要手动开启自启动 / 电池优化 / 最近任务锁",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("上一条通知: ${formatRelative(lastNotificationTime.value)}")
            Text("上次重启尝试: ${formatRelative(lastKickTime.value)}")
            Text("重启尝试次数: ${restartAttempts.value}")
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!state.isHealthy) {
                    Button(onClick = onClickKick) {
                        Text("立即恢复")
                    }
                }
                if (rom.needsRomSetup) {
                    OutlinedButton(onClick = onClickSetup) {
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

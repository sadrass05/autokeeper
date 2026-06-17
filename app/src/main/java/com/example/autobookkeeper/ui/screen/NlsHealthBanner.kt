package com.example.autobookkeeper.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.autobookkeeper.App
import com.example.autobookkeeper.notification.NlsHealthState

/**
 * 首页顶部 NLS 状态 Banner
 *
 * 行为:
 * - 健康时不渲染 (0 高度, 不占空间)
 * - 不健康时从顶部滑入 (展开/收起动画)
 * - 含状态文字 + 立即恢复按钮 + 查看详情按钮 + 关闭按钮
 *
 * 关闭按钮: 用户主动关掉本次提示,问题解决后下次再异常会再出现
 */
@Composable
fun NlsHealthBanner(
    onClickKick: () -> Unit,
    onClickDetail: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val rom = App.currentRom

    var notificationAccessGranted by remember {
        mutableStateOf(
            android.provider.Settings.Secure.getString(
                context.contentResolver, "enabled_notification_listeners"
            )?.contains(context.packageName) == true
        )
    }
    val nlsRunning = remember { mutableStateOf(App.notificationListenerRunning) }

    // 每 2 秒轮询
    LaunchedEffect(Unit) {
        while (true) {
            nlsRunning.value = App.notificationListenerRunning
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

    AnimatedVisibility(
        visible = !state.isHealthy,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bannerTitle(state.status),
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (rom.needsRomSetup && state.status != NlsHealthState.Status.Revoked) {
                        Text(
                            "此 ROM 需要手动开启自启动/电池优化/最近任务锁",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = onClickKick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) { Text("恢复") }
                TextButton(
                    onClick = onClickDetail,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) { Text("详情") }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "关闭", tint = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    }
}

private fun bannerTitle(status: NlsHealthState.Status): String = when (status) {
    NlsHealthState.Status.Revoked -> "通知权限被撤销, 自动记账已停止"
    NlsHealthState.Status.GaveUp -> "通知监听无法自动恢复"
    NlsHealthState.Status.Died -> "通知监听已停止"
    NlsHealthState.Status.Healthy -> ""  // 不会显示
}

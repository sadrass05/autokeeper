package com.example.autobookkeeper.ui.screen

import android.content.ComponentName
import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.autobookkeeper.BuildConfig
import com.example.autobookkeeper.ui.components.ChartType
import com.example.autobookkeeper.ui.components.CategoryDonutChart
import com.example.autobookkeeper.ui.components.DefaultCardPadding
import com.example.autobookkeeper.ui.components.GlassCard
import com.example.autobookkeeper.ui.components.GlassCardSection
import com.example.autobookkeeper.ui.components.TrendChart
import com.example.autobookkeeper.ui.components.MonthlyBarChart
import com.example.autobookkeeper.ui.components.MonthlyStackedBarChart
import com.example.autobookkeeper.ui.components.TopExpensesCard
import com.example.autobookkeeper.ui.viewmodel.MainViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.autobookkeeper.data.entity.ExpenseRecord
import kotlin.math.abs
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.autobookkeeper.App
import com.example.autobookkeeper.notification.NlsHealthState
import com.example.autobookkeeper.notification.NotificationListener

/** NLS 健康状态: 用于 ON_RESUME 时弹窗引导 */
private enum class NlsHealth { Revoked, Died, GaveUp }

@Composable
fun HomeScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onNavigateToRecords: () -> Unit = {},
    onNavigateToRomSetup: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val dailyExpense by viewModel.dailyExpense.collectAsStateWithLifecycle()
    val monthlyExpense by viewModel.monthlyExpense.collectAsStateWithLifecycle()
    val totalProfitState: State<Double> = if (BuildConfig.IS_PRO) {
        viewModel.totalProfit.collectAsStateWithLifecycle()
    } else {
        remember { mutableStateOf(0.0) }
    }
    val totalProfit by totalProfitState
    val trendData by viewModel.trendData.collectAsStateWithLifecycle()
    val todayCategoryData by viewModel.todayCategoryData.collectAsStateWithLifecycle()
    val monthlyStats by viewModel.monthlyStats.collectAsStateWithLifecycle()
    val topExpenses by viewModel.topExpenses.collectAsStateWithLifecycle()
    var chartType by remember { mutableStateOf(ChartType.LINE) }
    val context = LocalContext.current
    var notificationAccessGranted by remember {
        mutableStateOf(
            runCatching {
                Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                    ?.contains(context.packageName) == true
            }.getOrDefault(false)
        )
    }
    var batteryOptimized by remember {
        mutableStateOf(
            (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
                .isIgnoringBatteryOptimizations(context.packageName).not()
        )
    }
    var nlsOffline by remember {
        mutableStateOf(
            notificationAccessGranted &&
                !App.notificationListenerRunning &&
                App.nlsWatchdogMissCount >= 2
        )
    }
    // NLS 健康度弹窗: ON_RESUME 时如果发现异常, 就弹. null = 不弹
    var nlsHealthDialog by remember { mutableStateOf<NlsHealth?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationAccessGranted = runCatching {
                    Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                        ?.contains(context.packageName) == true
                }.getOrDefault(false)
                batteryOptimized = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
                    .isIgnoringBatteryOptimizations(context.packageName).not()
                nlsOffline = notificationAccessGranted &&
                    !App.notificationListenerRunning &&
                    App.nlsWatchdogMissCount >= 2

                // 弹窗: 每次 ON_RESUME 检查, 只在出现异常时弹一次
                // 使用 NlsHealthState 纯函数, 避免在 DisposableEffect 中直接读 App 静态字段造成 race
                val healthState = NlsHealthState.compute(
                    notificationAccessGranted = notificationAccessGranted,
                    nlsRunning = App.notificationListenerRunning,
                    watchdogMissCount = App.nlsWatchdogMissCount,
                    restartGiveUp = App.nlsRestartGiveUp
                )
                nlsHealthDialog = when (healthState.status) {
                    NlsHealthState.Status.Revoked -> NlsHealth.Revoked
                    NlsHealthState.Status.GaveUp -> NlsHealth.GaveUp
                    NlsHealthState.Status.Died -> NlsHealth.Died
                    NlsHealthState.Status.Healthy -> null
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Text(
                text = "自动记账助手",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 顶部 NLS 状态 Banner (仅在异常时显示, 健康时不占空间)
            NlsHealthBanner(
                onClickKick = { nlsHealthDialog = NlsHealth.Died },
                onClickDetail = onNavigateToSettings,
                onDismiss = { /* 关闭本次提示, 问题解决后下次异常会再出现 */ }
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp, end = 20.dp,
                    top = 8.dp,
                    bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                )
            ) {
                if (!notificationAccessGranted) {
                item {
                    GlassCard(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    context.startActivity(
                                        Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                                    )
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "警告",
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(24.dp)
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp)
                            ) {
                                Text(
                                    text = "通知监听未启用",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100),
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "点击前往设置开启，否则无法自动记账",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // NLS 频繁失联警告（看门狗累计 miss >= 2）
            if (nlsOffline) {
                item {
                    GlassCard(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "失联",
                                    tint = Color(0xFFE53935),
                                    modifier = Modifier.size(24.dp)
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 12.dp)
                                ) {
                                    Text(
                                        text = "通知监听已停止工作",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB71C1C),
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "系统已多次断开 NLS，点下方按钮尝试重启",
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        val component = ComponentName(
                                            context,
                                            NotificationListener::class.java
                                        )
                                        try {
                                            val cls = Class.forName("android.service.notification.NotificationListenerService")
                                            val method = cls.getMethod("requestRebind", ComponentName::class.java)
                                            method.invoke(null, component)
                                            android.widget.Toast.makeText(
                                                context,
                                                "已请求重连，请等待 3-5 秒",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        } catch (e: Throwable) {
                                            android.util.Log.e("HomeScreen", "重启 NLS 失败", e)
                                        }
                                    },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Color(0xFFFFEBEE)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("重启通知监听", color = Color(0xFFB71C1C))
                                }
                                FilledTonalButton(
                                    onClick = {
                                        val intent = Intent(
                                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                        ).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Color(0xFFE3F2FD)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("电池优化", color = Color(0xFF0D47A1))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // 电池优化引导
            if (batteryOptimized) {
                item {
                    GlassCard(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "电池",
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(24.dp)
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp)
                            ) {
                                Text(
                                    text = "电池优化已启用",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100),
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "MIUI 会在后台关闭通知监听，点击关闭电池限制",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                            if (notificationAccessGranted && App.notificationListenerRunning) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50))
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "今日支出",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "¥${"%.2f".format(dailyExpense)}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(0.6f),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "本月支出  ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "¥${"%.2f".format(monthlyExpense)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            if (BuildConfig.IS_PRO) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF8E1)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp, horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💰",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "理财收益",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF8D6E63)
                                )
                                Text(
                                    text = "¥${"%.2f".format(totalProfit)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF8F00)
                                )
                            }
                        }
                    }
                }
            }

            item {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            item(key = "expense_chart") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "支出趋势 (最近7天)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalButton(
                            onClick = { chartType = ChartType.LINE },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (chartType == ChartType.LINE)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else Color.Transparent,
                                contentColor = if (chartType == ChartType.LINE)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("折线", style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        FilledTonalButton(
                            onClick = { chartType = ChartType.BAR },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (chartType == ChartType.BAR)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else Color.Transparent,
                                contentColor = if (chartType == ChartType.BAR)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("柱状", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    GlassCard(
                        contentPadding = DefaultCardPadding
                    ) {
                        TrendChart(
                            data = trendData,
                            chartType = chartType,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                if (monthlyStats.isNotEmpty()) {
                    Column(modifier = Modifier.padding(vertical = 16.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "月度支出对比",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    if (BuildConfig.IS_PRO) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            LegendDot(
                                                color = MaterialTheme.colorScheme.primary,
                                                label = "日常"
                                            )
                                            LegendDot(
                                                color = MaterialTheme.colorScheme.tertiary,
                                                label = "理财"
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                if (BuildConfig.IS_PRO) {
                                    MonthlyStackedBarChart(
                                        data = monthlyStats,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(220.dp)
                                    )
                                } else {
                                    MonthlyBarChart(
                                        data = monthlyStats,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                TopExpensesCard(
                    expenses = topExpenses,
                    onViewAll = onNavigateToRecords,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            item {
                GlassCardSection(
                    title = "当日支出分类",
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    CategoryDonutChart(
                        data = todayCategoryData,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            item {
                GlassCardSection(
                    title = "最近交易",
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    if (expenses.isEmpty()) {
                        Text(
                            text = "暂无交易记录",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            expenses.take(10).forEach { expense ->
                                TransactionItem(expense = expense)
                            }
                        }
                    }
                }
            }
            }
        }
    }

    // NLS 健康度引导弹窗
    nlsHealthDialog?.let { health ->
        val (title, message, positiveText, onPositive) = when (health) {
            NlsHealth.Revoked -> Quadruple(
                "通知权限已关闭",
                "自动记账需要通知权限才能识别支付消息。是否前往设置开启？",
                "去设置"
            ) {
                runCatching {
                    context.startActivity(
                        Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                    )
                }.onFailure { android.util.Log.e("HomeScreen", "跳转通知设置失败", it) }
                nlsHealthDialog = null
            }
            NlsHealth.Died -> Quadruple(
                "通知监听已停止",
                "App 正在后台每 15 分钟自动尝试恢复。请先确认 ROM 设置 (自启动/电池优化/最近任务锁), 然后点击下方按钮。",
                "立即尝试恢复"
            ) {
                try {
                    com.example.autobookkeeper.App.clearNlsRestartAttempts()
                    com.example.autobookkeeper.notification.NlsRestartWorker.schedule(context)
                    val result = com.example.autobookkeeper.notification.NlsRestartWorker.kickOnce(context)
                    val msg = when (result) {
                        is com.example.autobookkeeper.notification.KickOnceResult.SkippedAlreadyAlive ->
                            "NLS 已经在运行, 无需恢复"
                        is com.example.autobookkeeper.notification.KickOnceResult.Success ->
                            "已恢复尝试, 2-4 秒后请查看 (耗时 ${result.durationMs}ms)"
                        is com.example.autobookkeeper.notification.KickOnceResult.Failed ->
                            "恢复失败: ${result.reason}, 请检查 ROM 设置 (自启动/电池优化/最近任务锁)"
                    }
                    android.widget.Toast.makeText(
                        context,
                        msg,
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                } catch (e: Throwable) {
                    android.util.Log.e("HomeScreen", "立即恢复 NLS 失败", e)
                    android.widget.Toast.makeText(
                        context,
                        "恢复失败: ${e.message ?: "未知错误"}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
                nlsHealthDialog = null
            }
            NlsHealth.GaveUp -> Quadruple(
                "通知监听无法自动恢复",
                "App 已尝试 6 次仍无法拉起通知监听。常见原因: MIUI 自启动未开 / 电池优化未关 / 最近任务被划掉。点击下方前往 ROM 设置引导。",
                "ROM 设置引导"
            ) {
                onNavigateToRomSetup()
                nlsHealthDialog = null
            }
        }
        AlertDialog(
            onDismissRequest = { nlsHealthDialog = null },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = onPositive) { Text(positiveText) }
            },
            dismissButton = {
                TextButton(onClick = { nlsHealthDialog = null }) { Text("稍后") }
            }
        )
    }
}

/** 用于解构 AlertDialog 内容的本地数据结构 */
private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
    where A : Any, B : Any, C : Any, D : Any

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TransactionItem(
    expense: ExpenseRecord,
    onClick: () -> Unit = {}
) {
    val financeGold = Color(0xFFFFB300)
    val isFinance = BuildConfig.IS_PRO && expense.isFinanceExpense
    val amountColor = if (expense.amount >= 0) {
        if (isFinance) financeGold
        else MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.tertiary
    }
    val amountPrefix = if (expense.amount >= 0) "-¥" else "+¥"
    val displayAmount = abs(expense.amount)

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.06f)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isFinance) Color(0xFFFFF8E1) else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (isFinance) {
                    Text(
                        text = "💰",
                        style = MaterialTheme.typography.titleMedium
                    )
                } else {
                    Text(
                        text = (expense.category.ifEmpty { "未分类" }).take(1),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.merchant.ifEmpty { expense.category.ifEmpty { "未分类" } },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${expense.platform} · ${expense.paymentChannel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isFinance) {
                        Text(
                            " 理财 ",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB300),
                            modifier = Modifier
                                .background(Color(0xFFFFF8E1), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = formatDate(expense.recordedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${amountPrefix}${"%.2f".format(displayAmount)}",
                style = MaterialTheme.typography.titleMedium,
                color = amountColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

package com.example.autobookkeeper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import com.example.autobookkeeper.ui.components.GlassNavigationBar
import com.example.autobookkeeper.ui.components.NavItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.autobookkeeper.BuildConfig
import com.example.autobookkeeper.ui.screen.HomeScreen
import com.example.autobookkeeper.ui.screen.RecordsScreen
import com.example.autobookkeeper.ui.screen.FinanceScreen
import com.example.autobookkeeper.ui.screen.NlsHealthDetailScreen
import com.example.autobookkeeper.ui.screen.RomSetupScreen
import com.example.autobookkeeper.ui.screen.SettingsScreen
import com.example.autobookkeeper.ui.theme.AutoBookkeeperTheme
import com.example.autobookkeeper.ui.theme.ThemePrefs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 启动时 NLS 健康检查: 如果用户已授权但 NLS 标志位是 false, 主动触发一次 rebind
        val app = applicationContext as? com.example.autobookkeeper.App
        if (app != null && app.isNotificationListenerEnabled() && !App.notificationListenerRunning) {
            android.util.Log.w("MainActivity", "⚠️ 启动时 NLS 失联, 尝试 rebind")
            val component = android.content.ComponentName(this, com.example.autobookkeeper.notification.NotificationListener::class.java)
            com.example.autobookkeeper.notification.NlsWatchdogWorker.schedule(applicationContext)
            // requestRebind 是 NotificationListenerService 的静态方法
            try {
                val cls = Class.forName("android.service.notification.NotificationListenerService")
                val method = cls.getMethod("requestRebind", android.content.ComponentName::class.java)
                method.invoke(null, component)
            } catch (e: Throwable) {
                android.util.Log.e("MainActivity", "启动时 rebind 失败", e)
            }
        }

        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                isDarkTheme = ThemePrefs.isDarkTheme(this@MainActivity).first()
                ThemePrefs.isDarkTheme(this@MainActivity).collect { dark ->
                    isDarkTheme = dark
                }
            }

            AutoBookkeeperTheme(darkTheme = isDarkTheme) {
                var selectedScreen by remember { mutableStateOf(0) }

                val navItems = if (BuildConfig.IS_PRO) {
                    listOf(
                        NavItem("首页", R.drawable.ic_home, R.drawable.ic_home_filled),
                        NavItem("记录", R.drawable.ic_records, R.drawable.ic_records_filled),
                        NavItem("理财", R.drawable.ic_finance, R.drawable.ic_finance_filled),
                        NavItem("设置", R.drawable.ic_settings, R.drawable.ic_settings_filled)
                    )
                } else {
                    listOf(
                        NavItem("首页", R.drawable.ic_home, R.drawable.ic_home_filled),
                        NavItem("记录", R.drawable.ic_records, R.drawable.ic_records_filled),
                        NavItem("设置", R.drawable.ic_settings, R.drawable.ic_settings_filled)
                    )
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {}
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = innerPadding.calculateTopPadding())
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Crossfade(targetState = selectedScreen) { screen ->
                                if (BuildConfig.IS_PRO) {
                                    when (screen) {
                                        0 -> HomeScreen(
                                            onNavigateToRecords = { selectedScreen = 1 },
                                            onNavigateToRomSetup = { selectedScreen = 4 },
                                            onNavigateToSettings = { selectedScreen = 3 }
                                        )
                                        1 -> RecordsScreen()
                                        2 -> FinanceScreen()
                                        3 -> SettingsScreen(
                                            onNavigateToNlsDetail = { selectedScreen = 5 }
                                        )
                                        4 -> RomSetupScreen(onBack = { selectedScreen = 0 })
                                        5 -> NlsHealthDetailScreen(
                                            onBack = { selectedScreen = 3 },
                                            onNavigateToRomSetup = { selectedScreen = 4 }
                                        )
                                    }
                                } else {
                                    when (screen) {
                                        0 -> HomeScreen(
                                            onNavigateToRecords = { selectedScreen = 1 },
                                            onNavigateToRomSetup = { selectedScreen = 3 },
                                            onNavigateToSettings = { selectedScreen = 2 }
                                        )
                                        1 -> RecordsScreen()
                                        2 -> SettingsScreen(
                                            onNavigateToNlsDetail = { selectedScreen = 4 }
                                        )
                                        3 -> RomSetupScreen(onBack = { selectedScreen = 0 })
                                        4 -> NlsHealthDetailScreen(
                                            onBack = { selectedScreen = 2 },
                                            onNavigateToRomSetup = { selectedScreen = 3 }
                                        )
                                    }
                                }
                            }
                        }

                        GlassNavigationBar(
                            items = navItems,
                            selectedIndex = selectedScreen,
                            onItemSelected = { selectedScreen = it },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }
}

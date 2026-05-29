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
import com.example.autobookkeeper.ui.screen.SettingsScreen
import com.example.autobookkeeper.ui.theme.AutoBookkeeperTheme
import com.example.autobookkeeper.ui.theme.ThemePrefs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialDarkTheme = runBlocking {
            ThemePrefs.isDarkTheme(this@MainActivity).first()
        }

        setContent {
            var isDarkTheme by remember { mutableStateOf(initialDarkTheme) }

            LaunchedEffect(Unit) {
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
                                        0 -> HomeScreen(onNavigateToRecords = { selectedScreen = 1 })
                                        1 -> RecordsScreen()
                                        2 -> FinanceScreen()
                                        3 -> SettingsScreen()
                                    }
                                } else {
                                    when (screen) {
                                        0 -> HomeScreen(onNavigateToRecords = { selectedScreen = 1 })
                                        1 -> RecordsScreen()
                                        2 -> SettingsScreen()
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

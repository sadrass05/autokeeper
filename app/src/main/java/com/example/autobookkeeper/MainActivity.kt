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
import androidx.compose.ui.Alignment
import com.example.autobookkeeper.ui.components.GlassNavigationBar
import com.example.autobookkeeper.ui.components.NavItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.example.autobookkeeper.ui.screen.FinanceScreen
import com.example.autobookkeeper.ui.screen.HomeScreen
import com.example.autobookkeeper.ui.screen.RecordsScreen
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

            androidx.compose.runtime.LaunchedEffect(Unit) {
                ThemePrefs.isDarkTheme(this@MainActivity).collect { dark ->
                    isDarkTheme = dark
                }
            }

            AutoBookkeeperTheme(darkTheme = isDarkTheme) {
                var selectedScreen by remember { mutableStateOf(0) }

                val screens : List<Pair<String,@Composable () -> Unit>> = listOf(
                    "首页" to {
                        HomeScreen(
                            onNavigateToRecords = { selectedScreen = 1 }
                        )
                    },
                    "记录" to { RecordsScreen() },
                    "理财" to { FinanceScreen() },
                    "设置" to { SettingsScreen() }
                )

                val navItems = listOf(
                    NavItem("首页", R.drawable.ic_home, R.drawable.ic_home_filled),
                    NavItem("记录", R.drawable.ic_records, R.drawable.ic_records_filled),
                    NavItem("理财", R.drawable.ic_finance, R.drawable.ic_finance_filled),
                    NavItem("设置", R.drawable.ic_settings, R.drawable.ic_settings_filled)
                )

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
                                screens[screen].second()
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

    @Composable
    private fun getIcon(index: Int, selected: Boolean): ImageVector {
        return when (index) {
            0 -> if (selected) ImageVector.vectorResource(R.drawable.ic_home_filled)
                 else ImageVector.vectorResource(R.drawable.ic_home)
            1 -> if (selected) ImageVector.vectorResource(R.drawable.ic_records_filled)
                 else ImageVector.vectorResource(R.drawable.ic_records)
            2 -> if (selected) ImageVector.vectorResource(R.drawable.ic_finance_filled)
                 else ImageVector.vectorResource(R.drawable.ic_finance)
            3 -> if (selected) ImageVector.vectorResource(R.drawable.ic_settings_filled)
                 else ImageVector.vectorResource(R.drawable.ic_settings)
            else -> ImageVector.vectorResource(R.drawable.ic_home)
        }
    }
}
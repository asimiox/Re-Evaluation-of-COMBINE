package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.BaselineScreen
import com.example.ui.BaselineViewModel
import com.example.ui.CombineSplashScreen
import com.example.ui.cooperative.CooperativeScreen
import com.example.ui.cooperative.CooperativeViewModel
import com.example.ui.theme.AccentGoldLight
import com.example.ui.theme.BentoCardBorder
import com.example.ui.theme.BentoCardSurface
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NavyDominant
import com.example.ui.theme.TextMuted
import com.example.ui.wifidirect.WifiDirectScreen
import com.example.ui.wifidirect.WifiDirectViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var showSplash by rememberSaveable { mutableStateOf(true) }

                    Crossfade(
                        targetState = showSplash,
                        animationSpec = tween(durationMillis = 400),
                        label = "SplashCrossfade"
                    ) { isSplash ->
                        if (isSplash) {
                            CombineSplashScreen(onDismiss = { showSplash = false })
                        } else {
                            MainTabbedContainer()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainTabbedContainer() {
    val baselineViewModel: BaselineViewModel = viewModel()
    val wifiDirectViewModel: WifiDirectViewModel = viewModel()
    val cooperativeViewModel: CooperativeViewModel = viewModel()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0: Download Test, 1: Wi-Fi Direct, 2: Cooperative

    Scaffold(
        containerColor = CanvasBackground,
        bottomBar = {
            NavigationBar(
                containerColor = BentoCardSurface,
                contentColor = NavyDominant,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .border(width = 1.dp, color = BentoCardBorder)
                    .testTag("main_bottom_nav_bar")
            ) {
                // Tab 0: Step 1 - Single-Device Download Test
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Download Test"
                        )
                    },
                    label = {
                        Text(
                            text = "Download",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NavyDominant,
                        selectedTextColor = NavyDominant,
                        indicatorColor = AccentGoldLight,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_download_test_tab")
                )

                // Tab 1: Step 2 - Wi-Fi Direct Test
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.WifiTethering,
                            contentDescription = "Wi-Fi Direct Test"
                        )
                    },
                    label = {
                        Text(
                            text = "Wi-Fi Direct",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NavyDominant,
                        selectedTextColor = NavyDominant,
                        indicatorColor = AccentGoldLight,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_wifi_direct_test_tab")
                )

                // Tab 2: Step 3 - Cooperative Download
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.GroupWork,
                            contentDescription = "Cooperative Download"
                        )
                    },
                    label = {
                        Text(
                            text = "Cooperative",
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NavyDominant,
                        selectedTextColor = NavyDominant,
                        indicatorColor = AccentGoldLight,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag("nav_cooperative_test_tab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            when (selectedTab) {
                0 -> BaselineScreen(viewModel = baselineViewModel)
                1 -> WifiDirectScreen(viewModel = wifiDirectViewModel)
                2 -> CooperativeScreen(viewModel = cooperativeViewModel)
            }
        }
    }
}

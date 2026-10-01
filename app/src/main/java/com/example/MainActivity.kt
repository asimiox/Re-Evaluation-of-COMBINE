package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.BaselineScreen
import com.example.ui.BaselineViewModel
import com.example.ui.CombineSplashScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var showSplash by rememberSaveable { mutableStateOf(true) }
                    val viewModel: BaselineViewModel = viewModel()

                    Crossfade(
                        targetState = showSplash,
                        animationSpec = tween(durationMillis = 400),
                        label = "SplashCrossfade"
                    ) { isSplash ->
                        if (isSplash) {
                            CombineSplashScreen(onDismiss = { showSplash = false })
                        } else {
                            BaselineScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

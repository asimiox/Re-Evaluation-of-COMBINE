package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CombineLogoC
import com.example.ui.theme.AccentGold
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyDominant
import com.example.ui.theme.NavySecondary
import kotlinx.coroutines.delay

/**
 * Premium Splash Screen for COMBINE with letter "C" logo hierarchy,
 * 60-30-10 palette (Navy background with Gold accents), and live initialization status.
 */
@Composable
fun CombineSplashScreen(
    onDismiss: () -> Unit
) {
    val scaleAnim = remember { Animatable(0.85f) }
    val alphaAnim = remember { Animatable(0f) }
    var statusText by remember { mutableStateOf("Initializing network telemetry...") }

    LaunchedEffect(Unit) {
        // Entrance animation
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 400)
        )

        // Status stages
        delay(500)
        statusText = "Syncing Anycast edge mirrors..."
        delay(600)
        statusText = "Ready for baseline benchmarking."
        delay(500)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        NavyDark,
                        NavyDominant,
                        NavySecondary
                    )
                )
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onDismiss()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(32.dp)
                .scale(scaleAnim.value)
                .alpha(alphaAnim.value)
        ) {
            // Big Iconic Letter "C" Logo
            CombineLogoC(
                size = 100.dp,
                containerColor = NavyDark,
                accentColor = AccentGold,
                showBorder = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Brand Title
            Text(
                text = "COMBINE",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 32.sp,
                    letterSpacing = 6.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle
            Text(
                text = "Cellular & Wi-Fi Benchmark Suite",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = AccentGold.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Loading bar with 10% Gold Accent
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    color = AccentGold,
                    trackColor = Color.Transparent
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // On-the-fly telemetry status
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Tap to skip",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.35f)
                )
            )
        }
    }
}

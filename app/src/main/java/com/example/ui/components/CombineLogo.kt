package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGoldBorder
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyDominant

/**
 * Minimalist, luxury brand monogram: Letter "C" for COMBINE.
 * Designed with precise geometric proportions, subtle gradient sweep,
 * and high-contrast navy & gold aesthetic.
 */
@Composable
fun CombineLogoC(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    containerColor: Color = NavyDominant,
    accentColor: Color = AccentGold,
    showBorder: Boolean = true
) {
    val cornerRadius = size * 0.28f

    Box(
        modifier = modifier
            .size(size)
            .background(containerColor, RoundedCornerShape(cornerRadius))
            .then(
                if (showBorder) {
                    Modifier.border(
                        width = maxOf(1.dp, size * 0.035f),
                        color = AccentGoldBorder.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(cornerRadius)
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.65f)) {
            val canvasWidth = this.size.width
            val canvasHeight = this.size.height
            val strokeWidth = canvasWidth * 0.22f

            val arcSize = Size(canvasWidth - strokeWidth, canvasHeight - strokeWidth)
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

            // Letter "C" Arc: Starts at 42° and sweeps 276° (opening to the right)
            drawArc(
                brush = Brush.linearGradient(
                    colors = listOf(
                        accentColor,
                        Color(0xFFFDE68A),
                        accentColor
                    ),
                    start = Offset.Zero,
                    end = Offset(canvasWidth, canvasHeight)
                ),
                startAngle = 42f,
                sweepAngle = 276f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )

            // Micro telemetry center dot for benchmark feel
            drawCircle(
                color = accentColor.copy(alpha = 0.85f),
                radius = strokeWidth * 0.38f,
                center = Offset(canvasWidth * 0.52f, canvasHeight * 0.5f)
            )
        }
    }
}

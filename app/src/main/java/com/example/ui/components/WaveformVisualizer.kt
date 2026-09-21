package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    amplitude: Float = 0f,
    barCount: Int = 36,
    activeColor: Color = NeonCyan,
    inactiveColor: Color = NeonPurple.copy(alpha = 0.35f)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_anim"
    )

    Canvas(modifier = modifier.fillMaxWidth().height(56.dp)) {
        val width = size.width
        val height = size.height
        val totalSpacing = width / barCount
        val barWidth = (totalSpacing * 0.65f).coerceAtLeast(3f)

        for (i in 0 until barCount) {
            val progress = i.toFloat() / barCount
            val barHeightFraction = if (isActive) {
                val wave = (sin(phase + progress * 7.5f) + 1f) / 2f
                val dynamicFactor = (0.25f + 0.75f * amplitude).coerceIn(0.1f, 1f)
                (0.15f + wave * 0.85f * dynamicFactor).coerceIn(0.1f, 1f)
            } else {
                val base = 0.15f + 0.2f * sin(progress * Math.PI.toFloat())
                base.coerceIn(0.1f, 0.4f)
            }

            val barHeight = height * barHeightFraction
            val x = i * totalSpacing + (totalSpacing - barWidth) / 2f
            val y = (height - barHeight) / 2f

            val brush = if (isActive) {
                Brush.verticalGradient(
                    colors = listOf(NeonCyan, NeonPurple),
                    startY = y,
                    endY = y + barHeight
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(inactiveColor, inactiveColor.copy(alpha = 0.15f)),
                    startY = y,
                    endY = y + barHeight
                )
            }

            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

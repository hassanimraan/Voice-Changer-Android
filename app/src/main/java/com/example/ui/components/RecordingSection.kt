package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRose
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File

@Composable
fun RecordingSection(
    isRecording: Boolean,
    recordingDurationMs: Long,
    liveAmplitude: Float,
    hasOriginalAudio: Boolean,
    isPlayingOriginal: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPlayOriginal: () -> Unit,
    onLoadSample: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_trans")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
        border = BorderStroke(1.dp, StudioCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "1. Record Your Voice Sample",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Speak clearly into your microphone",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                // Sample Voice quick-test button
                OutlinedButton(
                    onClick = onLoadSample,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.testTag("btn_load_sample_voice")
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Load Sample",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Load Sample", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Waveform visualizer
            WaveformVisualizer(
                isActive = isRecording || isPlayingOriginal,
                amplitude = if (isRecording) liveAmplitude else 0.5f,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Big Tactile Record Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(96.dp)
            ) {
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(NeonRose.copy(alpha = 0.25f))
                    )
                }

                val buttonBg = if (isRecording) {
                    Brush.radialGradient(listOf(NeonRose, Color(0xFF9F1239)))
                } else {
                    Brush.radialGradient(listOf(NeonPurple, Color(0xFF4C1D95)))
                }

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(buttonBg)
                        .clickable {
                            if (isRecording) onStopRecording() else onStartRecording()
                        }
                        .border(
                            BorderStroke(
                                2.dp,
                                if (isRecording) NeonRose else NeonPurple.copy(alpha = 0.8f)
                            ),
                            CircleShape
                        )
                        .testTag("btn_record_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Timer & Status
            val seconds = (recordingDurationMs / 1000) % 60
            val minutes = (recordingDurationMs / 1000) / 60
            val timerText = String.format("%02d:%02d", minutes, seconds)

            Text(
                text = if (isRecording) "Recording... $timerText" else if (hasOriginalAudio) "Sample Ready ($timerText)" else "Tap to Record Voice",
                fontWeight = FontWeight.SemiBold,
                color = if (isRecording) NeonRose else if (hasOriginalAudio) NeonCyan else TextMuted,
                fontSize = 14.sp
            )

            // Playback controls for original audio
            AnimatedVisibility(visible = hasOriginalAudio && !isRecording) {
                Row(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioSurfaceVariant)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPlayOriginal,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NeonCyan.copy(alpha = 0.2f))
                            .testTag("btn_play_original")
                    ) {
                        Icon(
                            imageVector = if (isPlayingOriginal) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlayingOriginal) "Pause Original" else "Play Original",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isPlayingOriginal) "Playing Original Voice..." else "Listen to Original Audio",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioFormatConverter
import com.example.data.model.Personality
import com.example.ui.TransformationMode
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File

@Composable
fun TransformationActionCard(
    hasOriginalAudio: Boolean,
    convertedAudioFile: File?,
    selectedPersonality: Personality,
    isTransforming: Boolean,
    statusMessage: String,
    transcriptionText: String,
    personalitySpeechText: String,
    transformationMode: TransformationMode,
    isGeminiAvailable: Boolean,
    isPlayingConverted: Boolean,
    playbackPositionMs: Long,
    playbackDurationMs: Long,
    selectedFormat: AudioFormatConverter.AudioFormatOption = AudioFormatConverter.AudioFormatOption.MP3,
    isConvertingFormat: Boolean = false,
    onModeChange: (TransformationMode) -> Unit,
    onFormatSelect: (AudioFormatConverter.AudioFormatOption) -> Unit = {},
    onConvertFormatClick: (AudioFormatConverter.AudioFormatOption) -> Unit = {},
    onConvertClick: () -> Unit,
    onPlayConverted: () -> Unit,
    onSeekPlayback: (Long) -> Unit,
    onShareAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
        border = BorderStroke(1.dp, StudioCardBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "2. Voice Conversion Engine",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Select morphing engine and output audio format",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Mode Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = transformationMode == TransformationMode.ACOUSTIC_DSP,
                    onClick = { onModeChange(TransformationMode.ACOUSTIC_DSP) },
                    label = { Text("Acoustic DSP Morph") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan.copy(alpha = 0.25f),
                        selectedLabelColor = Color.White
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (transformationMode == TransformationMode.ACOUSTIC_DSP) NeonCyan else StudioCardBorder
                    ),
                    modifier = Modifier.weight(1f).testTag("chip_mode_dsp")
                )

                FilterChip(
                    selected = transformationMode == TransformationMode.AI_NEURAL,
                    onClick = { onModeChange(TransformationMode.AI_NEURAL) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("AI Persona")
                            if (!isGeminiAvailable) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("(Auto)", fontSize = 10.sp, color = NeonAmber)
                            }
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = NeonPurple
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonPurple.copy(alpha = 0.25f),
                        selectedLabelColor = Color.White
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (transformationMode == TransformationMode.AI_NEURAL) NeonPurple else StudioCardBorder
                    ),
                    modifier = Modifier.weight(1f).testTag("chip_mode_ai")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Output Audio Format Selector
            Text(
                text = "Target Audio Format (Fix for phones that can't play .wav):",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AudioFormatConverter.AudioFormatOption.values().forEach { formatOpt ->
                    val isSelected = selectedFormat == formatOpt
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFormatSelect(formatOpt) },
                        label = {
                            Text(
                                text = formatOpt.label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonAmber.copy(alpha = 0.25f),
                            selectedLabelColor = NeonAmber,
                            containerColor = StudioSurfaceVariant,
                            labelColor = TextSecondary
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) NeonAmber else StudioCardBorder
                        ),
                        modifier = Modifier.weight(1f).testTag("chip_format_${formatOpt.extension}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Big Action Button
            val buttonEnabled = hasOriginalAudio && !isTransforming && !isConvertingFormat
            Button(
                onClick = onConvertClick,
                enabled = buttonEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btn_convert_voice"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonPurple,
                    disabledContainerColor = StudioSurfaceVariant
                )
            ) {
                if (isTransforming) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Morphing Voice...",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = "Convert Voice",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Convert Voice to ${selectedPersonality.name}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            if (statusMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (convertedAudioFile != null) NeonGreen else TextSecondary,
                    fontSize = 12.sp
                )
            }

            // Converted Voice Results Player & Converter
            AnimatedVisibility(visible = convertedAudioFile != null) {
                Column(modifier = Modifier.padding(top = 18.dp)) {
                    Surface(
                        color = StudioSurfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Player header
                            val currentExtension = convertedAudioFile?.extension?.uppercase() ?: "AUDIO"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(NeonGreen.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        IconButton(
                                            onClick = onPlayConverted,
                                            modifier = Modifier.testTag("btn_play_converted")
                                        ) {
                                            Icon(
                                                imageVector = if (isPlayingConverted) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = if (isPlayingConverted) "Pause Converted Voice" else "Play Converted Voice",
                                                tint = NeonGreen,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${selectedPersonality.name} Voice",
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = NeonGreen.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = currentExtension,
                                                    color = NeonGreen,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = if (isPlayingConverted) "Playing audio smoothly..." else "Ready to play on any device",
                                            color = NeonGreen,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = onShareAudio,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(StudioCardBorder)
                                        .testTag("btn_share_converted_audio")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share Converted Audio",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Playback timeline slider
                            val dur = playbackDurationMs.coerceAtLeast(1L)
                            val pos = playbackPositionMs.coerceIn(0L, dur)
                            Slider(
                                value = pos.toFloat(),
                                onValueChange = { onSeekPlayback(it.toLong()) },
                                valueRange = 0f..dur.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = NeonGreen,
                                    activeTrackColor = NeonGreen,
                                    inactiveTrackColor = StudioCardBorder
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("slider_playback_progress")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = String.format("%02d:%02d", (pos / 1000) / 60, (pos / 1000) % 60),
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                                Text(
                                    text = String.format("%02d:%02d", (dur / 1000) / 60, (dur / 1000) % 60),
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }

                            // Dynamic Waveform
                            WaveformVisualizer(
                                isActive = isPlayingConverted,
                                amplitude = 0.7f,
                                activeColor = NeonGreen,
                                modifier = Modifier.padding(top = 8.dp)
                            )

                            // Quick Format Converter Toolbar
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                color = StudioCardBg,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, StudioCardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Transform,
                                                contentDescription = null,
                                                tint = NeonCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Convert Format:",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            )
                                        }

                                        if (isConvertingFormat) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(14.dp),
                                                    color = NeonCyan,
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Converting...",
                                                    fontSize = 11.sp,
                                                    color = NeonCyan
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        AudioFormatConverter.AudioFormatOption.values().forEach { option ->
                                            val isCurrentFormat = convertedAudioFile?.extension.equals(option.extension, ignoreCase = true)
                                            OutlinedButton(
                                                onClick = { onConvertFormatClick(option) },
                                                enabled = !isConvertingFormat,
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = if (isCurrentFormat) NeonCyan.copy(alpha = 0.15f) else Color.Transparent
                                                ),
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (isCurrentFormat) NeonCyan else StudioCardBorder
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(36.dp)
                                                    .testTag("btn_convert_to_${option.extension}")
                                            ) {
                                                Text(
                                                    text = if (isCurrentFormat) "✓ ${option.label}" else "To ${option.label}",
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isCurrentFormat) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isCurrentFormat) NeonCyan else TextSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Transcript / Personality speech preview
                            if (personalitySpeechText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Vocal Delivery / Personality Phrasing:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonAmber
                                )
                                Text(
                                    text = "\"$personalitySpeechText\"",
                                    fontSize = 12.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

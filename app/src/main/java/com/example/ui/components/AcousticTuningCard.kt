package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Personality
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun AcousticTuningCard(
    pitch: Float,
    speed: Float,
    reverb: Float,
    bass: Float,
    selectedPersonality: Personality,
    onPitchChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onReverbChange: (Float) -> Unit,
    onBassChange: (Float) -> Unit,
    onResetToDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
        border = BorderStroke(1.dp, StudioCardBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Acoustic Tuning",
                        tint = NeonAmber,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Column {
                        Text(
                            text = "Acoustic Voice DSP Modulation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Fine-tune pitch, cadence, reverb, and bass resonance",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.testTag("btn_toggle_dsp_tuning")
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse Tuning" else "Expand Tuning",
                        tint = NeonAmber
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    // Pitch slider
                    TuningSliderRow(
                        label = "Pitch Modulation",
                        value = pitch,
                        valueDisplay = String.format(Locale.US, "%.2fx", pitch),
                        range = 0.5f..1.8f,
                        hint = if (pitch > 1.05f) "Higher (Melodic / Tenor / Rafi)" else if (pitch < 0.95f) "Deeper (Baritone / Leader / Imran Khan)" else "Neutral",
                        onValueChange = onPitchChange,
                        testTag = "slider_pitch"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Speed / Cadence slider
                    TuningSliderRow(
                        label = "Speech Cadence & Tempo",
                        value = speed,
                        valueDisplay = String.format(Locale.US, "%.2fx", speed),
                        range = 0.6f..1.4f,
                        hint = if (speed > 1.05f) "Upbeat & energetic" else if (speed < 0.95f) "Measured & deliberate" else "Normal tempo",
                        onValueChange = onSpeedChange,
                        testTag = "slider_speed"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Reverb / Echo
                    TuningSliderRow(
                        label = "Concert Hall Reverb & Echo",
                        value = reverb,
                        valueDisplay = "${(reverb * 100).toInt()}%",
                        range = 0.0f..0.8f,
                        hint = "Studio acoustic resonance and depth",
                        onValueChange = onReverbChange,
                        testTag = "slider_reverb"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bass boost
                    TuningSliderRow(
                        label = "Chest Resonance & Bass Boost",
                        value = bass,
                        valueDisplay = "${(bass * 100).toInt()}%",
                        range = 0.0f..0.9f,
                        hint = "Low-end authority & warmth",
                        onValueChange = onBassChange,
                        testTag = "slider_bass"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onResetToDefaults,
                            modifier = Modifier.testTag("btn_reset_tuning_defaults")
                        ) {
                            Text("Reset to ${selectedPersonality.name} Defaults", color = NeonCyan, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TuningSliderRow(
    label: String,
    value: Float,
    valueDisplay: String,
    range: ClosedFloatingPointRange<Float>,
    hint: String,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = valueDisplay,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = NeonCyan,
                activeTrackColor = NeonPurple,
                inactiveTrackColor = StudioCardBorder
            ),
            modifier = Modifier.testTag(testTag)
        )

        Text(
            text = hint,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}

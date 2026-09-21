package com.example.ui.components

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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioFormatConverter
import com.example.data.local.VoiceRecordingEntity
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRose
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SavedRecordingsList(
    recordings: List<VoiceRecordingEntity>,
    onPlayRecording: (VoiceRecordingEntity) -> Unit,
    onDeleteRecording: (VoiceRecordingEntity) -> Unit,
    onConvertRecording: (VoiceRecordingEntity, AudioFormatConverter.AudioFormatOption) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
        border = BorderStroke(1.dp, StudioCardBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Saved Recordings History",
                    tint = NeonCyan,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Saved Voice Conversions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${recordings.size} voice clip(s) saved in local studio library",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (recordings.isEmpty()) {
                Surface(
                    color = StudioSurfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No saved voice conversions yet",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Record audio and convert to Mohammad Rafi, Imran Khan or any personality to save it here in MP3, M4A or WAV format.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                    recordings.forEach { item ->
                        val ext = File(item.convertedFilePath).extension.uppercase().ifBlank { "MP3" }
                        var showFormatMenu by remember { mutableStateOf(false) }

                        Surface(
                            color = StudioSurfaceVariant,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, StudioCardBorder),
                            modifier = Modifier.fillMaxWidth().testTag("saved_recording_item_${item.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    IconButton(
                                        onClick = { onPlayRecording(item) },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(NeonPurple.copy(alpha = 0.25f))
                                            .testTag("btn_play_saved_${item.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play ${item.personalityName}",
                                            tint = NeonPurple,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.personalityName,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = when (ext) {
                                                    "MP3" -> NeonAmber.copy(alpha = 0.2f)
                                                    "M4A" -> NeonCyan.copy(alpha = 0.2f)
                                                    else -> NeonGreen.copy(alpha = 0.2f)
                                                },
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = ext,
                                                    color = when (ext) {
                                                        "MP3" -> NeonAmber
                                                        "M4A" -> NeonCyan
                                                        else -> NeonGreen
                                                    },
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.transformationMode,
                                                color = NeonCyan,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = " • ${dateFormat.format(Date(item.createdAt))}",
                                                color = TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Convert format popup
                                    Box {
                                        IconButton(
                                            onClick = { showFormatMenu = true },
                                            modifier = Modifier.testTag("btn_convert_saved_${item.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Transform,
                                                contentDescription = "Convert format",
                                                tint = NeonCyan,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = showFormatMenu,
                                            onDismissRequest = { showFormatMenu = false }
                                        ) {
                                            AudioFormatConverter.AudioFormatOption.values().forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text("Convert to ${option.label}") },
                                                    onClick = {
                                                        showFormatMenu = false
                                                        onConvertRecording(item, option)
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = { onDeleteRecording(item) },
                                        modifier = Modifier.testTag("btn_delete_saved_${item.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete recording",
                                            tint = NeonRose.copy(alpha = 0.8f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

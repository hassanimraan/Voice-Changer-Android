package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Personality
import com.example.data.model.PersonalityCatalog
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PersonalitySelector(
    selectedPersonality: Personality,
    customNameInput: String,
    onPersonalitySelected: (Personality) -> Unit,
    onCustomNameChanged: (String) -> Unit,
    onSpeakSample: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var isCustomMode by remember { mutableStateOf(selectedPersonality.isCustom) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
        border = BorderStroke(1.dp, StudioCardBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NeonPurple.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Personality Icon",
                            tint = NeonPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Target Personality Voice",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Choose a personality or type any custom name",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Featured Chips (specifically highlighting Mohammad Rafi and Imran Khan as requested)
            Text(
                text = "FEATURED VOICES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PersonalityCatalog.predefinedPersonalities.take(4).forEach { p ->
                    val isSelected = selectedPersonality.id == p.id
                    SuggestionChip(
                        onClick = {
                            isCustomMode = false
                            onPersonalitySelected(p)
                        },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = p.name,
                                    color = if (isSelected) Color.White else TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isSelected) NeonPurple.copy(alpha = 0.35f) else StudioSurfaceVariant
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) NeonCyan else StudioCardBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("chip_${p.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dropdown Menu Selector
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = if (isCustomMode) {
                        if (customNameInput.isNotBlank()) customNameInput else "Enter Custom Personality..."
                    } else {
                        selectedPersonality.name
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Selected Personality") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = StudioCardBorder,
                        focusedContainerColor = StudioSurfaceVariant,
                        unfocusedContainerColor = StudioSurfaceVariant,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                        .fillMaxWidth()
                        .testTag("personality_dropdown_trigger")
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(StudioCardBg)
                ) {
                    PersonalityCatalog.predefinedPersonalities.forEach { personality ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = personality.name,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${personality.title} • ${personality.category}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            },
                            onClick = {
                                isCustomMode = false
                                onPersonalitySelected(personality)
                                expanded = false
                            },
                            leadingIcon = {
                                val icon = when (personality.category) {
                                    "Music & Arts" -> Icons.Default.MusicNote
                                    "Leaders & Orators" -> Icons.Default.Public
                                    else -> Icons.Default.Mic
                                }
                                Icon(icon, contentDescription = null, tint = NeonPurple)
                            },
                            modifier = Modifier.testTag("dropdown_item_${personality.id}")
                        )
                    }

                    // Custom Personality Option
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = "✍️ Custom Personality Name...",
                                    fontWeight = FontWeight.Bold,
                                    color = NeonAmber
                                )
                                Text(
                                    text = "Type any politician, singer, actor or person",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        },
                        onClick = {
                            isCustomMode = true
                            expanded = false
                        },
                        modifier = Modifier.testTag("dropdown_item_custom")
                    )
                }
            }

            // Custom Personality Name Input Field
            AnimatedVisibility(visible = isCustomMode) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    OutlinedTextField(
                        value = customNameInput,
                        onValueChange = {
                            onCustomNameChanged(it)
                        },
                        label = { Text("Personality Name (e.g. Narendra Modi, Lata Mangeshkar)") },
                        placeholder = { Text("Enter any famous personality name...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonAmber,
                            unfocusedBorderColor = StudioCardBorder,
                            focusedContainerColor = StudioSurfaceVariant,
                            unfocusedContainerColor = StudioSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = NeonAmber
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_personality_input")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Selected Personality Info Card
            Surface(
                color = StudioSurfaceVariant,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, StudioCardBorder.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedPersonality.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            Text(
                                text = selectedPersonality.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        IconButton(
                            onClick = onSpeakSample,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.15f))
                                .testTag("btn_sample_personality_voice")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Hear Sample Personality Style",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = selectedPersonality.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"${selectedPersonality.signatureQuote}\"",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = NeonAmber
                    )
                }
            }
        }
    }
}

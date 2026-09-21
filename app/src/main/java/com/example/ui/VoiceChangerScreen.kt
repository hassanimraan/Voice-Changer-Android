package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SettingsVoice
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.R
import com.example.ui.components.AcousticTuningCard
import com.example.ui.components.PersonalitySelector
import com.example.ui.components.RecordingSection
import com.example.ui.components.SavedRecordingsList
import com.example.ui.components.TransformationActionCard
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceChangerScreen(
    viewModel: VoiceChangerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val savedRecordings by viewModel.savedRecordings.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Permission launcher for RECORD_AUDIO
    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecording()
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Microphone permission is required to record voice. You can also tap 'Load Sample'.")
            }
        }
    }

    fun handleRecordClick() {
        if (uiState.isRecording) {
            viewModel.stopRecording()
        } else {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            )
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                viewModel.startRecording()
            } else {
                recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    fun shareAudio(file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share converted voice audio"))
        } catch (e: Exception) {
            // Fallback direct share
            try {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "audio/*"
                    putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
                }
                context.startActivity(Intent.createChooser(intent, "Share converted voice audio"))
            } catch (err: Exception) {
                Toast.makeText(context, "Could not open share menu", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StudioDarkBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(NeonPurple),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SettingsVoice,
                                contentDescription = "Voice Changer",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Celebrity Voice Changer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Mohammad Rafi • Imran Khan • Custom Voices",
                                fontSize = 11.sp,
                                color = NeonCyan
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = StudioDarkBg
                ),
                actions = {
                    Surface(
                        color = if (uiState.isGeminiAvailable) NeonPurple.copy(alpha = 0.2f) else NeonAmber.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.isGeminiAvailable) Icons.Default.AutoAwesome else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (uiState.isGeminiAvailable) NeonPurple else NeonAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (uiState.isGeminiAvailable) "AI Online" else "DSP Offline",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (uiState.isGeminiAvailable) NeonPurple else NeonAmber
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Hero Banner
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_banner),
                            contentDescription = "Studio Soundwave Banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        listOf(
                                            Color.Transparent,
                                            StudioDarkBg.copy(alpha = 0.85f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Acoustic & Neural Voice Morphing",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Convert your voice sample into iconic personalities",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonCyan
                            )
                        }
                    }
                }
            }

            // 1. Audio Recording Section
            item {
                RecordingSection(
                    isRecording = uiState.isRecording,
                    recordingDurationMs = uiState.recordingDurationMs,
                    liveAmplitude = uiState.liveAmplitude,
                    hasOriginalAudio = uiState.originalAudioFile != null,
                    isPlayingOriginal = uiState.isPlayingOriginal,
                    onStartRecording = { handleRecordClick() },
                    onStopRecording = { viewModel.stopRecording() },
                    onPlayOriginal = { viewModel.playOriginal() },
                    onLoadSample = { viewModel.loadSampleVoice() },
                    modifier = Modifier.widthIn(max = 600.dp)
                )
            }

            // 2. Target Personality Selection (Dropdown & Quick Chips & Custom name)
            item {
                PersonalitySelector(
                    selectedPersonality = uiState.selectedPersonality,
                    customNameInput = uiState.customPersonalityName,
                    onPersonalitySelected = { viewModel.selectPersonality(it) },
                    onCustomNameChanged = { viewModel.setCustomPersonalityName(it) },
                    onSpeakSample = { viewModel.speakPersonalitySample() },
                    modifier = Modifier.widthIn(max = 600.dp)
                )
            }

            // 3. Acoustic DSP Tuning Sliders (Pitch, Speed, Reverb, Bass)
            item {
                AcousticTuningCard(
                    pitch = uiState.pitchMultiplier,
                    speed = uiState.speedMultiplier,
                    reverb = uiState.reverbAmount,
                    bass = uiState.bassBoost,
                    selectedPersonality = uiState.selectedPersonality,
                    onPitchChange = { viewModel.setPitch(it) },
                    onSpeedChange = { viewModel.setSpeed(it) },
                    onReverbChange = { viewModel.setReverb(it) },
                    onBassChange = { viewModel.setBass(it) },
                    onResetToDefaults = {
                        viewModel.setPitch(uiState.selectedPersonality.pitchMultiplier)
                        viewModel.setSpeed(uiState.selectedPersonality.speedMultiplier)
                        viewModel.setReverb(uiState.selectedPersonality.reverbAmount)
                        viewModel.setBass(uiState.selectedPersonality.bassBoost)
                    },
                    modifier = Modifier.widthIn(max = 600.dp)
                )
            }

            // 4. Voice Conversion Trigger & Converted Audio Player
            item {
                TransformationActionCard(
                    hasOriginalAudio = uiState.originalAudioFile != null,
                    convertedAudioFile = uiState.convertedAudioFile,
                    selectedPersonality = uiState.selectedPersonality,
                    isTransforming = uiState.isTransforming,
                    statusMessage = uiState.statusMessage,
                    transcriptionText = uiState.transcriptionText,
                    personalitySpeechText = uiState.personalitySpeechText,
                    transformationMode = uiState.transformationMode,
                    isGeminiAvailable = uiState.isGeminiAvailable,
                    isPlayingConverted = uiState.isPlayingConverted,
                    playbackPositionMs = uiState.playbackPositionMs,
                    playbackDurationMs = uiState.playbackDurationMs,
                    selectedFormat = uiState.selectedFormat,
                    isConvertingFormat = uiState.isConvertingFormat,
                    onModeChange = { viewModel.setTransformationMode(it) },
                    onFormatSelect = { viewModel.setTargetFormat(it) },
                    onConvertFormatClick = { viewModel.convertCurrentVoiceToFormat(it) },
                    onConvertClick = { viewModel.convertVoice() },
                    onPlayConverted = { viewModel.playConverted() },
                    onSeekPlayback = { viewModel.seekPlayback(it) },
                    onShareAudio = {
                        uiState.convertedAudioFile?.let { shareAudio(it) }
                    },
                    modifier = Modifier.widthIn(max = 600.dp)
                )
            }

            // 5. Library of Saved Converted Voices
            item {
                SavedRecordingsList(
                    recordings = savedRecordings,
                    onPlayRecording = { viewModel.playSavedRecording(it) },
                    onDeleteRecording = { viewModel.deleteRecording(it) },
                    onConvertRecording = { rec, fmt -> viewModel.convertSavedRecording(rec, fmt) },
                    modifier = Modifier.widthIn(max = 600.dp)
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

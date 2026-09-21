package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioFormatConverter
import com.example.audio.AudioPlayerManager
import com.example.audio.AudioRecorderManager
import com.example.audio.VoiceTransformerDsp
import com.example.audio.WavUtils
import com.example.data.local.AppDatabase
import com.example.data.local.VoiceRecordingEntity
import com.example.data.model.Personality
import com.example.data.model.PersonalityCatalog
import com.example.network.GeminiVoiceService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class TransformationMode(val label: String, val description: String) {
    AI_NEURAL("AI Neural Persona", "AI transcribes, rewrites in character, and synthesizes target personality vocal nuances"),
    ACOUSTIC_DSP("Acoustic DSP Morph", "Local real-time pitch shifting, formant resonance, bass boost, and studio reverb")
}

data class VoiceChangerUiState(
    val selectedPersonality: Personality = PersonalityCatalog.predefinedPersonalities[0], // Mohammad Rafi
    val customPersonalityName: String = "",
    val customPersonalityNotes: String = "",
    val isDropdownExpanded: Boolean = false,
    val isRecording: Boolean = false,
    val recordingDurationMs: Long = 0L,
    val liveAmplitude: Float = 0f,
    val originalAudioFile: File? = null,
    val convertedAudioFile: File? = null,
    val pitchMultiplier: Float = 1.15f,
    val speedMultiplier: Float = 0.96f,
    val reverbAmount: Float = 0.35f,
    val bassBoost: Float = 0.20f,
    val transformationMode: TransformationMode = TransformationMode.ACOUSTIC_DSP,
    val isTransforming: Boolean = false,
    val statusMessage: String = "",
    val transcriptionText: String = "",
    val personalitySpeechText: String = "",
    val isPlayingOriginal: Boolean = false,
    val isPlayingConverted: Boolean = false,
    val playbackPositionMs: Long = 0L,
    val playbackDurationMs: Long = 0L,
    val isGeminiAvailable: Boolean = false,
    val userNotice: String? = null,
    val selectedFormat: AudioFormatConverter.AudioFormatOption = AudioFormatConverter.AudioFormatOption.MP3,
    val isConvertingFormat: Boolean = false
)

class VoiceChangerViewModel(application: Application) : AndroidViewModel(application) {

    private val recorderManager = AudioRecorderManager(application)
    private val playerManager = AudioPlayerManager(application)
    private val voiceTransformer = VoiceTransformerDsp(application)
    private val geminiService = GeminiVoiceService(application)
    private val database = AppDatabase.getDatabase(application)
    private val recordingDao = database.voiceRecordingDao()

    val savedRecordings: StateFlow<List<VoiceRecordingEntity>> = recordingDao.getAllRecordings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(VoiceChangerUiState())
    val uiState: StateFlow<VoiceChangerUiState> = _uiState.asStateFlow()

    init {
        val hasKey = geminiService.isApiKeyConfigured()
        _uiState.update {
            it.copy(
                isGeminiAvailable = hasKey,
                transformationMode = if (hasKey) TransformationMode.AI_NEURAL else TransformationMode.ACOUSTIC_DSP
            )
        }

        // Collect recorder state
        viewModelScope.launch {
            recorderManager.isRecording.collect { rec ->
                _uiState.update { it.copy(isRecording = rec) }
            }
        }
        viewModelScope.launch {
            recorderManager.amplitude.collect { amp ->
                _uiState.update { it.copy(liveAmplitude = amp) }
            }
        }
        viewModelScope.launch {
            recorderManager.recordingDurationMs.collect { dur ->
                _uiState.update { it.copy(recordingDurationMs = dur) }
            }
        }

        // Collect player state
        viewModelScope.launch {
            playerManager.isPlaying.collect { playing ->
                val currentFile = playerManager.activeFilePath.value
                val isOrig = currentFile != null && currentFile == _uiState.value.originalAudioFile?.absolutePath
                val isConv = currentFile != null && currentFile == _uiState.value.convertedAudioFile?.absolutePath
                _uiState.update {
                    it.copy(
                        isPlayingOriginal = playing && isOrig,
                        isPlayingConverted = playing && isConv
                    )
                }
            }
        }
        viewModelScope.launch {
            playerManager.currentPositionMs.collect { pos ->
                _uiState.update { it.copy(playbackPositionMs = pos) }
            }
        }
        viewModelScope.launch {
            playerManager.durationMs.collect { dur ->
                _uiState.update { it.copy(playbackDurationMs = dur) }
            }
        }
    }

    fun selectPersonality(personality: Personality) {
        playerManager.stop()
        _uiState.update {
            it.copy(
                selectedPersonality = personality,
                pitchMultiplier = personality.pitchMultiplier,
                speedMultiplier = personality.speedMultiplier,
                reverbAmount = personality.reverbAmount,
                bassBoost = personality.bassBoost,
                isDropdownExpanded = false,
                statusMessage = "Selected ${personality.name}"
            )
        }
    }

    fun setCustomPersonalityName(name: String) {
        val customP = PersonalityCatalog.createCustomPersonality(
            name = if (name.isBlank()) "Custom Personality" else name,
            notes = _uiState.value.customPersonalityNotes
        )
        _uiState.update {
            it.copy(
                customPersonalityName = name,
                selectedPersonality = customP
            )
        }
    }

    fun setCustomPersonalityNotes(notes: String) {
        val name = _uiState.value.customPersonalityName.ifBlank { "Custom Personality" }
        val customP = PersonalityCatalog.createCustomPersonality(name, notes)
        _uiState.update {
            it.copy(
                customPersonalityNotes = notes,
                selectedPersonality = customP
            )
        }
    }

    fun toggleDropdown(expanded: Boolean) {
        _uiState.update { it.copy(isDropdownExpanded = expanded) }
    }

    fun setPitch(pitch: Float) {
        _uiState.update { it.copy(pitchMultiplier = pitch) }
    }

    fun setSpeed(speed: Float) {
        _uiState.update { it.copy(speedMultiplier = speed) }
    }

    fun setReverb(reverb: Float) {
        _uiState.update { it.copy(reverbAmount = reverb) }
    }

    fun setBass(bass: Float) {
        _uiState.update { it.copy(bassBoost = bass) }
    }

    fun setTransformationMode(mode: TransformationMode) {
        _uiState.update { it.copy(transformationMode = mode) }
    }

    fun startRecording(): Boolean {
        playerManager.stop()
        val cacheDir = getApplication<Application>().cacheDir
        val origFile = File(cacheDir, "original_voice_${System.currentTimeMillis()}.wav")
        val success = recorderManager.startRecording(origFile, viewModelScope)
        if (success) {
            _uiState.update {
                it.copy(
                    originalAudioFile = origFile,
                    statusMessage = "Recording voice sample..."
                )
            }
        } else {
            _uiState.update { it.copy(statusMessage = "Could not start recording. Check microphone permissions.") }
        }
        return success
    }

    fun stopRecording() {
        val duration = recorderManager.stopRecording()
        _uiState.update {
            it.copy(
                recordingDurationMs = duration,
                statusMessage = "Voice sample recorded (${duration / 1000}s). Ready to convert!"
            )
        }
    }

    fun loadSampleVoice() {
        playerManager.stop()
        viewModelScope.launch(Dispatchers.IO) {
            val cacheDir = getApplication<Application>().cacheDir
            val sampleFile = File(cacheDir, "sample_vocal_${System.currentTimeMillis()}.wav")
            recorderManager.generatePresetSampleVoice(sampleFile)
            _uiState.update {
                it.copy(
                    originalAudioFile = sampleFile,
                    recordingDurationMs = 3500L,
                    statusMessage = "Preset voice sample loaded. Ready to convert!"
                )
            }
        }
    }

    fun convertVoice() {
        val state = _uiState.value
        val origFile = state.originalAudioFile
        if (origFile == null || !origFile.exists() || origFile.length() == 0L) {
            _uiState.update { it.copy(statusMessage = "Please record or load a voice sample first!") }
            return
        }

        playerManager.stop()
        _uiState.update {
            it.copy(
                isTransforming = true,
                statusMessage = "Morphing voice to ${state.selectedPersonality.name}..."
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val cacheDir = getApplication<Application>().cacheDir
            val personalityNameClean = state.selectedPersonality.name.lowercase().replace("\\s+".toRegex(), "_")
            val convertedFile = File(cacheDir, "converted_${personalityNameClean}_${System.currentTimeMillis()}.wav")

            if (state.transformationMode == TransformationMode.AI_NEURAL && state.isGeminiAvailable) {
                // Mode 1: Gemini AI Neural Transformation
                val result = geminiService.convertVoiceWithGemini(
                    inputAudioFile = origFile,
                    targetPersonality = state.selectedPersonality,
                    outputAudioFile = convertedFile
                )

                result.onSuccess { convResult ->
                    var finalAudioFile = convResult.convertedAudioFile
                    // If Gemini direct audio was not available, synthesize via DSP/TTS using the personality transcript
                    if (finalAudioFile == null || !finalAudioFile.exists()) {
                        val textToUse = convResult.personalitySpeechText.ifBlank { state.selectedPersonality.sampleSpeech }
                        val ttsResult = voiceTransformer.synthesizeSpeechToFile(
                            text = textToUse,
                            outputFile = convertedFile,
                            personality = state.selectedPersonality
                        )
                        finalAudioFile = ttsResult.getOrNull()
                    }

                    // Fallback to DSP morphing of original file if needed
                    if (finalAudioFile == null || !finalAudioFile.exists()) {
                        val dspResult = voiceTransformer.transformAudio(
                            inputFile = origFile,
                            outputFile = convertedFile,
                            pitchMultiplier = state.pitchMultiplier,
                            speedMultiplier = state.speedMultiplier,
                            reverbAmount = state.reverbAmount,
                            bassBoost = state.bassBoost
                        )
                        finalAudioFile = dspResult.getOrNull()
                    }

                    if (finalAudioFile != null && finalAudioFile.exists()) {
                        saveToHistory(state.selectedPersonality, origFile, finalAudioFile, "AI Neural Persona", convResult.transcribedText, convResult.personalitySpeechText)
                        _uiState.update {
                            it.copy(
                                isTransforming = false,
                                convertedAudioFile = finalAudioFile,
                                transcriptionText = convResult.transcribedText,
                                personalitySpeechText = convResult.personalitySpeechText,
                                statusMessage = "Voice successfully transformed to ${state.selectedPersonality.name}!"
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isTransforming = false,
                                statusMessage = "Audio conversion encountered an error. Please try again."
                            )
                        }
                    }
                }.onFailure { err ->
                    // Fall back automatically to Acoustic DSP morphing
                    val dspResult = voiceTransformer.transformAudio(
                        inputFile = origFile,
                        outputFile = convertedFile,
                        pitchMultiplier = state.pitchMultiplier,
                        speedMultiplier = state.speedMultiplier,
                        reverbAmount = state.reverbAmount,
                        bassBoost = state.bassBoost
                    )
                    dspResult.onSuccess { dspFile ->
                        saveToHistory(state.selectedPersonality, origFile, dspFile, "Acoustic DSP Morph", "", state.selectedPersonality.sampleSpeech)
                        _uiState.update {
                            it.copy(
                                isTransforming = false,
                                convertedAudioFile = dspFile,
                                personalitySpeechText = state.selectedPersonality.signatureQuote,
                                statusMessage = "Converted with Acoustic DSP Morph (${err.message?.take(40)}...)"
                            )
                        }
                    }.onFailure { dspErr ->
                        _uiState.update {
                            it.copy(
                                isTransforming = false,
                                statusMessage = "Transformation failed: ${dspErr.message}"
                            )
                        }
                    }
                }
            } else {
                // Mode 2: Acoustic DSP Morphing (100% offline & immediate)
                val dspResult = voiceTransformer.transformAudio(
                    inputFile = origFile,
                    outputFile = convertedFile,
                    pitchMultiplier = state.pitchMultiplier,
                    speedMultiplier = state.speedMultiplier,
                    reverbAmount = state.reverbAmount,
                    bassBoost = state.bassBoost
                )

                dspResult.onSuccess { file ->
                    saveToHistory(
                        state.selectedPersonality,
                        origFile,
                        file,
                        "Acoustic DSP Morph",
                        "",
                        state.selectedPersonality.signatureQuote
                    )
                    _uiState.update {
                        it.copy(
                            isTransforming = false,
                            convertedAudioFile = file,
                            personalitySpeechText = state.selectedPersonality.signatureQuote,
                            statusMessage = "Acoustic DSP morphed to ${state.selectedPersonality.name}!"
                        )
                    }
                }.onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isTransforming = false,
                            statusMessage = "Transformation failed: ${err.message}"
                        )
                    }
                }
            }
        }
    }

    private suspend fun saveToHistory(
        personality: Personality,
        origFile: File,
        convFile: File,
        mode: String,
        transcript: String,
        personalityText: String
    ) = withContext(Dispatchers.IO) {
        try {
            val entity = VoiceRecordingEntity(
                personalityId = personality.id,
                personalityName = personality.name,
                originalFilePath = origFile.absolutePath,
                convertedFilePath = convFile.absolutePath,
                durationMs = _uiState.value.recordingDurationMs,
                transformationMode = mode,
                transcriptionText = transcript,
                convertedSpeechText = personalityText,
                pitchMultiplier = _uiState.value.pitchMultiplier,
                speedMultiplier = _uiState.value.speedMultiplier
            )
            recordingDao.insertRecording(entity)
        } catch (e: Exception) {
            // Ignore history save error
        }
    }

    fun playOriginal() {
        val file = _uiState.value.originalAudioFile ?: return
        if (_uiState.value.isPlayingOriginal) {
            playerManager.pause()
        } else {
            playerManager.playFile(file, viewModelScope)
        }
    }

    fun playConverted() {
        val file = _uiState.value.convertedAudioFile ?: return
        if (_uiState.value.isPlayingConverted) {
            playerManager.pause()
        } else {
            playerManager.playFile(file, viewModelScope)
        }
    }

    fun playSavedRecording(recording: VoiceRecordingEntity) {
        val file = File(recording.convertedFilePath)
        if (file.exists()) {
            _uiState.update {
                it.copy(
                    convertedAudioFile = file,
                    personalitySpeechText = recording.convertedSpeechText,
                    statusMessage = "Playing ${recording.personalityName} recording"
                )
            }
            playerManager.playFile(file, viewModelScope)
        } else {
            _uiState.update { it.copy(statusMessage = "Audio file no longer exists on storage") }
        }
    }

    fun seekPlayback(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun stopPlayback() {
        playerManager.stop()
    }

    fun deleteRecording(recording: VoiceRecordingEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            recordingDao.deleteRecording(recording)
            try {
                File(recording.convertedFilePath).delete()
            } catch (e: Exception) {}
        }
    }

    fun speakPersonalitySample() {
        voiceTransformer.speak(_uiState.value.selectedPersonality.sampleSpeech, _uiState.value.selectedPersonality)
    }

    fun setTargetFormat(format: AudioFormatConverter.AudioFormatOption) {
        _uiState.update { it.copy(selectedFormat = format) }
    }

    fun convertCurrentVoiceToFormat(targetFormat: AudioFormatConverter.AudioFormatOption) {
        val fileToConvert = _uiState.value.convertedAudioFile ?: _uiState.value.originalAudioFile
        if (fileToConvert == null || !fileToConvert.exists()) {
            _uiState.update { it.copy(statusMessage = "Please record or generate a voice first!") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isConvertingFormat = true,
                    statusMessage = "Converting voice to ${targetFormat.label} format..."
                )
            }
            val cacheDir = getApplication<Application>().cacheDir
            val result = AudioFormatConverter.convertAudio(fileToConvert, targetFormat, cacheDir)
            result.onSuccess { newFile ->
                _uiState.update {
                    it.copy(
                        isConvertingFormat = false,
                        convertedAudioFile = newFile,
                        selectedFormat = targetFormat,
                        statusMessage = "Successfully converted to ${targetFormat.label} (${newFile.name})!"
                    )
                }
                // Automatically preview converted audio
                playerManager.playFile(newFile, viewModelScope)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isConvertingFormat = false,
                        statusMessage = "Conversion to ${targetFormat.label} failed: ${error.message}"
                    )
                }
            }
        }
    }

    fun convertSavedRecording(recording: VoiceRecordingEntity, targetFormat: AudioFormatConverter.AudioFormatOption) {
        val file = File(recording.convertedFilePath)
        if (!file.exists()) {
            _uiState.update { it.copy(statusMessage = "Recording file not found on device storage") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isConvertingFormat = true,
                    statusMessage = "Converting '${recording.personalityName}' to ${targetFormat.label}..."
                )
            }
            val cacheDir = getApplication<Application>().cacheDir
            val result = AudioFormatConverter.convertAudio(file, targetFormat, cacheDir)
            result.onSuccess { newFile ->
                val newDuration = WavUtils.parseWav(newFile)?.durationMs ?: recording.durationMs
                val updated = recording.copy(
                    convertedFilePath = newFile.absolutePath,
                    durationMs = newDuration
                )
                recordingDao.updateRecording(updated)
                _uiState.update {
                    it.copy(
                        isConvertingFormat = false,
                        convertedAudioFile = newFile,
                        statusMessage = "Recording converted to ${targetFormat.label} format!"
                    )
                }
                playerManager.playFile(newFile, viewModelScope)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isConvertingFormat = false,
                        statusMessage = "Failed to convert recording: ${error.message}"
                    )
                }
            }
        }
    }

    fun clearNotice() {
        _uiState.update { it.copy(userNotice = null) }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
        voiceTransformer.release()
    }
}

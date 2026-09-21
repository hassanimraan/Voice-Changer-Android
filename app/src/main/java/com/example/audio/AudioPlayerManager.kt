package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream

class AudioPlayerManager(private val context: Context) {

    private val tag = "AudioPlayerManager"

    // Primary player: Android MediaPlayer
    private var mediaPlayer: MediaPlayer? = null
    private var currentFis: FileInputStream? = null

    // Fallback player: Direct AudioTrack for PCM / WAV
    private var audioTrack: AudioTrack? = null
    private var audioTrackJob: Job? = null
    private var activeWavInfo: WavUtils.WavInfo? = null
    private var audioTrackOffset = 0
    private var isAudioTrackPaused = false

    private var progressJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _activeFilePath = MutableStateFlow<String?>(null)
    val activeFilePath: StateFlow<String?> = _activeFilePath.asStateFlow()

    fun playFile(file: File, coroutineScope: CoroutineScope, onComplete: (() -> Unit)? = null) {
        if (!file.exists() || file.length() == 0L) {
            Log.w(tag, "Audio file does not exist or is empty: ${file.absolutePath}")
            return
        }

        stop()
        _activeFilePath.value = file.absolutePath

        // Attempt 1: Android MediaPlayer via FileDescriptor (works for MP3, M4A, AAC, standard WAV)
        try {
            val fis = FileInputStream(file)
            currentFis = fis
            val player = MediaPlayer()

            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )

            // Using FileDescriptor + offset + length avoids Android internal path permission issues
            player.setDataSource(fis.fd, 0, file.length())

            player.setOnErrorListener { _, what, extra ->
                Log.w(tag, "MediaPlayer error occurred: what=$what, extra=$extra. Falling back to AudioTrack...")
                stopMediaPlayerOnly()
                playViaAudioTrack(file, coroutineScope, onComplete)
                true
            }

            player.prepare()

            val duration = player.duration.toLong().coerceAtLeast(1L)
            _durationMs.value = duration
            _currentPositionMs.value = 0L

            player.setOnCompletionListener {
                _isPlaying.value = false
                _currentPositionMs.value = 0L
                progressJob?.cancel()
                onComplete?.invoke()
            }

            player.start()
            mediaPlayer = player
            _isPlaying.value = true

            progressJob = coroutineScope.launch(Dispatchers.Main) {
                while (isActive && _isPlaying.value) {
                    try {
                        val pos = mediaPlayer?.currentPosition?.toLong() ?: 0L
                        _currentPositionMs.value = pos
                    } catch (e: Exception) {
                        break
                    }
                    delay(50)
                }
            }
            Log.d(tag, "MediaPlayer started playing successfully: ${file.name}")
            return
        } catch (e: Exception) {
            Log.w(tag, "MediaPlayer prepare failed (${e.message}). Switching to guaranteed AudioTrack fallback...", e)
            stopMediaPlayerOnly()
        }

        // Attempt 2: Guaranteed AudioTrack fallback (reads raw PCM directly, zero permissions/mediaserver issues)
        playViaAudioTrack(file, coroutineScope, onComplete)
    }

    private fun playViaAudioTrack(
        file: File,
        coroutineScope: CoroutineScope,
        onComplete: (() -> Unit)?
    ) {
        val wavInfo = WavUtils.parseWav(file)
        if (wavInfo == null || wavInfo.pcmData.isEmpty()) {
            Log.e(tag, "AudioTrack fallback failed: unable to parse PCM data from ${file.name}")
            _isPlaying.value = false
            return
        }

        activeWavInfo = wavInfo
        audioTrackOffset = 0
        isAudioTrackPaused = false

        val sampleRate = wavInfo.sampleRate
        val channels = wavInfo.channels
        val bitsPerSample = wavInfo.bitsPerSample

        val channelConfig = if (channels == 2) AudioFormat.CHANNEL_OUT_STEREO else AudioFormat.CHANNEL_OUT_MONO
        val audioEncoding = if (bitsPerSample == 8) AudioFormat.ENCODING_PCM_8BIT else AudioFormat.ENCODING_PCM_16BIT

        val minBufSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioEncoding).coerceAtLeast(4096)

        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioEncoding)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(minBufSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack = track
            _durationMs.value = wavInfo.durationMs.coerceAtLeast(100L)
            _currentPositionMs.value = 0L
            _isPlaying.value = true

            track.play()

            val bytesPerSample = (bitsPerSample / 8) * channels
            val bytesPerMs = (sampleRate * bytesPerSample) / 1000

            audioTrackJob = coroutineScope.launch(Dispatchers.IO) {
                val pcm = wavInfo.pcmData
                val chunkSize = minBufSize

                try {
                    while (isActive && audioTrackOffset < pcm.size && _isPlaying.value) {
                        if (isAudioTrackPaused) {
                            delay(50)
                            continue
                        }

                        val bytesToWrite = (pcm.size - audioTrackOffset).coerceAtMost(chunkSize)
                        val written = track.write(pcm, audioTrackOffset, bytesToWrite)
                        if (written > 0) {
                            audioTrackOffset += written
                            if (bytesPerMs > 0) {
                                _currentPositionMs.value = (audioTrackOffset / bytesPerMs).toLong()
                            }
                        } else {
                            break
                        }
                    }
                } catch (e: Exception) {
                    Log.e(tag, "AudioTrack streaming error", e)
                } finally {
                    if (audioTrackOffset >= pcm.size) {
                        // Playback completed naturally
                        _isPlaying.value = false
                        _currentPositionMs.value = 0L
                        launch(Dispatchers.Main) {
                            onComplete?.invoke()
                        }
                    }
                }
            }
            Log.d(tag, "AudioTrack started playing successfully: ${file.name}")
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize AudioTrack", e)
            _isPlaying.value = false
        }
    }

    fun pause() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                _isPlaying.value = false
            } else if (audioTrack != null && _isPlaying.value) {
                isAudioTrackPaused = true
                audioTrack?.pause()
                _isPlaying.value = false
            }
        } catch (e: Exception) {
            Log.e(tag, "Error pausing", e)
        }
    }

    fun resume() {
        try {
            if (mediaPlayer != null && !_isPlaying.value) {
                mediaPlayer?.start()
                _isPlaying.value = true
            } else if (audioTrack != null && !_isPlaying.value) {
                isAudioTrackPaused = false
                audioTrack?.play()
                _isPlaying.value = true
            }
        } catch (e: Exception) {
            Log.e(tag, "Error resuming", e)
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer?.seekTo(positionMs.toInt())
                _currentPositionMs.value = positionMs
            } else if (audioTrack != null && activeWavInfo != null) {
                val wav = activeWavInfo!!
                val bytesPerSample = (wav.bitsPerSample / 8) * wav.channels
                val bytesPerMs = (wav.sampleRate * bytesPerSample) / 1000
                if (bytesPerMs > 0) {
                    val newOffset = (positionMs * bytesPerMs).toInt().coerceIn(0, wav.pcmData.size)
                    audioTrackOffset = newOffset
                    _currentPositionMs.value = positionMs
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error seeking", e)
        }
    }

    private fun stopMediaPlayerOnly() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.stop()
                }
                mediaPlayer?.reset()
                mediaPlayer?.release()
                mediaPlayer = null
            }
        } catch (e: Exception) {
            Log.e(tag, "Error releasing MediaPlayer", e)
        }
        try {
            currentFis?.close()
            currentFis = null
        } catch (e: Exception) {}
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null

        audioTrackJob?.cancel()
        audioTrackJob = null

        stopMediaPlayerOnly()

        try {
            if (audioTrack != null) {
                audioTrack?.stop()
                audioTrack?.release()
                audioTrack = null
            }
        } catch (e: Exception) {
            Log.e(tag, "Error releasing AudioTrack", e)
        }

        activeWavInfo = null
        audioTrackOffset = 0
        isAudioTrackPaused = false
        _isPlaying.value = false
        _currentPositionMs.value = 0L
    }

    fun release() {
        stop()
    }
}

package com.example.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

class AudioRecorderManager(private val context: Context) {

    private val tag = "AudioRecorderManager"

    private var audioRecord: AudioRecord? = null
    private var isRecordingInternal = false
    private var recordingJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

    val sampleRate = 44100
    val channelConfig = AudioFormat.CHANNEL_IN_MONO
    val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    fun startRecording(outputFile: File, coroutineScope: CoroutineScope): Boolean {
        if (isRecordingInternal) return false

        try {
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(4096)
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(tag, "AudioRecord initialization failed")
                return false
            }

            audioRecord?.startRecording()
            isRecordingInternal = true
            _isRecording.value = true
            _recordingDurationMs.value = 0L

            recordingJob = coroutineScope.launch(Dispatchers.IO) {
                val tempBuffer = ShortArray(bufferSize / 2)
                val fos = FileOutputStream(outputFile)
                // Write placeholder WAV header using WavUtils
                WavUtils.writeHeaderToStream(fos, sampleRate, 1, 16, 0)

                var totalAudioBytes = 0
                val startTime = System.currentTimeMillis()

                try {
                    while (isActive && isRecordingInternal) {
                        val readShorts = audioRecord?.read(tempBuffer, 0, tempBuffer.size) ?: 0
                        if (readShorts > 0) {
                            var maxSample = 0
                            val byteBuffer = ByteBuffer.allocate(readShorts * 2).order(ByteOrder.LITTLE_ENDIAN)
                            for (i in 0 until readShorts) {
                                val s = tempBuffer[i]
                                byteBuffer.putShort(s)
                                val absS = Math.abs(s.toInt())
                                if (absS > maxSample) maxSample = absS
                            }
                            fos.write(byteBuffer.array())
                            totalAudioBytes += readShorts * 2

                            // Normalize amplitude 0..1
                            val normAmp = (maxSample / 32767f).coerceIn(0f, 1f)
                            _amplitude.value = normAmp
                            _recordingDurationMs.value = System.currentTimeMillis() - startTime
                        }
                    }
                } finally {
                    try {
                        fos.flush()
                        fos.close()
                    } catch (e: Exception) {
                        Log.e(tag, "Error closing file stream", e)
                    }
                    // Fix WAV header with total file size
                    WavUtils.updateWavHeaderSize(outputFile, totalAudioBytes)
                }
            }
            return true
        } catch (e: SecurityException) {
            Log.e(tag, "Permission not granted to record audio", e)
            return false
        } catch (e: Exception) {
            Log.e(tag, "Failed to start recording", e)
            return false
        }
    }

    fun stopRecording(): Long {
        if (!isRecordingInternal) return 0L
        isRecordingInternal = false
        _isRecording.value = false
        _amplitude.value = 0f

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(tag, "Error stopping audioRecord", e)
        }
        audioRecord = null
        recordingJob?.cancel()
        recordingJob = null
        return _recordingDurationMs.value
    }

    /**
     * Generates a sample vocal audio clip (speech melody simulation) for immediate testing.
     * Extremely useful for testing in environments/emulators without mic access.
     */
    fun generatePresetSampleVoice(outputFile: File): File {
        val durationSeconds = 3.5
        val numSamples = (sampleRate * durationSeconds).toInt()
        val buffer = ShortArray(numSamples)

        // Generate vocal-like formant frequencies (F0 base ~140Hz with harmonic overtones and speech cadences)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val f0 = 135.0 + 20.0 * sin(2.0 * Math.PI * 1.5 * t) // pitch inflection
            val f1 = 500.0 // first vowel formant
            val f2 = 1500.0 // second vowel formant

            val wave = (0.5 * sin(2.0 * Math.PI * f0 * t)
                    + 0.3 * sin(2.0 * Math.PI * 2 * f0 * t)
                    + 0.15 * sin(2.0 * Math.PI * f1 * t)
                    + 0.05 * sin(2.0 * Math.PI * f2 * t))

            // Speech envelope / syllables
            val cadence = (0.5 + 0.5 * sin(2.0 * Math.PI * 3.0 * t)).coerceIn(0.1, 1.0)
            val sampleVal = (wave * cadence * 22000).toInt().coerceIn(-32767, 32767)
            buffer[i] = sampleVal.toShort()
        }

        val dataSize = numSamples * 2
        val byteBuffer = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
        for (s in buffer) {
            byteBuffer.putShort(s)
        }

        WavUtils.writeStandardWav(outputFile, sampleRate, 1, 16, byteBuffer.array())
        return outputFile
    }

    private fun writeWavHeader(
        out: FileOutputStream,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int,
        audioDataLength: Int
    ) {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()
        val totalDataLen = if (audioDataLength > 0) audioDataLength + 36 else 36

        val header = ByteArray(44)
        // RIFF/WAVE header
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        // 'fmt ' chunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 4 bytes: size of 'fmt ' chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // format = 1 (PCM)
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (blockAlign.toInt() and 0xff).toByte()
        header[33] = ((blockAlign.toInt() shr 8) and 0xff).toByte()
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        // 'data' chunk
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (audioDataLength and 0xff).toByte()
        header[41] = ((audioDataLength shr 8) and 0xff).toByte()
        header[42] = ((audioDataLength shr 16) and 0xff).toByte()
        header[43] = ((audioDataLength shr 24) and 0xff).toByte()

        out.write(header, 0, 44)
    }

    private fun updateWavHeader(file: File, totalAudioBytes: Int) {
        try {
            val raf = RandomAccessFile(file, "rw")
            val totalDataLen = totalAudioBytes + 36

            raf.seek(4)
            raf.write(
                byteArrayOf(
                    (totalDataLen and 0xff).toByte(),
                    ((totalDataLen shr 8) and 0xff).toByte(),
                    ((totalDataLen shr 16) and 0xff).toByte(),
                    ((totalDataLen shr 24) and 0xff).toByte()
                )
            )

            raf.seek(40)
            raf.write(
                byteArrayOf(
                    (totalAudioBytes and 0xff).toByte(),
                    ((totalAudioBytes shr 8) and 0xff).toByte(),
                    ((totalAudioBytes shr 16) and 0xff).toByte(),
                    ((totalAudioBytes shr 24) and 0xff).toByte()
                )
            )
            raf.close()
        } catch (e: Exception) {
            Log.e(tag, "Failed to update WAV header", e)
        }
    }
}

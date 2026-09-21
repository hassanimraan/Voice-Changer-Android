package com.example.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.model.Personality
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

class VoiceTransformerDsp(private val context: Context) {

    private val tag = "VoiceTransformerDsp"
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    init {
        initTts()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()
                isTtsReady = true
                Log.d(tag, "TextToSpeech initialized successfully")
            } else {
                Log.w(tag, "TextToSpeech initialization failed: $status")
            }
        }
    }

    /**
     * Transforms an input WAV file using Digital Signal Processing (DSP):
     * - Time-domain Pitch Shift / Resampling with interpolation
     * - Reverb / Delay feedback
     * - Low-frequency Bass Boost & Resonance filter
     */
    suspend fun transformAudio(
        inputFile: File,
        outputFile: File,
        pitchMultiplier: Float,
        speedMultiplier: Float,
        reverbAmount: Float,
        bassBoost: Float
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (!inputFile.exists() || inputFile.length() == 0L) {
                return@withContext Result.failure(IllegalArgumentException("Invalid or empty input audio file"))
            }

            val wavInfo = WavUtils.parseWav(inputFile)
                ?: return@withContext Result.failure(IllegalStateException("Unable to read audio samples from input file"))

            val channels = wavInfo.channels.coerceIn(1, 2)
            val sampleRate = wavInfo.sampleRate
            val bitsPerSample = wavInfo.bitsPerSample
            val rawDataBytes = wavInfo.pcmData

            if (rawDataBytes.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("No audio samples in file"))
            }

            // Parse 16-bit PCM samples
            val numSamples = rawDataBytes.size / 2
            val originalSamples = ShortArray(numSamples)
            val inByteBuffer = ByteBuffer.wrap(rawDataBytes).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until numSamples) {
                originalSamples[i] = inByteBuffer.short
            }

            // 1. Pitch Shift via Linear Interpolation Resampling + Segment Matching
            // Pitch ratio: > 1.0 = higher pitch (Mohammad Rafi / Taylor Swift), < 1.0 = deeper pitch (Imran Khan / Morgan Freeman)
            val pitchRatio = pitchMultiplier.coerceIn(0.5f, 2.0f)
            val speedRatio = speedMultiplier.coerceIn(0.6f, 1.5f)

            // Calculate output length based on pitch & speed
            val resampleRatio = (1.0 / pitchRatio) * (1.0 / speedRatio)
            val outputLength = (numSamples * resampleRatio).toInt().coerceAtLeast(100)
            val processedSamples = FloatArray(outputLength)

            for (i in 0 until outputLength) {
                val srcIdx = i / resampleRatio
                val idx0 = srcIdx.toInt().coerceIn(0, numSamples - 1)
                val idx1 = (idx0 + 1).coerceIn(0, numSamples - 1)
                val frac = (srcIdx - idx0).toFloat()

                // Linear interpolation between consecutive samples
                val s0 = originalSamples[idx0].toFloat()
                val s1 = originalSamples[idx1].toFloat()
                processedSamples[i] = s0 + frac * (s1 - s0)
            }

            // 2. Low-Frequency Bass Resonance Boost (for Baritone / Leader voices like Imran Khan & Amitabh Bachchan)
            if (bassBoost > 0.05f) {
                var prevSample = 0f
                val alpha = (0.25f + bassBoost * 0.45f).coerceIn(0.1f, 0.85f)
                for (i in processedSamples.indices) {
                    val current = processedSamples[i]
                    val lowPass = prevSample + alpha * (current - prevSample)
                    prevSample = lowPass
                    // Blend original with boosted low frequencies
                    processedSamples[i] = current + (lowPass * bassBoost * 1.4f)
                }
            }

            // 3. Reverb / Ambient Echo Comb Filter (for Singer / Studio acoustics like Mohammad Rafi)
            if (reverbAmount > 0.05f) {
                val delaySamples = (sampleRate * 0.065f).toInt() // 65ms room reflection
                val feedback = (reverbAmount * 0.55f).coerceIn(0.05f, 0.65f)
                val delayBuffer = FloatArray(delaySamples)
                var delayIndex = 0

                for (i in processedSamples.indices) {
                    val input = processedSamples[i]
                    val delayed = delayBuffer[delayIndex]
                    val output = input + delayed * feedback

                    delayBuffer[delayIndex] = input + delayed * (feedback * 0.7f)
                    delayIndex = (delayIndex + 1) % delaySamples

                    processedSamples[i] = (input * (1f - reverbAmount * 0.3f)) + (output * reverbAmount * 0.5f)
                }
            }

            // 4. Normalize & Clamp to 16-bit PCM Short
            var maxAmp = 1f
            for (s in processedSamples) {
                val abs = Math.abs(s)
                if (abs > maxAmp) maxAmp = abs
            }

            val scale = if (maxAmp > 32700f) 32700f / maxAmp else 1.0f
            val outByteBuffer = ByteBuffer.allocate(outputLength * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until outputLength) {
                val sampleValue = (processedSamples[i] * scale).toInt().coerceIn(-32767, 32767).toShort()
                outByteBuffer.putShort(sampleValue)
            }

            // Write output WAV using WavUtils for clean standard canonical structure
            WavUtils.writeStandardWav(
                outputFile = outputFile,
                sampleRate = sampleRate,
                channels = channels.coerceAtLeast(1),
                bitsPerSample = bitsPerSample.coerceAtLeast(16),
                pcmData = outByteBuffer.array()
            )

            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(tag, "Audio transformation failed", e)
            Result.failure(e)
        }
    }

    /**
     * Synthesizes text directly into a target celebrity speech WAV audio file using Android TTS
     * with tailored vocal pitch and cadence.
     */
    suspend fun synthesizeSpeechToFile(
        text: String,
        outputFile: File,
        personality: Personality
    ): Result<File> = withContext(Dispatchers.IO) {
        val tts = textToSpeech
        if (tts == null || !isTtsReady) {
            return@withContext Result.failure(IllegalStateException("TextToSpeech not ready yet"))
        }

        try {
            val deferred = CompletableDeferred<Boolean>()
            val utteranceId = "synth_${System.currentTimeMillis()}"

            tts.setPitch(personality.pitchMultiplier.coerceIn(0.5f, 2.0f))
            tts.setSpeechRate(personality.speedMultiplier.coerceIn(0.6f, 1.5f))

            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    deferred.complete(true)
                }
                override fun onError(utteranceId: String?) {
                    deferred.complete(false)
                }
            })

            val params = Bundle()
            val result = tts.synthesizeToFile(text, params, outputFile, utteranceId)
            if (result == TextToSpeech.SUCCESS) {
                val success = deferred.await()
                if (success && outputFile.exists() && outputFile.length() > 0) {
                    Result.success(outputFile)
                } else {
                    Result.failure(IllegalStateException("TTS synthesis failed to create audio file"))
                }
            } else {
                Result.failure(IllegalStateException("TTS synthesizeToFile returned error code $result"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error in synthesizeSpeechToFile", e)
            Result.failure(e)
        }
    }

    fun speak(text: String, personality: Personality) {
        val tts = textToSpeech ?: return
        tts.setPitch(personality.pitchMultiplier)
        tts.setSpeechRate(personality.speedMultiplier)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "speak_${System.currentTimeMillis()}")
    }

    fun release() {
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
        } catch (e: Exception) {
            Log.e(tag, "Error shutting down TTS", e)
        }
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
        val totalDataLen = audioDataLength + 36

        val header = ByteArray(44)
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
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1
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
}

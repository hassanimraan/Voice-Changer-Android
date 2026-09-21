package com.example.network

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.Personality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class GeminiVoiceService(private val context: Context) {

    private val tag = "GeminiVoiceService"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun isApiKeyConfigured(): Boolean {
        val key = getApiKey()
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    data class ConversionResult(
        val convertedAudioFile: File?,
        val transcribedText: String,
        val personalitySpeechText: String,
        val audioMimeType: String? = null
    )

    /**
     * Converts recorded audio sample to target personality voice using Gemini.
     */
    suspend fun convertVoiceWithGemini(
        inputAudioFile: File,
        targetPersonality: Personality,
        outputAudioFile: File
    ): Result<ConversionResult> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Gemini API Key is not configured. Please set GEMINI_API_KEY in the Secrets panel.")
            )
        }

        try {
            val audioBytes = inputAudioFile.readBytes()
            val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

            val prompt = """
                You are a renowned voice and personality transformation engine.
                The user has provided an audio recording.
                1. Accurately transcribe what the user said in the audio.
                2. Re-interpret and adapt what was said into the authentic vocal style, diction, phrasing, and mannerisms of: ${targetPersonality.name} (${targetPersonality.title}).
                Style instructions: ${targetPersonality.geminiStylePrompt}
                
                Respond strictly in JSON with format:
                {
                    "transcribedText": "exact user speech or melody summary",
                    "personalitySpeechText": "the speech rewritten as ${targetPersonality.name} would say/sing it"
                }
            """.trimIndent()

            val contentsArray = JSONArray().apply {
                val partsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", prompt)
                    })
                    put(JSONObject().apply {
                        put("inlineData", JSONObject().apply {
                            put("mimeType", "audio/wav")
                            put("data", base64Audio)
                        })
                    })
                }
                put(JSONObject().apply {
                    put("parts", partsArray)
                })
            }

            val requestJson = JSONObject().apply {
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.7)
                })
            }

            // Using modern gemini-3.5-flash as per skill guidelines
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            val httpRequest = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(httpRequest).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(tag, "Gemini API error code: ${response.code} body: $responseBody")
                return@withContext Result.failure(
                    Exception("Gemini API call failed (${response.code}): $responseBody")
                )
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val textPart = parts?.optJSONObject(0)?.optString("text", "") ?: ""

            var transcribed = ""
            var personalityText = ""

            try {
                val parsedContent = JSONObject(textPart)
                transcribed = parsedContent.optString("transcribedText", "")
                personalityText = parsedContent.optString("personalitySpeechText", "")
            } catch (e: Exception) {
                personalityText = textPart
            }

            // Next, attempt to generate speech audio via Gemini TTS
            val audioResultFile = generateAudioWithGeminiTts(personalityText, targetPersonality, outputAudioFile, apiKey)

            Result.success(
                ConversionResult(
                    convertedAudioFile = audioResultFile,
                    transcribedText = transcribed,
                    personalitySpeechText = personalityText,
                    audioMimeType = if (audioResultFile != null) "audio/wav" else null
                )
            )
        } catch (e: Exception) {
            Log.e(tag, "Gemini voice conversion failed", e)
            Result.failure(e)
        }
    }

    /**
     * Attempts to generate speech using Gemini's audio response modalities.
     */
    private suspend fun generateAudioWithGeminiTts(
        textToSpeak: String,
        personality: Personality,
        outputFile: File,
        apiKey: String
    ): File? = withContext(Dispatchers.IO) {
        if (textToSpeak.isBlank()) return@withContext null
        try {
            // Select voice persona: Puck, Charon, Kore, Fenrir, Aoede
            val voiceName = when {
                personality.pitchMultiplier > 1.15f -> "Kore"
                personality.pitchMultiplier < 0.85f -> "Fenrir"
                else -> "Puck"
            }

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "Perform this in character as ${personality.name} with natural pacing: $textToSpeak")
                        })
                    }
                    put(JSONObject().apply {
                        put("parts", parts)
                    })
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().apply {
                        put("AUDIO")
                    })
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", voiceName)
                            })
                        })
                    })
                })
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-tts:generateContent?key=$apiKey"
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val httpRequest = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(mediaType))
                .build()

            val response = okHttpClient.newCall(httpRequest).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(body)
                val candidates = json.optJSONArray("candidates")
                val parts = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
                for (i in 0 until (parts?.length() ?: 0)) {
                    val part = parts?.optJSONObject(i)
                    val inlineData = part?.optJSONObject("inlineData")
                    if (inlineData != null) {
                        val base64Data = inlineData.optString("data", "")
                        if (base64Data.isNotBlank()) {
                            val decodedAudio = Base64.decode(base64Data, Base64.DEFAULT)
                            val hasRiff = decodedAudio.size >= 12 &&
                                    decodedAudio[0] == 'R'.code.toByte() &&
                                    decodedAudio[1] == 'I'.code.toByte() &&
                                    decodedAudio[2] == 'F'.code.toByte() &&
                                    decodedAudio[3] == 'F'.code.toByte()
                            val hasId3 = decodedAudio.size >= 3 &&
                                    decodedAudio[0] == 'I'.code.toByte() &&
                                    decodedAudio[1] == 'D'.code.toByte() &&
                                    decodedAudio[2] == '3'.code.toByte()

                            if (hasRiff || hasId3) {
                                val fos = FileOutputStream(outputFile)
                                fos.write(decodedAudio)
                                fos.flush()
                                fos.close()
                            } else {
                                // Raw 24kHz PCM from Gemini: wrap with standard WAV header
                                com.example.audio.WavUtils.writeStandardWav(
                                    outputFile = outputFile,
                                    sampleRate = 24000,
                                    channels = 1,
                                    bitsPerSample = 16,
                                    pcmData = decodedAudio
                                )
                            }
                            return@withContext outputFile
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Gemini direct TTS audio generation not available, falling back to local synthesis: ${e.message}")
        }
        null
    }
}

package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_recordings")
data class VoiceRecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val personalityId: String,
    val personalityName: String,
    val originalFilePath: String,
    val convertedFilePath: String,
    val durationMs: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val transformationMode: String, // "AI Neural Voice" or "Acoustic DSP Morph"
    val transcriptionText: String = "",
    val convertedSpeechText: String = "",
    val pitchMultiplier: Float = 1.0f,
    val speedMultiplier: Float = 1.0f
)

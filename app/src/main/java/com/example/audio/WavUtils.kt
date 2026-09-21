package com.example.audio

import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavUtils {

    data class WavInfo(
        val sampleRate: Int,
        val channels: Int,
        val bitsPerSample: Int,
        val pcmData: ByteArray
    ) {
        val durationMs: Long
            get() {
                val bytesPerSec = sampleRate.toLong() * channels * (bitsPerSample / 8)
                return if (bytesPerSec > 0) (pcmData.size * 1000L) / bytesPerSec else 0L
            }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as WavInfo
            return sampleRate == other.sampleRate &&
                    channels == other.channels &&
                    bitsPerSample == other.bitsPerSample &&
                    pcmData.contentEquals(other.pcmData)
        }

        override fun hashCode(): Int {
            var result = sampleRate
            result = 31 * result + channels
            result = 31 * result + bitsPerSample
            result = 31 * result + pcmData.contentHashCode()
            return result
        }
    }

    /**
     * Parses a WAV file, accurately locating 'fmt ' and 'data' chunks regardless of
     * extra chunks like fact, LIST, PEAK, or metadata injected by TTS engines.
     * Also gracefully handles raw PCM without RIFF header.
     */
    fun parseWav(file: File): WavInfo? {
        if (!file.exists() || file.length() == 0L) return null
        val bytes = try {
            file.readBytes()
        } catch (e: Exception) {
            return null
        }
        if (bytes.isEmpty()) return null

        // Check for RIFF header
        val isRiff = bytes.size >= 12 &&
                bytes[0] == 'R'.code.toByte() &&
                bytes[1] == 'I'.code.toByte() &&
                bytes[2] == 'F'.code.toByte() &&
                bytes[3] == 'F'.code.toByte() &&
                bytes[8] == 'W'.code.toByte() &&
                bytes[9] == 'A'.code.toByte() &&
                bytes[10] == 'V'.code.toByte() &&
                bytes[11] == 'E'.code.toByte()

        if (!isRiff) {
            // Raw PCM data without header (e.g. from Gemini audio modality or TTS raw stream)
            return WavInfo(
                sampleRate = 44100,
                channels = 1,
                bitsPerSample = 16,
                pcmData = bytes
            )
        }

        var sampleRate = 44100
        var channels = 1
        var bitsPerSample = 16
        var dataOffset = -1
        var dataSize = 0

        var index = 12
        while (index + 8 <= bytes.size) {
            val chunkId = String(bytes, index, 4, Charsets.US_ASCII)
            val chunkSize = (bytes[index + 4].toInt() and 0xFF) or
                    ((bytes[index + 5].toInt() and 0xFF) shl 8) or
                    ((bytes[index + 6].toInt() and 0xFF) shl 16) or
                    ((bytes[index + 7].toInt() and 0xFF) shl 24)

            if (chunkId == "fmt ") {
                if (chunkSize >= 16 && index + 8 + 16 <= bytes.size) {
                    channels = (bytes[index + 10].toInt() and 0xFF) or
                            ((bytes[index + 11].toInt() and 0xFF) shl 8)
                    sampleRate = (bytes[index + 12].toInt() and 0xFF) or
                            ((bytes[index + 13].toInt() and 0xFF) shl 8) or
                            ((bytes[index + 14].toInt() and 0xFF) shl 16) or
                            ((bytes[index + 15].toInt() and 0xFF) shl 24)
                    bitsPerSample = (bytes[index + 22].toInt() and 0xFF) or
                            ((bytes[index + 23].toInt() and 0xFF) shl 8)
                }
            } else if (chunkId == "data") {
                dataOffset = index + 8
                dataSize = if (chunkSize > 0) chunkSize.coerceAtMost(bytes.size - dataOffset) else bytes.size - dataOffset
                break
            }

            index += 8 + chunkSize.coerceAtLeast(0)
            // WAV chunks are word-aligned to 2 bytes
            if (chunkSize % 2 != 0) index += 1
        }

        if (dataOffset == -1 || dataOffset >= bytes.size) {
            dataOffset = 44.coerceAtMost(bytes.size)
            dataSize = bytes.size - dataOffset
        }

        val actualDataSize = dataSize.coerceAtLeast(0).coerceAtMost(bytes.size - dataOffset)
        val pcm = bytes.copyOfRange(dataOffset, dataOffset + actualDataSize)

        return WavInfo(
            sampleRate = if (sampleRate in 8000..96000) sampleRate else 44100,
            channels = if (channels in 1..2) channels else 1,
            bitsPerSample = if (bitsPerSample == 8 || bitsPerSample == 16 || bitsPerSample == 24 || bitsPerSample == 32) bitsPerSample else 16,
            pcmData = pcm
        )
    }

    /**
     * Writes standard 44-byte RIFF/WAVE header + PCM samples to a clean file.
     */
    fun writeStandardWav(
        outputFile: File,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int,
        pcmData: ByteArray
    ) {
        val fos = FileOutputStream(outputFile)
        writeHeaderToStream(fos, sampleRate, channels, bitsPerSample, pcmData.size)
        fos.write(pcmData)
        fos.flush()
        fos.close()
    }

    /**
     * Writes 44-byte canonical WAV header into an output stream.
     */
    fun writeHeaderToStream(
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
        header[20] = 1 // PCM
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

    /**
     * Updates an existing WAV file's size fields in-place after recording completes.
     */
    fun updateWavHeaderSize(file: File, totalAudioBytes: Int) {
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
            // Ignored
        }
    }
}

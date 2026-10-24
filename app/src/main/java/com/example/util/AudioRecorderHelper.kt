package com.example.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

class AudioRecorderHelper(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    var isRecording: Boolean = false
        private set

    fun startRecording(): File? {
        try {
            val audioDir = File(context.cacheDir, "audio_notes").apply {
                if (!exists()) mkdirs()
            }
            val outputFile = File(audioDir, "hamed_voice_${System.currentTimeMillis()}.m4a")
            currentOutputFile = outputFile

            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            recorder = newRecorder
            isRecording = true
            return outputFile
        } catch (e: Exception) {
            Log.e("AudioRecorderHelper", "Failed to start audio recording", e)
            cleanup()
            return null
        }
    }

    fun stopRecording(): File? {
        if (!isRecording) return null
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            isRecording = false
            currentOutputFile
        } catch (e: Exception) {
            Log.e("AudioRecorderHelper", "Failed to stop recording cleanly", e)
            cleanup()
            null
        }
    }

    fun cancelRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {}
        cleanup()
    }

    private fun cleanup() {
        recorder = null
        isRecording = false
        try {
            currentOutputFile?.delete()
        } catch (_: Exception) {}
        currentOutputFile = null
    }

    fun getMaxAmplitude(): Int {
        return try {
            recorder?.maxAmplitude ?: 0
        } catch (_: Exception) {
            0
        }
    }
}

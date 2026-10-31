package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val TAG = "HamedAudioPlayer"

class HamedAudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val playerScope = kotlinx.coroutines.CoroutineScope(Dispatchers.Main + kotlinx.coroutines.SupervisorJob())
    private var playbackMonitorJob: kotlinx.coroutines.Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPlayingId = MutableStateFlow<String?>(null)
    val currentPlayingId: StateFlow<String?> = _currentPlayingId.asStateFlow()

    private var onCompletionCallback: (() -> Unit)? = null

    fun isMediaMuted(): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val currentVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 1
        return currentVolume == 0
    }

    fun getCachedAudioFile(messageId: String): File? {
        val file = File(context.cacheDir, "voice_${messageId}.wav")
        return if (file.exists() && file.length() > 44) file else null
    }

    suspend fun saveAndPrepareAudio(
        messageId: String,
        rawBytes: ByteArray,
        mimeType: String
    ): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "voice_${messageId}.wav")
        if (file.exists() && file.length() > 44) {
            return@withContext file
        }

        val wavBytes = if (isWavOrMp3(rawBytes, mimeType)) {
            rawBytes
        } else {
            // Raw PCM 24000Hz 16-bit mono -> prepend standard 44-byte WAV header
            createWavBytesFromPcm(
                pcmData = rawBytes,
                sampleRate = 24000,
                channels = 1,
                bitsPerSample = 16
            )
        }

        FileOutputStream(file).use { fos ->
            fos.write(wavBytes)
            fos.flush()
        }
        file
    }

    fun playAudioFile(
        file: File,
        messageId: String,
        playbackSpeed: Float = 1.0f,
        onDone: (() -> Unit)? = null
    ): Boolean {
        stop()

        return try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(context, Uri.fromFile(file))
                setOnPreparedListener { player ->
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            val clampedSpeed = playbackSpeed.coerceIn(0.5f, 2.0f)
                            player.playbackParams = player.playbackParams.setSpeed(clampedSpeed)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to set playback speed", e)
                    }
                    _isPlaying.value = true
                    _currentPlayingId.value = messageId
                    onCompletionCallback = onDone
                    player.start()

                    // Start automatic watchdog to guarantee icon resets to speaker immediately upon completion
                    startPlaybackWatchdog(player, playbackSpeed, onDone)
                }
                setOnCompletionListener {
                    stop()
                    onDone?.invoke()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                    stop()
                    onDone?.invoke()
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MediaPlayer for file ${file.absolutePath}", e)
            stop()
            false
        }
    }

    private fun startPlaybackWatchdog(player: MediaPlayer, speed: Float, onDone: (() -> Unit)?) {
        playbackMonitorJob?.cancel()
        playbackMonitorJob = playerScope.launch {
            val totalDurationMs = try { player.duration } catch (_: Exception) { 0 }
            val effectiveDurationMs = if (totalDurationMs > 0) {
                (totalDurationMs / speed.coerceIn(0.5f, 2.0f)).toLong() + 300L
            } else {
                30_000L
            }

            val startTime = System.currentTimeMillis()
            while (isActive) {
                delay(200L)
                val elapsed = System.currentTimeMillis() - startTime
                val isStillPlaying = try { player.isPlaying } catch (_: Exception) { false }
                val currentPos = try { player.currentPosition } catch (_: Exception) { 0 }

                // Check if playback reached the end
                val reachedEnd = (totalDurationMs > 0 && currentPos >= totalDurationMs - 120) ||
                        (!isStillPlaying && elapsed > 400L) ||
                        (elapsed >= effectiveDurationMs)

                if (reachedEnd) {
                    stop()
                    onDone?.invoke()
                    break
                }
            }
        }
    }

    fun stop() {
        playbackMonitorJob?.cancel()
        playbackMonitorJob = null

        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.reset()
                mp.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }

        _isPlaying.value = false
        _currentPlayingId.value = null
        onCompletionCallback = null
    }

    private fun isWavOrMp3(bytes: ByteArray, mimeType: String): Boolean {
        if (mimeType.contains("wav") || mimeType.contains("mp3") || mimeType.contains("mpeg")) {
            return true
        }
        if (bytes.size >= 4) {
            // Check for 'RIFF'
            if (bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
                bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte()
            ) {
                return true
            }
            // Check for 'ID3'
            if (bytes[0] == 'I'.code.toByte() && bytes[1] == 'D'.code.toByte() && bytes[2] == '3'.code.toByte()) {
                return true
            }
        }
        return false
    }

    private fun createWavBytesFromPcm(
        pcmData: ByteArray,
        sampleRate: Int,
        channels: Short,
        bitsPerSample: Short
    ): ByteArray {
        val totalAudioLen = pcmData.size
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()

        val header = ByteBuffer.allocate(44).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            // RIFF header
            put('R'.code.toByte())
            put('I'.code.toByte())
            put('F'.code.toByte())
            put('F'.code.toByte())
            putInt(totalDataLen)
            put('W'.code.toByte())
            put('A'.code.toByte())
            put('V'.code.toByte())
            put('E'.code.toByte())

            // "fmt " chunk
            put('f'.code.toByte())
            put('m'.code.toByte())
            put('t'.code.toByte())
            put(' '.code.toByte())
            putInt(16) // Subchunk1Size
            putShort(1) // AudioFormat 1 = PCM
            putShort(channels)
            putInt(sampleRate)
            putInt(byteRate)
            putShort(blockAlign)
            putShort(bitsPerSample)

            // "data" chunk
            put('d'.code.toByte())
            put('a'.code.toByte())
            put('t'.code.toByte())
            put('a'.code.toByte())
            putInt(totalAudioLen)
        }.array()

        val result = ByteArray(44 + totalAudioLen)
        System.arraycopy(header, 0, result, 0, 44)
        System.arraycopy(pcmData, 0, result, 44, totalAudioLen)
        return result
    }
}

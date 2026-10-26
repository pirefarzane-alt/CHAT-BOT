package com.example.util

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

private const val TAG = "HamedTtsHelper"

sealed interface SpeakResult {
    data object Success : SpeakResult
    data object Stopped : SpeakResult
    data object Initializing : SpeakResult
    data object MutedVolume : SpeakResult
    data class PersianVoiceMissing(val settingsIntent: Intent?) : SpeakResult
    data class Error(val message: String) : SpeakResult
}

class HamedTextToSpeechHelper(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    var isInitialized = false
        private set

    var isPersianVoiceAvailable = false
        private set

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentlySpeakingId = MutableStateFlow<String?>(null)
    val currentlySpeakingId: StateFlow<String?> = _currentlySpeakingId.asStateFlow()

    private var onSpeechDoneCallback: (() -> Unit)? = null

    init {
        initializeEngine()
    }

    private fun initializeEngine() {
        try {
            // Prioritize Google Speech Services if installed, as OEM engines (MIUI/Xiaomi) often lack Persian voice
            val googleTtsPackage = "com.google.android.tts"
            val hasGoogleTts = try {
                context.packageManager.getPackageInfo(googleTtsPackage, 0)
                true
            } catch (_: Exception) {
                false
            }

            tts = if (hasGoogleTts) {
                Log.d(TAG, "Initializing with Google TTS engine: $googleTtsPackage")
                TextToSpeech(context.applicationContext, this, googleTtsPackage)
            } else {
                Log.d(TAG, "Initializing with system default TTS engine")
                TextToSpeech(context.applicationContext, this)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to instantiate TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engine = tts ?: return
            isInitialized = true

            // Configure audio attributes for Media & Speech
            try {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                engine.setAudioAttributes(audioAttributes)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to set audio attributes on TTS", e)
            }

            // Check and select Persian voice
            setupPersianVoice(engine)

            engine.setPitch(1.0f)
            engine.setSpeechRate(0.92f)

            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    _currentlySpeakingId.value = null
                    onSpeechDoneCallback?.invoke()
                    onSpeechDoneCallback = null
                }

                override fun onError(utteranceId: String?) {
                    Log.w(TAG, "TTS Utterance error on ID: $utteranceId")
                    _isSpeaking.value = false
                    _currentlySpeakingId.value = null
                    onSpeechDoneCallback?.invoke()
                    onSpeechDoneCallback = null
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?, errorCode: Int) {
                    Log.w(TAG, "TTS Utterance error $errorCode on ID: $utteranceId")
                    _isSpeaking.value = false
                    _currentlySpeakingId.value = null
                    onSpeechDoneCallback?.invoke()
                    onSpeechDoneCallback = null
                }
            })
            Log.d(TAG, "TextToSpeech initialized successfully (Persian available: $isPersianVoiceAvailable)")
        } else {
            Log.e(TAG, "TextToSpeech initialization failed with status $status")
            isInitialized = false
        }
    }

    private fun setupPersianVoice(engine: TextToSpeech) {
        val faLocale = Locale("fa", "IR")
        val langResult = engine.setLanguage(faLocale)

        if (langResult == TextToSpeech.LANG_AVAILABLE || langResult == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
            isPersianVoiceAvailable = true
        } else {
            val fallbackResult = engine.setLanguage(Locale("fa"))
            if (fallbackResult == TextToSpeech.LANG_AVAILABLE || fallbackResult == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
                isPersianVoiceAvailable = true
            }
        }

        // Search engine voices directly
        try {
            val voices = engine.voices
            if (!voices.isNullOrEmpty()) {
                val bestVoice = voices.firstOrNull { it.locale.language == "fa" && !it.isNetworkConnectionRequired }
                    ?: voices.firstOrNull { it.locale.language == "fa" }

                if (bestVoice != null) {
                    engine.voice = bestVoice
                    isPersianVoiceAvailable = true
                    Log.d(TAG, "Selected Persian voice: ${bestVoice.name}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error querying engine voices", e)
        }
    }

    fun speak(
        text: String,
        messageId: String = UUID.randomUUID().toString(),
        onDone: (() -> Unit)? = null
    ): SpeakResult {
        val engine = tts
        if (!isInitialized || engine == null) {
            Log.w(TAG, "TTS not initialized yet")
            initializeEngine()
            return SpeakResult.Initializing
        }

        // If clicking the currently playing message, toggle/stop it
        if (_isSpeaking.value && _currentlySpeakingId.value == messageId) {
            stop()
            return SpeakResult.Stopped
        }

        stop()

        val cleanSpeechText = cleanTextForSpeech(text)
        if (cleanSpeechText.isBlank()) {
            return SpeakResult.Error("متنی برای خواندن یافت نشد.")
        }

        // Check if device media volume is muted (0)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val currentVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 1
        if (currentVolume == 0) {
            Log.w(TAG, "Media stream volume is 0 (muted)")
            // User can still proceed, but we warn about muted volume
        }

        onSpeechDoneCallback = onDone
        _currentlySpeakingId.value = messageId
        _isSpeaking.value = true

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, messageId)
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        val result = engine.speak(cleanSpeechText, TextToSpeech.QUEUE_FLUSH, params, messageId)
        return if (result == TextToSpeech.SUCCESS) {
            if (currentVolume == 0) {
                SpeakResult.MutedVolume
            } else {
                SpeakResult.Success
            }
        } else {
            Log.e(TAG, "engine.speak failed with code: $result")
            _isSpeaking.value = false
            _currentlySpeakingId.value = null

            if (!isPersianVoiceAvailable) {
                SpeakResult.PersianVoiceMissing(createTtsSettingsIntent())
            } else {
                SpeakResult.Error("موتور صوتی موفق به ادای متن نشد (کد: $result)")
            }
        }
    }

    fun createTtsSettingsIntent(): Intent {
        return Intent("com.android.settings.TTS_SETTINGS").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping TTS", e)
        }
        _isSpeaking.value = false
        _currentlySpeakingId.value = null
        onSpeechDoneCallback = null
    }

    fun shutdown() {
        stop()
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            Log.w(TAG, "Error shutting down TTS", e)
        }
        tts = null
        isInitialized = false
    }

    private fun cleanTextForSpeech(raw: String): String {
        return raw
            // Remove markdown formatting
            .replace(Regex("[*#_`~>]+"), " ")
            // Remove emojis & non-Persian special symbols
            .replace(Regex("[\\p{So}\\p{Cn}]"), "")
            // Remove tag-like markers [احساس: ...] or [نکته: ...]
            .replace(Regex("\\[.*?\\]"), "")
            // Collapse multiple spaces
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

package com.example.util

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

private const val TAG = "HamedTextToSpeechHelper"

class HamedTextToSpeechHelper(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentlySpeakingId = MutableStateFlow<String?>(null)
    val currentlySpeakingId: StateFlow<String?> = _currentlySpeakingId.asStateFlow()

    private var onSpeechDoneCallback: (() -> Unit)? = null

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to instantiate TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engine = tts ?: return
            // Try Persian locale, fallback to Iranian locale or default
            val faLocale = Locale("fa", "IR")
            val result = engine.setLanguage(faLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                val fallbackFa = Locale("fa")
                val fallbackResult = engine.setLanguage(fallbackFa)
                if (fallbackResult == TextToSpeech.LANG_MISSING_DATA || fallbackResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "Persian TTS language not fully supported on device, using default locale")
                    engine.language = Locale.getDefault()
                }
            }
            engine.setPitch(1.0f)
            engine.setSpeechRate(0.95f)

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
            isInitialized = true
            Log.d(TAG, "TextToSpeech initialized successfully")
        } else {
            Log.e(TAG, "TextToSpeech initialization failed with status $status")
            isInitialized = false
        }
    }

    fun speak(
        text: String,
        messageId: String = UUID.randomUUID().toString(),
        onDone: (() -> Unit)? = null
    ) {
        val engine = tts
        if (!isInitialized || engine == null) {
            Log.w(TAG, "TTS not initialized yet")
            onDone?.invoke()
            return
        }

        stop()

        val cleanSpeechText = cleanTextForSpeech(text)
        if (cleanSpeechText.isBlank()) {
            onDone?.invoke()
            return
        }

        onSpeechDoneCallback = onDone
        _currentlySpeakingId.value = messageId
        _isSpeaking.value = true

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, messageId)
        }

        engine.speak(cleanSpeechText, TextToSpeech.QUEUE_FLUSH, params, messageId)
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
            // Remove emojis & special symbols
            .replace(Regex("[\\p{So}\\p{Cn}]"), "")
            // Remove tag-like markers [احساس: ...] or [نکته: ...]
            .replace(Regex("\\[.*?\\]"), "")
            // Collapse multiple spaces
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

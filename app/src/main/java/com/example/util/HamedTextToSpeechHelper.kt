package com.example.util

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

private const val TAG = "HamedTtsHelper"
private const val PREFS_NAME = "hamed_voice_prefs"
private const val KEY_VOICE_NAME = "key_voice_name"
private const val KEY_SPEECH_RATE = "key_speech_rate"
private const val KEY_SPEECH_PITCH = "key_speech_pitch"
private const val KEY_CLOUD_VOICE = "key_cloud_voice"

data class TtsVoiceInfo(
    val name: String,
    val displayName: String,
    val isNetworkRequired: Boolean,
    val isFemale: Boolean
)

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
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isInitialized = false
        private set

    var isPersianVoiceAvailable = false
        private set

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentlySpeakingId = MutableStateFlow<String?>(null)
    val currentlySpeakingId: StateFlow<String?> = _currentlySpeakingId.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<TtsVoiceInfo>>(emptyList())
    val availableVoices: StateFlow<List<TtsVoiceInfo>> = _availableVoices.asStateFlow()

    private var onSpeechDoneCallback: (() -> Unit)? = null

    init {
        initializeEngine()
    }

    fun isMediaMuted(): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val currentVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 1
        return currentVolume == 0
    }

    private fun initializeEngine() {
        try {
            // Initialize with the standard system TTS service
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to instantiate TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engine = tts ?: return
            isInitialized = true

            // Configure audio attributes for media and speech
            try {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                engine.setAudioAttributes(audioAttributes)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to set audio attributes on TTS", e)
            }

            // Apply saved Speech Rate & Pitch
            val savedRate = getSpeechRate()
            val savedPitch = getSpeechPitch()
            engine.setSpeechRate(savedRate)
            engine.setPitch(savedPitch)

            // Setup Persian language
            val savedVoiceName = getSelectedVoiceName()
            setupPersianVoice(engine, savedVoiceName)

            // Populate voices list
            refreshAvailableVoices(engine)

            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    if (utteranceId != null && utteranceId.endsWith("_last")) {
                        _isSpeaking.value = false
                        _currentlySpeakingId.value = null
                        onSpeechDoneCallback?.invoke()
                        onSpeechDoneCallback = null
                    }
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

    private fun setupPersianVoice(engine: TextToSpeech, savedVoiceName: String?) {
        val candidates = listOf(
            Locale("fa", "IR"),
            Locale("fa"),
            Locale.forLanguageTag("fa-IR"),
            Locale.forLanguageTag("fa")
        )

        for (loc in candidates) {
            val res = engine.setLanguage(loc)
            if (res >= TextToSpeech.LANG_AVAILABLE) {
                isPersianVoiceAvailable = true
                Log.d(TAG, "Accepted Persian locale: ${loc.toLanguageTag()}")
                break
            }
        }

        try {
            val voices = engine.voices
            if (!voices.isNullOrEmpty()) {
                val persianVoices = voices.filter {
                    it.locale.language == "fa" ||
                    it.locale.toLanguageTag().startsWith("fa") ||
                    it.name.contains("fa-", ignoreCase = true)
                }

                if (persianVoices.isNotEmpty()) {
                    isPersianVoiceAvailable = true
                    val matchingSaved = if (!savedVoiceName.isNullOrBlank()) {
                        persianVoices.firstOrNull { it.name == savedVoiceName }
                    } else null

                    val voiceToApply = matchingSaved
                        ?: persianVoices.firstOrNull { !it.isNetworkConnectionRequired }
                        ?: persianVoices.first()

                    engine.voice = voiceToApply
                    Log.d(TAG, "Applied Persian voice: ${voiceToApply.name}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking engine voices", e)
        }
    }

    private fun refreshAvailableVoices(engine: TextToSpeech) {
        try {
            val voices = engine.voices ?: emptySet()
            val list = voices.filter { voice ->
                voice.locale.language == "fa" ||
                voice.locale.toLanguageTag().startsWith("fa") ||
                voice.name.contains("fa-", ignoreCase = true)
            }.mapIndexed { index, voice ->
                val isFemale = voice.name.contains("female", ignoreCase = true) ||
                               voice.name.contains("-faa-", ignoreCase = true)
                val genderFa = if (isFemale) "بانو (زنانه)" else "آقا (مردانه)"
                val networkFa = if (voice.isNetworkConnectionRequired) "آنلاین" else "آفلاین"
                val display = "صدای ${index + 1} ($genderFa • $networkFa)"
                TtsVoiceInfo(
                    name = voice.name,
                    displayName = display,
                    isNetworkRequired = voice.isNetworkConnectionRequired,
                    isFemale = isFemale
                )
            }
            _availableVoices.value = list
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh voices list", e)
        }
    }

    /**
     * Splits long text (> 6 lines or > 250 chars) into small sentence chunks
     * to avoid Android TTS buffer overflows and dropped utterances.
     */
    fun splitTextIntoChunks(text: String, maxChunkLength: Int = 220): List<String> {
        val clean = cleanTextForSpeech(text)
        if (clean.isBlank()) return emptyList()

        // Short messages can be spoken in a single utterance smoothly
        if (clean.length <= 260) {
            return listOf(clean)
        }

        val chunks = mutableListOf<String>()
        // Split by Persian and Latin punctuation: . ! ؟ ? \n ؛
        val rawSentences = clean.split(Regex("(?<=[.!?؟؛\\n])\\s+"))

        var buffer = StringBuilder()

        for (raw in rawSentences) {
            val sentence = raw.trim()
            if (sentence.isEmpty() || !sentence.any { it.isLetter() }) continue

            if (buffer.length + sentence.length + 1 <= maxChunkLength) {
                if (buffer.isNotEmpty()) buffer.append(" ")
                buffer.append(sentence)
            } else {
                if (buffer.isNotEmpty()) {
                    chunks.add(buffer.toString())
                    buffer = StringBuilder()
                }

                if (sentence.length <= maxChunkLength) {
                    buffer.append(sentence)
                } else {
                    // Split long sentence by comma (،)
                    val commaParts = sentence.split(Regex("(?<=[،,])\\s+"))
                    for (cp in commaParts) {
                        val part = cp.trim()
                        if (part.isEmpty() || !part.any { it.isLetter() }) continue

                        if (buffer.length + part.length + 1 <= maxChunkLength) {
                            if (buffer.isNotEmpty()) buffer.append(" ")
                            buffer.append(part)
                        } else {
                            if (buffer.isNotEmpty()) {
                                chunks.add(buffer.toString())
                                buffer = StringBuilder()
                            }
                            buffer.append(part)
                        }
                    }
                }
            }
        }

        if (buffer.isNotEmpty() && buffer.any { it.isLetter() }) {
            chunks.add(buffer.toString())
        }

        return if (chunks.isNotEmpty()) chunks else listOf(clean)
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
        if (cleanSpeechText.isBlank() || !cleanSpeechText.any { it.isLetter() }) {
            return SpeakResult.Error("متنی برای خواندن یافت نشد.")
        }

        // Check if Persian voice is installed on device
        if (!isPersianVoiceAvailable) {
            val faCheck = engine.isLanguageAvailable(Locale("fa", "IR"))
            if (faCheck >= TextToSpeech.LANG_AVAILABLE) {
                isPersianVoiceAvailable = true
                setupPersianVoice(engine, getSelectedVoiceName())
            } else {
                Log.w(TAG, "Persian voice not installed on device engine (code: $faCheck)")
                return SpeakResult.PersianVoiceMissing(createTtsSettingsIntent())
            }
        }

        // Check if device media volume is muted (0)
        if (isMediaMuted()) {
            Log.w(TAG, "Media stream volume is 0 (muted)")
        }

        // Apply current rate and pitch
        engine.setSpeechRate(getSpeechRate())
        engine.setPitch(getSpeechPitch())

        // Split text into manageable chunks (< 220 chars) to prevent buffer overflows on long responses
        val chunks = splitTextIntoChunks(cleanSpeechText, maxChunkLength = 220)
        if (chunks.isEmpty()) {
            return SpeakResult.Error("متن قابل خواندن نیست.")
        }

        onSpeechDoneCallback = onDone
        _currentlySpeakingId.value = messageId
        _isSpeaking.value = true

        val lastIndex = chunks.lastIndex
        var hasError = false

        for (i in chunks.indices) {
            val chunk = chunks[i]
            val isLast = (i == lastIndex)
            val utteranceId = if (isLast) "${messageId}_chunk_${i}_last" else "${messageId}_chunk_$i"
            val queueMode = if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD

            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }

            val result = engine.speak(chunk, queueMode, params, utteranceId)
            if (result != TextToSpeech.SUCCESS) {
                Log.e(TAG, "engine.speak failed on chunk $i with code: $result")
                hasError = true
                break
            }
        }

        return if (!hasError) {
            SpeakResult.Success
        } else {
            _isSpeaking.value = false
            _currentlySpeakingId.value = null

            if (!isPersianVoiceAvailable) {
                SpeakResult.PersianVoiceMissing(createTtsSettingsIntent())
            } else {
                SpeakResult.Error("موتور صوتی موفق به ادای کلام نشد.")
            }
        }
    }

    // Voice Customization Settings
    fun setSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(0.5f, 2.0f)
        prefs.edit().putFloat(KEY_SPEECH_RATE, clamped).apply()
        tts?.setSpeechRate(clamped)
    }

    fun getSpeechRate(): Float = prefs.getFloat(KEY_SPEECH_RATE, 1.0f)

    fun setSpeechPitch(pitch: Float) {
        val clamped = pitch.coerceIn(0.5f, 1.6f)
        prefs.edit().putFloat(KEY_SPEECH_PITCH, clamped).apply()
        tts?.setPitch(clamped)
    }

    fun getSpeechPitch(): Float = prefs.getFloat(KEY_SPEECH_PITCH, 1.0f)

    fun setSelectedVoice(voiceName: String?) {
        prefs.edit().putString(KEY_VOICE_NAME, voiceName).apply()
        val engine = tts ?: return
        if (voiceName != null) {
            try {
                val matching = engine.voices?.firstOrNull { it.name == voiceName }
                if (matching != null) {
                    engine.voice = matching
                    isPersianVoiceAvailable = true
                    Log.d(TAG, "Applied voice selection: $voiceName")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error applying voice $voiceName", e)
            }
        }
    }

    fun getSelectedVoiceName(): String? = prefs.getString(KEY_VOICE_NAME, null)

    fun setCloudVoice(voice: String) {
        prefs.edit().putString(KEY_CLOUD_VOICE, voice).apply()
    }

    fun getCloudVoice(): String = prefs.getString(KEY_CLOUD_VOICE, "Puck") ?: "Puck"

    fun applyPersona(personaId: String): Pair<Float, Float> {
        val (pitch, rate) = when (personaId) {
            "Fenrir" -> 0.74f to 0.88f // بم و عمیق (مردانه • متین)
            "Kore" -> 1.15f to 0.92f   // آرامش‌بخش (زنانه • گرم)
            "Aoede" -> 1.30f to 0.96f  // شیوا و ظریف (زنانه • شاعرانه)
            else -> 1.02f to 1.02f     // حامد استاندارد (مردانه • پرانرژی)
        }
        setSpeechPitch(pitch)
        setSpeechRate(rate)
        setCloudVoice(personaId)

        val devVoices = availableVoices.value
        if (devVoices.isNotEmpty()) {
            val preferFemale = (personaId == "Kore" || personaId == "Aoede")
            val matching = devVoices.firstOrNull { it.isFemale == preferFemale } ?: devVoices.first()
            setSelectedVoice(matching.name)
        }
        return pitch to rate
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

    fun cleanTextForSpeech(raw: String): String {
        return raw
            // Remove markdown syntax
            .replace(Regex("[*#_`~>\\[\\]()]+"), " ")
            // Remove emojis & pictographs
            .replace(Regex("[\\p{So}\\p{Cn}\\p{Cs}]+"), " ")
            // Remove internal system tags [احساس: ...] or [نکته: ...]
            .replace(Regex("\\[.*?\\]"), " ")
            // Remove bullets and excessive punctuation
            .replace(Regex("[-•–—«»]+"), " ")
            // Collapse whitespaces
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

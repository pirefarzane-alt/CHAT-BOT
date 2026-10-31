package com.example.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthManager
import com.example.data.HamedFirestoreRepository
import com.example.model.ChatMessage
import com.example.model.HamedTone
import com.example.model.LiveVoiceState
import com.example.model.PersianLearningTip
import com.example.model.PlatformTheme
import com.example.model.Sender
import com.example.model.SentimentType
import com.example.service.HamedAiService
import com.example.util.AudioRecorderHelper
import com.example.util.HamedAudioPlayer
import com.example.util.HamedTextToSpeechHelper
import com.example.util.SpeakResult
import com.example.util.TtsVoiceInfo
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "ChatViewModel"

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val selectedTone: HamedTone = HamedTone.FRIENDLY,
    val platformTheme: PlatformTheme = PlatformTheme.TELEGRAM,
    val isRecording: Boolean = false,
    val recordingDurationSec: Int = 0,
    val activeDetailMessage: ChatMessage? = null,
    val activeLearningTip: PersianLearningTip? = null,
    val currentUser: FirebaseUser? = null,
    val isAuthLoading: Boolean = false,
    val errorMessage: String? = null,
    val showTtsGuide: Boolean = false,
    // Voice playback & settings state
    val isSpeaking: Boolean = false,
    val speakingMessageId: String? = null,
    val isSynthesizingVoice: Boolean = false,
    val synthesizingMessageId: String? = null,
    val autoPlayVoice: Boolean = false,
    val showVoiceSettings: Boolean = false,
    val availableDeviceVoices: List<TtsVoiceInfo> = emptyList(),
    val selectedDeviceVoice: String? = null,
    val selectedCloudVoice: String = "Puck",
    val speechRate: Float = 0.95f,
    val speechPitch: Float = 1.0f,
    val isLiveVoiceActive: Boolean = false,
    val liveVoiceState: LiveVoiceState = LiveVoiceState.IDLE,
    val liveUserTranscript: String = "",
    val liveHamedResponse: String = "",
    val liveVoiceDurationSec: Int = 0
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val aiService = HamedAiService()
    private val audioRecorder = AudioRecorderHelper(application.applicationContext)
    val authManager = AuthManager(application.applicationContext)
    private val firestoreRepo = HamedFirestoreRepository(application.applicationContext)
    private val ttsHelper = HamedTextToSpeechHelper(application.applicationContext)
    private val audioPlayer = HamedAudioPlayer(application.applicationContext)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var recordingTimerJob: Job? = null
    private var liveTimerJob: Job? = null
    private var recordedAudioFile: File? = null
    private var liveAudioFile: File? = null
    private var firestoreSyncJob: Job? = null

    init {
        loadInitialMessages()
        observeAuth()
        observeAudioPlayback()
        observeTtsVoices()
        loadVoicePreferences()
        authManager.attemptAutoSignIn(viewModelScope)
    }

    private fun loadVoicePreferences() {
        _uiState.update {
            it.copy(
                selectedCloudVoice = ttsHelper.getCloudVoice(),
                speechRate = ttsHelper.getSpeechRate(),
                speechPitch = ttsHelper.getSpeechPitch(),
                selectedDeviceVoice = ttsHelper.getSelectedVoiceName()
            )
        }
    }

    private fun observeTtsVoices() {
        viewModelScope.launch {
            ttsHelper.availableVoices.collect { voices ->
                _uiState.update { it.copy(availableDeviceVoices = voices) }
            }
        }
    }

    private fun observeAuth() {
        viewModelScope.launch {
            authManager.currentUser.collect { user ->
                _uiState.update { it.copy(currentUser = user) }
                if (user != null) {
                    syncFromFirestore(user.uid)
                } else {
                    firestoreSyncJob?.cancel()
                }
            }
        }
    }

    private fun observeAudioPlayback() {
        // Observe cloud audio player
        viewModelScope.launch {
            audioPlayer.isPlaying.collect { playing ->
                if (playing) {
                    _uiState.update { it.copy(isSpeaking = true) }
                } else if (!ttsHelper.isSpeaking.value) {
                    _uiState.update { it.copy(isSpeaking = false) }
                }
            }
        }
        viewModelScope.launch {
            audioPlayer.currentPlayingId.collect { id ->
                if (id != null) {
                    _uiState.update { it.copy(speakingMessageId = id) }
                } else if (ttsHelper.currentlySpeakingId.value == null) {
                    _uiState.update { it.copy(speakingMessageId = null) }
                }
            }
        }
        // Observe on-device TTS helper
        viewModelScope.launch {
            ttsHelper.isSpeaking.collect { speaking ->
                if (speaking) {
                    _uiState.update { it.copy(isSpeaking = true) }
                } else if (!audioPlayer.isPlaying.value) {
                    _uiState.update { it.copy(isSpeaking = false) }
                }
            }
        }
        viewModelScope.launch {
            ttsHelper.currentlySpeakingId.collect { id ->
                if (id != null) {
                    _uiState.update { it.copy(speakingMessageId = id) }
                } else if (audioPlayer.currentPlayingId.value == null) {
                    _uiState.update { it.copy(speakingMessageId = null) }
                }
            }
        }
    }

    private fun syncFromFirestore(userId: String) {
        firestoreSyncJob?.cancel()
        firestoreSyncJob = viewModelScope.launch {
            try {
                firestoreRepo.observeMessages(userId).collect { cloudMessages ->
                    if (cloudMessages.isNotEmpty()) {
                        _uiState.update { it.copy(messages = cloudMessages) }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error observing cloud messages: ${e.message}")
            }
        }
    }

    private fun loadInitialMessages() {
        val welcomeMessage = ChatMessage(
            sender = Sender.HAMED,
            text = "درود و احترام! من حامد (hamed) هستم، چت‌بات هوش مصنوعی فارسی شما. در کنار پاسخ به انواع سوالات، به شما در یادگیری شیوایی، واژگان و دستور زبان فارسی کمک می‌کنم و احساسات گفت‌وگو را درک می‌کنم. اکنون می‌توانید با من مکالمه صوتی و زنده داشته باشید و صدای مرا بشنوید. چه موضوعی را بررسی کنیم؟",
            sentiment = SentimentType.POSITIVE,
            sentimentScore = 0.98f,
            sentimentAnalysis = "لحن خوش‌آمدگویی پرانرژی و متناسب با فرهنگ مهمان‌نوازی زبان فارسی",
            learningTip = PersianLearningTip(
                title = "واژه‌گزینی زیبای فارسی",
                explanation = "واژه «درود» از ریشه اوستایی «دْرْوَتات» به معنای تندرستی و سلامت است و زیباترین واژه فارسی برای آغاز مکالمه به شمار می‌رود.",
                example = "درود بر شما، روزگارتان شاد و خرم باد.",
                culturalNote = "فردوسی در شاهنامه می‌فرماید: ز یزدان بر آن شاه باد آفرین / کزو شادمان شد دل و داد و دین"
            )
        )
        _uiState.update { it.copy(messages = listOf(welcomeMessage)) }
    }

    fun signInWithGoogle(activity: Activity) {
        _uiState.update { it.copy(isAuthLoading = true) }
        authManager.signInWithGoogle(
            activity = activity,
            scope = viewModelScope,
            onSuccess = { user ->
                _uiState.update { it.copy(isAuthLoading = false, currentUser = user) }
            },
            onError = { error ->
                _uiState.update { it.copy(isAuthLoading = false, errorMessage = error) }
            },
            onCancelled = {
                _uiState.update { it.copy(isAuthLoading = false) }
            }
        )
    }

    fun signOut() {
        authManager.signOut(viewModelScope) {
            _uiState.update { it.copy(currentUser = null) }
            loadInitialMessages()
        }
    }

    fun setTone(tone: HamedTone) {
        _uiState.update { it.copy(selectedTone = tone) }
        val user = _uiState.value.currentUser
        if (user != null) {
            viewModelScope.launch {
                try {
                    firestoreRepo.savePreferences(user.uid, tone, _uiState.value.platformTheme)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to save tone to Firestore", e)
                }
            }
        }
    }

    fun setPlatformTheme(theme: PlatformTheme) {
        _uiState.update { it.copy(platformTheme = theme) }
        val user = _uiState.value.currentUser
        if (user != null) {
            viewModelScope.launch {
                try {
                    firestoreRepo.savePreferences(user.uid, _uiState.value.selectedTone, theme)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to save theme to Firestore", e)
                }
            }
        }
    }

    fun selectSentimentMessage(message: ChatMessage?) {
        _uiState.update { it.copy(activeDetailMessage = message) }
    }

    fun selectLearningTip(tip: PersianLearningTip?) {
        _uiState.update { it.copy(activeLearningTip = tip) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearChat() {
        stopSpeaking()
        val user = _uiState.value.currentUser
        if (user != null) {
            viewModelScope.launch {
                try {
                    firestoreRepo.clearUserMessages(user.uid)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear cloud messages", e)
                }
            }
        }
        loadInitialMessages()
    }

    // Voice Playback controls for Hamed's replies
    fun playMessageVoice(message: ChatMessage) {
        // If clicking the message that is currently playing -> toggle off/stop it
        if (_uiState.value.isSpeaking && _uiState.value.speakingMessageId == message.id) {
            stopSpeaking()
            return
        }

        // If clicking a message currently being synthesized -> ignore duplicate click
        if (_uiState.value.isSynthesizingVoice && _uiState.value.synthesizingMessageId == message.id) {
            return
        }

        stopSpeaking()

        val chosenVoice = _uiState.value.selectedCloudVoice
        val currentRate = _uiState.value.speechRate
        val cacheKey = "${message.id}_${chosenVoice}"

        // 1. Check if audio is already cached locally on disk
        val cachedFile = audioPlayer.getCachedAudioFile(cacheKey)
        if (cachedFile != null) {
            audioPlayer.playAudioFile(cachedFile, message.id, playbackSpeed = currentRate)
            return
        }

        // 2. Synthesize using high-fidelity Gemini Actor Neural Voice
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSynthesizingVoice = true,
                    synthesizingMessageId = message.id
                )
            }

            var isRateLimited = false
            try {
                val generatedAudio = aiService.synthesizeSpeech(message.text, voiceName = chosenVoice)
                if (generatedAudio != null) {
                    val file = audioPlayer.saveAndPrepareAudio(
                        messageId = cacheKey,
                        rawBytes = generatedAudio.bytes,
                        mimeType = generatedAudio.mimeType
                    )
                    _uiState.update {
                        it.copy(
                            isSynthesizingVoice = false,
                            synthesizingMessageId = null
                        )
                    }
                    audioPlayer.playAudioFile(file, message.id, playbackSpeed = currentRate)
                    return@launch
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini Actor voice failed for message ${message.id}: ${e.message}")
                val msgLower = e.message?.lowercase() ?: ""
                if (msgLower.contains("429") || msgLower.contains("quota") || msgLower.contains("resource_exhausted")) {
                    isRateLimited = true
                }
            }

            _uiState.update {
                it.copy(
                    isSynthesizingVoice = false,
                    synthesizingMessageId = null
                )
            }

            val friendlyMsg = if (isRateLimited) {
                "محدودیت موقت در تعداد درخواست صوت در دقیقه؛ لطفاً ۳۰ ثانیه صبر کرده و مجدداً علامت بلندگو را لمس کنید."
            } else {
                "صدا در حال حاضر دریافت نشد. لطفاً اتصال اینترنت خود را بررسی و مجدداً علامت بلندگو را لمس کنید."
            }

            // 3. Fallback to on-device TTS only if device actually has Persian voice
            if (ttsHelper.isPersianVoiceAvailable) {
                ttsHelper.speak(text = message.text, messageId = message.id)
            } else {
                _uiState.update {
                    it.copy(errorMessage = friendlyMsg)
                }
            }
        }
    }

    // Voice Settings Handlers
    fun openVoiceSettings() {
        _uiState.update { it.copy(showVoiceSettings = true) }
    }

    fun closeVoiceSettings() {
        _uiState.update { it.copy(showVoiceSettings = false) }
    }

    fun updateSpeechRate(rate: Float) {
        ttsHelper.setSpeechRate(rate)
        _uiState.update { it.copy(speechRate = rate) }
    }

    fun updateSpeechPitch(pitch: Float) {
        ttsHelper.setSpeechPitch(pitch)
        _uiState.update { it.copy(speechPitch = pitch) }
    }

    fun selectDeviceVoice(voiceName: String?) {
        ttsHelper.setSelectedVoice(voiceName)
        _uiState.update { it.copy(selectedDeviceVoice = voiceName) }
    }

    fun selectCloudVoice(voiceId: String) {
        val (pitch, rate) = ttsHelper.applyPersona(voiceId)
        _uiState.update {
            it.copy(
                selectedCloudVoice = voiceId,
                speechPitch = pitch,
                speechRate = rate,
                selectedDeviceVoice = ttsHelper.getSelectedVoiceName()
            )
        }
    }

    fun testCurrentVoice() {
        if (_uiState.value.isSpeaking) {
            stopSpeaking()
            return
        }

        stopSpeaking()

        val chosenVoice = _uiState.value.selectedCloudVoice
        val sampleText = when (chosenVoice) {
            "Aoede" -> "درود بر شما! من دستیار هوشمند شما هستم؛ با کمال افتخار و با این لحن زیبا در خدمتتان."
            "Kore" -> "سلام و درود دوست مهربانم؛ امیدوارم روزی پر از آرامش، شادی و برکت داشته باشید."
            "Fenrir" -> "با سلام و احترام؛ اینجانب حامد هستم، با صدایی باوقار و ادیبانه در خدمت شما."
            else -> "سلام! من حامد هستم؛ پرانرژی، پویا و با صدای مردانه آماده گفت‌وگو با شما."
        }

        playMessageVoice(
            ChatMessage(
                id = "test_preview_voice",
                sender = Sender.HAMED,
                text = sampleText,
                sentiment = SentimentType.POSITIVE
            )
        )
    }

    fun openTtsSettings(context: Context) {
        try {
            val intent = ttsHelper.createTtsSettingsIntent()
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch TTS settings", e)
        }
    }

    fun openStoreForTts(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.reecedunn.espeak")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.reecedunn.espeak")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch store for TTS", e)
            }
        }
    }

    fun dismissTtsGuide() {
        _uiState.update { it.copy(showTtsGuide = false) }
    }

    fun stopSpeaking() {
        audioPlayer.stop()
        ttsHelper.stop()
        _uiState.update {
            it.copy(
                isSpeaking = false,
                speakingMessageId = null,
                isSynthesizingVoice = false,
                synthesizingMessageId = null
            )
        }
    }

    fun toggleAutoPlayVoice() {
        _uiState.update { it.copy(autoPlayVoice = !it.autoPlayVoice) }
    }

    fun sendTextMessage(text: String) {
        val cleanText = text.trim()
        if (cleanText.isBlank() || _uiState.value.isLoading) return

        val userMessage = ChatMessage(
            sender = Sender.USER,
            text = cleanText,
            sentiment = detectQuickSentiment(cleanText)
        )

        val updatedList = _uiState.value.messages + userMessage
        _uiState.update {
            it.copy(
                messages = updatedList,
                isLoading = true,
                errorMessage = null
            )
        }

        val user = _uiState.value.currentUser
        if (user != null) {
            viewModelScope.launch {
                try {
                    firestoreRepo.saveMessage(user.uid, userMessage)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to persist user message to Firestore", e)
                }
            }
        }

        viewModelScope.launch {
            try {
                val hamedReply = aiService.sendMessage(
                    history = updatedList,
                    userMessage = cleanText,
                    tone = _uiState.value.selectedTone
                )

                _uiState.update { current ->
                    current.copy(
                        messages = current.messages + hamedReply,
                        isLoading = false
                    )
                }

                if (user != null) {
                    try {
                        firestoreRepo.saveMessage(user.uid, hamedReply)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to persist bot reply to Firestore", e)
                    }
                }

                // If user enabled autoPlayVoice, speak out Hamed's response automatically
                if (_uiState.value.autoPlayVoice) {
                    playMessageVoice(hamedReply)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get response from hamed", e)
                val friendlyError = when {
                    e is kotlinx.coroutines.TimeoutCancellationException || e.message?.contains("time", ignoreCase = true) == true ->
                        "پاسخ حامد با تاخیر مواجه شد. لطفاً اتصال اینترنت خود را بررسی کرده و دوباره امتحان کنید."
                    else ->
                        "متأسفانه در دریافت پاسخ مشکلی پیش آمد: ${e.localizedMessage ?: "خطای اتصال"}"
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = friendlyError
                    )
                }
            }
        }
    }

    fun startVoiceRecording(): Boolean {
        if (_uiState.value.isRecording) return false
        stopSpeaking()
        val file = audioRecorder.startRecording()
        return if (file != null) {
            recordedAudioFile = file
            _uiState.update { it.copy(isRecording = true, recordingDurationSec = 0) }
            startRecordingTimer()
            true
        } else {
            _uiState.update { it.copy(errorMessage = "امکان دسترسی به میکروفون میسر نشد.") }
            false
        }
    }

    fun stopVoiceRecording() {
        if (!_uiState.value.isRecording) return
        recordingTimerJob?.cancel()
        val duration = _uiState.value.recordingDurationSec
        val file = audioRecorder.stopRecording()
        _uiState.update { it.copy(isRecording = false, recordingDurationSec = 0) }

        if (file != null && file.exists() && file.length() > 0) {
            processVoiceNote(file, duration)
        } else {
            _uiState.update { it.copy(errorMessage = "صدا ضبط نشد، لطفاً مجدداً تلاش کنید.") }
        }
    }

    fun cancelVoiceRecording() {
        recordingTimerJob?.cancel()
        audioRecorder.cancelRecording()
        recordedAudioFile = null
        _uiState.update { it.copy(isRecording = false, recordingDurationSec = 0) }
    }

    private fun processVoiceNote(file: File, durationSec: Int) {
        val audioMsg = ChatMessage(
            sender = Sender.USER,
            text = "🎙 پیام صوتی ($durationSec ثانیه) • در حال رونویسی…",
            isAudio = true,
            audioDurationSec = durationSec,
            sentiment = SentimentType.CURIOUS
        )

        _uiState.update {
            it.copy(
                messages = it.messages + audioMsg,
                isLoading = true,
                errorMessage = null
            )
        }

        val user = _uiState.value.currentUser

        viewModelScope.launch {
            try {
                // 1. Transcribe audio using Gemini transcription
                val transcript = aiService.transcribeAudio(file)

                val updatedUserMsg = audioMsg.copy(
                    text = "🎙 $transcript",
                    sentiment = detectQuickSentiment(transcript)
                )

                // Update the user's message with real transcribed text
                _uiState.update { state ->
                    val updatedMessages = state.messages.map { msg ->
                        if (msg.id == audioMsg.id) updatedUserMsg else msg
                    }
                    state.copy(messages = updatedMessages)
                }

                if (user != null) {
                    try {
                        firestoreRepo.saveMessage(user.uid, updatedUserMsg)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to save transcribed message to Firestore", e)
                    }
                }

                // 2. Generate hamed's response to the transcribed audio
                val reply = aiService.sendMessage(
                    history = _uiState.value.messages,
                    userMessage = transcript,
                    tone = _uiState.value.selectedTone
                )

                _uiState.update { state ->
                    state.copy(
                        messages = state.messages + reply,
                        isLoading = false
                    )
                }

                if (user != null) {
                    try {
                        firestoreRepo.saveMessage(user.uid, reply)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to save voice reply to Firestore", e)
                    }
                }

                // Auto-play voice if toggle is on
                if (_uiState.value.autoPlayVoice) {
                    playMessageVoice(reply)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed during voice note processing", e)
                val friendly = if (e is kotlinx.coroutines.TimeoutCancellationException || e.message?.contains("time", ignoreCase = true) == true) {
                    "رونویسی صدا با تاخیر مواجه شد. لطفاً اتصال اینترنت خود را بررسی کنید."
                } else {
                    "رونویسی صدا با خطا مواجه شد: ${e.localizedMessage ?: "عدم ارتباط"}"
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = friendly
                    )
                }
            } finally {
                try {
                    file.delete()
                } catch (_: Exception) {}
            }
        }
    }

    // ==========================================
    // Live Voice Conversation Mode (تماس صوتی زنده)
    // ==========================================

    fun startLiveVoiceMode() {
        stopSpeaking()
        _uiState.update {
            it.copy(
                isLiveVoiceActive = true,
                liveVoiceState = LiveVoiceState.IDLE,
                liveUserTranscript = "",
                liveHamedResponse = "",
                liveVoiceDurationSec = 0,
                errorMessage = null
            )
        }
    }

    fun stopLiveVoiceMode() {
        liveTimerJob?.cancel()
        audioRecorder.cancelRecording()
        stopSpeaking()
        _uiState.update {
            it.copy(
                isLiveVoiceActive = false,
                liveVoiceState = LiveVoiceState.IDLE,
                liveVoiceDurationSec = 0
            )
        }
    }

    fun toggleLiveVoiceListening() {
        when (_uiState.value.liveVoiceState) {
            LiveVoiceState.LISTENING -> finishLiveVoiceListening()
            LiveVoiceState.SPEAKING -> {
                stopSpeaking()
                _uiState.update { it.copy(liveVoiceState = LiveVoiceState.IDLE) }
            }
            LiveVoiceState.PROCESSING -> {
                // Currently thinking, do nothing or wait
            }
            LiveVoiceState.IDLE -> startLiveVoiceListening()
        }
    }

    fun startLiveVoiceListening(): Boolean {
        stopSpeaking()
        val file = audioRecorder.startRecording()
        return if (file != null) {
            liveAudioFile = file
            _uiState.update {
                it.copy(
                    liveVoiceState = LiveVoiceState.LISTENING,
                    liveVoiceDurationSec = 0,
                    errorMessage = null
                )
            }
            startLiveTimer()
            true
        } else {
            _uiState.update {
                it.copy(
                    liveVoiceState = LiveVoiceState.IDLE,
                    errorMessage = "دسترسی به میکروفون برقرار نشد."
                )
            }
            false
        }
    }

    fun finishLiveVoiceListening() {
        liveTimerJob?.cancel()
        val duration = _uiState.value.liveVoiceDurationSec
        val file = audioRecorder.stopRecording()

        _uiState.update {
            it.copy(
                liveVoiceState = LiveVoiceState.PROCESSING,
                liveVoiceDurationSec = 0
            )
        }

        if (file != null && file.exists() && file.length() > 0) {
            processLiveAudioExchange(file, duration)
        } else {
            _uiState.update {
                it.copy(
                    liveVoiceState = LiveVoiceState.IDLE,
                    errorMessage = "صدایی برای تحلیل دریافت نشد."
                )
            }
        }
    }

    private fun processLiveAudioExchange(file: File, durationSec: Int) {
        val user = _uiState.value.currentUser

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(liveVoiceState = LiveVoiceState.PROCESSING) }

                // 1. Transcribe voice in real-time
                val transcript = aiService.transcribeAudio(file)
                if (transcript.isBlank()) {
                    _uiState.update {
                        it.copy(
                            liveVoiceState = LiveVoiceState.IDLE,
                            errorMessage = "صدای واضحی دریافت نشد؛ لطفاً دوباره دکمه را لمس کرده و صحبت کنید."
                        )
                    }
                    return@launch
                }

                _uiState.update { it.copy(liveUserTranscript = transcript) }

                // Save to chat history
                val userMsg = ChatMessage(
                    sender = Sender.USER,
                    text = "🎙 $transcript",
                    isAudio = true,
                    audioDurationSec = durationSec,
                    sentiment = detectQuickSentiment(transcript)
                )

                val updatedHistory = _uiState.value.messages + userMsg
                _uiState.update { it.copy(messages = updatedHistory) }

                if (user != null) {
                    try {
                        firestoreRepo.saveMessage(user.uid, userMsg)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to persist live user voice to Firestore", e)
                    }
                }

                // 2. Generate response from Hamed
                val hamedReply = aiService.sendMessage(
                    history = updatedHistory,
                    userMessage = transcript,
                    tone = _uiState.value.selectedTone
                )

                // Append bot reply to chat history and persist to Firestore
                val finalHistory = updatedHistory + hamedReply
                _uiState.update {
                    it.copy(
                        messages = finalHistory,
                        liveHamedResponse = hamedReply.text
                    )
                }

                if (user != null) {
                    try {
                        firestoreRepo.saveMessage(user.uid, hamedReply)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to persist live bot voice to Firestore", e)
                    }
                }

                // 3. Play Hamed's reply out loud using Gemini Actor voice
                try {
                    val generatedAudio = aiService.synthesizeSpeech(hamedReply.text, voiceName = _uiState.value.selectedCloudVoice)
                    if (generatedAudio != null) {
                        val audioFile = audioPlayer.saveAndPrepareAudio(
                            messageId = "${hamedReply.id}_${_uiState.value.selectedCloudVoice}",
                            rawBytes = generatedAudio.bytes,
                            mimeType = generatedAudio.mimeType
                        )
                        // Transition to SPEAKING only when audio is actually playing
                        _uiState.update {
                            if (it.isLiveVoiceActive) it.copy(liveVoiceState = LiveVoiceState.SPEAKING) else it
                        }
                        audioPlayer.playAudioFile(
                            file = audioFile,
                            messageId = hamedReply.id,
                            playbackSpeed = _uiState.value.speechRate,
                            onDone = {
                                _uiState.update { current ->
                                    if (current.isLiveVoiceActive && current.liveVoiceState == LiveVoiceState.SPEAKING) {
                                        current.copy(liveVoiceState = LiveVoiceState.IDLE)
                                    } else {
                                        current
                                    }
                                }
                            }
                        )
                    } else {
                        _uiState.update { current ->
                            if (current.isLiveVoiceActive) {
                                current.copy(
                                    liveVoiceState = LiveVoiceState.IDLE,
                                    errorMessage = "پاسخ متنی حامد آماده شد. برای پخش صوتی علامت بلندگو را بزنید."
                                )
                            } else current
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Audio playback error in live exchange: ${e.message}")
                    _uiState.update { current ->
                        if (current.isLiveVoiceActive) current.copy(liveVoiceState = LiveVoiceState.IDLE) else current
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during live voice exchange", e)
                val friendlyMsg = if (e is kotlinx.coroutines.TimeoutCancellationException || e.message?.contains("time", ignoreCase = true) == true) {
                    "مکالمه با تاخیر مواجه شد. لطفاً دوباره تلاش کنید."
                } else {
                    "مکالمه صوتی با خطا مواجه شد: ${e.localizedMessage ?: "خطای اتصال"}"
                }
                _uiState.update {
                    it.copy(
                        liveVoiceState = LiveVoiceState.IDLE,
                        errorMessage = friendlyMsg
                    )
                }
            } finally {
                try {
                    file.delete()
                } catch (_: Exception) {}
            }
        }
    }

    private fun startRecordingTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = viewModelScope.launch {
            while (isActive && _uiState.value.isRecording) {
                delay(1000L)
                _uiState.update { it.copy(recordingDurationSec = it.recordingDurationSec + 1) }
            }
        }
    }

    private fun startLiveTimer() {
        liveTimerJob?.cancel()
        liveTimerJob = viewModelScope.launch {
            while (isActive && _uiState.value.liveVoiceState == LiveVoiceState.LISTENING) {
                delay(1000L)
                _uiState.update { it.copy(liveVoiceDurationSec = it.liveVoiceDurationSec + 1) }
            }
        }
    }

    private fun detectQuickSentiment(text: String): SentimentType {
        val t = text.lowercase()
        return when {
            t.contains("عالی") || t.contains("خوب") || t.contains("ممنون") || t.contains("تشکر") || t.contains("سلام") -> SentimentType.POSITIVE
            t.contains("چرا") || t.contains("چگونه") || t.contains("چطور") || t.contains("چیست") || t.contains("؟") -> SentimentType.CURIOUS
            t.contains("نگران") || t.contains("استرس") || t.contains("ترس") || t.contains("مشکل") -> SentimentType.CONCERNED
            t.contains("بد") || t.contains("عصبانی") || t.contains("خسته") || t.contains("ناراحت") -> SentimentType.FRUSTRATED
            else -> SentimentType.CALM
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
        ttsHelper.shutdown()
        audioRecorder.cancelRecording()
    }
}

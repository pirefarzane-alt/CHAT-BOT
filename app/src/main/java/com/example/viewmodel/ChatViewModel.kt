package com.example.viewmodel

import android.app.Activity
import android.app.Application
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
import com.example.util.HamedTextToSpeechHelper
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
    // Voice playback & live voice conversation state
    val isSpeaking: Boolean = false,
    val speakingMessageId: String? = null,
    val autoPlayVoice: Boolean = false,
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
        observeTts()
        authManager.attemptAutoSignIn(viewModelScope)
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

    private fun observeTts() {
        viewModelScope.launch {
            ttsHelper.isSpeaking.collect { speaking ->
                _uiState.update { it.copy(isSpeaking = speaking) }
            }
        }
        viewModelScope.launch {
            ttsHelper.currentlySpeakingId.collect { id ->
                _uiState.update { it.copy(speakingMessageId = id) }
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
        if (_uiState.value.isSpeaking && _uiState.value.speakingMessageId == message.id) {
            ttsHelper.stop()
        } else {
            ttsHelper.speak(text = message.text, messageId = message.id)
        }
    }

    fun stopSpeaking() {
        ttsHelper.stop()
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
                    ttsHelper.speak(hamedReply.text, hamedReply.id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get response from hamed", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "متأسفانه در برقراری ارتباط مشکلی پیش آمد: ${e.localizedMessage ?: "خطای ناشناخته"}"
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
                    ttsHelper.speak(reply.text, reply.id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed during voice note processing", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "رونویسی صدا با خطا مواجه شد: ${e.localizedMessage}"
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
                // 1. Transcribe voice in real-time
                val transcript = aiService.transcribeAudio(file)
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
                        liveHamedResponse = hamedReply.text,
                        liveVoiceState = LiveVoiceState.SPEAKING
                    )
                }

                if (user != null) {
                    try {
                        firestoreRepo.saveMessage(user.uid, hamedReply)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to persist live bot voice to Firestore", e)
                    }
                }

                // 3. Speak Hamed's reply out loud
                ttsHelper.speak(
                    text = hamedReply.text,
                    messageId = hamedReply.id,
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

            } catch (e: Exception) {
                Log.e(TAG, "Error during live voice exchange", e)
                _uiState.update {
                    it.copy(
                        liveVoiceState = LiveVoiceState.IDLE,
                        errorMessage = "مکالمه صوتی با خطا مواجه شد: ${e.localizedMessage ?: "خطای ناشناخته"}"
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
        ttsHelper.shutdown()
        audioRecorder.cancelRecording()
    }
}

package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ChatBubble
import com.example.ui.components.ChatInputBar
import com.example.ui.components.HamedTopBar
import com.example.ui.components.LearningTipDialog
import com.example.ui.components.PlatformSwitcherBar
import com.example.ui.components.SentimentDetailDialog
import com.example.ui.components.ToneSelectorSheet
import com.example.ui.theme.MessengerThemePalette
import com.example.viewmodel.ChatViewModel

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()
    val colors = MessengerThemePalette.getColors(uiState.platformTheme, isDark)

    var showToneSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Auto-scroll on new messages
    LaunchedEffect(uiState.messages.size, uiState.isLoading) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Force RTL for Persian experience
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                Column {
                    HamedTopBar(
                        platformTheme = uiState.platformTheme,
                        currentUser = uiState.currentUser,
                        isAuthLoading = uiState.isAuthLoading,
                        colors = colors,
                        onSignInWithGoogle = { activity -> viewModel.signInWithGoogle(activity) },
                        onSignOut = { viewModel.signOut() },
                        onSwitchTheme = { viewModel.setPlatformTheme(it) },
                        onOpenToneSelector = { showToneSheet = true },
                        onClearChat = { viewModel.clearChat() }
                    )
                    PlatformSwitcherBar(
                        selectedTheme = uiState.platformTheme,
                        colors = colors,
                        onThemeSelect = { viewModel.setPlatformTheme(it) }
                    )

                    // Active Tone Pill Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.cardBackground.copy(alpha = 0.9f))
                            .clickable { showToneSheet = true }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = colors.accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "لحن پاسخ حامد: ${uiState.selectedTone.emoji} ${uiState.selectedTone.titleFa}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accentColor,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "تغییر لحن ❯",
                            fontSize = 11.sp,
                            color = colors.botBubbleTime
                        )
                    }
                }
            },
            bottomBar = {
                ChatInputBar(
                    isRecording = uiState.isRecording,
                    recordingDurationSec = uiState.recordingDurationSec,
                    isLoading = uiState.isLoading,
                    colors = colors,
                    onSendMessage = { viewModel.sendTextMessage(it) },
                    onStartRecording = { viewModel.startVoiceRecording() },
                    onStopRecording = { viewModel.stopVoiceRecording() },
                    onCancelRecording = { viewModel.cancelVoiceRecording() }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(
                        Brush.verticalGradient(
                            listOf(colors.chatBackgroundStart, colors.chatBackgroundEnd)
                        )
                    )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Error Notification if any
                    if (uiState.errorMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE53935).copy(alpha = 0.9f))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = uiState.errorMessage ?: "",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.dismissError() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "بستن",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Conversation Thread
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        items(uiState.messages, key = { it.id }) { msg ->
                            ChatBubble(
                                message = msg,
                                colors = colors,
                                onShowSentiment = { viewModel.selectSentimentMessage(it) },
                                onShowLearningTip = { viewModel.selectLearningTip(it) }
                            )
                        }

                        // Persian Starter Questions when conversation is fresh
                        if (uiState.messages.size <= 1) {
                            item {
                                PersianConversationStarters(
                                    colors = colors,
                                    onSelectStarter = { viewModel.sendTextMessage(it) }
                                )
                            }
                        }

                        // Typing / Loading Indicator
                        if (uiState.isLoading) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.botBubbleBackground.copy(alpha = 0.9f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = colors.accentColor
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "حامد (hamed) در حال پاسخگویی است…",
                                        fontSize = 12.sp,
                                        color = colors.botBubbleText
                                    )
                                }
                            }
                        }
                    }
                }

                // Tone Selector Sheet
                if (showToneSheet) {
                    ToneSelectorSheet(
                        currentTone = uiState.selectedTone,
                        colors = colors,
                        onToneSelected = { viewModel.setTone(it) },
                        onDismiss = { showToneSheet = false }
                    )
                }

                // Sentiment Detail Dialog
                uiState.activeDetailMessage?.let { detailMsg ->
                    SentimentDetailDialog(
                        message = detailMsg,
                        colors = colors,
                        onDismiss = { viewModel.selectSentimentMessage(null) }
                    )
                }

                // Persian Learning Tip Dialog
                uiState.activeLearningTip?.let { tip ->
                    LearningTipDialog(
                        tip = tip,
                        colors = colors,
                        onDismiss = { viewModel.selectLearningTip(null) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PersianConversationStarters(
    colors: com.example.ui.theme.MessengerColors,
    onSelectStarter: (String) -> Unit
) {
    val starters = listOf(
        "چند واژه اصیل و زیبای فارسی برای مکالمه روزمره معرفی کن.",
        "تفاوت کاربرد لحن رسمی و صمیمی در زبان فارسی چیست؟",
        "یک بیت شعر پرمعنا از حافظ یا سعدی را با تحلیل احساسش بگو.",
        "ریشه و داستان ضرب‌المثل «کوه به کوه نمی‌رسد» چیست؟"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "پرسش‌های پیشنهادی برای آغاز گفت‌وگو با حامد:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = colors.botBubbleTime,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        starters.forEach { prompt ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.cardBackground.copy(alpha = 0.85f))
                    .clickable { onSelectStarter(prompt) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "💬", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = prompt,
                        fontSize = 13.sp,
                        color = colors.botBubbleText,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

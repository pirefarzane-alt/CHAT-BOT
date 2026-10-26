package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.model.HamedTone
import com.example.model.LiveVoiceState
import com.example.ui.theme.MessengerColors

@Composable
fun LiveVoiceCallOverlay(
    state: LiveVoiceState,
    durationSec: Int,
    selectedTone: HamedTone,
    userTranscript: String,
    hamedResponse: String,
    colors: MessengerColors,
    onToggleListening: () -> Unit,
    onToneSelected: (HamedTone) -> Unit,
    onCloseCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onCloseCall,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "live_rings")

        // Pulsing scale for animated sound rings
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = if (state == LiveVoiceState.LISTENING || state == LiveVoiceState.SPEAKING) 1.28f else 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )

        val ringAlpha by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 0.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ring_alpha"
        )

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF1E293B),
                            Color(0xFF0F172A)
                        )
                    )
                )
                .statusBarsPadding()
                .padding(20.dp)
                .testTag("live_voice_overlay")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header: Title & Close
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onCloseCall,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .size(38.dp)
                            .testTag("close_live_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بازگشت به چت",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مکالمه صوتی زنده با حامد",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "صدا و تاریخچه گفت‌وگو ذخیره می‌شود",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }

                    // Active Tone Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.accentColor.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${selectedTone.emoji} ${selectedTone.titleFa.split(" ")[0]}",
                            color = colors.accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Central Avatar with Pulsing Waves
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(220.dp)
                ) {
                    // Outer Ring
                    if (state == LiveVoiceState.LISTENING || state == LiveVoiceState.SPEAKING) {
                        Box(
                            modifier = Modifier
                                .size(210.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(
                                    if (state == LiveVoiceState.LISTENING) {
                                        Color(0xFFE53935).copy(alpha = ringAlpha)
                                    } else {
                                        colors.accentColor.copy(alpha = ringAlpha)
                                    }
                                )
                        )
                        Box(
                            modifier = Modifier
                                .size(170.dp)
                                .clip(CircleShape)
                                .background(
                                    if (state == LiveVoiceState.LISTENING) {
                                        Color(0xFFE53935).copy(alpha = 0.25f)
                                    } else {
                                        colors.accentColor.copy(alpha = 0.25f)
                                    }
                                )
                        )
                    }

                    // Main Avatar Circle
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .border(
                                width = 3.dp,
                                color = when (state) {
                                    LiveVoiceState.LISTENING -> Color(0xFFE53935)
                                    LiveVoiceState.SPEAKING -> colors.accentColor
                                    LiveVoiceState.PROCESSING -> Color(0xFFFFB300)
                                    LiveVoiceState.IDLE -> Color.White.copy(alpha = 0.3f)
                                },
                                shape = CircleShape
                            )
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_hamed_logo),
                            contentDescription = "تصویر حامد",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Status Banner
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when (state) {
                            LiveVoiceState.IDLE -> {
                                Text(
                                    text = "🎙 آماده شنیدن سخن شما • دکمه میکروفون را لمس کنید",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            LiveVoiceState.LISTENING -> {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "در حال شنیدن صدای شما... (${formatSeconds(durationSec)})",
                                    color = Color(0xFFFF5252),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            LiveVoiceState.PROCESSING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFFFFB300)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "حامد در حال تحلیل کلام و تنظیم پاسخ...",
                                    color = Color(0xFFFFB300),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            LiveVoiceState.SPEAKING -> {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = colors.accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "حامد در حال صحبت صوتی است...",
                                    color = colors.accentColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Live Captions / Transcription Preview Card
                if (userTranscript.isNotBlank() || hamedResponse.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        if (userTranscript.isNotBlank()) {
                            Row(verticalAlignment = Alignment.Top) {
                                Text(text = "شما: ", color = Color(0xFF64B5F6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = userTranscript,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp
                                )
                            }
                        }

                        if (hamedResponse.isNotBlank()) {
                            if (userTranscript.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                            Row(verticalAlignment = Alignment.Top) {
                                Text(text = "حامد: ", color = colors.accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = hamedResponse,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Controls Row: Tone Selector, Big Mic, End Call
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tone quick change button
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable {
                                // Cycle to next tone
                                val tones = HamedTone.entries
                                val nextIndex = (tones.indexOf(selectedTone) + 1) % tones.size
                                onToneSelected(tones[nextIndex])
                            }
                            .testTag("live_change_tone_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = selectedTone.emoji,
                            fontSize = 24.sp
                        )
                    }

                    // Main Big Push/Tap to Talk Button
                    val micColor = when (state) {
                        LiveVoiceState.LISTENING -> Color(0xFFE53935)
                        LiveVoiceState.SPEAKING -> colors.accentColor
                        LiveVoiceState.PROCESSING -> Color(0xFF757575)
                        LiveVoiceState.IDLE -> colors.accentColor
                    }

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(micColor)
                            .clickable(enabled = state != LiveVoiceState.PROCESSING) {
                                onToggleListening()
                            }
                            .testTag("live_call_mic_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        when (state) {
                            LiveVoiceState.LISTENING -> {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "پایان صحبت و ارسال",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            LiveVoiceState.SPEAKING -> {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "توقف صدا",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            LiveVoiceState.PROCESSING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp,
                                    color = Color.White
                                )
                            }
                            LiveVoiceState.IDLE -> {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "شروع صحبت",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    // End Call Button
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935))
                            .clickable { onCloseCall() }
                            .testTag("live_end_call_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "قطع تماس و بازگشت به تاریخچه",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Help text at bottom
                Text(
                    text = when (state) {
                        LiveVoiceState.LISTENING -> "برای ارسال صحبت، دکمه قرمز وسط را لمس کنید"
                        LiveVoiceState.SPEAKING -> "برای قطع صدای حامد، دکمه وسط را لمس کنید"
                        LiveVoiceState.PROCESSING -> "لطفاً کمی صبر کنید..."
                        LiveVoiceState.IDLE -> "دکمه میکروفون وسط را لمس کنید و سخن بگویید"
                    },
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

private fun formatSeconds(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    val raw = "%d:%02d".format(m, s)
    val sb = StringBuilder()
    for (ch in raw) {
        if (ch in '0'..'9') sb.append(persianDigits[ch - '0']) else sb.append(ch)
    }
    return sb.toString()
}

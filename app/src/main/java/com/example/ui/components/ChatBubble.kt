package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChatMessage
import com.example.model.PersianLearningTip
import com.example.model.Sender
import com.example.ui.theme.MessengerColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatBubble(
    message: ChatMessage,
    colors: MessengerColors,
    onShowSentiment: (ChatMessage) -> Unit,
    onShowLearningTip: (PersianLearningTip) -> Unit,
    isSpeakingThis: Boolean = false,
    onPlayVoice: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isUser = message.sender == Sender.USER
    val context = LocalContext.current

    val bubbleShape = if (isUser) {
        RoundedCornerShape(
            topStart = colors.bubbleRadius,
            topEnd = colors.bubbleRadius,
            bottomStart = colors.bubbleRadius,
            bottomEnd = 4.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 4.dp,
            topEnd = colors.bubbleRadius,
            bottomStart = colors.bubbleRadius,
            bottomEnd = colors.bubbleRadius
        )
    }

    val bubbleBg = if (isUser) colors.userBubbleBackground else colors.botBubbleBackground
    val textColor = if (isUser) colors.userBubbleText else colors.botBubbleText
    val timeColor = if (isUser) colors.userBubbleTime else colors.botBubbleTime

    val timeFormatted = rememberFormattedTime(message.timestamp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 330.dp)
                .clip(bubbleShape)
                .background(bubbleBg)
                .border(
                    width = 0.5.dp,
                    color = if (isUser) colors.accentColor.copy(alpha = 0.2f) else colors.cardBorder,
                    shape = bubbleShape
                )
                .padding(12.dp)
                .testTag(if (isUser) "user_bubble_${message.id}" else "bot_bubble_${message.id}")
        ) {
            Column {
                // Sender label for bot + Sentiment Emoji Icon next to AI's response
                if (!isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = "حامد (hamed)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accentColor
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Real-time sentiment badge with emoji icon next to AI response
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.accentColor.copy(alpha = 0.14f))
                                .clickable { onShowSentiment(message) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("ai_sentiment_badge_${message.id}")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = message.sentiment.emoji,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = message.sentiment.titleFa,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.accentColor
                                )
                            }
                        }
                    }
                }

                // Audio message indicator
                if (message.isAudio) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.accentColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = colors.accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "پیام صوتی ضبط شده",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accentColor
                        )
                    }
                }

                // Main message text
                Text(
                    text = message.text,
                    fontSize = 15.sp,
                    color = textColor,
                    lineHeight = 22.sp
                )

                // Persian Learning Tip Chip (inside bot messages)
                if (!isUser && message.learningTip != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.accentColor.copy(alpha = 0.12f))
                            .clickable { onShowLearningTip(message.learningTip) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("learning_chip_${message.id}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = colors.accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "نکته فارسی: ${message.learningTip.title}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.accentColor,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "جزئیات ❯",
                                fontSize = 11.sp,
                                color = colors.accentColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom row: Sentiment badge (for user) or Actions (for bot) + Timestamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isUser) {
                        // Sentiment badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.accentColor.copy(alpha = 0.15f))
                                .clickable { onShowSentiment(message) }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .testTag("sentiment_badge_${message.id}")
                        ) {
                            Text(
                                text = "${message.sentiment.emoji} ${message.sentiment.titleFa}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.accentColor
                            )
                        }
                    } else {
                        // Action buttons: Play Voice, Copy, Share
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onPlayVoice,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSpeakingThis) colors.accentColor.copy(alpha = 0.2f) else Color.Transparent
                                    )
                                    .testTag("play_voice_${message.id}")
                            ) {
                                Icon(
                                    imageVector = if (isSpeakingThis) Icons.Default.Stop else Icons.Default.VolumeUp,
                                    contentDescription = if (isSpeakingThis) "توقف پخش صدا" else "پخش صوتی پیام حامد",
                                    tint = if (isSpeakingThis) colors.accentColor else timeColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(2.dp))
                            IconButton(
                                onClick = {
                                    copyToClipboard(context, message.text)
                                },
                                modifier = Modifier
                                    .size(26.dp)
                                    .testTag("copy_message_${message.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "کپی متن",
                                    tint = timeColor,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = {
                                    shareToMessenger(context, message.text)
                                },
                                modifier = Modifier
                                    .size(26.dp)
                                    .testTag("share_message_${message.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "اشتراک‌گذاری در پیام‌رسان",
                                    tint = timeColor,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    // Timestamp
                    Text(
                        text = timeFormatted,
                        fontSize = 11.sp,
                        color = timeColor
                    )
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("hamed_chat", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "متن کپی شد", Toast.LENGTH_SHORT).show()
}

private fun shareToMessenger(context: Context, text: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "از طرف حامد (hamed):\n\n$text")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "ارسال به پیام‌رسان‌ها (تلگرام، واتساپ، بله)")
    context.startActivity(shareIntent)
}

@Composable
private fun rememberFormattedTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formatted = sdf.format(Date(timestamp))
    return toPersianDigits(formatted)
}

private fun toPersianDigits(input: String): String {
    val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    val builder = java.lang.StringBuilder()
    for (ch in input) {
        if (ch in '0'..'9') {
            builder.append(persianDigits[ch - '0'])
        } else {
            builder.append(ch)
        }
    }
    return builder.toString()
}

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.ChatMessage
import com.example.ui.theme.MessengerColors

@Composable
fun SentimentDetailDialog(
    message: ChatMessage,
    colors: MessengerColors,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("sentiment_detail_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with emotion emoji
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = message.sentiment.emoji, fontSize = 28.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "تحلیل احساس کاربر",
                            fontSize = 13.sp,
                            color = colors.botBubbleTime
                        )
                        Text(
                            text = message.sentiment.titleFa,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.botBubbleText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Confidence Bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "میزان قطعیت تشخیص احساس:",
                            fontSize = 12.sp,
                            color = colors.botBubbleTime,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${(message.sentimentScore * 100).toInt()}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accentColor
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { message.sentimentScore },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = colors.accentColor,
                        trackColor = colors.inputFieldBackground
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Description Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.inputFieldBackground)
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "توضیحات و مفهوم احساس:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.botBubbleText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = message.sentiment.descriptionFa,
                            fontSize = 13.sp,
                            color = colors.botBubbleText.copy(alpha = 0.85f),
                            lineHeight = 20.sp
                        )

                        if (!message.sentimentAnalysis.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "بازخورد هوشمند حامد:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.accentColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = message.sentimentAnalysis,
                                fontSize = 12.sp,
                                color = colors.botBubbleText.copy(alpha = 0.85f),
                                lineHeight = 19.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("close_sentiment_dialog_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accentColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "متوجه شدم", color = colors.topBarContent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.example.model.PersianLearningTip
import com.example.ui.theme.MessengerColors

@Composable
fun LearningTipDialog(
    tip: PersianLearningTip,
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
                .testTag("learning_tip_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = colors.accentColor
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "یادگیری زبان فارسی با حامد",
                            fontSize = 12.sp,
                            color = colors.botBubbleTime
                        )
                        Text(
                            text = tip.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.botBubbleText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tip Explanation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.inputFieldBackground)
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "شرح و نکته آموزشی:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.botBubbleText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tip.explanation,
                            fontSize = 13.sp,
                            color = colors.botBubbleText.copy(alpha = 0.9f),
                            lineHeight = 21.sp
                        )

                        if (!tip.example.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "نمونه کاربرد در جمله:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.accentColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tip.example,
                                fontSize = 13.sp,
                                color = colors.botBubbleText.copy(alpha = 0.9f),
                                lineHeight = 20.sp
                            )
                        }

                        if (!tip.culturalNote.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "ریشه فرهنگی و ضرب‌المثل مرتبط:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.accentColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tip.culturalNote,
                                fontSize = 13.sp,
                                color = colors.botBubbleText.copy(alpha = 0.9f),
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("close_learning_dialog_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accentColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "عالی بود", color = colors.topBarContent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

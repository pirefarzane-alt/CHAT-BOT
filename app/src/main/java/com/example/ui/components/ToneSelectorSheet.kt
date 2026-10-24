package com.example.ui.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HamedTone
import com.example.ui.theme.MessengerColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToneSelectorSheet(
    currentTone: HamedTone,
    colors: MessengerColors,
    onToneSelected: (HamedTone) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.cardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "شخصی‌سازی لحن پاسخ‌های حامد (hamed)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.botBubbleText
            )
            Text(
                text = "لحن گفت‌وگوی حامد را متناسب با نیاز خود تغییر دهید:",
                fontSize = 13.sp,
                color = colors.botBubbleTime,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(HamedTone.entries) { tone ->
                    val isSelected = tone == currentTone
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) colors.accentColor.copy(alpha = 0.15f)
                                else colors.inputFieldBackground
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) colors.accentColor else colors.cardBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                onToneSelected(tone)
                                onDismiss()
                            }
                            .padding(14.dp)
                            .testTag("tone_option_${tone.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tone.emoji,
                            fontSize = 26.sp,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = tone.titleFa,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) colors.accentColor else colors.botBubbleText
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "(فعال)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.accentColor
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tone.subtitleFa,
                                fontSize = 12.sp,
                                color = colors.botBubbleTime
                            )
                        }
                    }
                }
            }
        }
    }
}

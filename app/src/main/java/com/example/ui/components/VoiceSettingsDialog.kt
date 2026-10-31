package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.ui.theme.MessengerColors
import com.example.util.TtsVoiceInfo
import java.util.Locale

data class CloudVoiceOption(
    val id: String,
    val titleFa: String,
    val descriptionFa: String
)

val CLOUD_VOICES = listOf(
    CloudVoiceOption("Puck", "حامد (مردانه • پرانرژی و جذاب)", "صدای طبیعی و زنده مردانه - پیش‌فرض حامد"),
    CloudVoiceOption("Aoede", "صدای بانو / هنرپیشه (زنانه • صدای اول برنامه)", "همان صدای تئاتری و فوق‌العاده زیبا که ابتدا شنیده بودید"),
    CloudVoiceOption("Fenrir", "حامد متین (مردانه • بم و باوقار)", "لحن سنگین و فاخر، مناسب برای اشعار و متون رسمی"),
    CloudVoiceOption("Kore", "صدای آرامش‌بخش (زنانه • گرم و مهربان)", "لحن ملایم و صمیمی، آرام و دلنشین")
)

@Composable
fun VoiceSettingsDialog(
    colors: MessengerColors,
    availableDeviceVoices: List<TtsVoiceInfo>,
    selectedDeviceVoice: String?,
    selectedCloudVoice: String,
    speechRate: Float,
    speechPitch: Float,
    isPlayingPreview: Boolean,
    onSpeechRateChange: (Float) -> Unit,
    onSpeechPitchChange: (Float) -> Unit,
    onSelectDeviceVoice: (String?) -> Unit,
    onSelectCloudVoice: (String) -> Unit,
    onTestVoice: () -> Unit,
    onOpenTtsSettings: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .testTag("voice_settings_dialog"),
            color = colors.cardBackground,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colors.accentColor.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = colors.accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "تنظیمات صدا و گفتار",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.botBubbleText
                            )
                            Text(
                                text = "شخصی‌سازی گفتار حامد (hamed)",
                                fontSize = 11.sp,
                                color = colors.botBubbleTime
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = colors.botBubbleTime
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 1: Speech Rate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = colors.accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "سرعت خوانش گفتار:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.botBubbleText
                        )
                    }
                    Text(
                        text = "${String.format(Locale.US, "%.2f", speechRate)}x",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accentColor
                    )
                }

                Slider(
                    value = speechRate,
                    onValueChange = onSpeechRateChange,
                    valueRange = 0.5f..1.8f,
                    steps = 12,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.accentColor,
                        activeTrackColor = colors.accentColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("speech_rate_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("آهسته (۰.۵x)", fontSize = 10.sp, color = colors.botBubbleTime)
                    Text("استاندارد (۱.۰x)", fontSize = 10.sp, color = colors.botBubbleTime)
                    Text("سریع (۱.۸x)", fontSize = 10.sp, color = colors.botBubbleTime)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Speech Pitch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = colors.accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "زیر و بمی صدا (تن / Pitch):",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.botBubbleText
                        )
                    }
                    Text(
                        text = "${String.format(Locale.US, "%.2f", speechPitch)}x",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accentColor
                    )
                }

                Slider(
                    value = speechPitch,
                    onValueChange = onSpeechPitchChange,
                    valueRange = 0.6f..1.4f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.accentColor,
                        activeTrackColor = colors.accentColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("speech_pitch_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("بم / عمیق", fontSize = 10.sp, color = colors.botBubbleTime)
                    Text("طبیعی (۱.۰x)", fontSize = 10.sp, color = colors.botBubbleTime)
                    Text("زیر / شاداب", fontSize = 10.sp, color = colors.botBubbleTime)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 3: Speech Personas
                Text(
                    text = "انتخاب سبک و شخصیت گفتاری حامد:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.botBubbleText,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                CLOUD_VOICES.forEach { voice ->
                    val isSelected = selectedCloudVoice == voice.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable { onSelectCloudVoice(voice.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) colors.accentColor.copy(alpha = 0.12f) else colors.cardBackground
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelectCloudVoice(voice.id) },
                                colors = RadioButtonDefaults.colors(selectedColor = colors.accentColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = voice.titleFa,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) colors.accentColor else colors.botBubbleText
                                )
                                Text(
                                    text = voice.descriptionFa,
                                    fontSize = 10.sp,
                                    color = colors.botBubbleTime
                                )
                            }
                        }
                    }
                }

                // Section 4: On-Device Voices or Missing Voice Guide
                if (availableDeviceVoices.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "صداهای فارسی شناسایی‌شده روی گوشی شما:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.botBubbleText,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    availableDeviceVoices.forEach { dVoice ->
                        val isSelected = selectedDeviceVoice == dVoice.name
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable { onSelectDeviceVoice(dVoice.name) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) colors.accentColor.copy(alpha = 0.12f) else colors.cardBackground
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onSelectDeviceVoice(dVoice.name) },
                                    colors = RadioButtonDefaults.colors(selectedColor = colors.accentColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = dVoice.displayName,
                                    fontSize = 11.sp,
                                    color = colors.botBubbleText
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenTtsSettings() },
                        colors = CardDefaults.cardColors(
                            containerColor = colors.accentColor.copy(alpha = 0.08f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = colors.accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "بسته صوتی زبان فارسی روی گوشی نصب نیست",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.botBubbleText
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "جهت شنیدن صدای حامد، اینجا را لمس کنید و داده صوتی فارسی Google Speech را فعال نمایید.",
                                    fontSize = 10.sp,
                                    lineHeight = 15.sp,
                                    color = colors.botBubbleTime
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons: Test Voice & Save/Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onTestVoice,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_voice_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlayingPreview) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = colors.accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPlayingPreview) "توقف تست" else "آزمایش صدا",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accentColor
                        )
                    }

                    Button(
                        onClick = {
                            Toast.makeText(context, "تنظیمات گفتار ذخیره شد", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_voice_settings_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accentColor)
                    ) {
                        Text(
                            text = "ذخیره و تأیید",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.topBarContent
                        )
                    }
                }
            }
        }
    }
}

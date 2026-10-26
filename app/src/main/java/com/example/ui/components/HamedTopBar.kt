package com.example.ui.components

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.PlatformTheme
import com.example.ui.theme.MessengerColors
import com.google.firebase.auth.FirebaseUser

@Composable
fun HamedTopBar(
    platformTheme: PlatformTheme,
    currentUser: FirebaseUser?,
    isAuthLoading: Boolean,
    colors: MessengerColors,
    autoPlayVoice: Boolean,
    onSignInWithGoogle: (Activity) -> Unit,
    onSignOut: () -> Unit,
    onSwitchTheme: (PlatformTheme) -> Unit,
    onOpenToneSelector: () -> Unit,
    onClearChat: () -> Unit,
    onOpenLiveCall: () -> Unit,
    onToggleAutoPlayVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? Activity

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.topBarBackground)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hamed Bot Avatar
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(colors.cardBackground)
                    .clickable { onOpenToneSelector() }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_hamed_logo),
                    contentDescription = "تصویر پروفایل حامد",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Bot Info & Status (Guaranteed Single-Line to prevent vertical letter wrapping)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenToneSelector() }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "hamed (حامد)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.topBarContent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(colors.accentColor)
                    )
                    if (currentUser != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "همگام‌سازی ابری فایربیس",
                            tint = colors.accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = if (currentUser != null) "${platformTheme.statusTextFa} • ابری" else platformTheme.statusTextFa,
                    fontSize = 11.sp,
                    color = colors.topBarSubtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Google Account / Sign-In (Compact Pill)
            if (currentUser != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.cardBackground.copy(alpha = 0.25f))
                        .clickable { onSignOut() }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("sign_out_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "خروج",
                            tint = colors.topBarContent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentUser.displayName?.split(" ")?.firstOrNull()?.take(7) ?: "کاربر",
                            fontSize = 11.sp,
                            color = colors.topBarContent,
                            maxLines = 1
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.accentColor)
                        .clickable(enabled = !isAuthLoading && activity != null) {
                            activity?.let { onSignInWithGoogle(it) }
                        }
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                        .testTag("sign_in_google_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isAuthLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = colors.topBarContent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = colors.topBarContent,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ورود",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.topBarContent
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Standard Messenger Call Button (Icon-only, compact and clean like WhatsApp/Telegram)
            IconButton(
                onClick = onOpenLiveCall,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.accentColor.copy(alpha = 0.2f))
                    .testTag("live_voice_call_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "مکالمه صوتی زنده با حامد",
                    tint = colors.accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            // More Options Menu
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("top_bar_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "گزینه‌های بیشتر",
                        tint = colors.topBarContent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    // Voice Autoplay Toggle
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (autoPlayVoice) "خواندن خودکار صدا: فعال ✓" else "خواندن خودکار صدا: خاموش",
                                fontWeight = if (autoPlayVoice) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (autoPlayVoice) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = null,
                                tint = if (autoPlayVoice) colors.accentColor else colors.botBubbleTime,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onToggleAutoPlayVoice()
                        },
                        modifier = Modifier.testTag("toggle_auto_play_voice")
                    )

                    // Tone Selector in menu
                    DropdownMenuItem(
                        text = {
                            Text(text = "تنظیمات لحن بیان", fontSize = 13.sp)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onOpenToneSelector()
                        },
                        modifier = Modifier.testTag("tone_menu_item")
                    )

                    PlatformTheme.entries.forEach { theme ->
                        val isSelected = theme == platformTheme
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "پوسته ${theme.nameFa} (${theme.messengerName}) ${if (isSelected) "✓" else ""}",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onSwitchTheme(theme)
                            },
                            modifier = Modifier.testTag("theme_menu_${theme.id}")
                        )
                    }

                    DropdownMenuItem(
                        text = { Text("پاکسازی گفت‌وگو", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            menuExpanded = false
                            onClearChat()
                        },
                        modifier = Modifier.testTag("clear_chat_button")
                    )
                }
            }
        }
    }
}

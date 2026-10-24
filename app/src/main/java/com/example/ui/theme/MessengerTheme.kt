package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.PlatformTheme

data class MessengerColors(
    val topBarBackground: Color,
    val topBarContent: Color,
    val topBarSubtitle: Color,
    val chatBackgroundStart: Color,
    val chatBackgroundEnd: Color,
    val userBubbleBackground: Color,
    val userBubbleText: Color,
    val userBubbleTime: Color,
    val botBubbleBackground: Color,
    val botBubbleText: Color,
    val botBubbleTime: Color,
    val accentColor: Color,
    val inputBarBackground: Color,
    val inputFieldBackground: Color,
    val inputFieldText: Color,
    val sendButtonColor: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val bubbleRadius: Dp
)

object MessengerThemePalette {

    fun getColors(theme: PlatformTheme, isDark: Boolean): MessengerColors {
        return when (theme) {
            PlatformTheme.TELEGRAM -> if (isDark) {
                MessengerColors(
                    topBarBackground = Color(0xFF212D3B),
                    topBarContent = Color(0xFFF5F5F5),
                    topBarSubtitle = Color(0xFF7E97AE),
                    chatBackgroundStart = Color(0xFF0E1621),
                    chatBackgroundEnd = Color(0xFF17212B),
                    userBubbleBackground = Color(0xFF2B5278),
                    userBubbleText = Color(0xFFF1F5F9),
                    userBubbleTime = Color(0xFF90B0D0),
                    botBubbleBackground = Color(0xFF182533),
                    botBubbleText = Color(0xFFE2E8F0),
                    botBubbleTime = Color(0xFF6C7E90),
                    accentColor = Color(0xFF54A9EB),
                    inputBarBackground = Color(0xFF17212B),
                    inputFieldBackground = Color(0xFF242F3D),
                    inputFieldText = Color(0xFFF8FAFC),
                    sendButtonColor = Color(0xFF2AABEE),
                    cardBackground = Color(0xFF1F2D3D),
                    cardBorder = Color(0xFF2C3E52),
                    bubbleRadius = 16.dp
                )
            } else {
                MessengerColors(
                    topBarBackground = Color(0xFF527DA3),
                    topBarContent = Color(0xFFFFFFFF),
                    topBarSubtitle = Color(0xFFD6E3EF),
                    chatBackgroundStart = Color(0xFFDCE6EF),
                    chatBackgroundEnd = Color(0xFFCAD8E6),
                    userBubbleBackground = Color(0xFFEFFDDE),
                    userBubbleText = Color(0xFF1E293B),
                    userBubbleTime = Color(0xFF5A8754),
                    botBubbleBackground = Color(0xFFFFFFFF),
                    botBubbleText = Color(0xFF0F172A),
                    botBubbleTime = Color(0xFF94A3B8),
                    accentColor = Color(0xFF2AABEE),
                    inputBarBackground = Color(0xFFFFFFFF),
                    inputFieldBackground = Color(0xFFF1F5F9),
                    inputFieldText = Color(0xFF0F172A),
                    sendButtonColor = Color(0xFF229ED9),
                    cardBackground = Color(0xFFFFFFFF),
                    cardBorder = Color(0xFFE2E8F0),
                    bubbleRadius = 16.dp
                )
            }

            PlatformTheme.WHATSAPP -> if (isDark) {
                MessengerColors(
                    topBarBackground = Color(0xFF1F2C34),
                    topBarContent = Color(0xFFE9EDEF),
                    topBarSubtitle = Color(0xFF8696A0),
                    chatBackgroundStart = Color(0xFF0B141A),
                    chatBackgroundEnd = Color(0xFF121B22),
                    userBubbleBackground = Color(0xFF005C4B),
                    userBubbleText = Color(0xFFE9EDEF),
                    userBubbleTime = Color(0xFF8696A0),
                    botBubbleBackground = Color(0xFF202C33),
                    botBubbleText = Color(0xFFE9EDEF),
                    botBubbleTime = Color(0xFF8696A0),
                    accentColor = Color(0xFF00A884),
                    inputBarBackground = Color(0xFF1F2C34),
                    inputFieldBackground = Color(0xFF2A3942),
                    inputFieldText = Color(0xFFE9EDEF),
                    sendButtonColor = Color(0xFF00A884),
                    cardBackground = Color(0xFF202C33),
                    cardBorder = Color(0xFF2A3942),
                    bubbleRadius = 10.dp
                )
            } else {
                MessengerColors(
                    topBarBackground = Color(0xFF075E54),
                    topBarContent = Color(0xFFFFFFFF),
                    topBarSubtitle = Color(0xFFB4D8D2),
                    chatBackgroundStart = Color(0xFFEFE7DE),
                    chatBackgroundEnd = Color(0xFFE5DDD5),
                    userBubbleBackground = Color(0xFFE7FFDB),
                    userBubbleText = Color(0xFF111B21),
                    userBubbleTime = Color(0xFF667781),
                    botBubbleBackground = Color(0xFFFFFFFF),
                    botBubbleText = Color(0xFF111B21),
                    botBubbleTime = Color(0xFF667781),
                    accentColor = Color(0xFF25D366),
                    inputBarBackground = Color(0xFFF0F2F5),
                    inputFieldBackground = Color(0xFFFFFFFF),
                    inputFieldText = Color(0xFF111B21),
                    sendButtonColor = Color(0xFF128C7E),
                    cardBackground = Color(0xFFFFFFFF),
                    cardBorder = Color(0xFFE0E0E0),
                    bubbleRadius = 10.dp
                )
            }

            PlatformTheme.BALE -> if (isDark) {
                MessengerColors(
                    topBarBackground = Color(0xFF152A42),
                    topBarContent = Color(0xFFFFFFFF),
                    topBarSubtitle = Color(0xFF90AFC9),
                    chatBackgroundStart = Color(0xFF0E1A29),
                    chatBackgroundEnd = Color(0xFF142436),
                    userBubbleBackground = Color(0xFF1D4773),
                    userBubbleText = Color(0xFFF8FAFC),
                    userBubbleTime = Color(0xFF94BBE2),
                    botBubbleBackground = Color(0xFF1A2B3D),
                    botBubbleText = Color(0xFFF1F5F9),
                    botBubbleTime = Color(0xFF7E97B0),
                    accentColor = Color(0xFFFF7A00),
                    inputBarBackground = Color(0xFF152A42),
                    inputFieldBackground = Color(0xFF1E3550),
                    inputFieldText = Color(0xFFF8FAFC),
                    sendButtonColor = Color(0xFFFF6F00),
                    cardBackground = Color(0xFF192C40),
                    cardBorder = Color(0xFF2B4461),
                    bubbleRadius = 14.dp
                )
            } else {
                MessengerColors(
                    topBarBackground = Color(0xFF1D70B8),
                    topBarContent = Color(0xFFFFFFFF),
                    topBarSubtitle = Color(0xFFD3E7FA),
                    chatBackgroundStart = Color(0xFFF2F6FA),
                    chatBackgroundEnd = Color(0xFFE3EDF7),
                    userBubbleBackground = Color(0xFFD6E9FC),
                    userBubbleText = Color(0xFF0F172A),
                    userBubbleTime = Color(0xFF4A729A),
                    botBubbleBackground = Color(0xFFFFFFFF),
                    botBubbleText = Color(0xFF0F172A),
                    botBubbleTime = Color(0xFF889BB0),
                    accentColor = Color(0xFFFF6F00),
                    inputBarBackground = Color(0xFFFFFFFF),
                    inputFieldBackground = Color(0xFFF0F4F8),
                    inputFieldText = Color(0xFF0F172A),
                    sendButtonColor = Color(0xFF1D70B8),
                    cardBackground = Color(0xFFFFFFFF),
                    cardBorder = Color(0xFFD9E4EF),
                    bubbleRadius = 14.dp
                )
            }
        }
    }
}

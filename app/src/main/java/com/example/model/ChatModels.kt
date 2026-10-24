package com.example.model

import java.util.UUID

enum class Sender {
    USER,
    HAMED
}

enum class LiveVoiceState {
    IDLE,        // Ready for voice input or idle
    LISTENING,   // Actively recording user speech
    PROCESSING,  // Transcribing user speech and generating Hamed reply
    SPEAKING     // Hamed is speaking reply aloud in real-time
}

enum class SentimentType(
    val titleFa: String,
    val emoji: String,
    val descriptionFa: String
) {
    POSITIVE("خوشحال و پرانرژی", "😊", "احساس شادی، رضایت، امیدواری و انرژی مثبت"),
    NEUTRAL("خنثی و منطقی", "😐", "بیان متین، علمی، واقع‌بینانه و بدون هیجان خاص"),
    SAD("غمگین یا متأثر", "😔", "بیان حزن، تأسف، درد یا همدردی عمیق"),
    CURIOUS("کنجکاو و متفکر", "🤔", "تمایل به کاوش، پرسشگری و تفکر عمیق"),
    CALM("آرام و ملایم", "😌", "بیان متین، آرامش‌بخش و آسوده‌خاطر"),
    CONCERNED("نگران و محتاط", "😟", "احساس دلواپسی، تردید یا نیاز به راهنمایی آرامش‌بخش"),
    FRUSTRATED("ناراحت یا شاکی", "😤", "نیاز به درک همدلانه، صبوری و لحن تسکین‌دهنده")
}

data class PersianLearningTip(
    val title: String,
    val explanation: String,
    val example: String? = null,
    val culturalNote: String? = null
)

enum class HamedTone(
    val id: String,
    val titleFa: String,
    val subtitleFa: String,
    val emoji: String,
    val promptInstruction: String
) {
    FORMAL(
        id = "formal",
        titleFa = "رسمی (Formal)",
        subtitleFa = "ادبیات اداری، محترمانه و دقیق کتابی",
        emoji = "👔",
        promptInstruction = "پاسخ‌ها را کاملاً رسمی، محترمانه، با ادبیات شایسته اداری و کتابی بنویس؛ بدون به‌کاربردن کلمات محاوره‌ای و شکسته."
    ),
    FRIENDLY(
        id = "casual",
        titleFa = "صمیمی (Casual)",
        subtitleFa = "لحن خودمانی، گرم و دوستانه",
        emoji = "🤝",
        promptInstruction = "پاسخ‌ها را کاملاً صمیمی، خودمانی، گرم و به زبان محاوره‌ای فارسی بنویس؛ مانند گفت‌وگوی دو دوست بسیار نزدیک و راحت."
    ),
    POETIC(
        id = "poetic",
        titleFa = "شاعرانه (Poetic)",
        subtitleFa = "آراسته به نظم و تمثیل‌های فارسی",
        emoji = "📜",
        promptInstruction = "پاسخ‌ها را با بیانی شاعرانه، ادبیاتی فاخر، همراه با استعاره‌های زیبا، تمثیل‌ها و در صورت امکان ابیات دلنشین کهن فارسی زینت ببخش."
    ),
    WITTY(
        id = "witty",
        titleFa = "شوخ‌طبع (Witty)",
        subtitleFa = "چاشنی طنز و خوش‌رویی",
        emoji = "😄",
        promptInstruction = "پاسخ‌ها را با انرژی بالا، شوخ‌طبعی مودبانه و نکته‌سنجی طنزآمیز ایرانی بنویس."
    ),
    ACADEMIC(
        id = "academic",
        titleFa = "علمی (Academic)",
        subtitleFa = "تحلیلی، مستند و نظام‌مند",
        emoji = "🔬",
        promptInstruction = "پاسخ‌ها را کاملاً ساختاریافته، علمی، مستدل و با واژگان دقیق تخصصی و بدون حاشیه‌پردازی ارائه بده."
    ),
    EDUCATIONAL(
        id = "educational",
        titleFa = "آموزشی (Educational)",
        subtitleFa = "راهنمای گام‌به‌گام و تقویت زبان",
        emoji = "📚",
        promptInstruction = "پاسخ‌ها را با رویکرد آموزشی و توضیح گام‌به‌گام بنویس و نکات واژگانی یا نگارشی فارسی را آموزش بده."
    )
}

enum class PlatformTheme(
    val id: String,
    val nameFa: String,
    val messengerName: String,
    val statusTextFa: String
) {
    TELEGRAM(
        id = "telegram",
        nameFa = "تلگرام",
        messengerName = "Telegram",
        statusTextFa = "آنلاین • hamed_bot"
    ),
    WHATSAPP(
        id = "whatsapp",
        nameFa = "واتساپ",
        messengerName = "WhatsApp",
        statusTextFa = "hamed • حساب تجاری رسمی"
    ),
    BALE(
        id = "bale",
        nameFa = "بله",
        messengerName = "Bale",
        statusTextFa = "حامد (hamed) • بازوی هوشمند"
    )
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: Sender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sentiment: SentimentType = SentimentType.CALM,
    val sentimentScore: Float = 0.85f,
    val sentimentAnalysis: String? = null,
    val learningTip: PersianLearningTip? = null,
    val isAudio: Boolean = false,
    val audioDurationSec: Int = 0
)

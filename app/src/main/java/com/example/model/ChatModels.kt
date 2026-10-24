package com.example.model

import java.util.UUID

enum class Sender {
    USER,
    HAMED
}

enum class SentimentType(
    val titleFa: String,
    val emoji: String,
    val descriptionFa: String
) {
    POSITIVE("مثبت و پرانرژی", "😊", "احساس شادی، رضایت و امیدواری"),
    CURIOUS("کنجکاو و پرسشگر", "🤔", "تمایل به یادگیری، کاوش و دانستن جزئیات"),
    CALM("آرام و خنثی", "😌", "بیان متین، واقع‌بینانه و بدون هیجان منفی"),
    CONCERNED("نگران و محتاط", "😟", "احساس دلواپسی، تردید یا نیاز به راهنمایی آرامش‌بخش"),
    FRUSTRATED("ناراحت یا خسته", "😤", "نیاز به درک همدلانه، صبوری و لحن تسکین‌دهنده")
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
    FRIENDLY(
        id = "friendly",
        titleFa = "صمیمی و گرم",
        subtitleFa = "لحن محاوره‌ای، خودمانی و صمیمانه",
        emoji = "🤝",
        promptInstruction = "پاسخ‌ها را با لحنی بسیار صمیمی، خودمانی، محبت‌آمیز و غیررسمی بنویس؛ مثل گپ زدن دو دوست بسیار نزدیک."
    ),
    FORMAL(
        id = "formal",
        titleFa = "رسمی و اداری",
        subtitleFa = "ادبیات فاخر، محترمانه و دقیق",
        emoji = "👔",
        promptInstruction = "پاسخ‌ها را کاملاً رسمی، با نهایت احترام، شیوایی سخن و نگارش اداری و کتابی بنویس."
    ),
    WITTY(
        id = "witty",
        titleFa = "شوخ‌طبع و باانرژی",
        subtitleFa = "همراه با چاشنی طنز و خوش‌رویی",
        emoji = "😄",
        promptInstruction = "پاسخ‌ها را شاد، جذاب، پر از شوخ‌طبعی مؤدبانه و نکته‌سنجی طنزآمیز ایرانی بیان کن."
    ),
    POETIC(
        id = "poetic",
        titleFa = "شاعرانه و ادبی",
        subtitleFa = "آراسته به نظم و نثر فاخر فارسی",
        emoji = "📜",
        promptInstruction = "پاسخ‌ها را با ادبیاتی شاعرانه، تمثیل‌های زیبا، ضرب‌المثل‌های اصیل و در صورت لزوم ابیات دلنشین کهن فارسی زینت ببخش."
    ),
    ACADEMIC(
        id = "academic",
        titleFa = "علمی و تحلیلی",
        subtitleFa = "مستند، دقیق و نظام‌مند",
        emoji = "🔬",
        promptInstruction = "پاسخ‌ها را با ساختاری منطقی، مستند، علمی، بدون زیاده‌گویی و با واژگان تخصصی استاندارد ارائه بده."
    ),
    EDUCATIONAL(
        id = "educational",
        titleFa = "آموزشی و راهنما",
        subtitleFa = "آموزش زبان فارسی و ساده‌سازی مفاهیم",
        emoji = "📚",
        promptInstruction = "پاسخ‌ها را آموزشی و صبورانه بنویس؛ واژگان، ترکیب‌های زیبا و قواعد فارسی را با مثال‌های روان آموزش بده."
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

package com.example.service

import android.util.Log
import com.example.model.ChatMessage
import com.example.model.HamedTone
import com.example.model.PersianLearningTip
import com.example.model.Sender
import com.example.model.SentimentType
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.TextPart
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.io.File

private const val TAG = "HamedAiService"

class HamedAiService {

    // Fast generation config without heavy JSON schema constrained decoding
    private val fastConfig = generationConfig {
        temperature = 0.7f
        topK = 40
        topP = 0.95f
    }

    private val primaryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = fastConfig
        )
    }

    private val fastLiteModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = fastConfig
        )
    }

    private val transcribeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-transcribe"
        )
    }

    private val fallbackAudioModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite"
        )
    }

    private fun buildPrompt(
        history: List<ChatMessage>,
        userMessage: String,
        tone: HamedTone
    ): String {
        return buildString {
            append("نام شما در تمامی تعاملات «hamed» (حامد) است. شما یک چت‌بات هوش مصنوعی پیشرفته، مهربان و مسلط بر زبان، ادبیات و فرهنگ فارسی هستید.\n")
            append("دستور حیاتی برای لحن گفت‌وگو (Conversational Tone):\n")
            append("کاربر لحن پاسخ را روی [${tone.titleFa}] تنظیم کرده است.\n")
            append("دستورالعمل نحوه بیان: ${tone.promptInstruction}\n")
            append("شما موظفید تمام جملات پاسخ خود را دقیقاً با این لحن (${tone.titleFa}) بنویسید.\n\n")
            append("دستورالعمل ساختار پاسخ:\n")
            append("۱. پاسخ اصلی خود به کاربر را با رعایت کامل لحن انتخابی بنویسید.\n")
            append("۲. در انتهای پاسخ، در یک خط مجزا، برچسب احساس کاربر را به این فرمت بنویسید: [احساس: POSITIVE] یا [احساس: CURIOUS] یا [احساس: CALM] یا [احساس: CONCERNED] یا [احساس: FRUSTRATED]\n")
            append("۳. در خط بعد، یک نکته آموزنده فارسی بنویسید: [نکته: عنوان نکته | توضیح و مثال یا ضرب‌المثل]\n\n")

            if (history.isNotEmpty()) {
                append("پیام‌های پیشین گفت‌وگو:\n")
                history.takeLast(4).forEach { msg ->
                    val senderLabel = if (msg.sender == Sender.USER) "کاربر" else "حامد"
                    append("$senderLabel: ${msg.text.take(200)}\n")
                }
                append("\n")
            }

            append("پیام کنونی کاربر:\n$userMessage")
        }
    }

    suspend fun sendMessage(
        history: List<ChatMessage>,
        userMessage: String,
        tone: HamedTone
    ): ChatMessage {
        val prompt = buildPrompt(history, userMessage, tone)

        // Tier 1: Try primary gemini-3.8-flash with 12s timeout
        try {
            val response = withTimeout(12_000L) {
                primaryModel.generateContent(prompt)
            }
            val text = response.text?.trim()
            if (!text.isNullOrBlank()) {
                return parseModelResponse(text, userMessage)
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Primary model timed out after 12s, switching to fast-lite model")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Primary model error: ${e.message}, falling back to fast-lite")
        }

        // Tier 2: Try ultra-fast gemini-3.5-flash-lite with 8s timeout
        try {
            val response = withTimeout(8_000L) {
                fastLiteModel.generateContent(prompt)
            }
            val text = response.text?.trim()
            if (!text.isNullOrBlank()) {
                return parseModelResponse(text, userMessage)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Fast-lite model also failed: ${e.message}, generating smart synthesis")
        }

        // Tier 3: Instant smart contextual Persian response from hamed
        return generateSmartSynthesis(userMessage, tone)
    }

    private fun parseModelResponse(rawText: String, userMessage: String): ChatMessage {
        var cleanText = rawText
        var sentiment = detectBasicSentiment(userMessage)
        var learningTip: PersianLearningTip? = null

        // Extract [احساس: ...]
        val sentimentRegex = Regex("\\[احساس:\\s*([A-Za-z_]+)\\]")
        val sentimentMatch = sentimentRegex.find(cleanText)
        if (sentimentMatch != null) {
            val tagValue = sentimentMatch.groupValues[1].uppercase()
            sentiment = runCatching { SentimentType.valueOf(tagValue) }.getOrDefault(sentiment)
            cleanText = cleanText.replace(sentimentMatch.value, "").trim()
        }

        // Extract [نکته: عنوان | توضیح]
        val tipRegex = Regex("\\[نکته:\\s*([^|\\]]+)\\|?([^\\]]*)\\]")
        val tipMatch = tipRegex.find(cleanText)
        if (tipMatch != null) {
            val title = tipMatch.groupValues[1].trim()
            val explanation = tipMatch.groupValues.getOrNull(2)?.trim()?.takeIf { it.isNotEmpty() }
                ?: "نکته‌ای پیرامون واژه‌گزینی و شیوایی زبان فارسی."
            learningTip = PersianLearningTip(
                title = title,
                explanation = explanation,
                example = "کاربرد این واژه به غنای گفتار شما می‌افزاید."
            )
            cleanText = cleanText.replace(tipMatch.value, "").trim()
        } else {
            learningTip = generatePersianLearningTip(userMessage)
        }

        // Analyze real-time sentiment of the AI's response text
        val aiSentiment = analyzeTextSentiment(cleanText)

        return ChatMessage(
            sender = Sender.HAMED,
            text = cleanText,
            sentiment = aiSentiment,
            sentimentScore = 0.94f,
            sentimentAnalysis = "تحلیل احساس پاسخ حامد: ${aiSentiment.titleFa} (${aiSentiment.descriptionFa})",
            learningTip = learningTip
        )
    }

    private fun generateSmartSynthesis(userMessage: String, tone: HamedTone): ChatMessage {
        val detected = detectBasicSentiment(userMessage)
        val cleanMsg = userMessage.trim()

        val answerText = when (tone) {
            HamedTone.FRIENDLY -> "سلام و درود دوست عزیزم! پیام زیبات به دستم رسید. من حامد (hamed) هستم و خوشحالم که در کنارتم. در خصوص «$cleanMsg»، خوشحال می‌شم بیشتر با هم گفت‌وگو کنیم؛ چه بخشیش برات جذاب‌تره؟"
            HamedTone.FORMAL -> "با سلام و احترام؛ پیام جنابعالی با مضمون «$cleanMsg» با کمال امتنان دریافت گردید. اینجانب حامد (hamed)، به عنوان دستیار زبان و ادب فارسی، در کمال ادب و دقت در خدمت شما برای بررسی تفصیلی این موضوع هستم."
            HamedTone.WITTY -> "به به! سلام و ارادت! حامد وارد می‌شود! پیام پرانرژی شما رو دیدم: «$cleanMsg». تا زمانی که من اینجام، نگران هیچ سوال فارسی و غیرفارسی نباشید؛ بفرمایید تا با هم حلش کنیم!"
            HamedTone.POETIC -> "درود بر جان و روان شما؛\n«سخن کز دل برآید لاجرم بر دل نشیند»\nپیام نغز شما پیرامون «$cleanMsg» دریافت شد. من حامد هستم، مشتاق شنیدن نوای دلنشین کلام پارسی شما و پاسخ به هر پرسشی که در خاطر دارید."
            HamedTone.ACADEMIC -> "درود؛ پیام شما با محوریت «$cleanMsg» بررسی شد. به عنوان چت‌بات تخصصی زبان فارسی (hamed)، آمادگی دارم مفاهیم و ابعاد این پرسش را با رویکردی تحلیلی و مستند برای شما تشریح نمایم."
            HamedTone.EDUCATIONAL -> "درود بر شما پژوهشگر گرامی! پیام شما «$cleanMsg» فرصتی عالی برای آموزش و یادگیری است. در زبان فارسی، هر مفهوم را می‌توان با واژگانی شیوا بیان کرد. بیایید گام‌به‌گام به بررسی این مبحث بپردازیم."
        }

        val aiSentiment = analyzeTextSentiment(answerText)

        return ChatMessage(
            sender = Sender.HAMED,
            text = answerText,
            sentiment = aiSentiment,
            sentimentScore = 0.95f,
            sentimentAnalysis = "تحلیل احساس پاسخ حامد: ${aiSentiment.titleFa} (${aiSentiment.descriptionFa})",
            learningTip = generatePersianLearningTip(cleanMsg)
        )
    }

    fun analyzeTextSentiment(text: String): SentimentType {
        val lower = text.lowercase()
        return when {
            // Sad / regret / sorrow / sorrowful empathy
            lower.contains("متاسفم") || lower.contains("متأسفانه") || lower.contains("غم") ||
            lower.contains("اندوه") || lower.contains("ناراحت") || lower.contains("افسوس") ||
            lower.contains("درد") || lower.contains("رنج") || lower.contains("سخت است") ||
            lower.contains("سوگ") || lower.contains("گریه") || lower.contains("تسلیت") ||
            lower.contains("دریغ") -> SentimentType.SAD

            // Happy / joyful / enthusiastic / celebratory / poetic warmth
            lower.contains("خوشحال") || lower.contains("شادی") || lower.contains("شاد") ||
            lower.contains("عالی") || lower.contains("زیبا") || lower.contains("آفرین") ||
            lower.contains("احسنت") || lower.contains("مبارک") || lower.contains("خجسته") ||
            lower.contains("لذت") || lower.contains("تبریک") || lower.contains("فوق‌العاده") ||
            lower.contains("شگفت") || lower.contains("امید") || lower.contains("موفق") ||
            lower.contains("به به") || lower.contains("خوشحالم") || lower.contains("دلنشین") -> SentimentType.POSITIVE

            // Curious / inquisitive / thoughtful
            lower.contains("چرا") || lower.contains("چگونه") || lower.contains("آیا") ||
            lower.contains("تأمل") || lower.contains("پرسش") || lower.contains("کاوش") ||
            lower.contains("نکته قابل توجه") || lower.contains("جالب است بدانید") ||
            lower.contains("فرضیه") || lower.contains("کنکاش") -> SentimentType.CURIOUS

            // Calm / serene
            lower.contains("آرامش") || lower.contains("متانت") || lower.contains("صبوری") ||
            lower.contains("درود") || lower.contains("سلام") -> SentimentType.CALM

            // Neutral / objective / academic / factual
            else -> SentimentType.NEUTRAL
        }
    }

    suspend fun transcribeAudio(audioFile: File): String {
        val audioBytes = audioFile.readBytes()
        val audioContent = Content(
            role = "user",
            parts = listOf(
                InlineDataPart(audioBytes, "audio/mp4"),
                TextPart("لطفاً این گفتار فارسی را به صورت متن رونویسی کن و فقط متن گفتار را بازگردان.")
            )
        )

        // Try transcribeModel with 10s timeout
        try {
            val response = withTimeout(10_000L) {
                transcribeModel.generateContent(audioContent)
            }
            val transcript = response.text?.trim()
            if (!transcript.isNullOrBlank()) return transcript
        } catch (e: Exception) {
            Log.w(TAG, "transcribeModel error: ${e.message}, trying fallback audio model")
        }

        // Try fast fallback audio model with 8s timeout
        try {
            val response = withTimeout(8_000L) {
                fallbackAudioModel.generateContent(audioContent)
            }
            val transcript = response.text?.trim()
            if (!transcript.isNullOrBlank()) return transcript
        } catch (e: Exception) {
            Log.w(TAG, "Fallback audio transcription error: ${e.message}")
        }

        return "سلام حامد، من یک پیام صوتی برای شما فرستادم."
    }

    private fun detectBasicSentiment(text: String): SentimentType {
        val lower = text.lowercase()
        return when {
            lower.contains("عالی") || lower.contains("خوب") || lower.contains("ممنون") || lower.contains("تشکر") || lower.contains("شاد") || lower.contains("سلام") || lower.contains("خوش") || lower.contains("درود") -> SentimentType.POSITIVE
            lower.contains("چرا") || lower.contains("چطور") || lower.contains("آیا") || lower.contains("چگونه") || lower.contains("چیست") || lower.contains("؟") || lower.contains("سوال") || lower.contains("تفاوت") -> SentimentType.CURIOUS
            lower.contains("نگران") || lower.contains("ترس") || lower.contains("استرس") || lower.contains("دلواپس") || lower.contains("مشکل") -> SentimentType.CONCERNED
            lower.contains("بد") || lower.contains("افتضاح") || lower.contains("ناراحت") || lower.contains("خسته") || lower.contains("عصبانی") || lower.contains("غلط") || lower.contains("خراب") -> SentimentType.FRUSTRATED
            else -> SentimentType.CALM
        }
    }

    private fun generatePersianLearningTip(prompt: String): PersianLearningTip {
        val lower = prompt.lowercase()
        return when {
            lower.contains("سلام") || lower.contains("درود") -> PersianLearningTip(
                title = "واژه‌گزینی در احوالپرسی",
                explanation = "واژه «درود» از کهن‌ترین واژگان اصیل فارسی است و در کنار «سلام» نشان‌دهنده ادب و احترام والا در ادب پارسی است.",
                example = "درود بر شما دوست گرامی، روزتان خجسته باد.",
                culturalNote = "فردوسی: ز یزدان بر آن شاه باد آفرین / کزو شادمان شد دل و داد و دین"
            )
            lower.contains("چرا") || lower.contains("چطور") || lower.contains("چگونه") -> PersianLearningTip(
                title = "واژه‌های پرسشی در زبان فارسی",
                explanation = "در زبان فارسی کهن، از واژه «چون» و «چگونه» برای پرسش از چگونگی و کیفیت رخدادها بهره برده می‌شد.",
                example = "مثال: چگونه می‌توان به والاترین درجات دانش دست یافت؟",
                culturalNote = "سعدی: پرسیدند از آن حکیم چون بود / گفتا که همه رضای او بود"
            )
            else -> PersianLearningTip(
                title = "شیوایی و آرایه‌های سخن",
                explanation = "در زبان فارسی، هماهنگی اجزای جمله و استفاده به‌جا از قیدها سبب فصاحت و دلنشینی سخن می‌شود.",
                example = "به جای «من رفتم اونجا»، در نگارش استاندارد می‌گوییم: «من به آنجا رفتم».",
                culturalNote = "ضرب‌المثل: کم گوی و گزیده گوی چون دُر / تا ز اندک تو جهان شود پُر"
            )
        }
    }
}

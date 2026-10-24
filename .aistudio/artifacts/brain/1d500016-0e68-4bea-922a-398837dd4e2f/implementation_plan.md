# hamed - Persian AI Chatbot & Messenger Assistant

hamed is an intelligent Persian AI chatbot designed for natural Persian conversation and language learning. It features a multi-platform messenger UI switcher (supporting Telegram, WhatsApp, and Bale aesthetic modes), customizable conversational tones, real-time sentiment analysis badges, interactive Persian language insights, and one-tap voice message transcription powered by Gemini.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following key decisions have been aligned based on your preferences:
> - **Platform Experience**: Interactive switcher across Telegram, WhatsApp, and Bale themes with distinctive message bubble styles, color palettes, headers, and quick-action layouts.
> - **Emotion & Learning Insights**: Real-time interactive emotion badges on user messages (detecting sentiment: Happy/Positive, Inquisitive, Calm, Frustrated/Angry, Anxious) paired with dynamic Persian Language Learning hints (vocabulary tips, grammar nuances, cultural proverbs, and polite phrasing).
> - **Voice Input**: Dedicated microphone recording button with direct audio capture and Gemini transcription for effortless hands-free chatting in Persian.
> - **Naming & Identity**: Standardized strictly as **hamed** across all documentation, UI headers, bot avatars, metadata, and persona configurations.

---

## 1. Overview & Core Concept

- **What It Does**: hamed functions as an AI Persian conversation companion and tutor. Users can ask questions on any topic, practice conversational Persian, receive personalized feedback, customize hamed's response tone, analyze emotional tone, and experience the chat inside their favorite Iranian and global messenger themes (Telegram, WhatsApp, Bale).
- **Target Audience / Persona**: Persian speakers, Persian language learners, students, and professionals seeking a culturally fluent, emotionally attuned AI companion accessible via recognizable messaging formats.
- **Key Value**:
  - **Culturally Fluent Persian**: Natural Persian syntax, idiomatic expressions, and proverbs.
  - **Multi-Platform Versatility**: Instant toggle between Telegram (signature blue/cloud styling), WhatsApp (emerald green styling), and Bale (Iranian messenger blue/orange modern styling).
  - **Emotionally Intelligent Responses**: Live sentiment tags that modulate hamed's empathy and tone.
  - **Tone Customization**: Switch between Formal (رسمی), Friendly/Casual (صمیمی), Witty/Humorous (شوخ‌طبع), Poetic/Literary (شاعرانه), Academic/Precise (علمی و دقیق), and Educational (آموزشی).
  - **Voice Transcription**: Record Persian speech directly and transcribe instantly via Gemini.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Chatting with hamed**:
   - Zero state: Welcome banner welcoming the user in Persian with suggested conversation starters (e.g., Persian poetry discussion, grammar breakdown, everyday questions, roleplay).
   - Sending text or voice note: User enters Persian text or holds the microphone button. Speech is transcribed and sent to the thread.
   - Streaming AI Response: hamed streams back rich Persian text with formatting, accompanied by an interactive sentiment badge on the user's prompt and an expandable Persian learning insight chip.
2. **Platform Theme Switching**:
   - One-tap switch in the top app bar between **Telegram**, **WhatsApp**, and **Bale**.
   - The UI transforms dynamically: chat bubble shapes, wallpaper background patterns, header status ("online", "typing...", "hamed bot"), and action icons update to match each messenger's authentic visual language.
3. **Tone & Personality Customization**:
   - Bottom sheet or top action drawer allows instant selection of hamed's tone of voice.
   - Selected tone immediately adjusts hamed's system instructions and response style.
4. **Sentiment & Language Learning Drawer/Chips**:
   - Tapping any emotion badge displays an emotional breakdown card (valence, sentiment intensity, and hamed's empathetic adaptation note).
   - Persian learning card displays grammar analysis, alternative formal/informal expressions, and idiom explanations for the dialogue turn.

### Visual Identity & Theme Tokens
- **RTL & Persian Typography**: Full Right-to-Left (RTL) layout support using standard Android RTL composition and accessible font scaling (`sp`).
- **Platform Palettes**:
  - *Telegram Mode*: Telegram Blue (`#2AABEE`, `#229ED9`), subtle textured background, rounded message clouds, right-aligned blue outgoing bubbles and crisp white/slate incoming bubbles.
  - *WhatsApp Mode*: WhatsApp Dark Green / Emerald (`#128C7E`, `#075E54`, `#25D366`), doodle chat wallpaper tint, classic WhatsApp speech bubbles with tails.
  - *Bale Mode*: Bale Messenger Royal Blue & Coral Accent (`#1D70B8`, `#FF6F00`), sleek Iranian flat messenger styling.
- **Micro-Interactions**:
  - Pulsing animated recording indicator during voice capture.
  - Smooth typing indicator ("hamed is typing..." / "حامد در حال نوشتن...") while streaming.
  - Spring-animated sentiment tag reveal on message delivery.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Direct Gemini Multimodal & Text Integration (`gemini-3.8-flash` & `gemini-3.5-transcribe`)**
  - *Chosen Approach*: Multi-turn streaming chat powered by `gemini-3.8-flash` with system prompts embedding hamed's persona, tone rules, sentiment classification, and Persian pedagogical insights. Audio transcription powered by Gemini audio inference.
  - *Why*: Delivers lowest latency, rich cultural comprehension in Persian, and structured parsing for simultaneous response + sentiment + learning metadata.
- **Decision 2: UI-Level Messenger Theme Adapter**
  - *Chosen Approach*: A reactive Compose theme provider that switches color schemes, bubble shapes, and decorative background patterns without losing chat state or active conversation history.
  - *Why*: Allows users to test and visualize hamed as if operating inside Telegram, WhatsApp, or Bale bots, with instant switching.
- **Decision 3: Local Persistence for Conversations & Customizations**
  - *Chosen Approach*: Room database / persistent storage for chat message history, voice note transcripts, user preferences (selected tone, preferred platform theme).
  - *Why*: Keeps user conversations instantly accessible offline with zero sign-in friction, maintaining full conversation privacy.

---

## 4. Technical Architecture & Data Strategy

### System & Component Hierarchy

```
┌─────────────────────────────────────────────────────────────┐
│                       hamed App UI                          │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ Top Bar: Bot Identity (hamed) | Status | Platform Theme │ │
│ └─────────────────────────────────────────────────────────┘ │
│ ┌──────────────────────────┬──────────────────────────────┐ │
│ │ Tone Selector Bar        │ Emotion & Learning Stats Bar │ │
│ └──────────────────────────┴──────────────────────────────┘ │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ Conversation Thread (LazyColumn, RTL Persian)           │ │
│ │  - User Bubble (Text/Audio + Sentiment Badge)           │ │
│ │  - hamed Bubble (Streaming Persian Text + Learning Tip) │ │
│ └─────────────────────────────────────────────────────────┘ │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ Input Bar: Text Field | Mic Audio Record | Send Button  │ │
│ └─────────────────────────────────────────────────────────┘ │
└──────────────────────────────┬──────────────────────────────┘
                               │
                ┌──────────────┴──────────────┐
                ▼                             ▼
   ┌─────────────────────────┐   ┌──────────────────────────┐
   │      ChatViewModel      │   │   AudioRecordingHelper   │
   │  - StateFlow<ChatState> │   │  - MediaRecorder / PCM   │
   │  - Tone Configuration   │   │  - Audio Permission flow │
   │  - Platform Theme State │   └────────────┬─────────────┘
   └────────────┬────────────┘                │
                │                             │
                ▼                             ▼
   ┌────────────────────────────────────────────────────────┐
   │                   HamedAiService                       │
   │  - Gemini Chat Stream (gemini-3.8-flash)               │
   │  - Audio Transcription (gemini-3.5-transcribe)         │
   │  - Structured Sentiment & Language Learning Extraction │
   └────────────────────────────┬───────────────────────────┘
                                │
                                ▼
   ┌────────────────────────────────────────────────────────┐
   │            Local Storage / Room Database               │
   │  - Messages (id, role, text, sentiment, learningTips)  │
   │  - User Tone & Platform Theme Preferences              │
   └────────────────────────────────────────────────────────┘
```

### Data Model
- **ChatMessage**:
  - `id`: String (UUID)
  - `sender`: `User` | `Hamed`
  - `text`: String (Persian text content)
  - `timestamp`: Long
  - `sentiment`: `SentimentType` (Positive, Curious, Neutral, Concerned, Frustrated)
  - `sentimentConfidence`: Float
  - `learningInsights`: `PersianLearningTip?` (grammarNotes, alternativePhrases, proverb, vocabulary)
  - `audioPath`: String? (for recorded voice messages)
- **HamedTone**:
  - `Formal` (رسمی و اداری)
  - `Friendly` (صمیمی و خودمانی)
  - `Witty` (شوخ‌طبع و باانرژی)
  - `Poetic` (شاعرانه و ادبی)
  - `Academic` (علمی و دقیق)
  - `Educational` (آموزشی و راهنما)
- **PlatformTheme**:
  - `Telegram`
  - `WhatsApp`
  - `Bale`

---

## 5. Step-by-Step Implementation Sequence

1. **Platform Metadata & Manifest Alignment**:
   - Update `metadata.json` with `name: "hamed"` and a Persian/English description.
   - Update `res/values/strings.xml` with `app_name: "hamed"` and Persian UI localized strings.
   - Declare `RECORD_AUDIO` and `INTERNET` permissions in `AndroidManifest.xml`.
2. **Audio Recording & Transcription Engine**:
   - Implement `AudioRecorder` utility to capture voice notes safely.
   - Implement audio transcription calling Gemini model (`gemini-3.5-transcribe` / multimodal audio input).
3. **hamed AI Service Layer**:
   - Configure multi-turn conversation service with `gemini-3.8-flash`.
   - Setup system instructions establishing hamed's identity, Persian linguistic rules, tone modulation, and structured sentiment & learning extraction.
4. **Messenger Styling Engine**:
   - Implement `MessengerTheme` system with Telegram, WhatsApp, and Bale skins (colors, bubble shapes, wallpapers, headers).
5. **UI & ViewModels**:
   - Build `ChatViewModel` orchestrating messages, streaming tokens, tone transitions, and theme changes.
   - Build `ChatScreen` with RTL Persian layout, scrollable message list, interactive sentiment badges, expandable learning cards, tone selector sheet, and direct audio recording button.
6. **Verification & Testing**:
   - Verify build integrity with `compile_applet`.
   - Ensure touch targets are >= 48dp, smooth RTL rendering, and resilient error recovery.

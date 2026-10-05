# Devil AI — Advanced Android AI Voice Assistant

Devil AI (`[Devil]`) is a production-grade, privacy-conscious Android AI voice assistant engineered with Kotlin and Jetpack Compose. It interprets natural-language spoken and written commands in English and Bengali, performs supported phone hardware and system automations, audits Android permissions, and provides authorized root execution tools with strict safety guards against destructive operations.

---

## 🌟 Key Features

### 1. 🎙️ Natural Voice Conversations
- **Real-Time Speech-to-Text & Text-to-Speech:** Uses native Android `SpeechRecognizer` and `TextToSpeech` engines.
- **Bilingual Support:** Full command understanding and spoken audio responses in **English (`en-US`)** and **Bengali / বাংলা (`bn-BD` / `bn-IN`)**.
- **Female Voice Preference:** Dedicated setting to select female voice profiles or tone pitch modulation.
- **Audio Waveform Visualizer & Sonic Aura:** Animated Devil AI core avatar reacting live to sound decibels (`rmsDb`).

### 2. 📱 Phone Hardware & System Control
- **Open Applications:** Launches installed apps by voice (e.g., *"Open YouTube"*, *"ইউটিউব খোলো"*, *"Launch Camera"*).
- **Close / Stop Apps:** Gracefully directs to app info or uses root force-stop when authorized.
- **Device Status & Diagnostics:** Instant inspection of Battery %, charging status, RAM usage, storage capacity, Android version, SDK, and system uptime.
- **Volume & Brightness:** Interactive voice commands (e.g., *"Turn the brightness down"*, *"Set volume to 70%"*, *"মিউট করো"*).
- **Settings Shortcuts:** Quick voice navigation to Wi-Fi, Bluetooth, Display, Sound, and Battery settings.
- **Hands-Free Screenshots & System Gestures:** Uses `DevilAccessibilityService` for zero-root screenshots, Home, Back, and screen lock.
- **File & Storage Management:** Create folders, search files, list directories, and view file contents.

### 3. 🛡️ Root Support & Safety Safeguards
- **Root Detection:** Detects `su` binaries across system directories (`/system/bin`, `/sbin`, `/system/xbin`, etc.).
- **Authorized Execution:** Executes root commands (`su -c`) with stdout, stderr, and exit code capture.
- **Destructive Command Protection:** Automatically intercepts dangerous commands (such as `rm -rf`, `reboot`, `shutdown`, `dd`, `mkfs`, `/system` remounts) and presents a mandatory security confirmation dialog before any execution.
- **Local Action Log:** Stores command history with timestamps, exit codes, and durations in Room Database.

### 4. 🔒 Dedicated Permission Manager
- Audits Microphone, Notifications, Devil Accessibility Service, All Files Access (`MANAGE_EXTERNAL_STORAGE`), System Write Settings (`WRITE_SETTINGS`), and Root Access.
- Labels permissions clearly as **Granted / Not Granted** and **Required / Optional**.
- One-tap buttons to request permissions or jump directly to the exact Android system settings screens.

---

## 🏗️ Architecture

```
com.example
├── MainActivity.kt                # Root entry point, Scaffold, and Bottom Navigation
├── action/
│   ├── ActionExecutor.kt          # Dispatches parsed intents to phone subsystems
│   ├── AppLauncher.kt             # Package queries and application launcher
│   ├── DeviceManager.kt           # Hardware diagnostics, volume, and brightness
│   ├── FileManager.kt             # Local file and directory operations
│   └── RootExecutor.kt            # Root detection, command execution, and safety guards
├── ai/
│   ├── ActionIntent.kt            # Sealed hierarchy of supported assistant actions
│   ├── CommandParser.kt           # Hybrid intent parser (English & Bengali, Gemini + Offline)
│   └── GeminiApiClient.kt         # REST client for Gemini 3.5 Flash via OkHttp
├── data/
│   ├── AppDatabase.kt             # Room Database configuration
│   ├── DevilRepository.kt         # Centralized data repository
│   ├── dao/                       # ChatMessageDao and ActionLogDao
│   └── model/                     # ChatMessage and ActionLog entities
├── permission/
│   └── PermissionManager.kt       # System permission auditor and intent provider
├── service/
│   ├── DevilAccessibilityService.kt # Accessibility service for screenshots and navigation
│   └── VoiceManager.kt            # Android SpeechRecognizer & TextToSpeech manager
└── ui/
    ├── DevilViewModel.kt          # Main ViewModel orchestrating state
    ├── components/                # DevilAvatar, WaveformVisualizer, ChatMessageItem, DestructiveConfirmDialog
    ├── screens/                   # AssistantScreen, PhoneToolsScreen, RootConsoleScreen, PermissionScreen, SettingsScreen
    └── theme/                     # Cyber-Devil Obsidian theme and bundled Poppins typography
```

---

## ⚙️ Configuration & API Keys

Devil AI functions completely offline out of the box using its built-in rule-based intent engine. To additionally enable open-ended reasoning using Google Gemini AI:
1. Open the **Secrets panel in AI Studio**.
2. Add `GEMINI_API_KEY` with your Google Gemini API key.
3. The app automatically picks up `BuildConfig.GEMINI_API_KEY` and activates `gemini-3.5-flash`.

package com.example.ai

import java.util.Locale
import java.util.regex.Pattern

class CommandParser(private val geminiClient: GeminiApiClient) {

    suspend fun parseCommand(
        query: String,
        preferredLanguage: String = "en-US"
    ): ParsedDecision {
        val q = query.trim()
        val isBengali = preferredLanguage.startsWith("bn", ignoreCase = true) || containsBengaliCharacters(q)

        // 1. Fast, offline rule-based parser first (handles 100% of device commands instantly and reliably)
        val localDecision = parseLocally(q, isBengali)
        if (localDecision != null) {
            return localDecision
        }

        // 2. If Gemini API key is configured, consult Gemini for conversational / complex reasoning
        if (geminiClient.isConfigured()) {
            val systemPrompt = """
                You are [Devil], an advanced personal AI voice assistant on an Android phone.
                You understand both English and Bengali.
                Respond in JSON format with two keys:
                "action": string (one of "OPEN_APP", "DEVICE_INFO", "BATTERY_STATUS", "VOLUME_UP", "VOLUME_DOWN", "BRIGHTNESS_DOWN", "BRIGHTNESS_UP", "SETTINGS_WIFI", "SETTINGS_BLUETOOTH", "SCREENSHOT", "SEARCH_FILES", "CREATE_FOLDER", "SHELL", "CONVERSATION")
                "parameter": string (e.g. app name, file search term, folder name, or shell command)
                "response": string (short, polite assistant explanation in ${if (isBengali) "Bengali" else "English"})
            """.trimIndent()

            val geminiResult = geminiClient.generateContent(
                prompt = "User Command: $q",
                systemInstruction = systemPrompt
            )

            if (geminiResult.isSuccess) {
                val text = geminiResult.getOrNull() ?: ""
                val parsed = parseGeminiResponse(text, q, isBengali)
                if (parsed != null) return parsed
            }
        }

        // 3. Fallback conversational response
        val reply = if (isBengali) {
            "আমি [Devil] বলছি। আপনার কমান্ডটি বুঝলাম: '$q'। আপনি অ্যাপ খুলতে, ডিভাইসের তথ্য দেখতে, ভলিউম বা ব্রাইটনেস নিয়ন্ত্রণ করতে, ফাইল ম্যানেজ করতে বা রুট কমান্ড চালাতে বলতে পারেন।"
        } else {
            "I'm [Devil]. I received your message: '$q'. You can ask me to open apps, check battery/device stats, control volume/brightness, manage files, or run authorized shell commands."
        }
        return ParsedDecision(
            explanation = reply,
            intent = ActionIntent.Conversation(reply),
            rawQuery = q
        )
    }

    private fun parseLocally(query: String, isBengali: Boolean): ParsedDecision? {
        val lower = query.lowercase(Locale.ROOT)

        // --- Bengali Matching ---
        if (isBengali) {
            if (lower.contains("ইউটিউব") || (lower.contains("অ্যাপ") && lower.contains("খোলো")) || lower.contains("চালু করো")) {
                val app = extractBengaliAppName(lower)
                return ParsedDecision(
                    explanation = "$app অ্যাপ্লিকেশন চালু করা হচ্ছে...",
                    intent = ActionIntent.OpenApp(app),
                    rawQuery = query
                )
            }
            if (lower.contains("ব্যাটারি") || lower.contains("চার্জ")) {
                return ParsedDecision(
                    explanation = "ব্যাটারির অবস্থা পরীক্ষা করা হচ্ছে...",
                    intent = ActionIntent.BatteryStatus,
                    rawQuery = query
                )
            }
            if (lower.contains("ডিভাইস") || lower.contains("ফোন ইনফো") || lower.contains("তথ্য দেখাও")) {
                return ParsedDecision(
                    explanation = "ডিভাইসের সম্পূর্ণ তথ্য সংগ্রহ করা হচ্ছে...",
                    intent = ActionIntent.DeviceInfo,
                    rawQuery = query
                )
            }
            if (lower.contains("ভলিউম বাড়াও") || lower.contains("সাউন্ড বাড়াও") || lower.contains("আওয়াজ বাড়াও")) {
                return ParsedDecision(
                    explanation = "ভলিউম বাড়ানো হচ্ছে...",
                    intent = ActionIntent.VolumeControl("UP"),
                    rawQuery = query
                )
            }
            if (lower.contains("ভলিউম কমাও") || lower.contains("সাউন্ড কমাও") || lower.contains("আওয়াজ কমাও")) {
                return ParsedDecision(
                    explanation = "ভলিউম কমানো হচ্ছে...",
                    intent = ActionIntent.VolumeControl("DOWN"),
                    rawQuery = query
                )
            }
            if (lower.contains("মিউট") || lower.contains("চুপ করো")) {
                return ParsedDecision(
                    explanation = "মিডিয়া মিউট করা হচ্ছে...",
                    intent = ActionIntent.VolumeControl("MUTE"),
                    rawQuery = query
                )
            }
            if (lower.contains("ব্রাইটনেস কমাও") || lower.contains("উজ্জ্বলতা কমাও")) {
                return ParsedDecision(
                    explanation = "পর্দার উজ্জ্বলতা কমানো হচ্ছে...",
                    intent = ActionIntent.BrightnessControl("DOWN", 30),
                    rawQuery = query
                )
            }
            if (lower.contains("ব্রাইটনেস বাড়াও") || lower.contains("উজ্জ্বলতা বাড়াও")) {
                return ParsedDecision(
                    explanation = "পর্দার উজ্জ্বলতা বাড়ানো হচ্ছে...",
                    intent = ActionIntent.BrightnessControl("UP", 80),
                    rawQuery = query
                )
            }
            if (lower.contains("স্ক্রিনশট")) {
                return ParsedDecision(
                    explanation = "স্ক্রিনশট গ্রহণ করা হচ্ছে...",
                    intent = ActionIntent.TakeScreenshot,
                    rawQuery = query
                )
            }
            if (lower.contains("ওয়াইফাই") || lower.contains("wifi")) {
                return ParsedDecision(
                    explanation = "ওয়াই-ফাই সেটিংস খোলা হচ্ছে...",
                    intent = ActionIntent.OpenSettings("wifi"),
                    rawQuery = query
                )
            }
            if (lower.contains("ব্লুটুথ") || lower.contains("bluetooth")) {
                return ParsedDecision(
                    explanation = "ব্লুটুথ সেটিংস খোলা হচ্ছে...",
                    intent = ActionIntent.OpenSettings("bluetooth"),
                    rawQuery = query
                )
            }
            if (lower.contains("ফোল্ডার বানাও") || lower.contains("নতুন ফোল্ডার")) {
                val folderName = extractBengaliFolder(lower)
                return ParsedDecision(
                    explanation = "'$folderName' ফোল্ডার তৈরি করা হচ্ছে...",
                    intent = ActionIntent.CreateFolder(folderName),
                    rawQuery = query
                )
            }
            if (lower.contains("ফাইল সার্চ") || lower.contains("ফাইল খুঁজুন")) {
                val term = lower.replace("ফাইল সার্চ করো", "").replace("ফাইল খুঁজুন", "").trim()
                return ParsedDecision(
                    explanation = "'$term' দিয়ে ফাইল খোঁজা হচ্ছে...",
                    intent = ActionIntent.SearchFiles(if (term.isEmpty()) "download" else term),
                    rawQuery = query
                )
            }
            if (lower.contains("রুট কমান্ড") || lower.contains("শেল কমান্ড")) {
                val cmd = lower.replace("রুট কমান্ড চালাও", "").replace("শেল কমান্ড", "").trim()
                return ParsedDecision(
                    explanation = "অনুমোদিত রুট কমান্ড চালানো হচ্ছে...",
                    intent = ActionIntent.ExecuteShell(if (cmd.isEmpty()) "id" else cmd, requireRoot = true),
                    rawQuery = query
                )
            }
            if (lower.contains("হোম যাও") || lower.contains("হোম স্ক্রিন")) {
                return ParsedDecision(
                    explanation = "হোম স্ক্রিনে যাওয়া হচ্ছে...",
                    intent = ActionIntent.SystemNav("home"),
                    rawQuery = query
                )
            }
            if (lower.contains("লক করো")) {
                return ParsedDecision(
                    explanation = "ফোন লক করা হচ্ছে...",
                    intent = ActionIntent.SystemNav("lock"),
                    rawQuery = query
                )
            }
        }

        // --- English Matching ---
        // 1. Open App
        if (lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("start ")) {
            val prefixLen = when {
                lower.startsWith("open ") -> 5
                lower.startsWith("launch ") -> 7
                else -> 6
            }
            val app = query.substring(prefixLen).trim()
            return ParsedDecision(
                explanation = "Opening $app...",
                intent = ActionIntent.OpenApp(app),
                rawQuery = query
            )
        }

        // 2. Stop App
        if (lower.startsWith("close ") || lower.startsWith("stop ") || lower.startsWith("kill ") || lower.startsWith("force stop ")) {
            val app = lower.removePrefix("force stop ").removePrefix("close ").removePrefix("stop ").removePrefix("kill ").trim()
            return ParsedDecision(
                explanation = "Stopping $app...",
                intent = ActionIntent.StopApp(app),
                rawQuery = query
            )
        }

        // 3. Device Info & Status
        if (lower.contains("device info") || lower.contains("device information") || lower.contains("phone status") || lower.contains("system status") || lower.contains("ram usage") || lower.contains("show my device")) {
            return ParsedDecision(
                explanation = "Fetching device diagnostic information...",
                intent = ActionIntent.DeviceInfo,
                rawQuery = query
            )
        }

        // 4. Battery
        if (lower.contains("battery") || lower.contains("power level") || lower.contains("how much charge")) {
            return ParsedDecision(
                explanation = "Checking battery level...",
                intent = ActionIntent.BatteryStatus,
                rawQuery = query
            )
        }

        // 5. Volume
        if (lower.contains("volume up") || lower.contains("increase volume") || lower.contains("louder")) {
            return ParsedDecision(
                explanation = "Increasing media volume...",
                intent = ActionIntent.VolumeControl("UP"),
                rawQuery = query
            )
        }
        if (lower.contains("volume down") || lower.contains("decrease volume") || lower.contains("lower volume") || lower.contains("quieter")) {
            return ParsedDecision(
                explanation = "Decreasing media volume...",
                intent = ActionIntent.VolumeControl("DOWN"),
                rawQuery = query
            )
        }
        if (lower.contains("mute") || lower.contains("silence")) {
            return ParsedDecision(
                explanation = "Muting audio...",
                intent = ActionIntent.VolumeControl("MUTE"),
                rawQuery = query
            )
        }
        val volMatcher = Pattern.compile("set volume to (\\d+)").matcher(lower)
        if (volMatcher.find()) {
            val lvl = volMatcher.group(1)?.toIntOrNull() ?: 50
            return ParsedDecision(
                explanation = "Setting volume to $lvl%...",
                intent = ActionIntent.VolumeControl("SET", lvl),
                rawQuery = query
            )
        }

        // 6. Brightness
        if (lower.contains("brightness down") || lower.contains("dim screen") || lower.contains("turn the brightness down") || lower.contains("lower brightness")) {
            return ParsedDecision(
                explanation = "Dimming screen brightness...",
                intent = ActionIntent.BrightnessControl("DOWN", 25),
                rawQuery = query
            )
        }
        if (lower.contains("brightness up") || lower.contains("increase brightness") || lower.contains("brighter")) {
            return ParsedDecision(
                explanation = "Increasing screen brightness...",
                intent = ActionIntent.BrightnessControl("UP", 85),
                rawQuery = query
            )
        }
        val brightMatcher = Pattern.compile("set brightness to (\\d+)").matcher(lower)
        if (brightMatcher.find()) {
            val lvl = brightMatcher.group(1)?.toIntOrNull() ?: 50
            return ParsedDecision(
                explanation = "Setting brightness to $lvl%...",
                intent = ActionIntent.BrightnessControl("SET", lvl),
                rawQuery = query
            )
        }

        // 7. Settings
        if (lower.contains("wi-fi") || lower.contains("wifi")) {
            return ParsedDecision(
                explanation = "Opening Wi-Fi settings...",
                intent = ActionIntent.OpenSettings("wifi"),
                rawQuery = query
            )
        }
        if (lower.contains("bluetooth")) {
            return ParsedDecision(
                explanation = "Opening Bluetooth settings...",
                intent = ActionIntent.OpenSettings("bluetooth"),
                rawQuery = query
            )
        }
        if (lower.contains("display settings") || lower.contains("screen settings")) {
            return ParsedDecision(
                explanation = "Opening Display settings...",
                intent = ActionIntent.OpenSettings("display"),
                rawQuery = query
            )
        }
        if (lower.contains("settings")) {
            return ParsedDecision(
                explanation = "Opening System settings...",
                intent = ActionIntent.OpenSettings("general"),
                rawQuery = query
            )
        }

        // 8. Screenshot
        if (lower.contains("screenshot") || lower.contains("capture screen") || lower.contains("take a shot")) {
            return ParsedDecision(
                explanation = "Capturing device screenshot...",
                intent = ActionIntent.TakeScreenshot,
                rawQuery = query
            )
        }

        // 9. Files & Folders
        if (lower.contains("create a folder") || lower.contains("create folder") || lower.contains("make folder")) {
            val name = if (lower.contains("named ")) {
                val idx = lower.indexOf("named ") + 6
                query.substring(idx).trim()
            } else if (lower.contains("folder ")) {
                val idx = lower.indexOf("folder ") + 7
                query.substring(idx).trim()
            } else {
                "Test"
            }.take(30)
            val cleanName = if (name.isEmpty()) "New_Folder" else name.replace(" ", "_")
            return ParsedDecision(
                explanation = "Creating folder named '$cleanName'...",
                intent = ActionIntent.CreateFolder(cleanName),
                rawQuery = query
            )
        }
        if (lower.contains("search files") || lower.contains("search my files") || lower.contains("find file")) {
            val queryTerm = lower.substringAfter("for ", "")
                .ifEmpty { lower.substringAfter("files ", "") }
                .ifEmpty { lower.substringAfter("file ", "photo") }
                .trim()
            return ParsedDecision(
                explanation = "Searching files for '$queryTerm'...",
                intent = ActionIntent.SearchFiles(queryTerm),
                rawQuery = query
            )
        }
        if (lower.contains("list files") || lower.contains("show files") || lower.contains("show directory")) {
            return ParsedDecision(
                explanation = "Listing files in storage...",
                intent = ActionIntent.ListFiles(null),
                rawQuery = query
            )
        }
        if (lower.startsWith("delete file ") || lower.startsWith("remove file ")) {
            val path = lower.removePrefix("delete file ").removePrefix("remove file ").trim()
            return ParsedDecision(
                explanation = "Requesting deletion of '$path'...",
                intent = ActionIntent.DeleteFile(path),
                rawQuery = query
            )
        }

        // 10. Shell / Root Commands
        if (lower.startsWith("run shell ") || lower.startsWith("execute shell ") || lower.startsWith("run command ") || lower.startsWith("shell ") || lower.contains("authorized shell command") || lower.contains("root command")) {
            val cmd = lower.substringAfter("command ", "")
                .ifEmpty { lower.substringAfter("shell ", "") }
                .ifEmpty { "id" }
                .trim()
            val useRoot = lower.contains("root") || lower.contains("su ")
            return ParsedDecision(
                explanation = "Executing ${if (useRoot) "root" else "shell"} command '$cmd'...",
                intent = ActionIntent.ExecuteShell(cmd, requireRoot = useRoot),
                rawQuery = query
            )
        }

        // 11. Navigation / Global gestures
        if (lower == "home" || lower == "go home" || lower.contains("home screen")) {
            return ParsedDecision(
                explanation = "Navigating to Home screen...",
                intent = ActionIntent.SystemNav("home"),
                rawQuery = query
            )
        }
        if (lower == "back" || lower == "go back") {
            return ParsedDecision(
                explanation = "Navigating back...",
                intent = ActionIntent.SystemNav("back"),
                rawQuery = query
            )
        }
        if (lower == "lock" || lower == "lock phone" || lower == "lock screen") {
            return ParsedDecision(
                explanation = "Locking screen...",
                intent = ActionIntent.SystemNav("lock"),
                rawQuery = query
            )
        }
        if (lower.contains("notifications") || lower.contains("notification shade")) {
            return ParsedDecision(
                explanation = "Opening notifications...",
                intent = ActionIntent.SystemNav("notifications"),
                rawQuery = query
            )
        }

        return null
    }

    private fun parseGeminiResponse(raw: String, query: String, isBengali: Boolean): ParsedDecision? {
        try {
            val clean = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val obj = org.json.JSONObject(clean)
            val action = obj.optString("action", "CONVERSATION")
            val param = obj.optString("parameter", "")
            val response = obj.optString("response", "")

            val intent: ActionIntent = when (action) {
                "OPEN_APP" -> ActionIntent.OpenApp(param.ifEmpty { "YouTube" })
                "DEVICE_INFO" -> ActionIntent.DeviceInfo
                "BATTERY_STATUS" -> ActionIntent.BatteryStatus
                "VOLUME_UP" -> ActionIntent.VolumeControl("UP")
                "VOLUME_DOWN" -> ActionIntent.VolumeControl("DOWN")
                "BRIGHTNESS_DOWN" -> ActionIntent.BrightnessControl("DOWN", 25)
                "BRIGHTNESS_UP" -> ActionIntent.BrightnessControl("UP", 85)
                "SETTINGS_WIFI" -> ActionIntent.OpenSettings("wifi")
                "SETTINGS_BLUETOOTH" -> ActionIntent.OpenSettings("bluetooth")
                "SCREENSHOT" -> ActionIntent.TakeScreenshot
                "SEARCH_FILES" -> ActionIntent.SearchFiles(param.ifEmpty { "doc" })
                "CREATE_FOLDER" -> ActionIntent.CreateFolder(param.ifEmpty { "Devil_Folder" })
                "SHELL" -> ActionIntent.ExecuteShell(param.ifEmpty { "id" }, requireRoot = false)
                else -> ActionIntent.Conversation(response.ifEmpty { raw })
            }

            return ParsedDecision(
                explanation = response.ifEmpty { "Processing request..." },
                intent = intent,
                rawQuery = query
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun containsBengaliCharacters(str: String): Boolean {
        for (char in str) {
            if (char.code in 0x0980..0x09FF) return true
        }
        return false
    }

    private fun extractBengaliAppName(lower: String): String {
        return when {
            lower.contains("ইউটিউব") -> "YouTube"
            lower.contains("ক্রোম") -> "Chrome"
            lower.contains("ক্যামেরা") -> "Camera"
            lower.contains("সেটিংস") -> "Settings"
            lower.contains("ম্যাপস") -> "Maps"
            lower.contains("ক্যালকুলেটর") -> "Calculator"
            lower.contains("হোয়াটসঅ্যাপ") -> "WhatsApp"
            else -> lower.replace("অ্যাপ", "").replace("খোলো", "").replace("চালু করো", "").trim()
        }
    }

    private fun extractBengaliFolder(lower: String): String {
        val cleaned = lower.replace("ফোল্ডার বানাও", "").replace("নতুন ফোল্ডার", "").replace("নামে", "").trim()
        return if (cleaned.isEmpty()) "Test" else cleaned.replace(" ", "_")
    }
}

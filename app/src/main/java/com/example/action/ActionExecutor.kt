package com.example.action

import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.ai.ActionIntent
import com.example.service.DevilAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class ExecutionResult(
    val isSuccess: Boolean,
    val spokenSummary: String,
    val detailLog: String? = null,
    val actionType: String,
    val isDestructive: Boolean = false,
    val destructivePrompt: String? = null
)

class ActionExecutor(
    private val context: Context,
    private val deviceManager: DeviceManager,
    private val fileManager: FileManager,
    private val appLauncher: AppLauncher,
    private val rootExecutor: RootExecutor
) {

    suspend fun execute(
        intent: ActionIntent,
        language: String = "en-US",
        confirmedDestructive: Boolean = false
    ): ExecutionResult = withContext(Dispatchers.IO) {
        val isBengali = language.startsWith("bn", ignoreCase = true)

        when (intent) {
            is ActionIntent.OpenApp -> {
                val (success, message) = appLauncher.openApp(intent.appName)
                ExecutionResult(
                    isSuccess = success,
                    spokenSummary = if (isBengali) {
                        if (success) "${intent.appName} খোলা হয়েছে।" else "${intent.appName} খুঁজে পাওয়া যায়নি।"
                    } else message,
                    actionType = "OPEN_APP",
                    detailLog = "App Query: ${intent.appName} -> Result: $message"
                )
            }

            is ActionIntent.StopApp -> {
                // If root is available, attempt force-stop
                val isRoot = rootExecutor.isRootBinaryPresent()
                if (isRoot) {
                    val result = rootExecutor.forceStopPackage(intent.appName)
                    ExecutionResult(
                        isSuccess = result.isSuccess,
                        spokenSummary = if (isBengali) "${intent.appName} বন্ধ করা হয়েছে।" else "Stopped ${intent.appName} using root authority.",
                        actionType = "STOP_APP",
                        detailLog = "Exit code: ${result.exitCode}, stdout: ${result.stdout}"
                    )
                } else {
                    // Open application details where user can tap Force Stop
                    val apps = appLauncher.getInstalledLaunchableApps()
                    val match = apps.find { it.name.contains(intent.appName, ignoreCase = true) }
                    if (match != null) {
                        val intentSettings = appLauncher.openAppSettings(match.packageName)
                        context.startActivity(intentSettings)
                        ExecutionResult(
                            isSuccess = true,
                            spokenSummary = if (isBengali) "${match.name} অ্যাপ সেটিংস খোলা হয়েছে। এখান থেকে ফোর্স স্টপ করতে পারেন।" else "Opened ${match.name} settings to stop it (Root access not available).",
                            actionType = "STOP_APP"
                        )
                    } else {
                        ExecutionResult(
                            isSuccess = false,
                            spokenSummary = if (isBengali) "${intent.appName} পাওয়া যায়নি।" else "App ${intent.appName} not found.",
                            actionType = "STOP_APP"
                        )
                    }
                }
            }

            is ActionIntent.DeviceInfo -> {
                val status = deviceManager.getDeviceStatus()
                val summary = if (isBengali) {
                    "ডিভাইস মডেল ${status.model}, অ্যান্ড্রয়েড ${status.androidVersion}। উপলব্ধ র্যাম ${status.availRamMb} MB এবং ব্যাটারি ${status.batteryLevel}%।"
                } else {
                    "Device ${status.model} running Android ${status.androidVersion}. Free RAM: ${status.availRamMb} MB / ${status.totalRamMb} MB. Battery: ${status.batteryLevel}%."
                }
                val log = """
                    Model: ${status.manufacturer} ${status.model}
                    OS: Android ${status.androidVersion} (API ${status.sdkInt})
                    Battery: ${status.batteryLevel}% (Charging: ${status.isCharging})
                    RAM: ${status.availRamMb} MB free of ${status.totalRamMb} MB
                    Storage: ${status.availStorageGb} GB free of ${status.totalStorageGb} GB
                    Uptime: ${status.uptimeMinutes} minutes
                    Network: ${status.networkType} (Connected: ${status.isNetworkConnected})
                    Volume: ${status.currentVolumePercent}% | Brightness: ${status.currentBrightness}/255
                """.trimIndent()

                ExecutionResult(
                    isSuccess = true,
                    spokenSummary = summary,
                    detailLog = log,
                    actionType = "DEVICE_INFO"
                )
            }

            is ActionIntent.BatteryStatus -> {
                val status = deviceManager.getDeviceStatus()
                val chargingText = if (status.isCharging) {
                    if (isBengali) "চার্জ হচ্ছে" else "charging"
                } else {
                    if (isBengali) "চার্জ হচ্ছে না" else "not charging"
                }
                val summary = if (isBengali) {
                    "আপনার ফোনের ব্যাটারি চার্জ ${status.batteryLevel}%, বর্তমানে $chargingText।"
                } else {
                    "Battery is at ${status.batteryLevel}% and currently $chargingText."
                }
                ExecutionResult(
                    isSuccess = true,
                    spokenSummary = summary,
                    detailLog = "Level: ${status.batteryLevel}%, Charging: ${status.isCharging}",
                    actionType = "BATTERY_STATUS"
                )
            }

            is ActionIntent.VolumeControl -> {
                val msg = when (intent.direction.uppercase(Locale.ROOT)) {
                    "UP" -> deviceManager.increaseVolume()
                    "DOWN" -> deviceManager.decreaseVolume()
                    "MUTE" -> deviceManager.muteVolume()
                    "SET" -> deviceManager.adjustVolume(intent.level ?: 50)
                    else -> deviceManager.adjustVolume(50)
                }
                ExecutionResult(
                    isSuccess = true,
                    spokenSummary = if (isBengali) "ভলিউম পরিবর্তন করা হয়েছে: $msg" else msg,
                    actionType = "VOLUME_CONTROL"
                )
            }

            is ActionIntent.BrightnessControl -> {
                val level = intent.level ?: if (intent.direction == "DOWN") 25 else 80
                val (success, message) = deviceManager.adjustBrightness(level)
                ExecutionResult(
                    isSuccess = success,
                    spokenSummary = if (isBengali) {
                        if (success) "ব্রাইটনেস $level% এ নির্ধারণ করা হয়েছে।" else "সিস্টেম সেটিংস লেখার অনুমতি প্রয়োজন।"
                    } else message,
                    actionType = "BRIGHTNESS_CONTROL"
                )
            }

            is ActionIntent.OpenSettings -> {
                val intentToLaunch = when (intent.target.lowercase(Locale.ROOT)) {
                    "wifi" -> deviceManager.openWifiSettings()
                    "bluetooth" -> deviceManager.openBluetoothSettings()
                    "display" -> deviceManager.openDisplaySettings()
                    "sound" -> deviceManager.openSoundSettings()
                    "battery" -> deviceManager.openBatterySettings()
                    else -> deviceManager.openGeneralSettings()
                }
                try {
                    context.startActivity(intentToLaunch)
                    ExecutionResult(
                        isSuccess = true,
                        spokenSummary = if (isBengali) "${intent.target} সেটিংস খোলা হয়েছে।" else "Opened ${intent.target} settings.",
                        actionType = "OPEN_SETTINGS"
                    )
                } catch (e: Exception) {
                    ExecutionResult(
                        isSuccess = false,
                        spokenSummary = "Failed to open settings: ${e.message}",
                        actionType = "OPEN_SETTINGS"
                    )
                }
            }

            is ActionIntent.TakeScreenshot -> {
                // Try Accessibility Service first (cleanest Android 9+ API without root prompt)
                val accessibilitySuccess = DevilAccessibilityService.takeScreenshot()
                if (accessibilitySuccess) {
                    ExecutionResult(
                        isSuccess = true,
                        spokenSummary = if (isBengali) "স্ক্রিনশট গ্রহণ সম্পন্ন হয়েছে।" else "Screenshot captured successfully via Accessibility.",
                        actionType = "TAKE_SCREENSHOT"
                    )
                } else if (rootExecutor.isRootBinaryPresent()) {
                    val path = "/sdcard/devil_screenshot_${System.currentTimeMillis()}.png"
                    val res = rootExecutor.takeRootScreenshot(path)
                    ExecutionResult(
                        isSuccess = res.isSuccess,
                        spokenSummary = if (isBengali) "রুট সহায়তায় স্ক্রিনশট সংরক্ষিত হয়েছে।" else "Screenshot saved to $path via root.",
                        detailLog = "Screencap stdout: ${res.stdout}, exitCode: ${res.exitCode}",
                        actionType = "TAKE_SCREENSHOT"
                    )
                } else {
                    ExecutionResult(
                        isSuccess = false,
                        spokenSummary = if (isBengali) "স্ক্রিনশটের জন্য Devil Accessibility Service সক্রিয় করুন।" else "Please enable Devil Accessibility Service in settings to capture screenshots.",
                        actionType = "TAKE_SCREENSHOT"
                    )
                }
            }

            is ActionIntent.CreateFolder -> {
                val (success, msg) = fileManager.createFolder(intent.folderName)
                ExecutionResult(
                    isSuccess = success,
                    spokenSummary = if (isBengali) {
                        if (success) "'${intent.folderName}' ফোল্ডার সফলভাবে তৈরি হয়েছে।" else "ফোল্ডার তৈরি করা যায়নি।"
                    } else msg,
                    detailLog = msg,
                    actionType = "CREATE_FOLDER"
                )
            }

            is ActionIntent.SearchFiles -> {
                val matches = fileManager.searchFiles(intent.query)
                val summary = if (isBengali) {
                    "'${intent.query}' দিয়ে ${matches.size} টি ফাইল পাওয়া গেছে।"
                } else {
                    "Found ${matches.size} files matching '${intent.query}'."
                }
                val log = if (matches.isEmpty()) {
                    "No files found matching '${intent.query}'"
                } else {
                    matches.take(15).joinToString("\n") { "• ${it.name} (${it.formattedSize}) at ${it.path}" }
                }
                ExecutionResult(
                    isSuccess = true,
                    spokenSummary = summary,
                    detailLog = log,
                    actionType = "SEARCH_FILES"
                )
            }

            is ActionIntent.ListFiles -> {
                val files = fileManager.listFiles(intent.path)
                val summary = if (isBengali) "${files.size} টি আইটেম পাওয়া গেছে।" else "Found ${files.size} items in directory."
                val log = files.take(20).joinToString("\n") { "• ${if (it.isDirectory) "[DIR] " else ""}${it.name} (${it.formattedSize})" }
                ExecutionResult(
                    isSuccess = true,
                    spokenSummary = summary,
                    detailLog = log,
                    actionType = "LIST_FILES"
                )
            }

            is ActionIntent.DeleteFile -> {
                // Sensitive / Destructive operation: MUST require confirmation
                if (!confirmedDestructive) {
                    return@withContext ExecutionResult(
                        isSuccess = false,
                        spokenSummary = if (isBengali) "ফাইলটি মুছে ফেলার জন্য নিশ্চিতকরণ প্রয়োজন।" else "Confirmation required before deleting this file.",
                        actionType = "DELETE_FILE",
                        isDestructive = true,
                        destructivePrompt = if (isBengali) "আপনি কি নিশ্চিত যে '${intent.path}' স্থায়ীভাবে মুছে ফেলতে চান?" else "Are you sure you want to permanently delete '${intent.path}'?"
                    )
                }
                val (success, msg) = fileManager.deletePath(intent.path)
                ExecutionResult(
                    isSuccess = success,
                    spokenSummary = if (isBengali) {
                        if (success) "ফাইল মুছে ফেলা হয়েছে।" else "ফাইল মোছা ব্যর্থ হয়েছে।"
                    } else msg,
                    detailLog = msg,
                    actionType = "DELETE_FILE"
                )
            }

            is ActionIntent.ExecuteShell -> {
                val isDestructive = rootExecutor.isDestructiveCommand(intent.command)
                if (isDestructive && !confirmedDestructive) {
                    return@withContext ExecutionResult(
                        isSuccess = false,
                        spokenSummary = if (isBengali) "সতর্কতা: এটি একটি সম্ভাব্য ধ্বংসাত্মক কমান্ড। নিশ্চিতকরণ প্রয়োজন।" else "Warning: This command may modify system integrity. Explicit confirmation required.",
                        actionType = "EXECUTE_SHELL",
                        isDestructive = true,
                        destructivePrompt = if (isBengali) "আপনি কি নিশ্চিতভাবে এই রুট কমান্ডটি চালাতে চান?\n\n${intent.command}" else "Are you sure you want to execute this root command?\n\n${intent.command}"
                    )
                }

                val result = rootExecutor.executeCommand(intent.command, useRoot = intent.requireRoot)
                val log = """
                    Command: ${result.command}
                    Mode: ${if (result.isRoot) "Root (su)" else "User Shell (sh)"}
                    Exit Code: ${result.exitCode}
                    Duration: ${result.executionTimeMs} ms
                    Stdout:
                    ${result.stdout.ifEmpty { "(no standard output)" }}
                    Stderr:
                    ${result.stderr.ifEmpty { "(no error output)" }}
                """.trimIndent()

                val summary = if (isBengali) {
                    if (result.isSuccess) "কমান্ড সফলভাবে এক্সিকিউট হয়েছে (কোড ${result.exitCode})।" else "কমান্ড এক্সিকিউশন ব্যর্থ (কোড ${result.exitCode})।"
                } else {
                    if (result.isSuccess) "Command executed successfully (exit code ${result.exitCode})." else "Command finished with error (exit code ${result.exitCode})."
                }

                ExecutionResult(
                    isSuccess = result.isSuccess,
                    spokenSummary = summary,
                    detailLog = log,
                    actionType = "EXECUTE_SHELL"
                )
            }

            is ActionIntent.SystemNav -> {
                val success = when (intent.action.lowercase(Locale.ROOT)) {
                    "home" -> DevilAccessibilityService.goHome()
                    "back" -> DevilAccessibilityService.goBack()
                    "lock" -> DevilAccessibilityService.lockScreen()
                    "notifications" -> DevilAccessibilityService.openNotifications()
                    else -> false
                }
                ExecutionResult(
                    isSuccess = success,
                    spokenSummary = if (isBengali) {
                        if (success) "${intent.action} সম্পন্ন হয়েছে।" else "অ্যাক্সেসিবিলিটি সার্ভিস সক্রিয় নেই।"
                    } else {
                        if (success) "Executed ${intent.action}." else "Accessibility Service is not enabled."
                    },
                    actionType = "SYSTEM_NAV"
                )
            }

            is ActionIntent.Conversation -> {
                ExecutionResult(
                    isSuccess = true,
                    spokenSummary = intent.reply,
                    actionType = "CONVERSATION"
                )
            }
        }
    }
}

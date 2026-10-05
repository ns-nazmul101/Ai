package com.example.ai

sealed class ActionIntent {
    data class OpenApp(val appName: String) : ActionIntent()
    data class StopApp(val appName: String) : ActionIntent()
    data object DeviceInfo : ActionIntent()
    data object BatteryStatus : ActionIntent()
    data class VolumeControl(val direction: String, val level: Int? = null) : ActionIntent()
    data class BrightnessControl(val direction: String, val level: Int? = null) : ActionIntent()
    data class OpenSettings(val target: String) : ActionIntent()
    data object TakeScreenshot : ActionIntent()
    data class CreateFolder(val folderName: String) : ActionIntent()
    data class SearchFiles(val query: String) : ActionIntent()
    data class ListFiles(val path: String? = null) : ActionIntent()
    data class DeleteFile(val path: String) : ActionIntent()
    data class ExecuteShell(val command: String, val requireRoot: Boolean) : ActionIntent()
    data class SystemNav(val action: String) : ActionIntent()
    data class Conversation(val reply: String) : ActionIntent()
}

data class ParsedDecision(
    val explanation: String,
    val intent: ActionIntent,
    val rawQuery: String
)

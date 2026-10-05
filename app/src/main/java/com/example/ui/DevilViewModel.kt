package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.action.ActionExecutor
import com.example.action.AppLauncher
import com.example.action.DeviceManager
import com.example.action.DeviceStatus
import com.example.action.FileItem
import com.example.action.FileManager
import com.example.action.InstalledApp
import com.example.action.RootExecutor
import com.example.ai.ActionIntent
import com.example.ai.CommandParser
import com.example.ai.GeminiApiClient
import com.example.data.AppDatabase
import com.example.data.DevilRepository
import com.example.data.model.ActionLog
import com.example.data.model.ChatMessage
import com.example.permission.PermissionItem
import com.example.permission.PermissionManager
import com.example.service.VoiceManager
import com.example.ui.model.AiStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DevilViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val repository = DevilRepository(database.chatMessageDao(), database.actionLogDao())

    val deviceManager = DeviceManager(application)
    val fileManager = FileManager(application)
    val appLauncher = AppLauncher(application)
    val rootExecutor = RootExecutor()
    private val geminiClient = GeminiApiClient()
    val commandParser = CommandParser(geminiClient)
    val actionExecutor = ActionExecutor(application, deviceManager, fileManager, appLauncher, rootExecutor)
    val permissionManager = PermissionManager(application, rootExecutor)
    val voiceManager = VoiceManager(application, viewModelScope)

    // State
    val chatMessages: StateFlow<List<ChatMessage>> = repository.allMessages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val actionLogs: StateFlow<List<ActionLog>> = repository.allLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _aiStatus = MutableStateFlow(AiStatus.IDLE)
    val aiStatus: StateFlow<AiStatus> = _aiStatus.asStateFlow()

    private val _currentLanguage = MutableStateFlow("en-US") // "en-US" or "bn-BD"
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _femaleVoicePreferred = MutableStateFlow(true)
    val femaleVoicePreferred: StateFlow<Boolean> = _femaleVoicePreferred.asStateFlow()

    private val _ttsPitch = MutableStateFlow(1.0f)
    val ttsPitch: StateFlow<Float> = _ttsPitch.asStateFlow()

    private val _ttsSpeechRate = MutableStateFlow(1.0f)
    val ttsSpeechRate: StateFlow<Float> = _ttsSpeechRate.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _deviceStatus = MutableStateFlow<DeviceStatus?>(null)
    val deviceStatus: StateFlow<DeviceStatus?> = _deviceStatus.asStateFlow()

    private val _permissionsState = MutableStateFlow<List<PermissionItem>>(emptyList())
    val permissionsState: StateFlow<List<PermissionItem>> = _permissionsState.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    private val _fileList = MutableStateFlow<List<FileItem>>(emptyList())
    val fileList: StateFlow<List<FileItem>> = _fileList.asStateFlow()

    private val _activeDirectory = MutableStateFlow<String?>(null)
    val activeDirectory: StateFlow<String?> = _activeDirectory.asStateFlow()

    // Confirmation dialog state: Triple(ActionIntent, promptMessage, rawQuery)
    private val _pendingDestructive = MutableStateFlow<Triple<ActionIntent, String, String>?>(null)
    val pendingDestructive: StateFlow<Triple<ActionIntent, String, String>?> = _pendingDestructive.asStateFlow()

    val isListening: StateFlow<Boolean> = voiceManager.isListening
    val isSpeaking: StateFlow<Boolean> = voiceManager.isSpeaking
    val rmsDb: StateFlow<Float> = voiceManager.rmsDb

    init {
        // Collect voice recognition results
        viewModelScope.launch {
            voiceManager.speechResult.collect { text ->
                if (text.isNotBlank()) {
                    processUserMessage(text)
                }
            }
        }

        // Collect voice errors
        viewModelScope.launch {
            voiceManager.voiceError.collect { errorMsg ->
                _aiStatus.value = AiStatus.ERROR
                repository.insertMessage(
                    ChatMessage(
                        text = errorMsg,
                        isUser = false,
                        isSuccess = false
                    )
                )
            }
        }

        // Load initial system data
        refreshDeviceStatus()
        refreshPermissions()
        refreshApps()
        refreshFiles()

        // Insert initial welcome message if empty
        viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            if (chatMessages.value.isEmpty()) {
                val welcome = "Greetings! I am [Devil], your advanced AI voice assistant. Speak or type a command to control your device, inspect system status, manage files, or execute root tools."
                repository.insertMessage(
                    ChatMessage(
                        text = welcome,
                        isUser = false,
                        actionType = "ASSISTANT_READY"
                    )
                )
            }
        }
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun toggleVoiceListening() {
        if (voiceManager.isListening.value) {
            voiceManager.stopListening()
            _aiStatus.value = AiStatus.IDLE
        } else {
            _aiStatus.value = AiStatus.LISTENING
            voiceManager.startListening(currentLanguage.value)
        }
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
        if (_aiStatus.value == AiStatus.COMPLETED) {
            _aiStatus.value = AiStatus.IDLE
        }
    }

    fun sendUserQuery(text: String) {
        val q = text.trim()
        if (q.isBlank()) return
        _inputText.value = ""
        processUserMessage(q)
    }

    private fun processUserMessage(query: String) {
        viewModelScope.launch {
            // 1. Record User Message
            repository.insertMessage(
                ChatMessage(
                    text = query,
                    isUser = true
                )
            )

            _aiStatus.value = AiStatus.THINKING

            // 2. Parse Intent
            val decision = commandParser.parseCommand(query, _currentLanguage.value)

            _aiStatus.value = AiStatus.EXECUTING

            // 3. Execute Decision
            val result = actionExecutor.execute(
                intent = decision.intent,
                language = _currentLanguage.value,
                confirmedDestructive = false
            )

            // If destructive confirmation is needed:
            if (result.isDestructive && result.destructivePrompt != null) {
                _pendingDestructive.value = Triple(decision.intent, result.destructivePrompt, query)
                _aiStatus.value = AiStatus.IDLE
                return@launch
            }

            handleExecutionResult(result, query)
        }
    }

    fun confirmDestructiveAction() {
        val pending = _pendingDestructive.value ?: return
        _pendingDestructive.value = null

        viewModelScope.launch {
            _aiStatus.value = AiStatus.EXECUTING
            val result = actionExecutor.execute(
                intent = pending.first,
                language = _currentLanguage.value,
                confirmedDestructive = true
            )
            handleExecutionResult(result, pending.third)
        }
    }

    fun cancelDestructiveAction() {
        _pendingDestructive.value = null
        _aiStatus.value = AiStatus.IDLE
        viewModelScope.launch {
            val isBn = _currentLanguage.value.startsWith("bn", ignoreCase = true)
            val cancelText = if (isBn) "অপারেশনটি বাতিল করা হয়েছে।" else "Destructive operation cancelled by user."
            repository.insertMessage(
                ChatMessage(
                    text = cancelText,
                    isUser = false,
                    isSuccess = true
                )
            )
        }
    }

    private suspend fun handleExecutionResult(result: com.example.action.ExecutionResult, originalQuery: String) {
        _aiStatus.value = if (result.isSuccess) AiStatus.COMPLETED else AiStatus.ERROR

        // Save AI Message to DB
        repository.insertMessage(
            ChatMessage(
                text = result.spokenSummary,
                isUser = false,
                actionType = result.actionType,
                isSuccess = result.isSuccess,
                executionLog = result.detailLog
            )
        )

        // Save Action Log to DB
        repository.insertLog(
            ActionLog(
                command = originalQuery,
                actionType = result.actionType,
                isRoot = result.detailLog?.contains("Root (su)") == true,
                isSuccess = result.isSuccess,
                output = result.detailLog ?: result.spokenSummary
            )
        )

        // Voice TTS Output
        voiceManager.speak(
            text = result.spokenSummary,
            languageCode = _currentLanguage.value,
            femalePreferred = _femaleVoicePreferred.value,
            pitch = _ttsPitch.value,
            speechRate = _ttsSpeechRate.value
        )

        // Refresh stats
        refreshDeviceStatus()
    }

    fun setLanguage(lang: String) {
        _currentLanguage.value = lang
    }

    fun setFemaleVoice(preferred: Boolean) {
        _femaleVoicePreferred.value = preferred
    }

    fun setPitch(pitch: Float) {
        _ttsPitch.value = pitch
    }

    fun setSpeechRate(rate: Float) {
        _ttsSpeechRate.value = rate
    }

    fun testTtsVoice() {
        val isBn = _currentLanguage.value.startsWith("bn", ignoreCase = true)
        val text = if (isBn) {
            "নমস্কার! আমি [Devil] ভয়েস অ্যাসিস্ট্যান্ট। আপনার ডিভাইসের নিয়ন্ত্রণে আমি প্রস্তুত।"
        } else {
            "Hello! I am [Devil], your AI voice assistant. This is a preview of my synthesized speech voice."
        }
        voiceManager.speak(
            text = text,
            languageCode = _currentLanguage.value,
            femalePreferred = _femaleVoicePreferred.value,
            pitch = _ttsPitch.value,
            speechRate = _ttsSpeechRate.value
        )
    }

    fun refreshDeviceStatus() {
        viewModelScope.launch {
            _deviceStatus.value = deviceManager.getDeviceStatus()
        }
    }

    fun refreshPermissions() {
        viewModelScope.launch {
            _permissionsState.value = permissionManager.getPermissionsState()
        }
    }

    fun refreshApps() {
        viewModelScope.launch {
            _installedApps.value = appLauncher.getInstalledLaunchableApps()
        }
    }

    fun refreshFiles(path: String? = null) {
        viewModelScope.launch {
            _activeDirectory.value = path
            _fileList.value = fileManager.listFiles(path)
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearMessages()
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    fun executeTerminalCommand(cmd: String, isRoot: Boolean) {
        sendUserQuery(if (isRoot) "run shell command su $cmd" else "run shell command $cmd")
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}

package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class VoiceManager(
    private val context: Context,
    private val scope: CoroutineScope
) : TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val _speechResult = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val speechResult: SharedFlow<String> = _speechResult.asSharedFlow()

    private val _speechPartial = MutableStateFlow("")
    val speechPartial: StateFlow<String> = _speechPartial.asStateFlow()

    private val _voiceError = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val voiceError: SharedFlow<String> = _voiceError.asSharedFlow()

    init {
        initTts()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                }
            })
        }
    }

    fun startListening(languageCode: String = "en-US") {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            scope.launch {
                _voiceError.emit("Speech recognition not available on this device.")
            }
            return
        }

        stopSpeaking()
        stopListening()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                    _speechPartial.value = ""
                }

                override fun onBeginningOfSpeech() {
                    _isListening.value = true
                }

                override fun onRmsChanged(rmsdB: Float) {
                    _rmsDb.value = (rmsdB.coerceAtLeast(0f) / 10f).coerceIn(0f, 1f)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _isListening.value = false
                    _rmsDb.value = 0f
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    _rmsDb.value = 0f
                    val errorMsg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized."
                        SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition."
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                        else -> "Speech recognition paused."
                    }
                    scope.launch { _voiceError.emit(errorMsg) }
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    _rmsDb.value = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spokenText = matches?.firstOrNull()?.trim()
                    if (!spokenText.isNullOrEmpty()) {
                        scope.launch { _speechResult.emit(spokenText) }
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.trim()
                    if (!text.isNullOrEmpty()) {
                        _speechPartial.value = text
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _isListening.value = false
            scope.launch { _voiceError.emit("Failed to start voice recognizer: ${e.message}") }
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        _isListening.value = false
        _rmsDb.value = 0f
    }

    fun speak(
        text: String,
        languageCode: String = "en-US",
        femalePreferred: Boolean = true,
        pitch: Float = 1.0f,
        speechRate: Float = 1.0f
    ) {
        if (!isTtsInitialized || textToSpeech == null) return

        val tts = textToSpeech ?: return

        // Set locale (support Bengali bn-BD / bn-IN or English)
        val locale = if (languageCode.startsWith("bn", ignoreCase = true)) {
            Locale("bn", "BD")
        } else {
            Locale.US
        }

        val res = tts.setLanguage(locale)
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback to default locale if Bengali voice data not installed
            tts.language = Locale.getDefault()
        }

        // Apply voice selection if supported and available
        try {
            val availableVoices = tts.voices
            if (!availableVoices.isNullOrEmpty()) {
                val matchedVoices = availableVoices.filter { it.locale.language == locale.language }
                if (matchedVoices.isNotEmpty()) {
                    var selectedVoice: Voice? = null
                    if (femalePreferred) {
                        selectedVoice = matchedVoices.find { voice ->
                            val name = voice.name.lowercase(Locale.ROOT)
                            name.contains("female") || name.contains("fem") || name.contains("-f-") || name.contains("woman")
                        }
                    }
                    if (selectedVoice == null) {
                        selectedVoice = matchedVoices.firstOrNull()
                    }
                    if (selectedVoice != null) {
                        tts.voice = selectedVoice
                    }
                }
            }
        } catch (_: Exception) {
            // Graceful fallback if device TTS does not provide voice list
        }

        // Apply female tone modulation if specific female voice is not distinct on device
        val effectivePitch = if (femalePreferred) (pitch * 1.15f).coerceIn(0.5f, 2.0f) else pitch
        tts.setPitch(effectivePitch)
        tts.setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))

        val utteranceId = "devil_tts_${System.currentTimeMillis()}"
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        try {
            textToSpeech?.stop()
        } catch (_: Exception) {}
        _isSpeaking.value = false
    }

    fun destroy() {
        stopListening()
        stopSpeaking()
        try {
            textToSpeech?.shutdown()
        } catch (_: Exception) {}
        textToSpeech = null
    }
}

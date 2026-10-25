package com.mukhhosto.aday.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mukhhosto.aday.compare.TextComparer
import com.mukhhosto.aday.data.*
import com.mukhhosto.aday.speech.DeviceSpeechRecognizer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

 data class UiState(
    val state: ScreenState = ScreenState.READY,
    val referenceText: String = "",
    val studentName: String = "",
    val languageMode: LanguageMode = LanguageMode.BANGLA,
    val isRecording: Boolean = false,
    val isRecordingPaused: Boolean = false,
    val isFinalizing: Boolean = false,
    val transcript: String = "",
    val partialTranscript: String = "",
    val editableTranscript: String = "",
    val result: ResultData? = null,
    val errorMessage: String? = null,
    val statusMessage: String? = null
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val speech = DeviceSpeechRecognizer(app)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    private var lastNextStudentAt = 0L
    private var finishTimeout: Job? = null

    fun setReferenceText(text: String) {
        val hasBangla = text.any { it in '\u0980'..'\u09FF' }
        val hasLatin = text.any { it in 'A'..'Z' || it in 'a'..'z' }
        val detected = when {
            hasBangla && hasLatin -> LanguageMode.MIXED
            hasLatin -> LanguageMode.ENGLISH
            else -> LanguageMode.BANGLA
        }
        _state.update { it.copy(referenceText = text, languageMode = detected) }
    }

    fun setStudentName(name: String) = _state.update { it.copy(studentName = name) }
    fun setLanguage(mode: LanguageMode) = _state.update { it.copy(languageMode = mode) }

    fun startRecording() {
        val current = _state.value
        if (current.isRecording || current.isFinalizing) return
        if (current.referenceText.isBlank()) {
            _state.update { it.copy(errorMessage = "মূল পাঠ লিখুন", state = ScreenState.ERROR) }
            return
        }
        if (current.studentName.isBlank()) {
            _state.update { it.copy(errorMessage = "শিক্ষার্থীর নাম লিখুন", state = ScreenState.ERROR) }
            return
        }
        _state.update {
            it.copy(
                state = ScreenState.RECORDING, isRecording = true, isRecordingPaused = false,
                isFinalizing = false, transcript = "", partialTranscript = "", editableTranscript = "",
                result = null, errorMessage = null, statusMessage = "শুনছি… পাঠ আড়াল করা হয়েছে"
            )
        }
        startRecognitionSegment()
    }

    private fun startRecognitionSegment() {
        val mode = _state.value.languageMode.apiCode
        speech.start(DeviceSpeechRecognizer.languageTag(mode), object : DeviceSpeechRecognizer.Listener {
            override fun onReady() {
                if (_state.value.isRecording && !_state.value.isRecordingPaused) {
                    _state.update { it.copy(statusMessage = "শুনছি… শিক্ষার্থীকে পাঠ বলতে দিন") }
                }
            }
            override fun onPartial(text: String) {
                if (_state.value.isRecording && !_state.value.isRecordingPaused) {
                    _state.update { it.copy(partialTranscript = text) }
                }
            }
            override fun onFinal(text: String) {
                appendRecognizedSegment(text)
                val latest = _state.value
                when {
                    latest.isFinalizing -> finishResult()
                    latest.isRecording && !latest.isRecordingPaused -> startRecognitionSegment()
                    else -> _state.update { it.copy(partialTranscript = "", statusMessage = "বিরতিতে আছে") }
                }
            }
            override fun onError(message: String) {
                val latest = _state.value
                if (latest.isFinalizing) {
                    finishResult()
                } else if (latest.isRecording && !latest.isRecordingPaused) {
                    _state.update {
                        it.copy(isRecording = false, isRecordingPaused = false, state = ScreenState.ERROR,
                            errorMessage = if (it.transcript.isBlank()) message else "$message আগের শনাক্ত করা অংশ রাখা হয়েছে।",
                            statusMessage = null, partialTranscript = "")
                    }
                    speech.cancel()
                }
            }
        })
    }

    private fun appendRecognizedSegment(text: String) {
        if (text.isBlank()) {
            _state.update { it.copy(partialTranscript = "") }
            return
        }
        _state.update { current ->
            val existing = current.transcript.trim()
            val combined = if (existing.isBlank()) text.trim() else "$existing ${text.trim()}"
            current.copy(transcript = combined, editableTranscript = combined, partialTranscript = "")
        }
    }

    fun toggleRecordingPause() {
        val current = _state.value
        if (!current.isRecording) return
        if (current.isRecordingPaused) {
            _state.update { it.copy(isRecordingPaused = false, statusMessage = "আবার শুনছি…", partialTranscript = "") }
            startRecognitionSegment()
        } else {
            _state.update { it.copy(isRecordingPaused = true, statusMessage = "বিরতিতে আছে • পাঠ এখনও আড়াল করা") }
            speech.pauseOrStop()
        }
    }

    fun stopRecording() {
        val current = _state.value
        if (!current.isRecording) return
        if (current.isRecordingPaused) {
            speech.cancel()
            _state.update { it.copy(isRecording = false, isRecordingPaused = false, isFinalizing = true, state = ScreenState.PROCESSING) }
            finishResult()
            return
        }
        _state.update { it.copy(isRecording = false, isRecordingPaused = false, isFinalizing = true, state = ScreenState.PROCESSING, statusMessage = "চূড়ান্ত বক্তব্য মিলিয়ে দেখা হচ্ছে…") }
        speech.pauseOrStop()
        finishTimeout?.cancel()
        finishTimeout = viewModelScope.launch {
            delay(1800)
            if (_state.value.isFinalizing) {
                speech.cancel()
                finishResult()
            }
        }
    }

    private fun finishResult() {
        finishTimeout?.cancel()
        finishTimeout = null
        val current = _state.value
        speech.cancel()
        val spoken = listOf(current.transcript.trim(), current.partialTranscript.trim())
            .filter { it.isNotBlank() }.joinToString(" ").trim()
        if (spoken.isBlank()) {
            _state.update { it.copy(isRecording = false, isRecordingPaused = false, isFinalizing = false,
                state = ScreenState.ERROR, statusMessage = null,
                errorMessage = "কোনো বক্তব্য শনাক্ত হয়নি। Google Speech Services ও বাংলা ভাষা-সাপোর্ট পরীক্ষা করে আবার চেষ্টা করুন।") }
            return
        }
        val result = TextComparer.compare(current.referenceText, spoken, current.studentName)
        _state.update { it.copy(state = ScreenState.RESULT, isRecording = false, isRecordingPaused = false,
            isFinalizing = false, transcript = spoken, partialTranscript = "", editableTranscript = spoken,
            result = result, errorMessage = null, statusMessage = null) }
    }

    fun recognizeSpeechGuarded() = stopRecording()
    fun editTranscript(newText: String) = _state.update { it.copy(editableTranscript = newText) }

    fun recalculate() {
        val cur = _state.value
        val result = TextComparer.compare(cur.referenceText, cur.editableTranscript, cur.studentName)
        _state.update { it.copy(result = result, state = ScreenState.RESULT) }
    }

    fun reRecord() {
        speech.cancel()
        _state.update { it.copy(state = ScreenState.READY, isRecording = false, isRecordingPaused = false,
            isFinalizing = false, transcript = "", partialTranscript = "", editableTranscript = "", result = null,
            errorMessage = null, statusMessage = null) }
    }

    fun nextStudent() {
        speech.cancel()
        _state.update { it.copy(state = ScreenState.READY, studentName = "", isRecording = false,
            isRecordingPaused = false, isFinalizing = false, transcript = "", partialTranscript = "",
            editableTranscript = "", result = null, errorMessage = null, statusMessage = null) }
    }

    fun nextStudentDebounced() {
        val now = System.currentTimeMillis()
        if (now - lastNextStudentAt < 500L) return
        lastNextStudentAt = now
        nextStudent()
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null, state = if (it.result != null) ScreenState.RESULT else ScreenState.READY) }
    }

    fun showTtsLanguageError() {
        _state.update { it.copy(errorMessage = "এই ফোনের Text-to-Speech ইঞ্জিনে পাঠের ভাষার কণ্ঠস্বর ইনস্টল নেই। ফোনের Text-to-Speech সেটিংস থেকে বাংলা/ইংরেজি voice data ইনস্টল করুন।") }
    }

    fun showPlaybackError() { _state.update { it.copy(errorMessage = "অডিও চালানো যায়নি।", state = ScreenState.ERROR) } }
    fun showPermissionDenied() { _state.update { it.copy(state = ScreenState.ERROR, errorMessage = "মাইক্রোফোন অনুমতি প্রয়োজন। অ্যাপের Settings থেকে অনুমতি দিন।") } }

    fun onAppBackgrounded() {
        val current = _state.value
        speech.cancel()
        finishTimeout?.cancel()
        if (current.isRecording || current.isFinalizing || current.result != null || current.studentName.isNotBlank()) {
            _state.update { it.copy(state = ScreenState.READY, studentName = "", isRecording = false,
                isRecordingPaused = false, isFinalizing = false, transcript = "", partialTranscript = "",
                editableTranscript = "", result = null, errorMessage = null, statusMessage = null) }
        }
    }

    override fun onCleared() {
        finishTimeout?.cancel()
        speech.cancel()
        super.onCleared()
    }
}

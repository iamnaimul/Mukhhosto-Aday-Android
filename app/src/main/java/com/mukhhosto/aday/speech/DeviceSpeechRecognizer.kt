package com.mukhhosto.aday.speech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Speech recognition adapter for Android 7+.
 * Android 12+ prefers the dedicated on-device recognizer when available.
 * Older devices (including Android 9) use the installed system recognizer,
 * which may use Google's online recognition service.
 */
class DeviceSpeechRecognizer(private val context: Context) {
    private var recognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile private var listeningRequested = false
    @Volatile private var activeLanguageTag = "bn-BD"
    @Volatile private var activeListener: Listener? = null
    @Volatile private var activeOnDevice = false
    private var retryCount = 0
    private var retryRunnable: Runnable? = null

    interface Listener {
        fun onReady()
        fun onPartial(text: String)
        fun onFinal(text: String)
        fun onError(message: String)
    }

    fun start(languageTag: String, listener: Listener) {
        stopAndDestroy()
        listeningRequested = true
        activeLanguageTag = languageTag
        activeListener = listener
        retryCount = 0
        val canUseOnDevice = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        startRecognizer(useOnDevice = canUseOnDevice, allowFallback = true)
    }

    private fun startRecognizer(useOnDevice: Boolean, allowFallback: Boolean) {
        if (!listeningRequested) return
        try {
            activeOnDevice = useOnDevice
            recognizer = if (useOnDevice) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } else {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    activeListener?.onError("এই ফোনে Speech Recognition সেবা পাওয়া যায়নি। Google Speech Services ইনস্টল/সক্রিয় করুন।")
                    return
                }
                SpeechRecognizer.createSpeechRecognizer(context)
            }

            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { retryCount = 0; activeListener?.onReady() }
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit

                override fun onPartialResults(partialResults: Bundle?) {
                    val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull().orEmpty()
                    if (text.isNotBlank()) { retryCount = 0; activeListener?.onPartial(text) }
                }

                override fun onResults(results: Bundle?) {
                    retryCount = 0
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull().orEmpty()
                    activeListener?.onFinal(text)
                }

                override fun onError(error: Int) {
                    // Some recognizers time out after a short pause in speech. Keep listening
                    // while the teacher's recording session is active instead of failing at once.
                    if (allowFallback && activeOnDevice &&
                        (error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
                         error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE)) {
                        stopAndDestroy()
                        if (listeningRequested) startRecognizer(useOnDevice = false, allowFallback = false)
                        return
                    }
                    val transient = error == SpeechRecognizer.ERROR_NO_MATCH ||
                        error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                        error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY
                    if (transient && listeningRequested && retryCount < 8) {
                        retryCount++
                        scheduleRetry()
                    } else {
                        activeListener?.onError(errorMessage(error))
                    }
                }
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, activeLanguageTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, activeLanguageTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                // Do NOT force offline: Android 9 phones often need Google's online service
                // for Bengali recognition, and an offline-only preference can cause no result.
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }
            recognizer?.startListening(intent)
        } catch (e: SecurityException) {
            activeListener?.onError("মাইক্রোফোন অনুমতি নেই। App Settings থেকে Microphone permission দিন।")
        } catch (e: Exception) {
            activeListener?.onError("Speech Recognition চালু করা যায়নি। Google Speech Services আপডেট করে আবার চেষ্টা করুন।")
        }
    }

    private fun scheduleRetry() {
        retryRunnable?.let(mainHandler::removeCallbacks)
        val retry = Runnable {
            if (listeningRequested) {
                stopRecognizerOnly()
                startRecognizer(useOnDevice = activeOnDevice, allowFallback = !activeOnDevice)
            }
        }
        retryRunnable = retry
        mainHandler.postDelayed(retry, 350L)
    }

    /** Stop the current recognition segment, but allow its final callback to arrive. */
    fun pauseOrStop() {
        listeningRequested = false
        retryRunnable?.let(mainHandler::removeCallbacks)
        retryRunnable = null
        try { recognizer?.stopListening() } catch (_: Exception) { }
    }

    fun cancel() {
        listeningRequested = false
        retryRunnable?.let(mainHandler::removeCallbacks)
        retryRunnable = null
        stopAndDestroy()
    }

    private fun stopRecognizerOnly() {
        try { recognizer?.cancel() } catch (_: Exception) { }
        try { recognizer?.destroy() } catch (_: Exception) { }
        recognizer = null
    }

    private fun stopAndDestroy() {
        stopRecognizerOnly()
    }

    private fun errorMessage(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "মাইক্রোফোনে সমস্যা হয়েছে। অন্য অ্যাপ মাইক্রোফোন ব্যবহার করছে কি না দেখুন।"
        SpeechRecognizer.ERROR_CLIENT -> "Speech Recognition সেশন বন্ধ হয়েছে। আবার ‘বলা শুরু’ চাপুন।"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "মাইক্রোফোন অনুমতি প্রয়োজন। App Settings থেকে অনুমতি দিন।"
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ইন্টারনেট-ভিত্তিক Speech Recognition-এ সমস্যা হয়েছে। ইন্টারনেট ও Google Speech Services পরীক্ষা করুন।"
        SpeechRecognizer.ERROR_NO_MATCH -> "কথা শনাক্ত হয়নি। আবার স্পষ্ট করে বলুন।"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech Recognition ব্যস্ত। কয়েক সেকেন্ড পর আবার চেষ্টা করুন।"
        SpeechRecognizer.ERROR_SERVER -> "Speech Recognition সেবা উত্তর দিচ্ছে না। Google Speech Services আপডেট করে আবার চেষ্টা করুন।"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "কথা শোনা যায়নি। মাইক্রোফোনের কাছে থেকে বলুন।"
        else -> "কথা শনাক্ত করা যায়নি (ত্রুটি $code)। Google Speech Services এবং ভাষা-সাপোর্ট পরীক্ষা করুন।"
    }

    companion object {
        fun languageTag(mode: String): String = when (mode) {
            "en" -> "en-US"
            // Mixed-script school lessons are Bangla-first; English-only lessons use en-US.
            "auto" -> "bn-BD"
            else -> "bn-BD"
        }
    }
}

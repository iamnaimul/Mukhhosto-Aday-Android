# মুখস্ত আদায় (Mukhhosto Aday)

Android Studio project — **v7.0 final UI refresh** (app version 1.4.0).

## What it does

- Bengali-first Kotlin + Jetpack Compose + Material 3 app.
- One student → one live speech session → one instant result → clear → next student.
- Detects Bangla, English, or mixed script from the reference পাঠ.
- Hides the reference পাঠ only while the student is speaking / speech recognition is active or finalizing.
- Keeps the reference পাঠ visible when using “মূল পাঠ শুনুন” (Text-to-Speech).
- Pause/Resume and Stop controls for speech recognition.
- Uses Android `SpeechRecognizer`; prefers the dedicated on-device recognizer on Android 12+ when the device provides one, otherwise uses the installed system recognition service.
- Uses the phone's Text-to-Speech engine to read the reference lesson aloud, with pause/resume (resume restarts the current sentence) and stop.
- Modern off-white UI with green/red accents and rounded cards.
- Accuracy visualization: circular percentage graph plus horizontal accuracy/difference bars.
- Local comparison: correct words, substitutions, omissions, extra words, WER, accuracy, exact match and word-by-word diff.
- Editable transcript and recalculation.
- No API key, app-side speech server, database, history, backup, analytics or student directory.
- Noto Sans Bengali bundled.

## Open in Android Studio

1. Extract this ZIP and open the `MukhhostoAday` folder.
2. Use JDK 17 and install Android SDK 34 if prompted.
3. Gradle Sync, then build/run on a real phone with a microphone.
4. Allow microphone permission. On first use, the app now starts listening immediately after permission is granted.
5. Install/update **Speech Recognition & Synthesis from Google** (or Google app, depending on your Android build) if no working recognition service is available.
6. Keep internet enabled for Android 9 online recognition. The app no longer requests offline-only recognition, which can fail when the Bengali offline model is absent.
7. For Bengali, test the actual phone/service. In Google app / Speech Services settings, make sure Bengali (Bangladesh) is available if the option is provided. English lessons are detected from the reference text and use en-US.

## Bengali recognition and privacy caveat

Bengali recognition may work through the phone's installed speech service, but Android does not guarantee Bengali or offline support on every device. The app prefers Android's dedicated on-device recognizer on Android 12+ when available. On older phones or unsupported devices, the fallback system recognizer may use a network service and may send audio to that service. The app itself does not upload audio, but it cannot guarantee the privacy/data-retention behaviour of another app/service. If strict no-network audio privacy is required, use only a confirmed on-device recognizer and do not enable fallback to a network recognizer.

`EXTRA_PREFER_OFFLINE` is explicitly set to false in this fix because Android 9 online recognition often needs network access when an offline Bengali model is unavailable. Bengali recognition still depends on the recognizer service supporting bn-BD. The app retries brief no-match/speech-timeout errors while the teacher session remains active. Recognition can mishear words; the score compares the recognized transcript, not a perfect ground-truth transcription.

## Privacy / lifecycle

- No database or persistent result storage.
- Student name, transcript and result remain in in-memory UI state only.
- `FLAG_SECURE` helps prevent screenshots and task-preview exposure.
- Speech session is stopped/cancelled when the app leaves the foreground; student-specific state is cleared.
- No audio file is created or replayed in this edition because Android's SpeechRecognizer listens to the microphone live and does not accept a previously recorded audio file.
- The TTS “Pause” stops speech and resume restarts the current sentence.

## Build

```bash
./gradlew test
./gradlew assembleDebug
```

Windows: `gradlew.bat test` and `gradlew.bat assembleDebug`. First Gradle sync needs internet to download dependencies. This source package has not been verified with a complete Android SDK build in the generation environment; please run Gradle Sync and Build in Android Studio.


## Developer credit
The app footer credits **Mr. Md. Naimul Hassan** and includes the WhatsApp link labelled `@iam.naimul`.

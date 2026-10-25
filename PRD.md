# মুখস্ত আদায় — Product Requirements Document (PRD)

**Version:** 7.0 — Modern UI and Accuracy Graph
**Platform:** Android 7.0+ (API 24+)
**Stack:** Kotlin, Jetpack Compose, Material 3, Android SpeechRecognizer and TextToSpeech

## Goal
One student → one live speech session → one instant comparison → clear → next student.

## Functional requirements
- Teacher enters/pastes one reference পাঠ. Detect Bangla/English/mixed script from the entered text.
- Only student name is requested.
- Hide the reference text only while the student is speaking / recognition is active or finalizing. Keep the reference text visible during “মূল পাঠ শুনুন” (TTS).
- Start listening only after explicit teacher action. Pause/resume and stop listening. Stop listening and clear student-specific data when app leaves foreground. Never background-listen.
- Prefer Android on-device `SpeechRecognizer` on API 31+ when available; otherwise use installed system recognizer for wider device compatibility. Do not force offline-only recognition: on Android 9, online recognition may be necessary when Bengali offline speech data is unavailable.
- Compare transcript locally with reference and show correct, substitution, omission, insertion, WER, accuracy, exact match, word-by-word diff.
- Permit transcript editing/recalculation; re-listen for same student; clear/next student.
- Use installed phone TTS to read the reference text, with stop and pause/resume controls. Keep the reference text visible during TTS; resume restarts the current sentence.
- No app-side audio file, database, history, backups, analytics, result uploads, or server/API key.

## Bengali and privacy limits
- Bengali support depends on the installed speech recognition service and its language packs. It cannot be guaranteed across Android devices.
- On-device recognition is selected only when Android reports it available. On older/unsupported devices, the default system recognizer fallback may use network processing. The app does not directly upload audio, but it cannot control another recognizer's data handling. A strict privacy deployment should disable network fallback and show a clear unsupported-device message.
- `EXTRA_PREFER_OFFLINE` is set to false in v6.1 to avoid failures on Android 9 when no offline Bengali model is installed. Test actual Bengali and English recognition on the target phone before classroom use.

## Explicit exclusions
No exam management, database, student profile, roll/class fields, history, ranking, backup/restore, cloud result storage, persistent audio, background recording, or API keys.

## Acceptance criteria
- Android Studio project opens and Gradle build succeeds.
- Microphone begins only after teacher action; reference text is hidden while listening.
- Pause/resume and stop work, and app backgrounding cancels listening and clears student-specific state.
- Comparison runs locally and no app-managed result persistence exists.
- Bengali recognition limitations are disclosed and no claim of universal/offline support is made.


## UI and attribution (v7.0)
- Modern off-white interface with green and red status colors and rounded cards.
- Results include a circular accuracy percentage graph and horizontal accuracy/difference bars.
- Footer credit: “Developed by Mr. Md. Naimul Hassan” with a WhatsApp link labelled `@iam.naimul`.
- App version: 1.4.0.

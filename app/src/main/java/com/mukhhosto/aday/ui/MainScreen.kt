package com.mukhhosto.aday.ui

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mukhhosto.aday.data.*
import com.mukhhosto.aday.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    vm: MainViewModel,
    onRequestMicPermission: () -> Boolean
) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val uriHandler = LocalUriHandler.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    var ttsActive by remember { mutableStateOf(false) }
    var ttsPaused by remember { mutableStateOf(false) }
    var ttsChunks by remember { mutableStateOf<List<String>>(emptyList()) }
    var ttsChunkIndex by remember { mutableIntStateOf(0) }
    var speakChunkAction by remember { mutableStateOf<((Int) -> Unit)?>(null) }

    DisposableEffect(context) {
        val engine = TextToSpeech(context) { status -> ttsReady = status == TextToSpeech.SUCCESS }
        tts = engine
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { ttsActive = true }
            override fun onDone(utteranceId: String?) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    val next = ttsChunkIndex + 1
                    ttsChunkIndex = next
                    if (next < ttsChunks.size && !ttsPaused) speakChunkAction?.invoke(next)
                    else if (next >= ttsChunks.size) { ttsActive = false; ttsPaused = false }
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { ttsActive = false }
            override fun onError(utteranceId: String?, errorCode: Int) { ttsActive = false }
        })
        onDispose {
            engine.stop()
            engine.shutdown()
            tts = null
            ttsActive = false
            ttsPaused = false
        }
    }

    fun stopTts() {
        tts?.stop()
        ttsActive = false
        ttsPaused = false
        ttsChunks = emptyList()
        ttsChunkIndex = 0
    }

    fun speakChunk(index: Int) {
        val engine = tts ?: return
        if (index !in ttsChunks.indices) return
        ttsChunkIndex = index
        ttsActive = true
        ttsPaused = false
        engine.speak(ttsChunks[index], TextToSpeech.QUEUE_FLUSH, null, "lesson_chunk_$index")
    }
    speakChunkAction = ::speakChunk

    fun speakReference() {
        val engine = tts ?: return
        if (!ttsReady || ui.referenceText.isBlank()) return
        val hasBangla = ui.referenceText.any { it in '\u0980'..'\u09FF' }
        val locale = if (hasBangla) Locale("bn", "BD") else Locale.US
        if (engine.isLanguageAvailable(locale) < TextToSpeech.LANG_AVAILABLE) {
            vm.showTtsLanguageError()
            return
        }
        stopTts()
        val chunks = ui.referenceText.split(Regex("(?<=[।.!?\\n])\\s*"))
            .map { it.trim() }.filter { it.isNotBlank() }
        ttsChunks = if (chunks.isEmpty()) listOf(ui.referenceText) else chunks
        ttsChunkIndex = 0
        engine.language = locale
        speakChunk(0)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                stopTts()
                vm.onAppBackgrounded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Hide the reference only while the student is speaking / recognition is finalizing.
    // TTS is for the teacher/student to listen to the original lesson, so keep text visible.
    val shouldHideLesson = ui.isRecording || ui.isFinalizing || ui.state == ScreenState.PROCESSING

    Scaffold(
        containerColor = OffWhite,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("মুখস্ত আদায়", fontWeight = FontWeight.Bold)
                        Text("পাঠ যাচাই • তাৎক্ষণিক ফলাফল", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.9f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ForestGreen, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = PaleGreen)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("পাঠ শুনুন • বলুন • যাচাই করুন", style = MaterialTheme.typography.titleLarge, color = ForestGreen, fontWeight = FontWeight.Bold)
                    Text("একজন শিক্ষার্থী • একটি পাঠ • একটি তাৎক্ষণিক ফলাফল। পরবর্তী শিক্ষার্থীর আগে আগের ফলাফল মুছে যাবে।", style = MaterialTheme.typography.bodyMedium)
                }
            }

            SectionCard(title = "১. মূল পাঠ") {
                if (shouldHideLesson) {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = PaleRed)) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🔒 পাঠ আড়াল করা হয়েছে", color = ErrorRed, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("শিক্ষার্থী যেন স্ক্রিন দেখে বলতে না পারে—পাঠের লেখা লুকানো আছে।", color = DarkGray, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = ui.referenceText, onValueChange = vm::setReferenceText,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 128.dp),
                        placeholder = { Text("এখানে পাঠ লিখুন অথবা Paste করুন") },
                        enabled = ui.state != ScreenState.PROCESSING && ui.state != ScreenState.RESULT,
                        shape = RoundedCornerShape(16.dp), minLines = 4
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = {}, label = { Text("ভাষা: ${ui.languageMode.label} • স্বয়ংক্রিয়") }, enabled = false)
                        Spacer(Modifier.weight(1f))
                        Text("${ui.referenceText.trim().split(Regex("\\s+")).count { it.isNotBlank() }} শব্দ", style = MaterialTheme.typography.labelMedium, color = MutedText)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { speakReference() },
                            enabled = ui.referenceText.isNotBlank() && ttsReady && !ttsActive && !ttsPaused,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("🔊 মূল পাঠ শুনুন") }
                        OutlinedButton(
                            onClick = {
                                if (ttsPaused) speakChunk(ttsChunkIndex)
                                else {
                                    tts?.stop()
                                    ttsPaused = true
                                    ttsActive = false
                                }
                            },
                            enabled = ttsActive || ttsPaused,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text(if (ttsPaused) "▶ আবার শুনুন" else "Ⅱ Pause") }
                    }
                    if (ttsActive || ttsPaused) {
                        OutlinedButton(
                            onClick = { stopTts() },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("■ শোনা বন্ধ") }
                        Text("মূল পাঠ দৃশ্যমান থাকবে—এটি শুধু পাঠ শোনার মোড।", style = MaterialTheme.typography.bodySmall, color = MutedText)
                    }
                    Text("পাঠের ভাষা লেখার ধরন দেখে নির্ধারিত হয়। ফোনের Speech Recognition সার্ভিসে বাংলা সাপোর্ট ও ইন্টারনেটের প্রয়োজন ডিভাইসভেদে আলাদা হতে পারে।", style = MaterialTheme.typography.bodySmall, color = MutedText)
                }
            }

            SectionCard(title = "২. শিক্ষার্থীর নাম") {
                OutlinedTextField(value = ui.studentName, onValueChange = vm::setStudentName,
                    modifier = Modifier.fillMaxWidth(), placeholder = { Text("শুধু নাম লিখুন") }, singleLine = true,
                    enabled = !ui.isRecording && !ui.isFinalizing && ui.state != ScreenState.PROCESSING && ui.state != ScreenState.RESULT,
                    shape = RoundedCornerShape(14.dp))
            }

            SectionCard(title = "৩. পাঠ বলা শনাক্ত করুন") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { stopTts(); if (onRequestMicPermission()) vm.startRecording() },
                        enabled = !ui.isRecording && !ui.isFinalizing && ui.state != ScreenState.PROCESSING && ui.state != ScreenState.RESULT && ui.referenceText.isNotBlank() && ui.studentName.isNotBlank(),
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp), colors = ButtonDefaults.buttonColors(containerColor = ForestGreen), shape = RoundedCornerShape(14.dp)) { Text("● বলা শুরু") }
                    Button(onClick = vm::stopRecording, enabled = ui.isRecording,
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp), colors = ButtonDefaults.buttonColors(containerColor = ErrorRed), shape = RoundedCornerShape(14.dp)) { Text("■ শেষ ও যাচাই") }
                }
                if (ui.isRecording) {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = if (ui.isRecordingPaused) Color(0xFFFFF3D6) else PaleRed)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(if (ui.isRecordingPaused) "Ⅱ বিরতিতে আছে" else "● শুনছি • পাঠ আড়াল করা", color = if (ui.isRecordingPaused) DarkGray else ErrorRed, fontWeight = FontWeight.Bold)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = vm::toggleRecordingPause, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text(if (ui.isRecordingPaused) "▶ আবার শুনুন" else "Ⅱ Pause") }
                                OutlinedButton(onClick = vm::stopRecording, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("■ শেষ করুন") }
                            }
                        }
                    }
                }
                if (ui.isFinalizing || ui.state == ScreenState.PROCESSING) {
                    Row(Modifier.fillMaxWidth().background(PaleGreen, RoundedCornerShape(14.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp, color = ForestGreen)
                        Column { Text("বক্তব্য চূড়ান্ত করা হচ্ছে", fontWeight = FontWeight.Bold); Text("ফোনের Speech Recognition-এর ফলাফল অপেক্ষা করা হচ্ছে।", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }

            ui.errorMessage?.let { message ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = PaleRed)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("মনোযোগ দিন", color = ErrorRed, fontWeight = FontWeight.Bold)
                        Text(message, color = ErrorRed)
                        TextButton(onClick = vm::dismissError) { Text("ঠিক আছে") }
                    }
                }
            }
            ui.result?.let { result -> ResultCard(result, ui, vm) }
            Text(
                "গোপনীয়তা: অ্যাপ নিজে অডিও সার্ভারে আপলোড করে না। তবে ফোনের সাধারণ Speech Recognition সার্ভিস ইন্টারনেট ব্যবহার করতে পারে; নিশ্চিতভাবে অফলাইন শনাক্তকরণ কেবল ডিভাইসের On-device recognizer পাওয়া গেলে সম্ভব।",
                style = MaterialTheme.typography.bodySmall, color = MutedText
            )
            HorizontalDivider(color = BorderGray)
            Column(
                Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text("Developed by", style = MaterialTheme.typography.labelMedium, color = MutedText)
                Text("Mr. Md. Naimul Hassan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ForestGreen)
                Text(
                    "WhatsApp  @iam.naimul",
                    modifier = Modifier.clickable {
                        runCatching { uriHandler.openUri("https://wa.me/iam.naimul") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = ForestGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ForestGreen)
                content()
            }
        )
    }
}

@Composable
private fun ResultCard(result: ResultData, ui: UiState, vm: MainViewModel) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("পাঠ যাচাইয়ের ফলাফল", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BlueDark)
            Text("শিক্ষার্থী: ${result.studentName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = if (result.accuracy >= 80.0) Color(0xFFEAF5EC) else Color(0xFFFFF3E8))
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("পাঠের সঠিকতার গ্রাফ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Box(Modifier.size(154.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxSize().padding(8.dp)) {
                            val stroke = Stroke(width = 13.dp.toPx(), cap = StrokeCap.Round)
                            drawArc(color = Color(0xFFD8E5D9), startAngle = -90f, sweepAngle = 360f, useCenter = false, style = stroke)
                            drawArc(
                                color = if (result.accuracy >= 80.0) SuccessGreen else WarnAmber,
                                startAngle = -90f,
                                sweepAngle = (result.accuracy.coerceIn(0.0, 100.0) * 3.6).toFloat(),
                                useCenter = false,
                                style = stroke
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            Text("${"%.1f".format(result.accuracy)}%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = if (result.accuracy >= 80.0) SuccessGreen else WarnAmber)
                            Text("সঠিকতা", style = MaterialTheme.typography.labelMedium, color = MutedText)
                        }
                    }
                    Text(if (result.exactMatch) "চমৎকার! সব শব্দ মিলে গেছে" else "${result.wrongWords + result.missingWords + result.extraWords}টি শব্দের পার্থক্য পাওয়া গেছে", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("সঠিকতা", style = MaterialTheme.typography.labelMedium, color = SuccessGreen)
                            Text("${"%.1f".format(result.accuracy)}%", style = MaterialTheme.typography.labelMedium, color = SuccessGreen, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { (result.accuracy / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(9.dp),
                            color = SuccessGreen,
                            trackColor = Color(0xFFD8E5D9)
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("পার্থক্য", style = MaterialTheme.typography.labelMedium, color = ErrorRed)
                            Text("${(100.0 - result.accuracy).coerceIn(0.0, 100.0).let { "%.1f".format(it) }}%", style = MaterialTheme.typography.labelMedium, color = ErrorRed, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { ((100.0 - result.accuracy) / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(7.dp),
                            color = ErrorRed,
                            trackColor = Color(0xFFF1D8D5)
                        )
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("সঠিক", result.correctWords.toString(), SuccessGreen, Modifier.weight(1f))
                StatBox("ভুল", result.wrongWords.toString(), ErrorRed, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("বাদ পড়েছে", result.missingWords.toString(), WarnAmber, Modifier.weight(1f))
                StatBox("অতিরিক্ত", result.extraWords.toString(), BluePrimary, Modifier.weight(1f))
            }

            Text("WER: ${"%.3f".format(result.wordErrorRate)}   •   Exact Match: ${if (result.exactMatch) "হ্যাঁ" else "না"}", style = MaterialTheme.typography.bodyMedium)
            Text("মূল পাঠ: ${result.referenceWordCount} শব্দ  •  শনাক্ত: ${result.recognizedWordCount} শব্দ", style = MaterialTheme.typography.bodySmall, color = MutedText)

            HorizontalDivider(color = BorderGray)
            Text("শব্দভিত্তিক তুলনা", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                result.diffs.forEach { diff -> DiffRow(diff) }
            }

            HorizontalDivider(color = BorderGray)
            Text("শনাক্ত করা পাঠ (সংশোধন করা যাবে)", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = ui.editableTranscript,
                onValueChange = vm::editTranscript,
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                shape = RoundedCornerShape(14.dp)
            )
            OutlinedButton(onClick = vm::recalculate, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text("সংশোধিত বক্তব্য দিয়ে আবার হিসাব করুন")
            }
            OutlinedButton(onClick = vm::reRecord, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text("একই শিক্ষার্থীর জন্য আবার রেকর্ড")
            }
            Button(
                onClick = vm::nextStudentDebounced,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                shape = RoundedCornerShape(14.dp)
            ) { Text("ফলাফল মুছে পরবর্তী শিক্ষার্থী", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, color: Color, modifier: Modifier) {
    Card(
        modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.09f))
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = color)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun DiffRow(diff: WordDiff) {
    val (color, label, text) = when (diff.status) {
        WordStatus.CORRECT -> Triple(SuccessGreen, "সঠিক", diff.reference ?: "")
        WordStatus.SUBSTITUTION -> Triple(ErrorRed, "ভুল", "${diff.reference} → ${diff.recognized}")
        WordStatus.DELETION -> Triple(WarnAmber, "বাদ", "${diff.reference} — বলা হয়নি")
        WordStatus.INSERTION -> Triple(BluePrimary, "অতিরিক্ত", "${diff.recognized} — মূল পাঠে নেই")
    }
    Row(
        Modifier.fillMaxWidth().background(color.copy(alpha = 0.07f), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        Text(text, color = DarkGray, style = MaterialTheme.typography.bodyMedium)
    }
}


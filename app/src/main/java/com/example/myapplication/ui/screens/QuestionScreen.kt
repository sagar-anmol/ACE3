package com.example.myapplication.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import android.net.Uri

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.Question
import com.example.myapplication.data.model.QuestionType
import com.example.myapplication.data.model.UserInfo
import com.example.myapplication.ui.components.AnimatedProgressBar
import com.example.myapplication.ui.components.AudioRecorderView
import com.example.myapplication.ui.components.BigButton
import java.io.File
import java.util.Locale

@Composable
fun QuestionScreen(
    userInfo: UserInfo,

    question: Question,
    questionIndex: Int,
    totalQuestions: Int,
    selectedOption: Int?,
    textAnswer: String,
    audioPath: String?,
    actionScore: Int? = null,
    serverScore: String?,
    isPending: Boolean,
    onSelectOption: (Int) -> Unit,
    onTextChange: (String) -> Unit,
    onAudioRecorded: (String) -> Unit,
    onSubmitAudio: (File) -> Unit, // Added this line
    onActionSequenceCompleted: (Int) -> Unit = {},
    onUploadImage: (File) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    // Fix crash: If null → default to TEXT
    val qType: QuestionType = question.type ?: QuestionType.TEXT

    // IMPORTANT: Reset image per-question to avoid “previous upload showing”
    var selectedImage by remember(question.id) { mutableStateOf<Uri?>(null) }

    // ---- TTS ----
    DisposableEffect(context) {
        val t = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = Locale.US
        }
        tts = t
        onDispose { t.stop(); t.shutdown() }
    }

    LaunchedEffect(question.id) {
        tts?.speak(question.title, TextToSpeech.QUEUE_FLUSH, null, "Q")
    }

    val scrollState = rememberScrollState()

    // Utility: Safe drawable loader
    fun getDrawableId(image: String?): Int? {
        if (image.isNullOrBlank()) return null
        val clean = image.substringBeforeLast(".")
        val id = context.resources.getIdentifier(clean, "drawable", context.packageName)
        return if (id != 0) id else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {

        // ---------- TOP PROGRESS ----------
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            AnimatedProgressBar(
                progress = (questionIndex + 1) / totalQuestions.toFloat(),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                "${questionIndex + 1} / $totalQuestions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // ---------- ACTION SEQUENCE ----------
        if (qType == QuestionType.ACTION_SEQUENCE) {
            ActionSequenceScreen(
                question = question,
                onResult = { score -> onActionSequenceCompleted(score) },
                onBack = onPrev
            )
        } else {

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ----- TITLE -----
                Text(
                    question.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                if (question.description.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        question.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(40.dp))

                // ---------- MAIN LOGIC ----------
                when (qType) {

                    // ---------------- SINGLE CHOICE ----------------
                    QuestionType.SINGLE_CHOICE -> {
                        question.options.forEachIndexed { idx, opt ->
                            OutlinedButton(
                                onClick = { onSelectOption(idx) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(6.dp)
                                    .height(60.dp)
                            ) { Text(opt) }
                        }
                    }

                    // ---------------- IMAGE MAP ----------------
                    QuestionType.IMAGE_MAP_SELECTION -> {
                        ImageMapSelectionScreen(
                            correctRegion = question.correctRegion ?: "",
                            onResult = { correct ->
                                onSelectOption(if (correct) 1 else 0)
                            }
                        )
                    }

                    // ---------------- IMAGE UPLOAD ----------------
                    QuestionType.IMAGE_UPLOAD -> {
                        val referenceId = getDrawableId(question.image)

                        ImageUploadScreen(
                            diagramResId = referenceId,
                            selectedImage = selectedImage,
                            onImageSelected = { uri ->
                                selectedImage = uri
                                if (uri != null) {
                                    val file = uri.path?.let { File(it) }
                                    if (file != null) {
                                        onUploadImage(file)
                                    }
                                }
                            }
                        )
                    }

                    // ---------------- TEXT INPUT ----------------
                    QuestionType.TEXT -> {

                        // ⬇️ SHOW IMAGE IF QUESTION HAS ONE
                        question.image?.let { img ->
                            val resId = context.resources.getIdentifier(
                                img.substringBefore("."), "drawable", context.packageName
                            )

                            if (resId != 0) {
                                Image(
                                    painter = painterResource(id = resId),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(260.dp)
                                        .padding(bottom = 16.dp)
                                )
                            }
                        }

                        // ⬇️ TEXT ANSWER INPUT
                        OutlinedTextField(
                            value = textAnswer,
                            onValueChange = onTextChange,
                            label = { Text("Type your answer here…") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            shape = RoundedCornerShape(16.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 18.sp)
                        )
                    }


                    // ---------------- AUDIO ----------------
                    QuestionType.AUDIO -> {

                        // SHOW LETTER IMAGE
                        question.image?.let { img ->
                            val imgId = context.resources.getIdentifier(
                                img.substringBefore("."), "drawable", context.packageName
                            )

                            if (imgId != 0) {
                                Image(
                                    painter = painterResource(id = imgId),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(260.dp)
                                        .padding(bottom = 16.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }

                        // AUDIO RECORDER UI
                        AudioRecorderView(
                            questionId = question.id,
                            existingPath = audioPath,
                            onAudioRecorded = onAudioRecorded
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (audioPath != null) {
                            Button(onClick = { onSubmitAudio(File(audioPath)) }) {
                                Text("Submit Audio")
                            }
                        }

                        Text(
                            "Tap to record your answer",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.LightGray
                        )
                    }


                    else -> { /* fallback text */ }
                }
            }
        }

        // ---------- NEXT BUTTON ENABLE STATE ----------
        val isAnswered = when (qType) {
            QuestionType.SINGLE_CHOICE -> selectedOption != null
            QuestionType.TEXT -> textAnswer.isNotBlank()
            QuestionType.AUDIO -> audioPath != null
            QuestionType.ACTION_SEQUENCE -> actionScore != null
            QuestionType.IMAGE_MAP_SELECTION -> selectedOption != null
            QuestionType.IMAGE_UPLOAD -> selectedImage != null || isPending || serverScore != null
        }

        BigButton(
            text = if (questionIndex == totalQuestions - 1) "FINISH" else "NEXT",
            onClick = onNext,
            enabled = isAnswered
        )
    }
}

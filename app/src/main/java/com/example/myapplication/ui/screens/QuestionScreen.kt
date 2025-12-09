package com.example.myapplication.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.RectangleShape
import android.net.Uri

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
    onSelectOption: (Int) -> Unit,
    onTextChange: (String) -> Unit,
    onAudioRecorded: (String) -> Unit,
    onActionSequenceCompleted: (Int) -> Unit = {},
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    // Defensive: if question.type is null, default to TEXT (prevents NPE crashes)
    val qType: QuestionType = question.type ?: QuestionType.TEXT
    var selectedImage by remember(question.id) { mutableStateOf<Uri?>(null) }

    // Initialize TTS
    DisposableEffect(context) {
        val textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
        tts = textToSpeech
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }


    // Speak question text (use qType not question.type)
    LaunchedEffect(question, tts) {
        tts?.let {
            if (!it.isSpeaking) {
                val textToSpeak = "${question.title}. ${question.description}"
                it.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "QuestionId")

                if (qType == QuestionType.AUDIO) {
                    it.speak(
                        "Please record your answer after the beep.",
                        TextToSpeech.QUEUE_ADD,
                        null,
                        "Prompt"
                    )
                }
            }
        }
    }

    val scrollState = rememberScrollState()
    @Composable
    fun getDrawableId(name: String, context: android.content.Context = LocalContext.current): Int {
        if (name.isBlank()) return 0
        val clean = name.substringBeforeLast(".")
        return context.resources.getIdentifier(clean, "drawable", context.packageName)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {

        // TOP PROGRESS BAR
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

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "${questionIndex + 1} / $totalQuestions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // ----------------------------------------------
        // SPECIAL CASE: ACTION_SEQUENCE (Canvas screen)
        // ----------------------------------------------
        if (qType == QuestionType.ACTION_SEQUENCE) {

            ActionSequenceScreen(
                question = question,
                onResult = { score ->
                    // report to parent (parent will decide how to navigate)
                    onActionSequenceCompleted(score)
                    // don't call navigation here to avoid navigation-in-composition issues
                },
                onBack = onPrev
            )

        } else {

            // ----------------------------------------------
            // NORMAL QUESTIONS (AUDIO, TEXT, SINGLE_CHOICE)
            // ----------------------------------------------

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                // TITLE
                Text(
                    text = question.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                // DESCRIPTION
                if (question.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = question.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // DYNAMIC INPUT UI
                when (qType) {

                    QuestionType.SINGLE_CHOICE -> {
                        question.options.forEachIndexed { index, option ->
                            val isSelected = selectedOption == index

                            OutlinedButton(
                                onClick = { onSelectOption(index) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .height(60.dp),
                                border = ButtonDefaults.outlinedButtonBorder
                            ) {
                                Text(option)
                            }
                        }
                    }
                    QuestionType.IMAGE_MAP_SELECTION -> {
                        ImageMapSelectionScreen(
                            correctRegion = question.correctRegion ?: "",
                            onResult = { isCorrect ->
                                onSelectOption(if (isCorrect) 1 else 0) // store score
                                // DO NOT call onNext()
                            }
                        )
                    }


                    QuestionType.IMAGE_UPLOAD -> {
                        ImageUploadScreen(
                            diagramResId = getDrawableId(question.image ?: ""),
                            selectedImage = selectedImage,
                            onImageSelected = { uri ->
                                selectedImage = uri
                                onSelectOption(if (uri != null) 1 else 0)
                            }
                        )
                    }



                    QuestionType.TEXT -> {

                        // Display picture if JSON contains "image"
                        if (question.image != null) {
                            Image(
                                painter = painterResource(
                                    id = context.resources.getIdentifier(
                                        question.image.substringBeforeLast("."), // allow "book.png"
                                        "drawable",
                                        context.packageName
                                    )
                                ),
                                contentDescription = question.title,
                                modifier = Modifier
                                    .padding(vertical = 12.dp)
                                    .size(180.dp)
                            )
                        }

                        OutlinedTextField(
                            value = textAnswer,
                            onValueChange = onTextChange,
                            label = { Text("Type your answer here...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 18.sp)
                        )
                    }


                    QuestionType.AUDIO -> {
                        AudioRecorderView(
                            questionId = question.id,
                            existingPath = audioPath,
                            onAudioRecorded = onAudioRecorded
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Tap to record your answer",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    else -> {
                        // If question type is unknown, show a fallback text field
                        OutlinedTextField(
                            value = textAnswer,
                            onValueChange = onTextChange,
                            label = { Text("Answer") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 18.sp)
                        )
                    }
                }
            }
        }

        // ----------------------------------------------
        // BOTTOM NEXT / FINISH BUTTON
        // ----------------------------------------------
        val isAnswered = when (qType) {
            QuestionType.SINGLE_CHOICE -> selectedOption != null
            QuestionType.TEXT -> textAnswer.isNotBlank()
            QuestionType.AUDIO -> audioPath != null
            QuestionType.ACTION_SEQUENCE -> actionScore != null
            QuestionType.IMAGE_MAP_SELECTION -> selectedOption != null
            QuestionType.IMAGE_UPLOAD -> selectedImage != null

        }

        BigButton(
            text = if (questionIndex == totalQuestions - 1) "FINISH" else "NEXT",
            onClick = onNext,
            enabled = isAnswered
        )
    }

}

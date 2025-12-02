package com.example.myapplication.ui.screens

import android.speech.tts.TextToSpeech
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
import com.example.myapplication.ui.components.BigOutlinedButton
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
    onSelectOption: (Int) -> Unit,
    onTextChange: (String) -> Unit,
    onAudioRecorded: (String) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

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

    // Announce Question when it changes
    LaunchedEffect(question, tts) {
        tts?.let {
            if (!it.isSpeaking) {
                val textToSpeak = "${question.title}. ${question.description}"
                it.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "QuestionId")
                
                // If it's an audio question, we could append instruction
                if (question.type == QuestionType.AUDIO) {
                     // delay slightly or just queue it
                     it.speak("Please record your answer after the beep.", TextToSpeech.QUEUE_ADD, null, "Prompt")
                }
            }
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // 1. Top Progress Bar & Counter
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

        // 2. Content Area (Centered)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            Text(
                text = question.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            if (question.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = question.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(48.dp)) // Space between text and input

            // Dynamic Content
            when (question.type) {
                QuestionType.SINGLE_CHOICE -> {
                    question.options.forEachIndexed { index, option ->
                        val isSelected = selectedOption == index
                        val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

                        OutlinedButton(
                            onClick = { onSelectOption(index) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .height(60.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = containerColor,
                                contentColor = contentColor
                            ),
                            border = androidx.compose.foundation.BorderStroke(2.dp, borderColor)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Text((index + 65).toChar().toString(), color = Color.White, fontWeight = FontWeight.Bold)
                                    } else {
                                        Text((index + 65).toChar().toString(), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(text = option, fontSize = 18.sp)
                            }
                        }
                    }
                }
                QuestionType.TEXT -> {
                    OutlinedTextField(
                        value = textAnswer,
                        onValueChange = onTextChange,
                        label = { Text("Type your answer here...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        shape = RoundedCornerShape(16.dp),
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
            }
        }

        // 3. Bottom Navigation
        Column(modifier = Modifier.padding(top = 16.dp)) {
            // Only show "Check" or "Next" button if an answer is provided
            val isAnswered = when (question.type) {
                QuestionType.SINGLE_CHOICE -> selectedOption != null
                QuestionType.TEXT -> textAnswer.isNotBlank()
                QuestionType.AUDIO -> audioPath != null
            }
            
            BigButton(
                text = if (questionIndex == totalQuestions - 1) "FINISH" else "NEXT",
                onClick = onNext,
                enabled = isAnswered
            )
        }
    }
}

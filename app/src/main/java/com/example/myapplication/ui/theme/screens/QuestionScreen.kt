package com.example.myapplication.ui.screens

import com.example.myapplication.ui.components.AudioRecorderView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.Question
import com.example.myapplication.data.model.QuestionType
import com.example.myapplication.data.model.UserInfo
// Note: You need to move AudioRecorderView to ui/components for this import to work,
// or keep it in the same file temporarily.

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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Question ${questionIndex + 1} / $totalQuestions",
                style = MaterialTheme.typography.labelLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = question.title, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = question.description, style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Dynamic Rendering
                    when (question.type) {
                        QuestionType.SINGLE_CHOICE -> {
                            question.options.forEachIndexed { index, option ->
                                Button(
                                    onClick = { onSelectOption(index) },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if(selectedOption == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if(selectedOption == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Text(option)
                                }
                            }
                        }
                        QuestionType.TEXT -> {
                            OutlinedTextField(
                                value = textAnswer,
                                onValueChange = onTextChange,
                                label = { Text("Your Answer") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        QuestionType.AUDIO -> {
                            // Make sure to add this import at the top of the file:
                            // import com.example.myapplication.ui.components.AudioRecorderView

                            AudioRecorderView(
                                questionId = question.id,
                                existingPath = audioPath,
                                onAudioRecorded = onAudioRecorded
                            )
                        }
                    }
                }
            }
        }

        // Navigation Buttons
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onPrev) { Text("Back") }
            Button(onClick = onNext) { Text("Next") }
        }
    }
}
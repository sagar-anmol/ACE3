package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.Question
import com.example.myapplication.data.model.QuestionType
import com.example.myapplication.data.model.UserInfo

@Composable
fun ResultScreen(
    userInfo: UserInfo,
    questions: List<Question>,
    selectedOptions: List<Int?>,
    textAnswers: List<String>,
    onRestart: () -> Unit
) {
    // Calculate Score (Only for Single Choice for now)
    var score = 0
    var totalScorable = 0

    questions.forEachIndexed { index, question ->
        if (question.type == QuestionType.SINGLE_CHOICE) {
            totalScorable++
            if (question.correctOptionIndex != null && selectedOptions[index] == question.correctOptionIndex) {
                score++
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Result for ${userInfo.name}", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Score: $score / $totalScorable", style = MaterialTheme.typography.displayMedium)
        Text("(Audio answers are analyzed separately)", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onRestart) { Text("Restart Test") }
    }
}
package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.Question
import com.example.myapplication.data.model.UserInfo
import com.example.myapplication.network.ApiRepository
import kotlinx.coroutines.launch

@Composable
fun ResultScreen(
    userInfo: UserInfo,
    questions: List<Question>,
    selectedOptions: List<Int?>,
    textAnswers: List<String>,
    onRestart: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val apiRepository = remember { ApiRepository() }
    var submissionStatus by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isUploading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Submitting results...")
        } else if (submissionStatus == null) {
            Text("You have completed the test!", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = {
                isUploading = true
                coroutineScope.launch {
                    val response = apiRepository.submitResults(
                        userInfo,
                        questions,
                        selectedOptions,
                        textAnswers
                    )
                    submissionStatus = "${response.message}\nScore: ${response.score}/100\nServer Status: ${response.statusCode}"
                    isUploading = false
                }
            }) { Text("Submit Your Results") }
        } else {
            Text(
                text = submissionStatus!!,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        if (!isUploading) {
            Button(onClick = onRestart) { Text("Back to Start") }
        }
    }
}

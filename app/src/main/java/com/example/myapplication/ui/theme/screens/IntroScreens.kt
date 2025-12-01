package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.UserInfo

@Composable
fun IntroScreen1(onNext: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ACE Dementia\nSelf-Test", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onNext) { Text("Start") }
        }
    }
}

@Composable
fun IntroScreen2(onBack: () -> Unit, onNext: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(modifier = Modifier.align(Alignment.Center)) {
            Text("Instructions", style = MaterialTheme.typography.headlineSmall)
            Text("• Find a quiet place.\n• Wear glasses if needed.\n• Do not ask for help.")
            Spacer(modifier = Modifier.height(24.dp))
            Row {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = onNext, modifier = Modifier.weight(1f)) { Text("Next") }
            }
        }
    }
}

@Composable
fun IntroScreen3(
    userInfo: UserInfo,
    onUserInfoChange: (UserInfo) -> Unit,
    onBack: () -> Unit,
    onStartTest: () -> Unit
) {
    val isValid = userInfo.name.isNotBlank() && userInfo.age.isNotBlank()
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(modifier = Modifier.align(Alignment.Center)) {
            Text("Your Details", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = userInfo.name,
                onValueChange = { onUserInfoChange(userInfo.copy(name = it)) },
                label = { Text("Name") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = userInfo.age,
                onValueChange = { onUserInfoChange(userInfo.copy(age = it)) },
                label = { Text("Age") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = onStartTest, enabled = isValid, modifier = Modifier.weight(1f)) { Text("Start Test") }
            }
        }
    }
}
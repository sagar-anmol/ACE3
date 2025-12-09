package com.example.myapplication.ui.screens

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.Question
import com.example.myapplication.data.model.UserInfo

// --- DATA MODELS ---

data class TestResult(
    val timestamp: Long,
    val totalScore: Int,
    val categoryScores: Map<String, Int>
)

// --- HELPER FUNCTIONS ---

private fun getCategoryScores(
    context: Context,
    questions: List<Question>,
    categoryMaxScores: Map<String, Int>
): Map<String, Int> {
    val prefs = context.getSharedPreferences("TestResults", Context.MODE_PRIVATE)
    val categoryScores = mutableMapOf<String, Int>()

    for (question in questions) {
        val category = question.category ?: continue
        val score = prefs.getInt("q_${question.id}", 0)
        categoryScores[category] = (categoryScores[category] ?: 0) + score
    }

    // Clamp the scores to their max values
    val finalScores = mutableMapOf<String, Int>()
    for ((category, score) in categoryScores) {
        finalScores[category] = score.coerceAtMost(categoryMaxScores[category] ?: 0)
    }

    return finalScores
}

private fun getTotalScore(categoryScores: Map<String, Int>): Int {
    return categoryScores.values.sum()
}

private fun saveTestResult(
    context: Context,
    totalScore: Int,
    categoryScores: Map<String, Int>
) {
    val prefs = context.getSharedPreferences("TestHistory", Context.MODE_PRIVATE)
    val editor = prefs.edit()
    val history = prefs.getStringSet("history", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

    val newResult = "${System.currentTimeMillis()};$totalScore;" + categoryScores.map { "${it.key}:${it.value}" }.joinToString(",")
    history.add(newResult)

    editor.putStringSet("history", history)
    editor.apply()
}

private fun getTestHistory(context: Context): List<TestResult> {
    val prefs = context.getSharedPreferences("TestHistory", Context.MODE_PRIVATE)
    val history = prefs.getStringSet("history", emptySet()) ?: emptySet()

    return history.mapNotNull { entry ->
        try {
            val parts = entry.split(";")
            val timestamp = parts[0].toLong()
            val totalScore = parts[1].toInt()
            val categoryScores = parts[2].split(",").associate {
                val catParts = it.split(":")
                catParts[0] to catParts[1].toInt()
            }
            TestResult(timestamp, totalScore, categoryScores)
        } catch (e: Exception) {
            null
        }
    }.sortedBy { it.timestamp }
}

// --- COMPOSABLES ---

@Composable
fun DetailedReportScreen(
    userInfo: UserInfo,
    questions: List<Question>,
    onRestart: () -> Unit
) {
    val context = LocalContext.current

    val categoryMaxScores = mapOf(
        "attention & orientation" to 18,
        "memory" to 26,
        "fluency" to 14,
        "language" to 26,
        "visuospatial skills" to 16
    )

    val categoryScores = remember {
        getCategoryScores(context, questions, categoryMaxScores)
    }

    val totalScore = remember(categoryScores) {
        getTotalScore(categoryScores)
    }

    LaunchedEffect(Unit) {
        saveTestResult(context, totalScore, categoryScores)
    }

    val testHistory = remember {
        getTestHistory(context)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Detailed Report", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            // --- Score Sections ---
            ScoreSection("Attention & Orientation", categoryScores["attention & orientation"] ?: 0, 18)
            ScoreSection("Memory", categoryScores["memory"] ?: 0, 26)
            ScoreSection("Fluency", categoryScores["fluency"] ?: 0, 14)
            ScoreSection("Language", categoryScores["language"] ?: 0, 26)
            ScoreSection("Visuospatial Skills", categoryScores["visuospatial skills"] ?: 0, 16)

            Spacer(modifier = Modifier.height(24.dp))

            // --- Total Score ---
            Text("Final Score: $totalScore / 100", style = MaterialTheme.typography.headlineSmall)

            Spacer(modifier = Modifier.height(32.dp))

            // --- Trend Graph ---
            Text("Your Trend", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            TrendGraph(testHistory)

            Spacer(modifier = Modifier.weight(1f))

            Button(onClick = onRestart) {
                Text("Restart Test")
            }
        }
    }
}

@Composable
fun ScoreSection(title: String, score: Int, maxScore: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 18.sp)
        Text(text = "$score / $maxScore", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
fun TrendGraph(history: List<TestResult>) {
    val paint = Paint().asFrameworkPaint().apply {
        isAntiAlias = true
        textSize = 30f
        color = android.graphics.Color.BLACK
    }

    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(200.dp)) {
        // Define Zones
        drawRect(color = Color(0xFFC8E6C9), topLeft = Offset(0f, 0f), size = size.copy(height = size.height * 0.12f))
        drawRect(color = Color(0xFFFFF9C4), topLeft = Offset(0f, size.height * 0.12f), size = size.copy(height = size.height * 0.06f))
        drawRect(color = Color(0xFFFFCDD2), topLeft = Offset(0f, size.height * 0.18f), size = size.copy(height = size.height * 0.82f))

        // Draw Labels
        drawContext.canvas.nativeCanvas.drawText("Healthy", 10f, size.height * 0.1f, paint)
        drawContext.canvas.nativeCanvas.drawText("Might Have Dementia", 10f, size.height * 0.16f, paint)
        drawContext.canvas.nativeCanvas.drawText("Dementia", 10f, size.height * 0.3f, paint)

        if (history.size > 1) {
            val maxScore = 100f
            for (i in 0 until history.size - 1) {
                val startX = (i.toFloat() / (history.size - 1)) * size.width
                val startY = (1 - history[i].totalScore / maxScore) * size.height
                val endX = ((i + 1).toFloat() / (history.size - 1)) * size.width
                val endY = (1 - history[i + 1].totalScore / maxScore) * size.height

                drawLine(
                    color = Color.Blue,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 5f
                )
            }
        }
    }
}

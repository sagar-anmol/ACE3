package com.sih2025.ddd.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.File
import java.io.IOException

// --- 1. Network Interface & Data Classes ---
interface EvaluationApiService {
    @Multipart
    @POST("/api/evaluate/drawing")
    suspend fun submitDrawingForEvaluation(
        @Part("questionId") questionId: RequestBody,
        @Part image: MultipartBody.Part
    ): Response<SubmissionResponse>

    @GET("/api/evaluation-result/{taskId}")
    suspend fun getEvaluationResult(@Path("taskId") taskId: String): Response<EvaluationResult>
}

data class SubmissionResponse(val taskId: String, val status: String)
data class EvaluationResult(val status: String, val score: String?)

// --- 2. The Main Composable: UI and Logic Hub ---

@Composable
fun QuizScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var questions by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    val pendingTasks = remember { mutableStateMapOf<String, Int>() }
    var feedbackMessage by remember { mutableStateOf("") }

    val maxScore = remember(questions) {
        questions.sumOf { it.optString("Score", "0").toIntOrNull() ?: 0 }
    }

    val apiService = remember {
        Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8080/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EvaluationApiService::class.java)
    }

    LaunchedEffect(key1 = true) {
        questions = loadQuestions(context)
        feedbackMessage = "Questions loaded. Total: ${questions.size}"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (questions.isNotEmpty() && currentQuestionIndex < questions.size) {
            val currentQuestion = questions[currentQuestionIndex]
            val questionId = currentQuestion.getInt("id")
            val questionTitle = currentQuestion.getString("title")
            val questionType = currentQuestion.getString("type")
            val userScore = currentQuestion.optString("userScore", "")

            Text(text = "Question ${questionId}: $questionTitle", fontSize = 20.sp)
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Type: $questionType")
            Spacer(modifier = Modifier.height(8.dp))

            val isPending = pendingTasks.containsValue(questionId)
            val currentScoreText = when {
                isPending -> "PENDING"
                userScore.isNotEmpty() -> userScore
                else -> "Unanswered"
            }

            Text(text = "Score: $currentScoreText", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(30.dp))

            Row {
                Button(onClick = {
                    val newScore = scoreLocally(currentQuestion, "Book")
                    updateQuestionScore(questions, currentQuestionIndex, newScore) { newQuestions ->
                        questions = newQuestions
                        val updatedTotalScore = calculateTotalScore(newQuestions)
                        feedbackMessage = "Current Score: $newScore, Total Score: $updatedTotalScore / $maxScore"
                    }
                }) {
                    Text("Score Locally (Correct)")
                }
                Spacer(modifier = Modifier.width(16.dp))

                Button(onClick = {
                    feedbackMessage = "Submitting Q:$questionId to server..."
                    val dummyFile = createDummyFile(context, "dummy_drawing_for_q$questionId.png")
                    submitAndPoll(
                        question = currentQuestion,
                        imageFile = dummyFile,
                        apiService = apiService,
                        pendingTasks = pendingTasks,
                        scope = coroutineScope,
                        onResult = { resultScore ->
                            updateQuestionScore(questions, currentQuestionIndex, resultScore) { newQuestions ->
                                questions = newQuestions
                                val updatedTotalScore = calculateTotalScore(newQuestions)
                                feedbackMessage = "Current Score: $resultScore, Total Score: $updatedTotalScore / $maxScore"
                            }
                        }
                    )
                }) {
                    Text("Score on Server")
                }
            }

            Spacer(modifier = Modifier.height(50.dp))

            Row {
                Button(
                    onClick = { if (currentQuestionIndex > 0) currentQuestionIndex-- },
                    enabled = currentQuestionIndex > 0
                ) {
                    Text("Previous")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = { if (currentQuestionIndex < questions.size - 1) currentQuestionIndex++ },
                    enabled = currentQuestionIndex < questions.size - 1
                ) {
                    Text("Next")
                }
            }

        } else {
            CircularProgressIndicator()
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(text = feedbackMessage)
    }
}

// --- 3. Helper Functions ---

private fun calculateTotalScore(questions: List<JSONObject>): Int {
    return questions.sumOf {
        it.optString("userScore", "0").toIntOrNull() ?: 0
    }
}

private fun loadQuestions(context: Context): List<JSONObject> {
    return try {
        val jsonString = context.assets.open("questions_en.json").bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(jsonString)
        List(jsonArray.length()) { i ->
            val question = jsonArray.getJSONObject(i)
            if (!question.has("userScore")) {
                question.put("userScore", "")
            }
            question
        }
    } catch (e: IOException) {
        e.printStackTrace()
        emptyList()
    }
}

private fun updateQuestionScore(
    currentList: List<JSONObject>,
    index: Int,
    newScore: String,
    onUpdate: (List<JSONObject>) -> Unit
) {
    val updatedList = currentList.toMutableList()
    val questionToUpdate = updatedList[index]
    questionToUpdate.put("userScore", newScore)
    onUpdate(updatedList)
}

private fun scoreLocally(question: JSONObject, userAnswer: String): String {
    val correctAnswers = question.getJSONArray("correctTextAnswers")
    if (correctAnswers.length() > 0) {
        val correctAnswer = correctAnswers.getString(0)
        if (userAnswer.equals(correctAnswer, ignoreCase = true)) {
            return question.optString("Score", "1")
        }
    }
    return "0"
}

private fun createDummyFile(context: Context, fileName: String): File {
    val file = File(context.cacheDir, fileName)
    file.createNewFile()
    file.writeText("This is a dummy file for testing uploads.")
    return file
}

private fun submitAndPoll(
    question: JSONObject,
    imageFile: File,
    apiService: EvaluationApiService,
    pendingTasks: MutableMap<String, Int>,
    scope: CoroutineScope,
    onResult: (String) -> Unit
) {
    val questionId = question.getInt("id")

    scope.launch(Dispatchers.IO) {
        try {
            val requestFile = imageFile.asRequestBody("image/png".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
            val idPart = RequestBody.create("text/plain".toMediaTypeOrNull(), questionId.toString())

            val submissionResponse = apiService.submitDrawingForEvaluation(idPart, imagePart)

            if (submissionResponse.isSuccessful && submissionResponse.body() != null) {
                val taskId = submissionResponse.body()!!.taskId
                withContext(Dispatchers.Main) {
                    pendingTasks[taskId] = questionId
                }

                while (pendingTasks.containsKey(taskId)) {
                    delay(10000)

                    val resultResponse = apiService.getEvaluationResult(taskId)

                    if (resultResponse.isSuccessful && resultResponse.body()?.status == "COMPLETE") {
                        val finalScore = resultResponse.body()?.score ?: "0"
                        withContext(Dispatchers.Main) {
                            onResult(finalScore)
                            pendingTasks.remove(taskId)
                        }
                        break
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

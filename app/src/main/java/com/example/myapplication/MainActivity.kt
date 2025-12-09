package com.example.myapplication

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.myapplication.data.QuestionRepository
import com.example.myapplication.data.model.Question
import com.example.myapplication.data.model.UserInfo
import com.example.myapplication.ui.screens.*
import com.example.myapplication.ui.theme.DementiaAppTheme
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.File

// --- 1. Network Interfaces & Data Classes ---

// For Drawing Evaluation
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

// For Audio Evaluation
interface AudioApiService {
    @Multipart
    @POST("api/pipeline")
    suspend fun submitAudioForEvaluation(
        @Part("type") type: RequestBody,
        @Part("extra_param") extraParam: RequestBody,
        @Part audioFile: MultipartBody.Part
    ): Response<AudioSubmissionResponse>

    @GET("api/status/{token_id}")
    suspend fun getAudioEvaluationResult(@Path("token_id") tokenId: String): Response<AudioEvaluationResult>
}

data class AudioSubmissionResponse(
    val success: Boolean,
    @SerializedName("token_id") val tokenId: Int,
    val status: String
)

data class AudioEvaluationResult(
    @SerializedName("token_id") val tokenId: String,
    val data: AudioResultData?
)

data class AudioResultData(
    val status: String,
    val result: String?,
    val timestamps: String? // JSON string for word timestamps
)


// --- 2. STORAGE & SCORE HELPER FUNCTIONS (Top Level) ---

fun saveScoreToPreferences(context: Context, questionId: Int, score: Int) {
    val prefs = context.getSharedPreferences("TestResults", Context.MODE_PRIVATE)
    val editor = prefs.edit()
    editor.putInt("q_$questionId", score)
    editor.apply()
}

fun calculateFinalScore(context: Context, questions: List<Question>): Int {
    val prefs = context.getSharedPreferences("TestResults", Context.MODE_PRIVATE)
    var total = 0
    for (question in questions) {
        total += prefs.getInt("q_${question.id}", 0)
    }
    return total
}

fun getQuestionMaxScore(question: Question): Int {
    // The "score" from the data model is the intended max score for each question.
    return question.score.coerceAtLeast(0)
}

fun calculateTotalMaxPossible(questions: List<Question>): Int {
    return questions.sumOf { getQuestionMaxScore(it) }
}

// --- 3. Main Activity ---
class MainActivity : ComponentActivity() {

    private val audioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)

        setContent {
            DementiaAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DementiaRemoteSelfTestApp()
                }
            }
        }
    }
}

enum class ScreenState { INTRO_1, INTRO_2, INTRO_3, TEST, RESULT, DETAILED_REPORT }

@Composable
fun DementiaRemoteSelfTestApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val repository = remember { QuestionRepository(context) }
    var currentLanguage by remember { mutableStateOf("en") }
    var questions by remember { mutableStateOf<List<Question>>(emptyList()) }

    // Retrofit Clients
    val drawingApiService = remember {
        Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8080/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EvaluationApiService::class.java)
    }
    val audioApiService = remember {
        Retrofit.Builder()
            .baseUrl("https://9aab173591931.notebooks.jarvislabs.net/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AudioApiService::class.java)
    }

    LaunchedEffect(currentLanguage) {
        questions = repository.getQuestions(currentLanguage)
    }

    var currentScreen by remember { mutableStateOf(ScreenState.INTRO_1) }
    var userInfo by remember { mutableStateOf(UserInfo(age = 0, education = 0)) }
    var currentIndex by remember { mutableIntStateOf(0) }

    val selectedOptions = remember { mutableStateListOf<Int?>() }
    val textAnswers = remember { mutableStateListOf<String>() }
    val audioPaths = remember { mutableStateListOf<String?>() }
    val actionScores = remember { mutableStateListOf<Int?>() }
    val serverScores = remember { mutableStateListOf<String?>() }

    // Separate pending task maps for clarity
    val pendingDrawingTasks = remember { mutableStateMapOf<Int, String>() }
    val pendingAudioTasks = remember { mutableStateMapOf<Int, String>() }


    fun resetTest() {
        val prefs = context.getSharedPreferences("TestResults", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        selectedOptions.clear()
        textAnswers.clear()
        audioPaths.clear()
        actionScores.clear()
        serverScores.clear()
        pendingDrawingTasks.clear()
        pendingAudioTasks.clear()

        questions.forEach { _ ->
            selectedOptions.add(null)
            textAnswers.add("")
            audioPaths.add(null)
            actionScores.add(null)
            serverScores.add(null)
        }
        currentIndex = 0
        currentScreen = ScreenState.INTRO_1
    }

    LaunchedEffect(questions) {
        if (questions.isNotEmpty()) resetTest()
    }

    fun showScoreToast(currentMarks: Int) {
        val totalObtained = calculateFinalScore(context, questions)
        val maxPossible = calculateTotalMaxPossible(questions)
        Toast.makeText(
            context,
            "Marks: $currentMarks | Total: $totalObtained / $maxPossible",
            Toast.LENGTH_LONG
        ).show()
    }

    fun submitDrawingAndPoll(question: Question, imageFile: File) {
        val questionId = question.id
        if (pendingDrawingTasks.containsKey(questionId)) return

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val requestFile = imageFile.asRequestBody("image/png".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
                val idPart = questionId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                val submissionResponse = drawingApiService.submitDrawingForEvaluation(idPart, imagePart)

                if (submissionResponse.isSuccessful && submissionResponse.body() != null) {
                    val taskId = submissionResponse.body()!!.taskId
                    withContext(Dispatchers.Main) { pendingDrawingTasks[questionId] = taskId }

                    while (pendingDrawingTasks.containsKey(questionId)) {
                        delay(15000)
                        val resultResponse = drawingApiService.getEvaluationResult(taskId)
                        if (resultResponse.isSuccessful && resultResponse.body()?.status == "COMPLETE") {
                            val finalScoreString = resultResponse.body()?.score ?: "0"
                            withContext(Dispatchers.Main) {
                                val index = questions.indexOfFirst { it.id == questionId }
                                if (index != -1) {
                                    serverScores[index] = finalScoreString
                                    val scoreInt = finalScoreString.toDoubleOrNull()?.toInt() ?: 0
                                    saveScoreToPreferences(context, questionId, scoreInt)
                                    showScoreToast(scoreInt)
                                }
                                pendingDrawingTasks.remove(questionId)
                            }
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { pendingDrawingTasks.remove(questionId) }
            }
        }
    }

    fun submitAudioAndPoll(question: Question, audioFile: File) {
        val questionId = question.id
        if (pendingAudioTasks.containsKey(questionId)) return

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val typePart = "audio".toRequestBody("text/plain".toMediaTypeOrNull())
                val extraParamPart = "test_v1".toRequestBody("text/plain".toMediaTypeOrNull())
                val requestFile = audioFile.asRequestBody("audio/mp3".toMediaTypeOrNull())
                val audioFilePart =
                    MultipartBody.Part.createFormData("audio_file", audioFile.name, requestFile)

                val subResponse =
                    audioApiService.submitAudioForEvaluation(typePart, extraParamPart, audioFilePart)

                if (subResponse.isSuccessful && subResponse.body()?.success == true) {
                    val taskId = subResponse.body()!!.tokenId.toString()
                    withContext(Dispatchers.Main) { pendingAudioTasks[questionId] = taskId }

                    while (pendingAudioTasks.containsKey(questionId)) {
                        delay(20000) // Poll every 20 seconds
                        val resultResponse = audioApiService.getAudioEvaluationResult(taskId)
                        val resultData = resultResponse.body()?.data
                        if (resultResponse.isSuccessful && resultData?.status == "completed") {
                            val transcribedText = resultData.result?.trim()?.lowercase()
                            val correctAnswers = question.correctTextAnswers ?: emptyList()
                            val maxScore = getQuestionMaxScore(question)

                            var score = 0
                            if (!transcribedText.isNullOrEmpty() && correctAnswers.isNotEmpty()) {
                                // If there is one answer and it contains spaces, treat it as a sentence
                                if (correctAnswers.size == 1 && correctAnswers.first().contains(" ")) {
                                    val correctAnswerSentence = correctAnswers.first().lowercase()
                                    if (transcribedText == correctAnswerSentence) {
                                        score = maxScore
                                    }
                                } else { // Otherwise, treat it as a list of words
                                    val transcribedWords = transcribedText.split(" ").map { it.trim() }
                                    val correctWords = correctAnswers.map { it.lowercase() }
                                    score = transcribedWords.count { it in correctWords }
                                }
                            }

                            val finalScore = minOf(score, maxScore)

                            withContext(Dispatchers.Main) {
                                val index = questions.indexOfFirst { it.id == questionId }
                                if (index != -1) {
                                    serverScores[index] = "$finalScore / $maxScore"
                                    saveScoreToPreferences(context, questionId, finalScore)
                                    showScoreToast(finalScore)
                                }
                                pendingAudioTasks.remove(questionId)
                            }
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { pendingAudioTasks.remove(questionId) }
            }
        }
    }


    if (questions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        when (currentScreen) {
            ScreenState.INTRO_1 -> IntroScreen1(onNext = { currentScreen = ScreenState.INTRO_2 })
            ScreenState.INTRO_2 -> IntroScreen2(
                onBack = { currentScreen = ScreenState.INTRO_1 },
                onNext = { currentScreen = ScreenState.INTRO_3 }
            )
            ScreenState.INTRO_3 -> IntroScreen3(
                userInfo = userInfo,
                onUserInfoChange = { userInfo = it },
                onBack = { currentScreen = ScreenState.INTRO_2 },
                onStartTest = {
                    currentIndex = 0
                    currentScreen = ScreenState.TEST
                }
            )

            ScreenState.TEST -> {
                val question = questions[currentIndex]

                QuestionScreen(
                    userInfo = userInfo,
                    question = question,
                    questionIndex = currentIndex,
                    totalQuestions = questions.size,
                    selectedOption = selectedOptions.getOrNull(currentIndex),
                    textAnswer = textAnswers.getOrNull(currentIndex) ?: "",
                    audioPath = audioPaths.getOrNull(currentIndex),
                    actionScore = actionScores.getOrNull(currentIndex),
                    serverScore = serverScores.getOrNull(currentIndex),
                    isPending = pendingDrawingTasks.containsKey(question.id) || pendingAudioTasks.containsKey(question.id),
                    onUploadImage = { imageFile -> submitDrawingAndPoll(question, imageFile) },
                    onSubmitAudio = { audioFile -> submitAudioAndPoll(question, audioFile) },

                    onSelectOption = { selectedIndex ->
                        selectedOptions[currentIndex] = selectedIndex
                        val maxPoints = getQuestionMaxScore(question)
                        val points = if (selectedIndex == question.correctOptionIndex) maxPoints else 0
                        saveScoreToPreferences(context, question.id, points)
                        showScoreToast(points)
                    },

                    onTextChange = { textAnswers[currentIndex] = it },
                    onAudioRecorded = { audioPaths[currentIndex] = it },

                    onActionSequenceCompleted = { score ->
                        actionScores[currentIndex] = score
                        saveScoreToPreferences(context, question.id, score)
                        showScoreToast(score)

                        if (currentIndex < questions.size - 1) {
                            currentIndex++
                        } else {
                            currentScreen = ScreenState.DETAILED_REPORT
                        }
                    },
                    onPrev = {
                        if (currentIndex > 0) currentIndex--
                        else currentScreen = ScreenState.INTRO_3
                    },
                    onNext = {
                        if (currentIndex < questions.size - 1) currentIndex++
                        else currentScreen = ScreenState.RESULT
                    }
                )
            }

            ScreenState.RESULT -> {
                ResultScreen(
                    userInfo = userInfo,
                    questions = questions,
                    selectedOptions = selectedOptions,
                    textAnswers = textAnswers,
                    actionScores = actionScores,
                    serverScores = serverScores,
                    onRestart = { resetTest() },
                    onViewDetailedReport = { currentScreen = ScreenState.DETAILED_REPORT }
                )
            }

            ScreenState.DETAILED_REPORT -> {
                DetailedReportScreen(
                    userInfo = userInfo,
                    questions = questions,
                    onRestart = { resetTest() }
                )
            }
        }
    }
}

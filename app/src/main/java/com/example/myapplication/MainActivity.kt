package com.example.myapplication

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.File

// --- 1. Network Interface & Data Classes (for server communication) ---
interface EvaluationApiService {
    @Multipart
    @POST("/api/evaluate/drawing") // Example endpoint for image upload
    suspend fun submitDrawingForEvaluation(
        @Part("questionId") questionId: RequestBody,
        @Part image: MultipartBody.Part
    ): Response<SubmissionResponse>

    @GET("/api/evaluation-result/{taskId}")
    suspend fun getEvaluationResult(@Path("taskId") taskId: String): Response<EvaluationResult>
}

data class SubmissionResponse(val taskId: String, val status: String)
data class EvaluationResult(val status: String, val score: String?)


class MainActivity : ComponentActivity() {

    private val audioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide Status Bar and Navigation Bar for immersive mode
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        // Request mic permission on start
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

enum class ScreenState { INTRO_1, INTRO_2, INTRO_3, TEST, RESULT }

@Composable
fun DementiaRemoteSelfTestApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 1. Setup Repository and State
    val repository = remember { QuestionRepository(context) }
    var currentLanguage by remember { mutableStateOf("en") } // Toggle this to 'hi' to test Hindi
    var questions by remember { mutableStateOf<List<Question>>(emptyList()) }

    // --- ADDED: Retrofit Client for network calls ---
    val apiService = remember {
        Retrofit.Builder()
            // IMPORTANT: REPLACE URL. 10.0.2.2 is for the Android emulator to connect to the host's localhost.
            .baseUrl("http://10.0.2.2:8080/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EvaluationApiService::class.java)
    }

    // 2. Load Questions from JSON when language changes
    LaunchedEffect(currentLanguage) {
        questions = repository.getQuestions(currentLanguage)
    }

    // 3. User & Navigation State
    var currentScreen by remember { mutableStateOf(ScreenState.INTRO_1) }
    var userInfo by remember { mutableStateOf(UserInfo(age = 0, education = 0)) }
    var currentIndex by remember { mutableIntStateOf(0) }

    // 4. Answer Storage
    val selectedOptions = remember { mutableStateListOf<Int?>() }
    val textAnswers = remember { mutableStateListOf<String>() }
    val audioPaths = remember { mutableStateListOf<String?>() }
    val actionScores = remember { mutableStateListOf<Int?>() }
    // --- ADDED: State for server-side scoring ---
    val serverScores = remember { mutableStateListOf<String?>() }
    val pendingTasks = remember { mutableStateMapOf<Int, String>() } // Map<QuestionID, TaskID>

    // Helper to reset all answers and states when starting a new test
    fun resetTest() {
        selectedOptions.clear()
        textAnswers.clear()
        audioPaths.clear()
        actionScores.clear()
        serverScores.clear() // ADDED
        pendingTasks.clear() // ADDED
        questions.forEach { _ ->
            selectedOptions.add(null)
            textAnswers.add("")
            audioPaths.add(null)
            actionScores.add(null)
            serverScores.add(null) // ADDED: Initialize server score list
        }
        currentIndex = 0
        currentScreen = ScreenState.INTRO_1
    }

    // Initialize or reset lists when the questions are loaded
    LaunchedEffect(questions) {
        if (questions.isNotEmpty()) resetTest()
    }

    // --- ADDED: The "submit and poll" logic as a self-contained function ---
    fun submitAndPoll(question: Question, imageFile: File) {
        val questionId = question.id
        // Prevent re-submission if already pending
        if (pendingTasks.containsKey(questionId)) return

        coroutineScope.launch(Dispatchers.IO) { // Use IO dispatcher for network calls
            try {
                // Prepare file for upload
                val requestFile = imageFile.asRequestBody("image/png".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
                val idPart = questionId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                val submissionResponse = apiService.submitDrawingForEvaluation(idPart, imagePart)

                if (submissionResponse.isSuccessful && submissionResponse.body() != null) {
                    val taskId = submissionResponse.body()!!.taskId
                    // Switch back to Main thread to safely update UI state
                    withContext(Dispatchers.Main) {
                        pendingTasks[questionId] = taskId
                    }

                    // Start polling in the background
                    while (pendingTasks.containsKey(questionId)) {
                        delay(15000) // Poll every 15 seconds (adjust for production)

                        val resultResponse = apiService.getEvaluationResult(taskId)
                        if (resultResponse.isSuccessful && resultResponse.body()?.status == "COMPLETE") {
                            val finalScore = resultResponse.body()?.score ?: "0"
                            withContext(Dispatchers.Main) {
                                // Find the index of the question to update its score
                                val index = questions.indexOfFirst { it.id == questionId }
                                if (index != -1) {
                                    serverScores[index] = finalScore
                                }
                                pendingTasks.remove(questionId) // Stop polling for this task
                            }
                            break // Exit the while loop
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle exceptions (e.g., network error, server down)
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    pendingTasks.remove(questionId) // Stop polling on error
                }
            }
        }
    }

    // 5. Navigation Logic
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
                    // --- MODIFIED/ADDED parameters for server scoring ---
                    serverScore = serverScores.getOrNull(currentIndex),
                    isPending = pendingTasks.containsKey(question.id),
                    onUploadImage = { imageFile ->
                        // This gets called from QuestionScreen when the user submits a drawing
                        submitAndPoll(question, imageFile)
                    },
                    // ---
                    onSelectOption = { selectedOptions[currentIndex] = it },
                    onTextChange = { textAnswers[currentIndex] = it },
                    onAudioRecorded = { audioPaths[currentIndex] = it },
                    onActionSequenceCompleted = { score ->
                        actionScores[currentIndex] = score
                        if (currentIndex < questions.size - 1) {
                            currentIndex++
                        } else {
                            currentScreen = ScreenState.RESULT
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

            ScreenState.RESULT -> ResultScreen(
                userInfo = userInfo,
                questions = questions,
                selectedOptions = selectedOptions,
                textAnswers = textAnswers,
                actionScores = actionScores,
                // --- ADDED parameter for showing final server scores ---
                serverScores = serverScores,
                onRestart = { resetTest() }
            )
        }
    }
}

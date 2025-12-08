package com.example.myapplication

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
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

    // 1. Setup Repository and State
    val repository = remember { QuestionRepository(context) }
    var currentLanguage by remember { mutableStateOf("en") } // Toggle this to 'hi' to test Hindi
    var questions by remember { mutableStateOf<List<Question>>(emptyList()) }

    // 2. Load Questions from JSON when language changes
    LaunchedEffect(currentLanguage) {
        questions = repository.getQuestions(currentLanguage)
    }

    // 3. User & Navigation State
    var currentScreen by remember { mutableStateOf(ScreenState.INTRO_1) }
    var userInfo by remember { mutableStateOf(UserInfo(age = 0, education = 0)) }
    var currentIndex by remember { mutableStateOf(0) }

    // 4. Answer Storage
    val selectedOptions = remember { mutableStateListOf<Int?>() }
    val textAnswers = remember { mutableStateListOf<String>() }
    val audioPaths = remember { mutableStateListOf<String?>() }
    val actionScores = remember { mutableStateListOf<Int?>() } // NEW for ACTION_SEQUENCE answers

    // Helper to reset answers when starting fresh
    fun resetTest() {
        selectedOptions.clear()
        textAnswers.clear()
        audioPaths.clear()
        actionScores.clear()
        questions.forEach { _ ->
            selectedOptions.add(null)
            textAnswers.add("")
            audioPaths.add(null)
            actionScores.add(null) // initialize action score
        }
        currentIndex = 0
        currentScreen = ScreenState.INTRO_1
    }

    // Initialize lists when questions are loaded
    LaunchedEffect(questions) {
        if (questions.isNotEmpty()) resetTest()
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
                    actionScore = actionScores.getOrNull(currentIndex), // NEW parameter
                    onSelectOption = { selectedOptions[currentIndex] = it },
                    onTextChange = { textAnswers[currentIndex] = it },
                    onAudioRecorded = { audioPaths[currentIndex] = it },
                    onActionSequenceCompleted = { score ->
                        // Save score
                        actionScores[currentIndex] = score

                        // Move to NEXT question
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
                onRestart = { resetTest() }
            )
        }
    }
}

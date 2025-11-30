package com.example.myapplication

import android.Manifest
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.DementiaAppTheme
import java.io.File

// ---------------------- MODELS ----------------------

enum class QuestionType {
    SINGLE_CHOICE,
    TEXT,
    AUDIO      // 🔹 NEW: audio answer type
}

data class Question(
    val id: Int,
    val title: String,
    val description: String,
    val type: QuestionType,
    val options: List<String> = emptyList(),
    val correctOptionIndex: Int? = null,
    val correctTextAnswers: List<String> = emptyList()
)

data class UserInfo(
    val name: String = "",
    val age: String = "",
    val education: String = ""
)

enum class Screen {
    INTRO_1,
    INTRO_2,
    INTRO_3,
    TEST,
    RESULT
}

// ---------------------- QUESTIONS (SELF TEST) ----------------------
//
// Q1 = AUDIO: day + date + month + season
// Q2 = Orientation to place (country)

val questions: List<Question> = listOf(
    Question(
        id = 1,
        title = "Current Day, Date & Season",
        description = "Kripya apni awaaz me batayein:\n\n" +
                "• Aaj ka din (Monday / Tuesday ...)\n" +
                "• Aaj ki tareekh (date)\n" +
                "• Kaunsa mahina hai\n" +
                "• Kaunsi season chal rahi hai (jaise: summer, monsoon, winter)\n\n" +
                "Neeche wale mic button ko dabakar recording shuru karein,\n" +
                "fir dobara dabakar recording band karein.",
        type = QuestionType.AUDIO
    ),
    Question(
        id = 2,
        title = "Orientation – Place",
        description = "Ab batayein, abhi aap kis desh me maujood hain?",
        type = QuestionType.SINGLE_CHOICE,
        options = listOf("India", "USA", "UK", "Other"),
        correctOptionIndex = 0
    ),
    Question(
        id = 3,
        title = "Calendar Knowledge",
        description = "Ek saal me kitne mahine hote hain?",
        type = QuestionType.SINGLE_CHOICE,
        options = listOf("10", "11", "12", "13"),
        correctOptionIndex = 2
    ),
    Question(
        id = 4,
        title = "Simple Calculation",
        description = "15 - 7 ka sahi jawab kya hai?",
        type = QuestionType.SINGLE_CHOICE,
        options = listOf("5", "6", "7", "8"),
        correctOptionIndex = 3
    ),
    Question(
        id = 5,
        title = "Attention",
        description = "10 + 5 ka sahi jawab kya hai?",
        type = QuestionType.SINGLE_CHOICE,
        options = listOf("12", "13", "14", "15"),
        correctOptionIndex = 3
    ),
    Question(
        id = 6,
        title = "Memory – Words Yaad rakhna",
        description = "In teen shabdon ko dhyaan se padhiye:\n\nAPPLE, TABLE, PENNY\n\n" +
                "Inhe yaad karne ki koshish kijiye. Jab tayyar ho jao, Next dabao.\n" +
                "(Agla sawaal inhi shabdon par hoga.)",
        type = QuestionType.SINGLE_CHOICE,
        options = listOf("Main ready hoon"),
        correctOptionIndex = 0
    ),
    Question(
        id = 7,
        title = "Memory – Recall",
        description = "Ab batayein, aapko pehle wale question me jo teen shabd the,\n" +
                "unme se jitne yaad hain unko likhiye.\n(Example: apple table penny)",
        type = QuestionType.TEXT,
        correctTextAnswers = listOf(
            "apple table penny",
            "apple, table, penny",
            "apple table",
            "apple penny",
            "table penny"
        )
    ),
    Question(
        id = 8,
        title = "Language",
        description = "Inme se kaunsa shabd ek janwar ka naam hai?",
        type = QuestionType.SINGLE_CHOICE,
        options = listOf("Table", "Dog", "House", "Rain"),
        correctOptionIndex = 1
    ),
    Question(
        id = 9,
        title = "Everyday Understanding",
        description = "Agar aapko baar-baar cheezein yaad rakhne me dikkat ho,\n" +
                "toh sabse pehle kis se baat karni chahiye?",
        type = QuestionType.SINGLE_CHOICE,
        options = listOf(
            "Kisi bhi random website se",
            "Doctor ya qualified health professional se",
            "Kisi unknown WhatsApp group se",
            "Bilkul kisi se baat nahi karni chahiye"
        ),
        correctOptionIndex = 1
    )
)

// ---------------------- MAIN ACTIVITY ----------------------

class MainActivity : ComponentActivity() {

    // optional: permission launcher (for Android 6+)
    private val audioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // yahan UI me state handle hoga, isliye kuch nahi kar rahe directly
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // just trigger permission once (you can move this logic to UI as well)
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

// ---------------------- ROOT APP ----------------------

@Composable
fun DementiaRemoteSelfTestApp() {
    var currentScreen by remember { mutableStateOf(Screen.INTRO_1) }
    var userInfo by remember { mutableStateOf(UserInfo()) }

    var currentIndex by remember { mutableStateOf(0) }

    // MCQ answers
    val selectedOptions = remember {
        mutableStateListOf<Int?>().apply {
            repeat(questions.size) { add(null) }
        }
    }

    // Text answers
    val textAnswers = remember {
        mutableStateListOf<String>().apply {
            repeat(questions.size) { add("") }
        }
    }

    // Audio answers (file path)
    val audioPaths = remember {
        mutableStateListOf<String?>().apply {
            repeat(questions.size) { add(null) }
        }
    }

    when (currentScreen) {
        Screen.INTRO_1 -> IntroScreen1(
            onNext = { currentScreen = Screen.INTRO_2 }
        )

        Screen.INTRO_2 -> IntroScreen2(
            onBack = { currentScreen = Screen.INTRO_1 },
            onNext = { currentScreen = Screen.INTRO_3 }
        )

        Screen.INTRO_3 -> IntroScreen3(
            userInfo = userInfo,
            onUserInfoChange = { userInfo = it },
            onBack = { currentScreen = Screen.INTRO_2 },
            onStartTest = {
                currentIndex = 0
                currentScreen = Screen.TEST
            }
        )

        Screen.TEST -> {
            val question = questions[currentIndex]
            val selected = selectedOptions[currentIndex]
            val textAns = textAnswers[currentIndex]
            val audioPath = audioPaths[currentIndex]

            QuestionScreen(
                userInfo = userInfo,
                question = question,
                questionIndex = currentIndex,
                totalQuestions = questions.size,
                selectedOption = selected,
                textAnswer = textAns,
                audioPath = audioPath,
                onSelectOption = { index ->
                    selectedOptions[currentIndex] = index
                },
                onTextChange = { value ->
                    textAnswers[currentIndex] = value
                },
                onAudioRecorded = { path ->
                    audioPaths[currentIndex] = path

                    // 🔹 Yahin se tum backend ko hit kar sakte ho:
                    // uploadAudioToBackend(path, question.id, userInfo)
                },
                onPrev = {
                    if (currentIndex == 0) {
                        currentScreen = Screen.INTRO_3
                    } else {
                        currentIndex -= 1
                    }
                },
                onNext = {
                    if (currentIndex < questions.size - 1) {
                        currentIndex += 1
                    } else {
                        currentScreen = Screen.RESULT
                    }
                }
            )
        }

        Screen.RESULT -> ResultScreen(
            userInfo = userInfo,
            selectedOptions = selectedOptions,
            textAnswers = textAnswers,
            onRestart = {
                userInfo = UserInfo()
                for (i in selectedOptions.indices) selectedOptions[i] = null
                for (i in textAnswers.indices) textAnswers[i] = ""
                for (i in audioPaths.indices) audioPaths[i] = null
                currentIndex = 0
                currentScreen = Screen.INTRO_1
            }
        )
    }
}

// ---------------------- INTRO SCREENS (same as before) ----------------------

@Composable
fun IntroScreen1(
    onNext: () -> Unit
) { /* same as previous version */
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ACE Dementia\nSelf-Test",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Ye app aapko apni yaad-dasht, dhyaan\n" +
                        "aur sochne ki kshamata ka ek\n" +
                        "rough idea deti hai.\n\n" +
                        "Aap apne ghar se shanti se\n" +
                        "ye test de sakte hain.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onNext,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Continue")
            }
        }
    }
}

@Composable
fun IntroScreen2(
    onBack: () -> Unit,
    onNext: () -> Unit
) { /* same body as earlier */
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Before you start",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "• Test ek shant jagah par lijiye.\n" +
                        "• Agar aapko chashma / hearing aid chahiye,\n  to pehen kar rakhiye.\n" +
                        "• Haar question dhyaan se padh kar answer dijiye.\n" +
                        "• Koi bhi question skip na karein.\n\n" +
                        "⚠️ IMPORTANT:\n" +
                        "Ye app sirf ek screening tool hai.\n" +
                        "Ye diagnose NAHI karta ki aapko dementia hai\n" +
                        "ya nahi. Agar aap ya aapke family ko\n" +
                        "koi concern ho, to doctor se zaroor milen.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back")
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = onNext,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Next")
                }
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
) { /* same as earlier */
    val isFormValid = userInfo.name.isNotBlank() &&
            userInfo.age.isNotBlank() &&
            userInfo.education.isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Your Details",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Ye details sirf aapke result ko samajhne\n" +
                            "ke liye hain. Ye kahin online send nahi hoti.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = userInfo.name,
                    onValueChange = { onUserInfoChange(userInfo.copy(name = it)) },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = userInfo.age,
                    onValueChange = { onUserInfoChange(userInfo.copy(age = it)) },
                    label = { Text("Age") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = userInfo.education,
                    onValueChange = { onUserInfoChange(userInfo.copy(education = it)) },
                    label = { Text("Education (e.g., 12th, Graduate)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = onStartTest,
                        enabled = isFormValid,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start Test")
                    }
                }
            }
        }
    }
}

// ---------------------- QUESTION SCREEN ----------------------

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
    val progress = (questionIndex + 1f) / totalQuestions.toFloat()

    val isAnswered = when (question.type) {
        QuestionType.SINGLE_CHOICE -> selectedOption != null
        QuestionType.TEXT -> textAnswer.isNotBlank()
        QuestionType.AUDIO -> audioPath != null    // audio recorded hai ya nahi
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {

        // Top header
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            Text(
                text = "Dementia Self-Test",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (userInfo.name.isNotBlank()) {
                Text(
                    text = "For: ${userInfo.name}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Question ${questionIndex + 1} of $totalQuestions",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        // Center card
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = question.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = question.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (question.type) {
                    QuestionType.SINGLE_CHOICE -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            question.options.forEachIndexed { index, optionText ->
                                OptionRow(
                                    text = optionText,
                                    selected = selectedOption == index,
                                    onClick = { onSelectOption(index) }
                                )
                            }
                        }
                    }

                    QuestionType.TEXT -> {
                        OutlinedTextField(
                            value = textAnswer,
                            onValueChange = onTextChange,
                            label = { Text("Type your answer here") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false
                        )
                    }

                    QuestionType.AUDIO -> {
                        AudioRecorderView(
                            questionId = question.id,
                            existingPath = audioPath,
                            onAudioRecorded = onAudioRecorded
                        )
                    }
                }
            }
        }

        // Bottom navigation buttons
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = onPrev,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Previous")
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = onNext,
                enabled = isAnswered,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (questionIndex == totalQuestions - 1) "Finish" else "Next")
            }
        }
    }
}

// ---------------------- AUDIO RECORDER UI + LOGIC ----------------------

@Composable
fun AudioRecorderView(
    questionId: Int,
    existingPath: String?,
    onAudioRecorded: (String) -> Unit
) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var recorder: MediaRecorder? by remember { mutableStateOf(null) }
    var localPath by remember { mutableStateOf(existingPath) }

    fun startRecording() {
        val outputDir: File = context.cacheDir
        val outputFile = File(outputDir, "q${questionId}_answer_${System.currentTimeMillis()}.m4a")

        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }

        recorder = mediaRecorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }

        localPath = outputFile.absolutePath
        isRecording = true
    }

    fun stopRecording() {
        try {
            recorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (_: Exception) {
        }
        recorder = null
        isRecording = false
        localPath?.let { onAudioRecorded(it) }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(
            onClick = {
                if (!isRecording) startRecording() else stopRecording()
            },
            shape = RoundedCornerShape(50)
        ) {
            Text(if (isRecording) "Stop Recording" else "Start Recording")
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (localPath != null) {
            Text(
                text = "Recording saved.\n(Backend isko analyse karega)",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = "Mic button dabakar apna answer record karein.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ---------------------- OPTION ROW ----------------------

@Composable
fun OptionRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surface,
        tonalElevation = if (selected) 2.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// ---------------------- RESULT SCREEN ----------------------

@Composable
fun ResultScreen(
    userInfo: UserInfo,
    selectedOptions: List<Int?>,
    textAnswers: List<String>,
    onRestart: () -> Unit
) {
    // AUDIO wale questions ko scoring me ignore kar rahe hain
    val scoredQuestions = questions.filter { it.type != QuestionType.AUDIO }
    var correctCount = 0

    for (i in questions.indices) {
        val q = questions[i]
        if (q.type == QuestionType.AUDIO) continue

        when (q.type) {
            QuestionType.SINGLE_CHOICE -> {
                val sel = selectedOptions[i]
                if (sel != null && sel == q.correctOptionIndex) {
                    correctCount++
                }
            }

            QuestionType.TEXT -> {
                val ans = textAnswers[i].trim()
                if (ans.isNotEmpty() && q.correctTextAnswers.any { correct ->
                        correct.equals(ans, ignoreCase = true)
                    }
                ) {
                    correctCount++
                }
            }

            else -> {}
        }
    }

    val totalScored = scoredQuestions.size
    val percentage: Int =
        if (totalScored == 0) 0 else (correctCount * 100) / totalScored

    val (headline, color, suggestion) = when {
        percentage >= 80 -> Triple(
            "Aapke answers me zyada dikkat nazar nahi aayi.",
            MaterialTheme.colorScheme.primary,
            "Phir bhi agar aapko yaad-dasht, confusion, ya roz ke kaamon me dikkat mehsoos ho rahi hai, " +
                    "to apne doctor ya qualified specialist se calmly baat karna faydemand hoga."
        )

        percentage >= 50 -> Triple(
            "Kuch questions me aapko dikkat hui.",
            MaterialTheme.colorScheme.tertiary,
            "Ye zaroori nahi ki aapko dementia ho, lekin itna zaroor hai ki aap apne family doctor " +
                    "ya neurologist se ek baar discuss karein. Jaldi consult karna safe rehta hai."
        )

        else -> Triple(
            "Kaafi questions me aapko dikkat hui.",
            MaterialTheme.colorScheme.error,
            "Please jaldi se apne family doctor, neurologist ya psychiatrist se consult karein. " +
                    "Early check-up se problems ko better samjha ja sakta hai."
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (userInfo.name.isNotBlank()) {
                Text(
                    text = "Result for ${userInfo.name}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(
                text = "Your Score (excluding audio)",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$correctCount / $totalScored",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$percentage %",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = headline,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = color,
                    fontWeight = FontWeight.SemiBold
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = suggestion,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "⚠️ IMPORTANT:\n" +
                        "Ye test sirf ek basic screening tool hai.\n" +
                        "Ye diagnose nahi karta ki aapko dementia hai ya nahi.\n" +
                        "Final decision hamesha doctor hi karega.\n\n" +
                        "Audio answers (jaise day/date/season) ka detailed\n" +
                        "analysis backend (server) par hoga.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRestart,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Restart / Take test again")
            }
        }
    }
}

// ---------------------- PREVIEW ----------------------

@Preview(showBackground = true)
@Composable
fun PreviewDementiaApp() {
    DementiaAppTheme {
        DementiaRemoteSelfTestApp()
    }
}

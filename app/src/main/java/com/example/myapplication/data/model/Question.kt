package com.example.myapplication.data.model

// Add ACTION_SEQUENCE here
enum class QuestionType {
    SINGLE_CHOICE, TEXT, AUDIO, ACTION_SEQUENCE,IMAGE_MAP_SELECTION,IMAGE_UPLOAD
}

// New model for action steps used by ACTION_SEQUENCE questions
data class ActionStep(
    val command: String = "",
    val requiredActions: List<String> = emptyList()
)

data class Question(
    val id: Int,
    val title: String,
    val description: String,
    val type: QuestionType,
    val score: Int = 10, // Default score for each question
    val category: String? = null, // Added this line
    val options: List<String> = emptyList(),
    val correctOptionIndex: Int? = null,
    val correctTextAnswers: List<String> = emptyList(),
    val correctRegion: String? = null,
    val image: String? = null,
    // Optional: steps used only for ACTION_SEQUENCE questions
    val steps: List<ActionStep>? = null
)

data class UserInfo(
    val name: String = "",
    val age: Int,
    val education: Int,
    val dob: String = ""
)

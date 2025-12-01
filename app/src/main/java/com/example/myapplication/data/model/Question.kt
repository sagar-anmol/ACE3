package com.example.myapplication.data.model

enum class QuestionType {
    SINGLE_CHOICE, TEXT, AUDIO
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
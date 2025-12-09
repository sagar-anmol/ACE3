package com.example.myapplication.network

import com.example.myapplication.data.model.Question
import com.example.myapplication.data.model.QuestionType
import com.example.myapplication.data.model.UserInfo
import kotlinx.coroutines.delay

// A data class to represent what we might get from the server.
data class ServerResponse(
    val score: Int,
    val message: String,
    val statusCode: Int = 200
)

class ApiRepository {

    // A suspend function to simulate a network call.
    suspend fun submitResults(
        userInfo: UserInfo,
        questions: List<Question>,
        selectedOptions: List<Int?>,
        textAnswers: List<String>,
        actionScores: List<Int?>
    ): ServerResponse {
        // Simulate a network delay of 2 seconds
        delay(2000)

        var totalScore = 0

        questions.forEachIndexed { index, question ->
            when (question.type) {
                QuestionType.SINGLE_CHOICE -> {
                    if (selectedOptions.getOrNull(index) == question.correctOptionIndex) {
                        totalScore += question.score
                    }
                }
                QuestionType.TEXT -> {
                    if (question.correctTextAnswers.any { it.equals(textAnswers.getOrNull(index), ignoreCase = true) }) {
                        totalScore += question.score
                    }
                }
                QuestionType.ACTION_SEQUENCE -> {
                    totalScore += actionScores.getOrNull(index) ?: 0
                }
                else -> {
                    // For other question types, you might have different logic
                }
            }
        }

        // Return a fake success response with the calculated score
        return ServerResponse(
            score = totalScore,
            message = "Success",
            statusCode = 200
        )
    }
}

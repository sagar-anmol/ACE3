package com.example.myapplication.network

import com.example.myapplication.data.model.Question
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
        textAnswers: List<String>
    ): ServerResponse {
        // Simulate a network delay of 2 seconds
        delay(2000)

        // Return a fake success response with full marks (assuming 100 is full)
        return ServerResponse(
            score = 100,
            message = "Success",
            statusCode = 200
        )
    }
}

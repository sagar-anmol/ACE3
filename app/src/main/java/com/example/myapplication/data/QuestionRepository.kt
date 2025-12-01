package com.example.myapplication.data

import android.content.Context
import com.example.myapplication.data.model.Question
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException

class QuestionRepository(private val context: Context) {

    fun getQuestions(languageCode: String): List<Question> {
        val fileName = if (languageCode == "hi") "questions_hi.json" else "questions_en.json"
        val jsonString = getJsonDataFromAsset(context, fileName) ?: return emptyList()

        val listType = object : TypeToken<List<Question>>() {}.type
        return Gson().fromJson(jsonString, listType)
    }

    private fun getJsonDataFromAsset(context: Context, fileName: String): String? {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            null
        }
    }
}
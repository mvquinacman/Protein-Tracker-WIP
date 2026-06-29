package com.example.network

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import org.json.JSONObject

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun analyzeFoodImage(bitmap: Bitmap): FoodAnalysisResult {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("Gemini API key is not configured. Please add it to your Secrets in AI Studio.")
        }

        val prompt = """
            You are an expert nutritionist. Analyze the food in this image and estimate its nutritional content.
            Output EXACTLY a JSON object with no markdown formatting outside of the JSON block matching this structure:
            {
              "foodName": "Name of the food item",
              "proteinGrams": 24.5,
              "calories": 320.0,
              "carbsGrams": 15.0,
              "fatsGrams": 8.0
            }
            Provide your best scientific estimate. Return ONLY the raw JSON block.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData(mimeType = "image/jpeg", data = bitmap.toBase64()))
                    )
                )
            )
        )

        val response = apiService.generateContent(apiKey, request)
        val textResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("Empty response from Gemini API")

        return parseResult(textResponse)
    }

    private fun parseResult(text: String): FoodAnalysisResult {
        // Find JSON block
        val jsonStart = text.indexOf("{")
        val jsonEnd = text.lastIndexOf("}")
        if (jsonStart == -1 || jsonEnd == -1) {
            throw IllegalArgumentException("Could not parse nutritional data from model response")
        }
        val jsonString = text.substring(jsonStart, jsonEnd + 1)
        val jsonObject = JSONObject(jsonString)

        return FoodAnalysisResult(
            foodName = jsonObject.optString("foodName", "Unknown Food"),
            proteinGrams = jsonObject.optDouble("proteinGrams", 0.0).toFloat(),
            calories = jsonObject.optDouble("calories", 0.0).toFloat(),
            carbsGrams = jsonObject.optDouble("carbsGrams", 0.0).toFloat(),
            fatsGrams = jsonObject.optDouble("fatsGrams", 0.0).toFloat()
        )
    }
}

data class FoodAnalysisResult(
    val foodName: String,
    val proteinGrams: Float,
    val calories: Float,
    val carbsGrams: Float,
    val fatsGrams: Float
)

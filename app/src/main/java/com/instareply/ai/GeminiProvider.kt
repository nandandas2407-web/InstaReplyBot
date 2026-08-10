package com.instareply.ai

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.instareply.data.model.AiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GeminiProvider(private val client: OkHttpClient = DEFAULT_CLIENT) : AiProvider {

    companion object {
        private val DEFAULT_CLIENT = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        private val gson = Gson()
    }

    override suspend fun generateReply(
        message: String,
        senderName: String,
        config: AiConfig
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val model = config.model.ifEmpty { "gemini-2.0-flash" }
            val url = "${config.provider.baseUrl}/models/${model}:generateContent?key=${config.apiKey}"

            val systemPrompt = buildSystemPrompt(senderName, config)
            val requestBody = mapOf(
                "contents" to listOf(
                    mapOf(
                        "parts" to listOf(
                            mapOf("text" to "$systemPrompt\n\nMessage from $senderName: $message")
                        )
                    )
                ),
                "generationConfig" to mapOf(
                    "temperature" to 0.7,
                    "maxOutputTokens" to 200
                )
            )

            val json = gson.toJson(requestBody)
            val body = json.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")

            if (!response.isSuccessful) {
                throw Exception("API error ${response.code}: $responseBody")
            }

            val jsonResponse = JsonParser.parseString(responseBody).asJsonObject
            val candidates = jsonResponse.getAsJsonArray("candidates")
            val text = candidates[0].asJsonObject
                .getAsJsonObject("content")
                .getAsJsonArray("parts")[0]
                .asJsonObject
                .get("text").asString

            Result.success(text.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildSystemPrompt(senderName: String, config: AiConfig): String {
        return buildString {
            append("You are a helpful assistant replying to Instagram messages on behalf of ")
            append(config.userName.ifEmpty { "the user" })
            append(". ")
            if (config.userLocation.isNotEmpty()) {
                append("They live in ${config.userLocation}. ")
            }
            if (config.userBio.isNotEmpty()) {
                append("About them: ${config.userBio}. ")
            }
            append("Keep replies natural, friendly, and concise (under 150 characters). ")
            append("Don't use hashtags or emojis excessively. ")
            if (config.systemPrompt.isNotEmpty()) {
                append(config.systemPrompt)
            }
        }
    }
}

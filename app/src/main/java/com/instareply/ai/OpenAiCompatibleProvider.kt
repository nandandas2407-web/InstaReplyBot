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

class OpenAiCompatibleProvider : AiProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    override suspend fun generateReply(
        message: String,
        senderName: String,
        config: AiConfig
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = if (config.customBaseUrl.isNotEmpty()) {
                config.customBaseUrl
            } else {
                config.provider.baseUrl
            }

            val model = config.model.ifEmpty {
                when (config.provider) {
                    AiProvider.OPENROUTER -> "openai/gpt-3.5-turbo"
                    AiProvider.NVIDIA_NIM -> "meta/llama-3.1-8b-instruct"
                    AiProvider.OPENAI -> "gpt-3.5-turbo"
                    AiProvider.OPENCODE -> "gpt-3.5-turbo"
                    else -> "gpt-3.5-turbo"
                }
            }

            val systemPrompt = buildSystemPrompt(senderName, config)
            val requestBody = mapOf(
                "model" to model,
                "messages" to listOf(
                    mapOf("role" to "system", "content" to systemPrompt),
                    mapOf("role" to "user", "content" to "Message from $senderName: $message")
                ),
                "max_tokens" to 200,
                "temperature" to 0.7
            )

            val json = gson.toJson(requestBody)
            val body = json.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$baseUrl/chat/completions")
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .addHeader("Content-Type", "application/json")
                .apply {
                    if (config.provider == AiProvider.OPENROUTER) {
                        addHeader("HTTP-Referer", "https://instareply.app")
                        addHeader("X-Title", "InstaReply Bot")
                    }
                }
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")

            if (!response.isSuccessful) {
                throw Exception("API error ${response.code}: $responseBody")
            }

            val jsonResponse = JsonParser.parseString(responseBody).asJsonObject
            val choices = jsonResponse.getAsJsonArray("choices")
            val text = choices[0].asJsonObject
                .getAsJsonObject("message")
                .get("content").asString

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

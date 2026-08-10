package com.instareply.ai

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.instareply.data.model.AiConfig
import com.instareply.data.model.AiProvider as AiProviderEnum
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
                    AiProviderEnum.OPENROUTER -> "nvidia/nemotron-3-super-120b-a12b:free"
                    AiProviderEnum.NVIDIA_NIM -> "nvidia/llama-3.3-nemotron-super-49b-v1.5"
                    AiProviderEnum.OPENAI -> "gpt-5.4-mini"
                    AiProviderEnum.OPENCODE -> "gpt-5.6-luna"
                    AiProviderEnum.GROQ -> "llama-3.3-70b-versatile"
                    else -> "gpt-5.4-mini"
                }
            }
            if (model == "openrouter/free") {
                throw Exception("openrouter/free requires a paid OpenRouter plan; pick a specific :free model")
            }

            val systemPrompt = buildSystemPrompt(senderName, config)
            val requestBody = mapOf(
                "model" to model,
                "messages" to listOf(
                    mapOf("role" to "system", "content" to systemPrompt),
                    mapOf("role" to "user", "content" to "Message from $senderName: $message")
                ),
                "max_tokens" to config.maxTokens,
                "temperature" to 0.7
            )

            val json = gson.toJson(requestBody)
            val body = json.toRequestBody("application/json".toMediaType())

            val requestBuilder = Request.Builder()
                .url("$baseUrl/chat/completions")
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .addHeader("Content-Type", "application/json")
                .apply {
                    if (config.provider == AiProviderEnum.OPENROUTER) {
                        addHeader("HTTP-Referer", "https://instareply.app")
                        addHeader("X-Title", "InstaReply Bot")
                    }
                }
                .post(body)

            var response = client.newCall(requestBuilder.build()).execute()
            var usingResponsesApi = false
            if (!response.isSuccessful && (response.code == 400 || response.code == 404)) {
                // Newer models (GPT-5.x, Grok 4.5,...) reject /chat/completions.
                // Fall back to the OpenAI-compatible Responses API.
                response.close()
                val responsesBody = gson.toJson(
                    mapOf(
                        "model" to model,
                        "instructions" to systemPrompt,
                        "input" to "Message from $senderName: $message"
                    )
                ).toRequestBody("application/json".toMediaType())
                val responsesRequest = Request.Builder()
                    .url("${baseUrl.trimEnd('/')}/responses")
                    .addHeader("Authorization", "Bearer ${config.apiKey}")
                    .addHeader("Content-Type", "application/json")
                    .post(responsesBody)
                    .build()
                response = client.newCall(responsesRequest).execute()
                usingResponsesApi = true
            }

            val responseBody = response.body?.string() ?: throw Exception("Empty response")

            if (!response.isSuccessful) {
                throw Exception("API error ${response.code}: $responseBody")
            }

            val text = if (usingResponsesApi) {
                parseResponsesText(responseBody)
            } else {
                val jsonResponse = JsonParser.parseString(responseBody).asJsonObject
                val choices = jsonResponse.getAsJsonArray("choices")
                choices[0].asJsonObject
                    .getAsJsonObject("message")
                    .get("content").asString
            }

            Result.success(text.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseResponsesText(body: String): String {
        val obj = JsonParser.parseString(body).asJsonObject
        obj.get("output_text")?.let {
            if (!it.isJsonNull && it.isJsonPrimitive && it.asString.isNotEmpty()) return it.asString
        }
        val output = obj.getAsJsonArray("output")
        for (entry in output) {
            val content = entry.asJsonObject.getAsJsonArray("content") ?: continue
            for (part in content) {
                val text = part.asJsonObject.get("text") ?: continue
                if (!text.isJsonNull && text.isJsonPrimitive && text.asString.isNotEmpty()) return text.asString
            }
        }
        throw Exception("No text found in response: $body")
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
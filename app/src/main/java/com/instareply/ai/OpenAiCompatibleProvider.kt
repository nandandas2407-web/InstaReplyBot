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
import kotlin.math.max

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
                    AiProviderEnum.OPENCODE -> "opencode/deepseek-v4-flash-free"
                    AiProviderEnum.GROQ -> "llama-3.3-70b-versatile"
                    else -> "gpt-5.4-mini"
                }
            }
            if (model == "openrouter/free") {
                throw Exception("openrouter/free requires a paid OpenRouter plan; pick a specific :free model")
            }

            val systemPrompt = buildSystemPrompt(senderName, config)
            val userInput = "Message from $senderName: $message"
            // Reasoning models (gpt-oss etc) burn tokens on chain-of-thought BEFORE the
            // answer; with tiny max_tokens they return content=null or cut off mid-sentence.
            // Floor the request at 1024 so the answer always has room.
            val requestMaxTokens = max(config.maxTokens, 1024)
            val requestBuilder = buildChatRequest(model, systemPrompt, userInput, requestMaxTokens, baseUrl, config)

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
                        "input" to userInput
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
                throw Exception("API error ${response.code}: ${responseBody.take(400)}")
            }

            val text = if (usingResponsesApi) {
                parseResponsesText(responseBody)
            } else {
                var jsonResponse = JsonParser.parseString(responseBody).asJsonObject
                checkApiError(jsonResponse)

                var contentValue = extractChatContent(jsonResponse)

                if (contentValue == null || contentValue.isBlank() || wasCutOff(jsonResponse)) {
                    // Reasoning model burned the budget: retry once with a large cap.
                    Log.w(TAG, "Empty/truncated content from $model, retrying with max_tokens=4096")
                    response.close()
                    val retry = client.newCall(
                        buildChatRequest(model, systemPrompt, userInput, 4096, baseUrl, config).build()
                    ).execute()
                    val retryBody = retry.body?.string() ?: throw Exception("Empty response on retry")
                    if (!retry.isSuccessful) {
                        throw Exception("API error on retry ${retry.code}: ${retryBody.take(400)}")
                    }
                    jsonResponse = JsonParser.parseString(retryBody).asJsonObject
                    checkApiError(jsonResponse)
                    contentValue = extractChatContent(jsonResponse)
                        ?: throw Exception("Model returned empty content again: ${retryBody.take(300)}")
                }
                if (contentValue.isBlank()) {
                    throw Exception("Model returned empty content: ${jsonResponse.toString().take(300)}")
                }
                contentValue
            }

            Result.success(text.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildChatRequest(
        model: String,
        systemPrompt: String,
        userInput: String,
        maxTokens: Int,
        baseUrl: String,
        config: AiConfig
    ): Request.Builder {
        val requestBody = mapOf(
            "model" to model,
            "messages" to listOf(
                mapOf("role" to "system", "content" to systemPrompt),
                mapOf("role" to "user", "content" to userInput)
            ),
            "max_tokens" to maxTokens,
            "temperature" to 0.7
        )
        val body = gson.toJson(requestBody).toRequestBody("application/json".toMediaType())
        return Request.Builder()
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
    }

    /** Scans every choice for the first non-empty content (reasoning models may return it late). */
    private fun extractChatContent(jsonResponse: com.google.gson.JsonObject): String? {
        val choices = jsonResponse.getAsJsonArray("choices") ?: return null
        for (choice in choices) {
            val message = choice.asJsonObject.getAsJsonObject("message") ?: continue
            val content = message.get("content")
            val value = if (content != null && !content.isJsonNull) content.asString else null
            if (!value.isNullOrBlank()) return value
            // Some providers return the answer in reasoning_content instead
            val reasoning = message.get("reasoning_content")
            val rValue = if (reasoning != null && !reasoning.isJsonNull) reasoning.asString else null
            if (!rValue.isNullOrBlank() && value == null) return rValue
        }
        return null
    }

    /** True when the completion hit the token cap mid-sentence. */
    private fun wasCutOff(jsonResponse: com.google.gson.JsonObject): Boolean {
        val choices = jsonResponse.getAsJsonArray("choices") ?: return false
        for (choice in choices) {
            val reason = choice.asJsonObject.get("finish_reason")
            if (reason != null && !reason.isJsonNull && reason.asString == "length") return true
        }
        return false
    }

    private fun checkApiError(jsonResponse: com.google.gson.JsonObject) {
        jsonResponse.get("error")?.let { err ->
            if (!err.isJsonNull) {
                val msg = runCatching { err.asJsonObject?.get("message")?.asString }
                    .getOrNull() ?: err.toString()
                throw Exception("API error: $msg")
            }
        }
    }

    private fun parseResponsesText(body: String): String {
        val obj = JsonParser.parseString(body).asJsonObject
        obj.get("error")?.let { err ->
            if (!err.isJsonNull) {
                val msg = runCatching { err.asJsonObject?.get("message")?.asString }
                    .getOrNull() ?: err.toString()
                throw Exception("API error: $msg")
            }
        }
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
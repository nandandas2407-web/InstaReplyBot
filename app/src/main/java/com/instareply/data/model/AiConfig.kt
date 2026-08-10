package com.instareply.data.model

enum class AiProvider(val displayName: String, val baseUrl: String) {
    GEMINI("Google Gemini", "https://generativelanguage.googleapis.com/v1beta"),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1"),
    NVIDIA_NIM("NVIDIA NIM", "https://integrate.api.nvidia.com/v1"),
    OPENAI("OpenAI", "https://api.openai.com/v1"),
    OPENCODE("OpenCode", "https://opencode.ai/zen/v1"),
    GROQ("Groq", "https://api.groq.com/openai/v1"),
    CUSTOM("Custom API", "")
}

data class AiConfig(
    val provider: AiProvider,
    val apiKey: String,
    val model: String = "",
    val customBaseUrl: String = "",
    val userName: String = "",       // User's name for context
    val userLocation: String = "",   // User's location for context
    val userBio: String = "",        // Additional context
    val systemPrompt: String = "",   // Custom system prompt
    val maxTokens: Int = 200         // Max output tokens
)

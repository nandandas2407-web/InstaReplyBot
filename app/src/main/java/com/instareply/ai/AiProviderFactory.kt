package com.instareply.ai

import com.instareply.data.model.AiConfig
import com.instareply.data.model.AiProvider as AiProviderEnum

class AiProviderFactory {
    companion object {
        fun getProvider(provider: AiProviderEnum): AiProvider {
            return when (provider) {
                AiProviderEnum.GEMINI -> GeminiProvider()
                AiProviderEnum.OPENROUTER,
                AiProviderEnum.NVIDIA_NIM,
                AiProviderEnum.OPENAI,
                AiProviderEnum.OPENCODE,
                AiProviderEnum.GROQ -> OpenAiCompatibleProvider()
                AiProviderEnum.CUSTOM -> OpenAiCompatibleProvider()
            }
        }

        fun fromStorageName(name: String): AiProviderEnum {
            return when (name.lowercase()) {
                "gemini" -> AiProviderEnum.GEMINI
                "openrouter" -> AiProviderEnum.OPENROUTER
                "nvidia", "nvidia_nim" -> AiProviderEnum.NVIDIA_NIM
                "openai" -> AiProviderEnum.OPENAI
                "opencode" -> AiProviderEnum.OPENCODE
                "groq" -> AiProviderEnum.GROQ
                "custom" -> AiProviderEnum.CUSTOM
                else -> runCatching { AiProviderEnum.valueOf(name.uppercase()) }
                    .getOrDefault(AiProviderEnum.GEMINI)
            }
        }
    }
}

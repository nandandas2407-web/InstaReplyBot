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
                AiProviderEnum.OPENCODE -> OpenAiCompatibleProvider()
                AiProviderEnum.CUSTOM -> OpenAiCompatibleProvider()
            }
        }
    }
}

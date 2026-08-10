package com.instareply.ai

import com.instareply.data.model.AiConfig

interface AiProvider {
    suspend fun generateReply(
        message: String,
        senderName: String,
        config: AiConfig
    ): Result<String>
}

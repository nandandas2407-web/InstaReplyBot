package com.instareply.service

import android.content.Context
import android.content.Intent
import android.util.Log
import com.instareply.ai.AiProviderFactory
import com.instareply.data.db.AppDatabase
import com.instareply.data.model.*
import com.instareply.util.PrefsManager
import kotlinx.coroutines.delay

class ReplyEngine(private val context: Context) {

    companion object {
        private const val TAG = "ReplyEngine"
        private const val ACTION_REPLY = "com.instareply.ACTION_REPLY"
        private const val EXTRA_MESSAGE = "com.instareply.EXTRA_MESSAGE"
        private const val EXTRA_RECIPIENT = "com.instareply.EXTRA_RECIPIENT"
    }

    suspend fun processReply(
        senderName: String,
        message: String,
        rule: Rule,
        db: AppDatabase
    ) {
        try {
            val prefs = PrefsManager(context)

            // Get AI config from preferences
            val config = AiConfig(
                provider = AiProvider.valueOf(rule.aiProvider.uppercase()),
                apiKey = prefs.getApiKey(rule.aiProvider) ?: "",
                model = prefs.getModel(rule.aiProvider),
                userName = prefs.getUserName(),
                userLocation = prefs.getUserLocation(),
                userBio = prefs.getUserBio(),
                systemPrompt = prefs.getSystemPrompt()
            )

            if (config.apiKey.isEmpty()) {
                Log.w(TAG, "No API key configured for ${rule.aiProvider}")
                return
            }

            // Generate reply using AI
            val provider = AiProviderFactory.getProvider(config.provider)
            val result = provider.generateReply(message, senderName, config)

            if (result.isSuccess) {
                val replyText = result.getOrNull() ?: return
                Log.d(TAG, "Generated reply for $senderName: $replyText")

                // Send reply via accessibility service
                sendReplyViaAccessibility(senderName, replyText)

                // Log the reply
                db.replyLogDao().insertLog(
                    ReplyLog(
                        contactName = senderName,
                        receivedMessage = message,
                        replyMessage = replyText,
                        ruleId = rule.id,
                        aiProvider = rule.aiProvider,
                        success = true
                    )
                )

                // Update contact reply count
                db.contactDao().incrementReply(senderName, System.currentTimeMillis())
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                Log.e(TAG, "AI generation failed: $error")

                db.replyLogDao().insertLog(
                    ReplyLog(
                        contactName = senderName,
                        receivedMessage = message,
                        replyMessage = "",
                        ruleId = rule.id,
                        aiProvider = rule.aiProvider,
                        success = false,
                        errorMessage = error
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing reply", e)
        }
    }

    private fun sendReplyViaAccessibility(recipient: String, message: String) {
        try {
            // Use accessibility service to type and send
            // This requires the accessibility service to be enabled
            val intent = Intent(ACTION_REPLY).apply {
                putExtra(EXTRA_RECIPIENT, recipient)
                putExtra(EXTRA_MESSAGE, message)
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)

            // Alternative: Use clipboard + accessibility action
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("reply", message)
            clipboard.setPrimaryClip(clip)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send reply", e)
        }
    }
}

package com.instareply.service

import android.app.PendingIntent
import android.content.Context
import android.util.Log
import com.instareply.ai.AiProviderFactory
import com.instareply.data.db.AppDatabase
import com.instareply.data.model.*
import com.instareply.util.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class ReplyEngine(private val context: Context) {

    companion object {
        private const val TAG = "ReplyEngine"
    }

    suspend fun processReply(
        senderName: String,
        message: String,
        rule: Rule,
        db: AppDatabase,
        contentIntent: PendingIntent? = null
    ) {
        try {
            val prefs = PrefsManager(context)

            // Get AI config from preferences
            val config = AiConfig(
                provider = AiProviderFactory.fromStorageName(rule.aiProvider),
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

                // Send reply via accessibility service (opens the exact chat thread first)
                val sent = sendReplyViaAccessibility(senderName, replyText, contentIntent, rule.delayMs)

                db.replyLogDao().insertLog(
                    ReplyLog(
                        contactName = senderName,
                        receivedMessage = message,
                        replyMessage = replyText,
                        ruleId = rule.id,
                        aiProvider = rule.aiProvider,
                        success = sent,
                        errorMessage = if (sent) null else "Send failed: accessibility service unavailable or chat could not be opened"
                    )
                )

                if (sent) {
                    // Update contact reply count
                    db.contactDao().incrementReply(senderName, System.currentTimeMillis())
                }
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

    private suspend fun sendReplyViaAccessibility(
        recipient: String,
        message: String,
        contentIntent: PendingIntent?,
        ruleDelayMs: Long
    ): Boolean {
        val accessibilityService = InstaAccessibilityService.instance
        if (accessibilityService == null) {
            Log.e(TAG, "Accessibility service not connected, cannot send reply")
            return false
        }
        return try {
            // 1. Fire the notification's content intent: opens the exact DM conversation
            var openedChat = fireContentIntent(contentIntent)
            // 2. Wait for Instagram UI to settle
            delay(2600)
            // 3. Fallback: launch Instagram directly if the intent failed
            if (!openedChat) {
                openedChat = accessibilityService.openInstagramChat(recipient)
                if (openedChat) delay(2000)
            }
            if (openedChat && ruleDelayMs > 0) delay(ruleDelayMs)
            // 4. Type and send the message on the main thread
            if (openedChat) {
                withContext(Dispatchers.Main) {
                    accessibilityService.typeAndSendMessage(message)
                }
            } else {
                Log.e(TAG, "Could not open Instagram chat for $recipient")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send reply", e)
            false
        }
    }

    private fun fireContentIntent(intent: PendingIntent?): Boolean {
        if (intent == null) return false
        return try {
            intent.send()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Content intent failed, falling back to app launch", e)
            false
        }
    }
}
package com.instareply.service

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
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
        notification: Notification? = null,
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
                systemPrompt = prefs.getSystemPrompt(),
                maxTokens = prefs.getMaxTokens()
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
                // Never send blank or literal "null" as a reply
                if (replyText.isBlank() || replyText.equals("null", ignoreCase = true)) {
                    Log.e(TAG, "AI returned unusable reply text '$replyText'")
                    db.replyLogDao().insertLog(
                        ReplyLog(
                            contactName = senderName,
                            receivedMessage = message,
                            replyMessage = "",
                            ruleId = rule.id,
                            aiProvider = rule.aiProvider,
                            success = false,
                            errorMessage = "AI returned empty reply (model ${config.model})"
                        )
                    )
                    return
                }
                Log.d(TAG, "Generated reply for $senderName: $replyText")

                // 1. Primary: inject reply through Instagram's own notification Reply action
                //    (same mechanism the reference app uses - no accessibility needed)
                var sent = sendReplyViaRemoteInput(notification, replyText)

                // 2. Fallback: accessibility service types into the chat
                if (!sent) {
                    Log.d(TAG, "No reply action available, falling back to accessibility")
                    sent = sendReplyViaAccessibility(senderName, replyText, contentIntent, rule.delayMs)
                }

                db.replyLogDao().insertLog(
                    ReplyLog(
                        contactName = senderName,
                        receivedMessage = message,
                        replyMessage = replyText,
                        ruleId = rule.id,
                        aiProvider = rule.aiProvider,
                        success = sent,
                        errorMessage = if (sent) null else "Send failed: notification has no Reply action and accessibility service unavailable or chat could not be opened"
                    )
                )

                if (sent) {
                    // Update contact reply count
                    db.contactDao().incrementReply(senderName, System.currentTimeMillis())
                }
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                Log.e(TAG, "AI generation failed (model ${config.model}): $error")

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

    /**
     * Fires the notification's "Reply" action PendingIntent with our text injected
     * as the RemoteInput result. Instagram's own receiver then sends the DM -
     * exactly like a quick reply from the notification shade. No accessibility,
     * no screen-on, works in background.
     */
    private fun sendReplyViaRemoteInput(notification: Notification?, replyText: String): Boolean {
        if (notification == null) return false
        try {
            val actions = notification.actions ?: return false
            for (action in actions) {
                val remoteInputs = action.remoteInputs
                if (remoteInputs == null || remoteInputs.isEmpty()) continue
                if (action.actionIntent == null) continue

                // Build the results bundle: each RemoteInput key -> our reply text
                val results = Bundle()
                for (ri in remoteInputs) {
                    results.putCharSequence(ri.resultKey, replyText)
                }

                // Fill an intent with the results and fire the action intent.
                // Instagram's reply receiver reads RemoteInput.getResultsFromIntent(intent).
                val fillInIntent = Intent()
                RemoteInput.addResultsToIntent(remoteInputs, fillInIntent, results)
                action.actionIntent.send(context, 0, fillInIntent)

                Log.d(TAG, "Injected reply via notification action '${action.title}' (${remoteInputs.size} remote input(s))")
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "RemoteInput reply failed", e)
        }
        return false
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
            // 4. Type and send the message (with retries while Instagram settles)
            if (openedChat) {
                // Type once, click send at most once: guarantees one reply per message.
                // Retries only happen while the text field was never found (nothing sent yet).
                var textTyped = false
                for (attempt in 1..3) {
                    if (!textTyped) {
                        textTyped = withContext(Dispatchers.Main) {
                            accessibilityService.typeReplyText(message)
                        }
                        if (!textTyped) {
                            delay(1500)
                            continue
                        }
                        delay(250)
                    }
                    val sent = withContext(Dispatchers.Main) {
                        accessibilityService.clickSendButton()
                    }
                    // After one click attempt, stop: never risk a second send
                    return sent
                }
                false
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
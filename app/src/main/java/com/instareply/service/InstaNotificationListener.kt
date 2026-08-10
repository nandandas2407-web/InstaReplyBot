package com.instareply.service

import android.app.Notification
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.instareply.data.db.AppDatabase
import com.instareply.data.model.Contact
import com.instareply.util.PrefsManager
import kotlinx.coroutines.*

class InstaNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "InstaNotificationListener"
        private const val INSTAGRAM_PACKAGE = "com.instagram.android"

        fun isNotificationListenerEnabled(context: android.content.Context): Boolean {
            val flat = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            return flat.split(":").contains(context.packageName)
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var replyEngine: ReplyEngine? = null
    private val recentKeys = java.util.concurrent.ConcurrentHashMap<String, Long>()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NotificationListener created")
        replyEngine = ReplyEngine(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        replyEngine = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        if (sbn.packageName != INSTAGRAM_PACKAGE) return
        if (sbn.isOngoing) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()

        // Skip group summary notifications
        if (extras.getBoolean("android.isGroupSummary", false)) return

        // Get the actual message (prefer bigText if available)
        val message = bigText ?: text

        // Skip if message is from group chat summary or contains "liked" etc
        if (message.contains("liked your") || message.contains("commented on") ||
            message.contains("started following") || message.contains("mentioned you") ||
            message.contains("sent you a post") || message.contains("shared a post") ||
            message.contains("sent you a reel") || message.contains("sent you a video") ||
            message.contains("sent you a voice") || message.contains("reacted") ||
            message.contains("watched your") || message.contains("is now following") ||
            message.contains("missed a call") || message.contains("started a live") ||
            message.contains("suggested for you")) {
            return
        }

        Log.d(TAG, "Instagram notification from: $title, message: $message")

        val dedupeKey = "$title|$message"
        if (isDuplicate(dedupeKey)) {
            Log.d(TAG, "Skipping duplicate notification for $title")
            return
        }

        scope.launch {
            processNotification(title, message, notification, notification.contentIntent)
        }
    }

    private fun isDuplicate(key: String): Boolean {
        val now = System.currentTimeMillis()
        val last = recentKeys.put(key, now)
        if (last != null && now - last < 60_000) return true
        if (recentKeys.size > 64) {
            recentKeys.entries.removeAll { now - it.value > 5 * 60_000 }
        }
        return false
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Not needed
    }

    private suspend fun processNotification(
        senderName: String,
        message: String,
        notification: Notification?,
        contentIntent: android.app.PendingIntent?
    ) {
        val db = AppDatabase.getDatabase(this)
        val prefs = PrefsManager(this)

        if (!prefs.isEnabled()) return

        // Update contact stats
        val contactDao = db.contactDao()
        var contact = contactDao.getContactByName(senderName)
        if (contact == null) {
            contact = Contact(
                name = senderName,
                isGroup = false,
                firstMessageTimestamp = System.currentTimeMillis()
            )
            contactDao.insertContact(contact)
        }
        contactDao.incrementReceived(senderName, System.currentTimeMillis())

        // Find matching rule
        val ruleDao = db.ruleDao()
        val rules = ruleDao.getEnabledRules()
        val matchingRule = rules.firstOrNull { rule ->
            val specific = rule.specificContacts.split(",")
                .map { it.trim() }.filter { it.isNotEmpty() }
            if (specific.isNotEmpty() && specific.none { it.equals(senderName, ignoreCase = true) }) {
                return@firstOrNull false
            }
            val ignored = rule.ignoredContacts.split(",")
                .map { it.trim() }.filter { it.isNotEmpty() }
            if (ignored.any { it.equals(senderName, ignoreCase = true) }) {
                return@firstOrNull false
            }
            matchesRule(message, senderName, rule.triggerPattern, rule.matchType.name)
        }

        if (matchingRule != null) {
            // NEVER reply twice to the same message (persisted across restarts:
            // covers IG re-posting the same notification while AI was generating)
            val alreadyReplied = db.replyLogDao().countSentForMessage(
                senderName, message, System.currentTimeMillis() - 10 * 60_000L
            )
            if (alreadyReplied > 0) {
                Log.d(TAG, "Already replied to this message from $senderName, skipping")
                return
            }

            // Check rate limit (only successful replies consume the daily quota)
            val todayStart = getTodayStart()
            val replyCountToday = db.replyLogDao().getSuccessCountSince(todayStart)
            if (replyCountToday >= matchingRule.maxRepliesPerDay) {
                Log.d(TAG, "Rate limit reached for rule: ${matchingRule.label}")
                return
            }

            // Generate and send reply
            replyEngine?.processReply(
                senderName = senderName,
                message = message,
                rule = matchingRule,
                db = db,
                notification = notification,
                contentIntent = contentIntent
            )
        }
    }

    private fun matchesRule(message: String, senderName: String, pattern: String, matchType: String): Boolean {
        if (pattern.isEmpty() && matchType == "ANY") return true

        return when (matchType) {
            "CONTAINS" -> message.contains(pattern, ignoreCase = true)
            "EXACT" -> message.equals(pattern, ignoreCase = true)
            "STARTS_WITH" -> message.startsWith(pattern, ignoreCase = true)
            "ENDS_WITH" -> message.endsWith(pattern, ignoreCase = true)
            "REGEX" -> try {
                Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(message)
            } catch (e: Exception) {
                false
            }
            "ANY" -> true
            else -> false
        }
    }

    private fun getTodayStart(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}

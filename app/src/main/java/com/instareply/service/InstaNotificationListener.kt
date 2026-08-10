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
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var replyEngine: ReplyEngine? = null

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
        if (extras.getBoolean(Notification.EXTRA_IS_GROUP_SUMMARY, false)) return

        // Get the actual message (prefer bigText if available)
        val message = bigText ?: text

        // Skip if message is from group chat summary or contains "liked" etc
        if (message.contains("liked your") || message.contains("commented on") ||
            message.contains("started following") || message.contains("mentioned you")) {
            return
        }

        Log.d(TAG, "Instagram notification from: $title, message: $message")

        scope.launch {
            processNotification(title, message)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Not needed
    }

    private suspend fun processNotification(senderName: String, message: String) {
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
            matchesRule(message, senderName, rule.triggerPattern, rule.matchType.name)
        }

        if (matchingRule != null) {
            // Check rate limit
            val todayStart = getTodayStart()
            val replyCountToday = db.replyLogDao().getReplyCountSince(todayStart)
            if (replyCountToday >= matchingRule.maxRepliesPerDay) {
                Log.d(TAG, "Rate limit reached for rule: ${matchingRule.label}")
                return
            }

            // Delay if configured
            if (matchingRule.delayMs > 0) {
                delay(matchingRule.delayMs)
            }

            // Generate and send reply
            replyEngine?.processReply(
                senderName = senderName,
                message = message,
                rule = matchingRule,
                db = db
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

    fun isNotificationListenerEnabled(): Boolean {
        val pkgName = packageName
        val flat = android.provider.Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat?.contains(pkgName) == true
    }
}

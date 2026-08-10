package com.instareply.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class InstaAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "InstaAccessibility"
        private const val INSTAGRAM_PACKAGE = "com.instagram.android"
        var instance: InstaAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        Log.d(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used for this implementation
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    fun openInstagramChat(recipient: String): Boolean {
        try {
            // Try to open Instagram via intent
            val intent = packageManager.getLaunchIntentForPackage(INSTAGRAM_PACKAGE)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Instagram", e)
        }
        return false
    }

    fun typeAndSendMessage(message: String): Boolean {
        try {
            val rootNode = rootInActiveWindow ?: return false

            // Find the message input field
            val inputField = findEditText(rootNode)
            if (inputField != null) {
                // Set text
                val arguments = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, message)
                }
                inputField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

                // Find and click send button
                val sendButton = findSendButton(rootNode)
                if (sendButton != null) {
                    sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to type and send", e)
        }
        return false
    }

    private fun findEditText(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.className == "android.widget.EditText") {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findEditText(child)
            if (result != null) return result
        }
        return null
    }

    private fun findSendButton(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Instagram send button usually has content description "Send" or similar
        if (node.contentDescription?.toString()?.contains("Send", ignoreCase = true) == true) {
            return node
        }
        if (node.text?.toString()?.contains("Send", ignoreCase = true) == true) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findSendButton(child)
            if (result != null) return result
        }
        return null
    }
}

package com.instareply.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rules")
data class Rule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String = "",
    val isEnabled: Boolean = true,
    val triggerPattern: String = "",      // Regex or exact match
    val matchType: MatchType = MatchType.CONTAINS,
    val replyTemplate: String = "",       // Static reply or AI prompt
    val useAI: Boolean = true,            // Use AI provider or static reply
    val aiProvider: String = "gemini",    // gemini, openrouter, nvidia, opencode, openai
    val delayMs: Long = 0,               // Delay before reply
    val maxRepliesPerDay: Int = 10,      // Rate limit
    val applyToGroups: Boolean = false,
    val applyToContacts: Boolean = true, // true = all, false = specific
    val specificContacts: String = "",   // Comma-separated contact names
    val ignoredContacts: String = "",    // Comma-separated ignored names
    val priority: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

enum class MatchType {
    CONTAINS,       // Message contains pattern
    EXACT,          // Exact match
    STARTS_WITH,    // Starts with pattern
    ENDS_WITH,      // Ends with pattern
    REGEX,          // Regex pattern
    ANY             // Match any message
}

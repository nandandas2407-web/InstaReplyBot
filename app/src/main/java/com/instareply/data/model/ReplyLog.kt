package com.instareply.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reply_log")
data class ReplyLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactName: String,
    val receivedMessage: String,
    val replyMessage: String,
    val ruleId: Long,
    val aiProvider: String,
    val timestamp: Long = System.currentTimeMillis(),
    val success: Boolean = true,
    val errorMessage: String? = null
)

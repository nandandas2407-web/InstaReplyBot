package com.instareply.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isGroup: Boolean = false,
    val receivedCount: Int = 0,
    val replyCount: Int = 0,
    val lastMessageTimestamp: Long = 0,
    val lastReplyTimestamp: Long = 0,
    val firstMessageTimestamp: Long = System.currentTimeMillis()
)

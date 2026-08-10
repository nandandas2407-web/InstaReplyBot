package com.instareply.data.db

import androidx.room.*
import com.instareply.data.model.Contact
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY lastMessageTimestamp DESC")
    fun getAllContacts(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE name = :name LIMIT 1")
    suspend fun getContactByName(name: String): Contact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact): Long

    @Update
    suspend fun updateContact(contact: Contact)

    @Query("UPDATE contacts SET receivedCount = receivedCount + 1, lastMessageTimestamp = :timestamp WHERE name = :name")
    suspend fun incrementReceived(name: String, timestamp: Long)

    @Query("UPDATE contacts SET replyCount = replyCount + 1, lastReplyTimestamp = :timestamp WHERE name = :name")
    suspend fun incrementReply(name: String, timestamp: Long)

    @Query("SELECT SUM(replyCount) FROM contacts")
    suspend fun getTotalReplies(): Int?

    @Query("SELECT COUNT(*) FROM contacts")
    suspend fun getTotalContacts(): Int
}

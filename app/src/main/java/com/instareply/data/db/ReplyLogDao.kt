package com.instareply.data.db

import androidx.room.*
import com.instareply.data.model.ReplyLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ReplyLogDao {
    @Query("SELECT * FROM reply_log ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<ReplyLog>>

    @Query("SELECT * FROM reply_log WHERE contactName = :name ORDER BY timestamp DESC")
    fun getLogsForContact(name: String): Flow<List<ReplyLog>>

    @Insert
    suspend fun insertLog(log: ReplyLog)

    @Query("DELETE FROM reply_log WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("SELECT COUNT(*) FROM reply_log WHERE timestamp > :since")
    suspend fun getReplyCountSince(since: Long): Int
}

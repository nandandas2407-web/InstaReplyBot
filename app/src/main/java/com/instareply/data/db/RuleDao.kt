package com.instareply.data.db

import androidx.room.*
import com.instareply.data.model.Rule
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules ORDER BY priority DESC, createdAt DESC")
    fun getAllRules(): Flow<List<Rule>>

    @Query("SELECT * FROM rules WHERE isEnabled = 1 ORDER BY priority DESC")
    suspend fun getEnabledRules(): List<Rule>

    @Query("SELECT * FROM rules WHERE id = :id")
    suspend fun getRuleById(id: Long): Rule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: Rule): Long

    @Update
    suspend fun updateRule(rule: Rule)

    @Delete
    suspend fun deleteRule(rule: Rule)

    @Query("SELECT COUNT(*) FROM rules")
    suspend fun getTotalRules(): Int

    @Query("SELECT COUNT(*) FROM rules WHERE isEnabled = 1")
    suspend fun getEnabledRuleCount(): Int

    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)
}

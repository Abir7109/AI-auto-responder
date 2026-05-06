package com.zai.autoresponder.data.dao

import androidx.room.*
import com.zai.autoresponder.data.entity.AutoReplyRule
import kotlinx.coroutines.flow.Flow

@Dao
interface AutoReplyRuleDao {

    @Query("SELECT * FROM auto_reply_rules ORDER BY priority DESC")
    fun getAllRules(): Flow<List<AutoReplyRule>>

    @Query("SELECT * FROM auto_reply_rules ORDER BY priority DESC")
    suspend fun getAllRulesSync(): List<AutoReplyRule>

    @Query("SELECT * FROM auto_reply_rules WHERE enabled = 1 ORDER BY priority DESC")
    fun getEnabledRules(): Flow<List<AutoReplyRule>>

    @Query("SELECT * FROM auto_reply_rules WHERE id = :id")
    suspend fun getRuleById(id: String): AutoReplyRule?

    @Query("SELECT * FROM auto_reply_rules WHERE trigger = :trigger LIMIT 1")
    suspend fun getRuleByTrigger(trigger: String): AutoReplyRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: AutoReplyRule)

    @Update
    suspend fun update(rule: AutoReplyRule)

    @Delete
    suspend fun delete(rule: AutoReplyRule)

    @Query("DELETE FROM auto_reply_rules WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM auto_reply_rules WHERE enabled = 1")
    fun getEnabledRulesCount(): Flow<Int>
}

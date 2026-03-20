package com.zai.autoresponder.data.dao

import androidx.room.*
import com.zai.autoresponder.data.entity.ReplyHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface ReplyHistoryDao {

    @Query("SELECT * FROM reply_history ORDER BY createdAt DESC")
    fun getAllHistory(): Flow<List<ReplyHistory>>

    @Query("SELECT * FROM reply_history ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): Flow<List<ReplyHistory>>

    @Query("SELECT * FROM reply_history WHERE platform = :platform ORDER BY createdAt DESC")
    fun getHistoryByPlatform(platform: String): Flow<List<ReplyHistory>>

    @Query("SELECT COUNT(*) FROM reply_history")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM reply_history WHERE createdAt >= :startTime")
    fun getTodayCount(startTime: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: ReplyHistory)

    @Delete
    suspend fun delete(history: ReplyHistory)

    @Query("DELETE FROM reply_history")
    suspend fun clearAll()

    // Get all replies in the last 7 days (for chart)
    @Query("SELECT * FROM reply_history WHERE createdAt >= :startTime ORDER BY createdAt ASC")
    fun getRecentReplies(startTime: Long): Flow<List<ReplyHistory>>

    // Get total replies in last 7 days
    @Query("SELECT COUNT(*) FROM reply_history WHERE createdAt >= :startTime")
    fun getWeeklyTotal(startTime: Long): Flow<Int>
}

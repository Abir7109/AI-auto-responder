package com.zai.autoresponder.data.dao

import androidx.room.*
import com.zai.autoresponder.data.entity.AIBehavior
import kotlinx.coroutines.flow.Flow

@Dao
interface AIBehaviorDao {
    @Query("SELECT * FROM ai_behaviors ORDER BY createdAt DESC")
    fun getAllBehaviors(): Flow<List<AIBehavior>>

    @Query("SELECT * FROM ai_behaviors WHERE isActive = 1 LIMIT 1")
    fun getActiveBehavior(): Flow<AIBehavior?>

    @Query("SELECT * FROM ai_behaviors WHERE id = :id")
    suspend fun getBehaviorById(id: String): AIBehavior?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(behavior: AIBehavior)

    @Update
    suspend fun update(behavior: AIBehavior)

    @Delete
    suspend fun delete(behavior: AIBehavior)

    @Query("UPDATE ai_behaviors SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE ai_behaviors SET isActive = 1 WHERE id = :id")
    suspend fun activateBehavior(id: String)

    @Transaction
    suspend fun setActiveBehavior(id: String) {
        deactivateAll()
        activateBehavior(id)
    }
}

package com.zai.autoresponder.data.dao

import androidx.room.*
import com.zai.autoresponder.data.entity.KnowledgeSnippet
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeSnippetDao {
    @Query("SELECT * FROM knowledge_snippets WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllActiveSnippets(): Flow<List<KnowledgeSnippet>>

    @Query("SELECT * FROM knowledge_snippets ORDER BY createdAt DESC")
    fun getAllSnippets(): Flow<List<KnowledgeSnippet>>

    @Query("SELECT * FROM knowledge_snippets WHERE isActive = 1")
    suspend fun getAllActiveSnippetsSync(): List<KnowledgeSnippet>

    @Query("SELECT * FROM knowledge_snippets WHERE category = :category AND isActive = 1")
    fun getSnippetsByCategory(category: String): Flow<List<KnowledgeSnippet>>

    @Query("SELECT * FROM knowledge_snippets WHERE keyword LIKE '%' || :keyword || '%' AND isActive = 1")
    suspend fun searchByKeyword(keyword: String): List<KnowledgeSnippet>

    @Query("SELECT * FROM knowledge_snippets WHERE id = :id")
    suspend fun getSnippetById(id: String): KnowledgeSnippet?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snippet: KnowledgeSnippet)

    @Update
    suspend fun update(snippet: KnowledgeSnippet)

    @Delete
    suspend fun delete(snippet: KnowledgeSnippet)

    @Query("DELETE FROM knowledge_snippets WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE knowledge_snippets SET isActive = :isActive WHERE id = :id")
    suspend fun setActive(id: String, isActive: Boolean)

    @Query("SELECT COUNT(*) FROM knowledge_snippets WHERE isActive = 1")
    fun getActiveCount(): Flow<Int>

    @Query("DELETE FROM knowledge_snippets")
    suspend fun deleteAll()
}

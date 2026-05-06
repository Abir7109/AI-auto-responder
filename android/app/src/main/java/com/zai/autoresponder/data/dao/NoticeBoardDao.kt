package com.zai.autoresponder.data.dao

import androidx.room.*
import com.zai.autoresponder.data.entity.NoticeBoardItem
import kotlinx.coroutines.flow.Flow

@Dao
interface NoticeBoardDao {
    @Query("SELECT * FROM notice_board WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getAllNotices(): Flow<List<NoticeBoardItem>>
    
    @Query("SELECT * FROM notice_board WHERE isRead = 0 AND isArchived = 0 ORDER BY createdAt DESC")
    fun getUnreadNotices(): Flow<List<NoticeBoardItem>>
    
    @Query("SELECT * FROM notice_board WHERE isArchived = 1 ORDER BY createdAt DESC")
    fun getArchivedNotices(): Flow<List<NoticeBoardItem>>
    
    @Query("SELECT COUNT(*) FROM notice_board WHERE isRead = 0 AND isArchived = 0")
    fun getUnreadCount(): Flow<Int>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notice: NoticeBoardItem)
    
    @Update
    suspend fun update(notice: NoticeBoardItem)
    
    @Delete
    suspend fun delete(notice: NoticeBoardItem)
    
    @Query("DELETE FROM notice_board WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("UPDATE notice_board SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)
    
    @Query("UPDATE notice_board SET isArchived = 1 WHERE id = :id")
    suspend fun archive(id: String)
    
    @Query("UPDATE notice_board SET isArchived = 0 WHERE id = :id")
    suspend fun unarchive(id: String)
    
    @Query("DELETE FROM notice_board WHERE isArchived = 1")
    suspend fun clearArchived()
}

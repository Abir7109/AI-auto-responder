package com.zai.autoresponder.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.zai.autoresponder.data.dao.AppSettingsDao
import com.zai.autoresponder.data.dao.AutoReplyRuleDao
import com.zai.autoresponder.data.dao.KnowledgeSnippetDao
import com.zai.autoresponder.data.dao.NoticeBoardDao
import com.zai.autoresponder.data.dao.ReplyHistoryDao
import com.zai.autoresponder.data.dao.UserProfileDao
import com.zai.autoresponder.data.entity.AppSettings
import com.zai.autoresponder.data.entity.AutoReplyRule
import com.zai.autoresponder.data.entity.KnowledgeSnippet
import com.zai.autoresponder.data.entity.NoticeBoardItem
import com.zai.autoresponder.data.entity.ReplyHistory
import com.zai.autoresponder.data.entity.UserProfile

@Database(
    entities = [
        AutoReplyRule::class,
        AppSettings::class,
        ReplyHistory::class,
        UserProfile::class,
        KnowledgeSnippet::class,
        NoticeBoardItem::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun autoReplyRuleDao(): AutoReplyRuleDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun replyHistoryDao(): ReplyHistoryDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun knowledgeSnippetDao(): KnowledgeSnippetDao
    abstract fun noticeBoardDao(): NoticeBoardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from version 2 to 3 - add notice_board table
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS notice_board (
                        id TEXT NOT NULL PRIMARY KEY,
                        contactName TEXT NOT NULL,
                        contactNumber TEXT,
                        noticeType TEXT NOT NULL,
                        noticeContent TEXT NOT NULL,
                        originalMessage TEXT NOT NULL,
                        isRead INTEGER NOT NULL DEFAULT 0,
                        isArchived INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_auto_responder_db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.zai.autoresponder.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Entity representing an auto-reply rule
 * Matches Prisma schema: AutoReplyRule
 */
@Entity(tableName = "auto_reply_rules")
data class AutoReplyRule(
    @PrimaryKey
    val id: String,
    val trigger: String,
    val response: String?,
    val useAI: Boolean = false,
    val enabled: Boolean = true,
    val priority: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Entity representing app settings
 * Matches Prisma schema: AppSettings
 */
@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val id: String = "default",
    val serviceEnabled: Boolean = false,
    val replyWhatsApp: Boolean = true,
    val replyMessenger: Boolean = true,
    val replyTelegram: Boolean = false,
    val replyFacebook: Boolean = false,
    val replyInstagram: Boolean = false,
    val aiPersona: String = "professional",
    val autoDelay: String = "2",
    val quietHours: Boolean = false,
    val quietStart: String = "22:00",
    val quietEnd: String = "08:00",
    val apiKey: String? = null,
    val apiKeyValid: Boolean = false
)

/**
 * Entity representing reply history
 * Matches Prisma schema: ReplyHistory
 */
@Entity(tableName = "reply_history")
data class ReplyHistory(
    @PrimaryKey
    val id: String,
    val platform: String,
    val triggerMatch: String?,
    val originalMsg: String,
    val sentReply: String,
    val usedAI: Boolean = false,
    val responseTime: Int?,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * User entity
 * Matches Prisma schema: User
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String,
    val email: String,
    val name: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Post entity
 * Matches Prisma schema: Post
 */
@Entity(tableName = "posts")
data class Post(
    @PrimaryKey
    val id: String,
    val title: String,
    val content: String?,
    val published: Boolean = false,
    val authorId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * User Profile entity for personalized AI context
 * Stores the user's identity information
 */
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey
    val id: String = "default",
    val name: String = "",
    val age: Int = 0,
    val profession: String = "",
    val location: String = "",
    val bio: String = "",
    val hobbies: String = "",
    val customBehavior: String = "",
    val customFAQ: String = "",  // JSON string of Q&A pairs
    val availability: String = "",  // e.g., "Currently on vacation until Monday"
    val responseLength: String = "medium",  // short, medium, long
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Knowledge Snippet entity for storing quick facts
 * This is the "Brain" - small pieces of information about the user
 */
@Entity(tableName = "knowledge_snippets")
data class KnowledgeSnippet(
    @PrimaryKey
    val id: String,
    val keyword: String,        // What triggers this snippet (e.g., "dog", "work")
    val content: String,        // The fact (e.g., "My dog's name is Rex")
    val category: String = "general",  // general, work, personal, availability
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * AI Behavior entity - stores multiple behavior profiles that can be toggled
 */
@Entity(tableName = "ai_behaviors")
data class AIBehavior(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",  // Behavior name like "Work", "Personal", "Weekend"
    val availability: String = "",  // e.g., "Available", "In a meeting", "On vacation"
    val responseLength: String = "medium",  // short, medium, long
    val isActive: Boolean = false,  // Only one behavior can be active at a time
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Notice Board entity for storing important messages
 * The AI detects important info (reminders, meetings, numbers) and saves them here
 */
@Entity(tableName = "notice_board")
data class NoticeBoardItem(
    @PrimaryKey
    val id: String,
    val contactName: String,      // Who sent the message
    val contactNumber: String? = null,  // Phone number if shared
    val noticeType: String,        // reminder, meeting, important, call_back, number_shared, other
    val noticeContent: String,     // The important info extracted
    val originalMessage: String,   // The full original message
    val isRead: Boolean = false,  // Has the user seen this?
    val isArchived: Boolean = false,  // Has it been archived?
    val createdAt: Long = System.currentTimeMillis()
)

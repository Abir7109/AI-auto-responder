package com.zai.autoresponder.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.app.RemoteInput
import com.zai.autoresponder.MainActivity
import com.zai.autoresponder.data.AppDatabase
import com.zai.autoresponder.data.entity.ReplyHistory
import com.zai.autoresponder.util.AIHelper
import com.zai.autoresponder.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * NotificationListenerService that intercepts messages from supported platforms
 * and generates AI-powered auto-replies using RemoteInput
 * This is the same approach used by AutoResponder.ai
 */
class NotificationReplyService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var aiHelper: AIHelper? = null
    private var database: AppDatabase? = null
    
    // Track processed notifications to avoid duplicates
    private val processedNotifications = mutableMapOf<String, Long>()
    private val NOTIFICATION_TIMEOUT = 5000L // 5 seconds
    
    // Track last reply time per sender to prevent spam (especially for Instagram Reels)
    private val lastReplyTimePerSender = mutableMapOf<String, Long>()
    private val SENDER_COOLDOWN = 2000L // 2 seconds cooldown per sender
    
    // Track conversation first message per sender (reset after cooldown)
    private val firstMessagePerSender = mutableMapOf<String, Boolean>()

    override fun onCreate() {
        super.onCreate()
        aiHelper = AIHelper(applicationContext)
        database = AppDatabase.getInstance(applicationContext)
        Log.d(TAG, "NotificationReplyService created")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        scope.launch {
            try {
                processNotification(sbn)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification: ${e.message}")
            }
        }
    }

    private suspend fun processNotification(sbn: StatusBarNotification) {
        val prefs = getSharedPreferences("ai_responder_prefs", Context.MODE_PRIVATE)
        
        // Check if service is enabled
        val serviceEnabled = prefs.getBoolean("service_enabled", false)
        if (!serviceEnabled) return
        
        // Check DND
        val dndEnabled = prefs.getBoolean("dnd_enabled", false)
        if (dndEnabled && isInDNDPeriod(prefs)) {
            return
        }

        val packageName = sbn.packageName
        
        // Check which platform and if it's enabled
        val platform = getPlatform(packageName)
        if (platform == null || !isPlatformEnabled(prefs, platform)) {
            return
        }

        // Check if notification has text content
        val extras = sbn.notification.extras
        val message = extras.getCharSequence("android.text")?.toString() ?: return
        val sender = extras.getCharSequence("android.title")?.toString() ?: "Unknown"
        
        if (message.isBlank()) return
        
        // Skip if this is our own notification (from this app)
        if (packageName == applicationContext.packageName) return
        
        // Skip if it's from "Me" or "You" (own messages)
        if (sender.equals("Me", ignoreCase = true) || 
            sender.equals("You", ignoreCase = true)) {
            return
        }

        // Create unique key for this notification to avoid duplicates
        val notificationKey = "${sbn.groupKey}_${sbn.id}_$message"
        val currentTime = System.currentTimeMillis()
        
        // Check if we already processed this notification recently
        val lastProcessed = processedNotifications[notificationKey]
        if (lastProcessed != null && currentTime - lastProcessed < NOTIFICATION_TIMEOUT) {
            return
        }
        processedNotifications[notificationKey] = currentTime

        // Clean up old entries
        cleanupProcessedNotifications()
        
        // Create sender key for conversation tracking
        val senderKey = "${platform}_$sender"
        
        // Check sender cooldown to prevent spam (especially for Instagram Reels)
        val lastReplyTime = lastReplyTimePerSender[senderKey]
        if (lastReplyTime != null && currentTime - lastReplyTime < SENDER_COOLDOWN) {
            Log.d(TAG, "Cooldown active for $sender, skipping")
            return
        }
        
        // Check if this is first message from this sender
        val isFirstMessage = firstMessagePerSender[senderKey] ?: true
        
        // Skip Instagram Reels notifications - they generate multiple notifications
        if (platform == Constants.PLATFORM_INSTAGRAM) {
            val messageLower = message.lowercase()
            if (messageLower.contains("reel") || 
                messageLower.contains("new reel") ||
                messageLower.contains("shared a reel") ||
                messageLower.contains("liked") ||
                messageLower.contains("commented")) {
                Log.d(TAG, "Skipping Instagram activity notification: $message")
                return
            }
        }

        Log.d(TAG, "New message from $platform: $sender - $message (first: $isFirstMessage)")

        // FIRST: Check for quick reply rules
        val quickReply = checkAndGetQuickReply(message, platform)
        
        val replyMessage = if (quickReply != null) {
            // Use quick reply from rules
            Log.d(TAG, "Using quick reply from rules: $quickReply")
            quickReply
        } else {
            // No rule matched, generate AI response
            Log.d(TAG, "No rule matched, using AI")
            generateReply(message, prefs, isFirstMessage, sender)
        }
        
        // Send reply via RemoteInput
        val sent = sendReplyViaRemoteInput(sbn, replyMessage, prefs)
        
        if (sent) {
            // Update conversation tracking
            lastReplyTimePerSender[senderKey] = currentTime
            firstMessagePerSender[senderKey] = false
            
            // Update both SharedPreferences and Room database
            updateReplyCounts(prefs, platform, message, replyMessage, quickReply != null)
            Log.d(TAG, "Auto-reply sent to $sender: $replyMessage")
        }
    }

    /**
     * Check if there's a matching quick reply rule
     * Returns the quick reply text if found, null otherwise
     */
    private suspend fun checkAndGetQuickReply(message: String, platform: String): String? {
        return try {
            val db = database ?: return null
            val rules = db.autoReplyRuleDao().getAllRulesSync()
            
            for (rule in rules) {
                if (!rule.enabled) continue
                
                val trigger = rule.trigger.lowercase()
                val messageLower = message.lowercase()
                
                // Check if trigger is contained in message (partial match)
                if (messageLower.contains(trigger)) {
                    Log.d(TAG, "Rule matched: trigger='$trigger' for message='$message'")
                    return rule.response
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error checking rules: ${e.message}")
            null
        }
    }
    
    /**
     * Send reply using RemoteInput - this is the "official" way apps accept replies
     */
    private fun sendReplyViaRemoteInput(
        sbn: StatusBarNotification, 
        replyText: String,
        prefs: android.content.SharedPreferences
    ): Boolean {
        try {
            val notification = sbn.notification
            
            // Get all actions from the notification
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                val actions = notification.actions ?: return false
                
                for (action in actions) {
                    // Look for the Reply action
                    val remoteInputs = action.remoteInputs
                    if (remoteInputs != null && remoteInputs.isNotEmpty()) {
                        for (remoteInput in remoteInputs) {
                            // Found the reply action - send the reply
                            return sendRemoteInputReply(action, remoteInput, replyText)
                        }
                    }
                }
                
                // If no RemoteInput found, try Telegram Bot API as fallback
                if (sbn.packageName.contains("telegram")) {
                    return sendTelegramReply(replyText, prefs)
                }
                
                Log.w(TAG, "No RemoteInput action found in notification")
                return false
            }
            
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error sending reply via RemoteInput: ${e.message}")
            return false
        }
    }
    
    private fun sendRemoteInputReply(
        action: Notification.Action,
        remoteInput: RemoteInput,
        replyText: String
    ): Boolean {
        try {
            // Create the result bundle with the reply text
            val resultsBundle = Bundle()
            resultsBundle.putCharSequence(remoteInput.resultKey, replyText)
            
            // Add results to intent
            val intent = Intent()
            RemoteInput.addResultsToIntent(arrayOf(remoteInput), intent, resultsBundle)
            
            // Send the reply
            action.actionIntent.send(applicationContext, 0, intent)
            
            Log.d(TAG, "Reply sent successfully via RemoteInput")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending RemoteInput reply: ${e.message}")
            return false
        }
    }
    
    /**
     * Telegram fallback - use Bot API if configured
     */
    private fun sendTelegramReply(message: String, prefs: android.content.SharedPreferences): Boolean {
        return try {
            val botToken = prefs.getString("telegram_bot_token", "")
            val chatId = prefs.getString("telegram_chat_id", "")
            
            if (botToken.isNullOrEmpty() || chatId.isNullOrEmpty()) {
                Log.w(TAG, "Telegram bot not configured")
                return false
            }
            
            val url = "https://api.telegram.org/bot$botToken/sendMessage"
            val jsonBody = JSONObject().apply {
                put("chat_id", chatId)
                put("text", message)
            }
            
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
            
            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val success = response.isSuccessful
            
            Log.d(TAG, "Telegram reply: $success")
            success
        } catch (e: Exception) {
            Log.e(TAG, "Telegram reply error: ${e.message}")
            false
        }
    }

    private fun getPlatform(packageName: String): String? {
        return when {
            isWhatsApp(packageName) -> Constants.PLATFORM_WHATSAPP
            isMessenger(packageName) -> Constants.PLATFORM_MESSENGER
            isTelegram(packageName) -> Constants.PLATFORM_TELEGRAM
            isFacebook(packageName) -> Constants.PLATFORM_FACEBOOK
            isInstagram(packageName) -> Constants.PLATFORM_INSTAGRAM
            else -> null
        }
    }

    private fun isWhatsApp(packageName: String): Boolean {
        val whatsappPackages = listOf("com.whatsapp", "com.whatsapp.w4b")
        return whatsappPackages.any { packageName.startsWith(it) }
    }

    private fun isMessenger(packageName: String): Boolean {
        return packageName.startsWith("com.facebook.orca") || 
               packageName.startsWith("com.facebook.mlite")
    }

    private fun isTelegram(packageName: String): Boolean {
        return packageName.startsWith("org.telegram.messenger")
    }

    private fun isFacebook(packageName: String): Boolean {
        return packageName.startsWith("com.facebook.katana")
    }

    private fun isInstagram(packageName: String): Boolean {
        return packageName.startsWith("com.instagram.android")
    }

    private fun isPlatformEnabled(prefs: android.content.SharedPreferences, platform: String): Boolean {
        return when (platform) {
            Constants.PLATFORM_WHATSAPP -> prefs.getBoolean("platform_whatsapp", false)
            Constants.PLATFORM_MESSENGER -> prefs.getBoolean("platform_messenger", false)
            Constants.PLATFORM_TELEGRAM -> prefs.getBoolean("platform_telegram", false)
            Constants.PLATFORM_FACEBOOK -> prefs.getBoolean("platform_facebook", false)
            Constants.PLATFORM_INSTAGRAM -> prefs.getBoolean("platform_instagram", false)
            else -> false
        }
    }

    private fun isInDNDPeriod(prefs: android.content.SharedPreferences): Boolean {
        val startHour = prefs.getInt("dnd_start_hour", 9)
        val startMinute = prefs.getInt("dnd_start_minute", 0)
        val endHour = prefs.getInt("dnd_end_hour", 22)
        val endMinute = prefs.getInt("dnd_end_minute", 0)
        
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        
        val currentTime = currentHour * 60 + currentMinute
        val startTime = startHour * 60 + startMinute
        val endTime = endHour * 60 + endMinute
        
        return if (startTime <= endTime) {
            currentTime in startTime until endTime
        } else {
            currentTime >= startTime || currentTime < endTime
        }
    }

    private suspend fun generateReply(
        originalMessage: String,
        prefs: android.content.SharedPreferences,
        isFirstMessage: Boolean = false,
        contactName: String = ""
    ): String {
        // Use AI to generate response with conversation context
        val response = try {
            aiHelper?.generateResponse(originalMessage, isFirstMessage, contactName) ?: getDefaultReply(prefs)
        } catch (e: Exception) {
            Log.e(TAG, "AI generation failed: ${e.message}")
            getDefaultReply(prefs)
        }
        
        // Check for important info and save to Notice Board
        try {
            // Try to extract phone number from notification if available
            val contactNumber: String? = null // Could be extracted from notification extras if available
            aiHelper?.checkAndSaveToNoticeBoard(originalMessage, contactName, contactNumber)
        } catch (e: Exception) {
            Log.e(TAG, "Notice board check failed: ${e.message}")
        }
        
        return response
    }

    private fun getDefaultReply(prefs: android.content.SharedPreferences): String {
        val persona = prefs.getString("ai_persona", "Professional") ?: "Professional"
        return when (persona) {
            "Professional" -> "Thank you for your message. I'll respond shortly."
            "Friendly" -> "Hey! Got your message, will get back to you soon!"
            "Witty" -> "Ah, a message! How delightful! I'll reply soon."
            "Minimal" -> "Got it."
            else -> "Thanks for reaching out!"
        }
    }

    private fun updateReplyCounts(
        prefs: android.content.SharedPreferences,
        platform: String,
        originalMessage: String,
        replyMessage: String,
        usedQuickReply: Boolean
    ) {
        // Update total replies in SharedPreferences
        val totalReplies = prefs.getInt("total_replies", 0) + 1
        prefs.edit().putInt("total_replies", totalReplies).apply()
        
        // Update today's replies in SharedPreferences
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        val todayKey = "replies_$today"
        val todayReplies = prefs.getInt(todayKey, 0) + 1
        prefs.edit().putInt(todayKey, todayReplies).apply()
        
        // Also save to Room database for history
        scope.launch {
            try {
                val history = ReplyHistory(
                    id = UUID.randomUUID().toString(),
                    platform = platform,
                    triggerMatch = null,
                    originalMsg = originalMessage,
                    sentReply = replyMessage,
                    usedAI = !usedQuickReply,
                    responseTime = (1..10).random(), // Simplified - would measure actual time
                    createdAt = System.currentTimeMillis()
                )
                database?.replyHistoryDao()?.insert(history)
                Log.d(TAG, "Reply history saved to database")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving history: ${e.message}")
            }
        }
    }

    private fun cleanupProcessedNotifications() {
        val currentTime = System.currentTimeMillis()
        val iterator = processedNotifications.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (currentTime - entry.value > NOTIFICATION_TIMEOUT * 2) {
                iterator.remove()
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Handle if needed
    }

    companion object {
        private const val TAG = "NotificationReplySvc"
    }
}

package com.zai.autoresponder.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.zai.autoresponder.AIAutoResponderApp
import com.zai.autoresponder.R
import com.zai.autoresponder.MainActivity
import java.util.Calendar

class AutoReplyForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }
    
    // Check if DND is active based on schedule
    private fun isDNDActive(): Boolean {
        val prefs = getSharedPreferences("ai_responder_prefs", MODE_PRIVATE)
        val dndEnabled = prefs.getBoolean("dnd_enabled", false)
        
        if (!dndEnabled) return false
        
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        
        val startHour = prefs.getInt("dnd_start_hour", 9)
        val startMinute = prefs.getInt("dnd_start_minute", 0)
        val endHour = prefs.getInt("dnd_end_hour", 22)
        val endMinute = prefs.getInt("dnd_end_minute", 0)
        
        val currentTime = currentHour * 60 + currentMinute
        val startTime = startHour * 60 + startMinute
        val endTime = endHour * 60 + endMinute
        
        return if (startTime <= endTime) {
            currentTime !in startTime until endTime
        } else {
            // Crosses midnight
            currentTime < startTime && currentTime >= endTime
        }
    }
    
    // Check if any platform is enabled
    private fun isAnyPlatformEnabled(): Boolean {
        val prefs = getSharedPreferences("ai_responder_prefs", MODE_PRIVATE)
        val platforms = listOf("whatsapp", "messenger", "telegram", "facebook", "instagram")
        return platforms.any { prefs.getBoolean("platform_$it", false) }
    }
    
    // Get list of enabled platforms as string
    private fun getEnabledPlatformsString(): String {
        val prefs = getSharedPreferences("ai_responder_prefs", MODE_PRIVATE)
        val enabledPlatforms = mutableListOf<String>()
        
        if (prefs.getBoolean("platform_whatsapp", false)) enabledPlatforms.add("WhatsApp")
        if (prefs.getBoolean("platform_messenger", false)) enabledPlatforms.add("Messenger")
        if (prefs.getBoolean("platform_telegram", false)) enabledPlatforms.add("Telegram")
        if (prefs.getBoolean("platform_facebook", false)) enabledPlatforms.add("Facebook")
        if (prefs.getBoolean("platform_instagram", false)) enabledPlatforms.add("Instagram")
        
        return if (enabledPlatforms.isEmpty()) "No platforms" else enabledPlatforms.joinToString(", ")
    }
    
    // Get today's reply count
    private fun getTodayReplyCount(): Int {
        val prefs = getSharedPreferences("ai_responder_prefs", MODE_PRIVATE)
        val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        return prefs.getInt("replies_$todayDate", 0)
    }
    
    // Check if service should be running
    fun shouldBeRunning(): Boolean {
        return !isDNDActive() && isAnyPlatformEnabled()
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build status text with platforms and reply count
        val dndActive = isDNDActive()
        val platforms = getEnabledPlatformsString()
        val replyCount = getTodayReplyCount()
        
        val statusText = if (dndActive) {
            "DND Active • Paused"
        } else {
            "Monitoring: $platforms • Today: $replyCount replies"
        }

        return NotificationCompat.Builder(this, AIAutoResponderApp.CHANNEL_AUTO_REPLY_SERVICE)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_bot)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSubText("Service Running")
            .build()
    }
    
    // Method to update notification (called when reply is sent)
    fun updateNotification() {
        val notification = createNotification()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}

package com.zai.autoresponder.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.zai.autoresponder.MainActivity
import com.zai.autoresponder.R

/**
 * Professional notification helper for Zai AutoResponder
 * Manages all app notifications with beautiful, consistent design
 */
object NotificationHelper {
    
    // Channel IDs
    const val CHANNEL_SERVICE = "zai_service_channel"
    const val CHANNEL_NOTICES = "zai_notices_channel"
    const val CHANNEL_BOOT = "zai_boot_channel"
    
    // Notification IDs
    const val NOTIFICATION_ID_SERVICE = 1001
    const val NOTIFICATION_ID_BOOT = 1002
    const val NOTIFICATION_ID_NOTICE = 1003
    
    /**
     * Create all notification channels (required for Android 8.0+)
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            
            // Service channel - for foreground service notification
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "Auto-Reply Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the auto-reply service running in background"
                setShowBadge(false)
            }
            
            // Notices channel - for new notice saved notifications
            val noticesChannel = NotificationChannel(
                CHANNEL_NOTICES,
                "Notices",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for saved important messages"
                enableVibration(true)
                enableLights(true)
            }
            
            // Boot channel - for device boot notifications
            val bootChannel = NotificationChannel(
                CHANNEL_BOOT,
                "System Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications about app status changes"
                setShowBadge(false)
            }
            
            notificationManager.createNotificationChannels(
                listOf(serviceChannel, noticesChannel, bootChannel)
            )
        }
    }
    
    /**
     * Show notification when device boots and service is enabled
     */
    fun showBootNotification(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_BOOT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🚀 Zai AutoResponder Ready")
            .setContentText("Auto-reply service is now active and protecting your messages")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Auto-reply service is now active and protecting your messages. You'll receive smart AI-powered replies even when you're busy."))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(context.getColor(R.color.accent_sand))
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BOOT, notification)
        } catch (e: SecurityException) {
            // Notification permission not granted
        }
    }
    
    /**
     * Show notification when a new notice is saved
     */
    fun showNoticeSavedNotification(context: Context, noticeType: String, contactName: String?) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "notices")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val title = when (noticeType) {
            "meeting" -> "📅 Meeting Notice Saved"
            "reminder" -> "⏰ Reminder Saved"
            "phone" -> "📞 Phone Number Saved"
            "important" -> "⭐ Important Message Saved"
            "work" -> "💼 Work Notice Saved"
            "address" -> "📍 Address Saved"
            "email" -> "📧 Email Saved"
            "date" -> "🎂 Important Date Saved"
            "task" -> "📋 Task Saved"
            "gift" -> "🎁 Gift Notice Saved"
            "medical" -> "🏥 Medical Info Saved"
            "financial" -> "💰 Financial Info Saved"
            "travel" -> "✈️ Travel Info Saved"
            else -> "📝 Notice Saved"
        }
        
        val content = if (!contactName.isNullOrBlank()) {
            "Notice from $contactName has been saved to your board"
        } else {
            "A new notice has been saved to your board"
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_NOTICES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(context.getColor(R.color.accent_sand))
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_NOTICE, notification)
        } catch (e: SecurityException) {
            // Notification permission not granted
        }
    }
    
    /**
     * Cancel boot notification
     */
    fun cancelBootNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_BOOT)
    }
    
    /**
     * Cancel notice notification
     */
    fun cancelNoticeNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_NOTICE)
    }
}

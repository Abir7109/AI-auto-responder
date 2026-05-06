package com.zai.autoresponder

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.zai.autoresponder.data.AppDatabase

class AIAutoResponderApp : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize database
        database = AppDatabase.getInstance(this)
        
        // Create notification channels
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Auto-reply service channel
            val serviceChannel = NotificationChannel(
                CHANNEL_AUTO_REPLY_SERVICE,
                "Auto-Reply Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for auto-reply service status"
                setShowBadge(false)
            }

            // Notification alerts channel
            val alertsChannel = NotificationChannel(
                CHANNEL_NOTIFICATION_ALERTS,
                "Notification Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for incoming messages"
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(listOf(serviceChannel, alertsChannel))
        }
    }

    companion object {
        const val CHANNEL_AUTO_REPLY_SERVICE = "auto_reply_service"
        const val CHANNEL_NOTIFICATION_ALERTS = "notification_alerts"

        @Volatile
        private var instance: AIAutoResponderApp? = null

        fun getInstance(): AIAutoResponderApp {
            return instance ?: throw IllegalStateException("Application not initialized")
        }
    }
}

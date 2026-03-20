package com.zai.autoresponder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.content.SharedPreferences
import com.zai.autoresponder.service.AutoReplyForegroundService
import com.zai.autoresponder.util.NotificationHelper

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            context?.let {
                // Create notification channels first
                NotificationHelper.createNotificationChannels(it)
                
                // Check if service was enabled before reboot
                val prefs: SharedPreferences = it.getSharedPreferences("ai_responder_prefs", Context.MODE_PRIVATE)
                val serviceEnabled = prefs.getBoolean("service_enabled", false)
                
                if (serviceEnabled) {
                    // Start the foreground service after boot
                    val serviceIntent = Intent(it, AutoReplyForegroundService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        it.startForegroundService(serviceIntent)
                    } else {
                        it.startService(serviceIntent)
                    }
                    
                    // Show boot notification
                    NotificationHelper.showBootNotification(it)
                }
            }
        }
    }
}

package com.zai.autoresponder.service

import android.content.ComponentName
import android.content.SharedPreferences
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.zai.autoresponder.MainActivity

/**
 * Quick Settings Tile Service for Zai AutoResponder
 * Allows users to quickly toggle AI on/off from the Quick Settings panel
 */
class MyTileService : TileService() {

    companion object {
        const val PREFS_NAME = "ai_responder_prefs"
        const val KEY_SERVICE_ENABLED = "service_enabled"
    }

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        
        val isEnabled = prefs.getBoolean(KEY_SERVICE_ENABLED, false)
        
        // Toggle the setting
        prefs.edit().putBoolean(KEY_SERVICE_ENABLED, !isEnabled).apply()

        // Update tile visual
        updateTileState()
        
        // Handle service based on new state
        if (!isEnabled) {
            startAutoReplyService()
        } else {
            stopAutoReplyService()
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isActive = prefs.getBoolean(KEY_SERVICE_ENABLED, false)

        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (isActive) "AI: ON" else "AI: OFF"
        
        // Update the tile UI
        tile.updateTile()
    }
    
    private fun startAutoReplyService() {
        val intent = android.content.Intent(this, AutoReplyForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
    
    private fun stopAutoReplyService() {
        val intent = android.content.Intent(this, AutoReplyForegroundService::class.java)
        stopService(intent)
    }
    
    /**
     * Request the tile to update from outside the service
     * Call this when toggling from the app
     */
    fun requestTileUpdate() {
        updateTileState()
    }
}

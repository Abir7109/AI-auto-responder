package com.zai.autoresponder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.service.quicksettings.TileService
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.zai.autoresponder.data.AppDatabase
import com.zai.autoresponder.data.entity.AutoReplyRule
import com.zai.autoresponder.data.entity.AppSettings
import com.zai.autoresponder.fragments.*
import com.zai.autoresponder.service.AutoReplyForegroundService
import com.zai.autoresponder.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var database: AppDatabase
    private lateinit var prefs: SharedPreferences

    // Fragment instances
    private val dashboardFragment by lazy { DashboardFragment() }
    private val brainFragment by lazy { BrainFragment() }
    private val apiFragment by lazy { ApiFragment() }
    private val rulesFragment by lazy { RulesFragment() }
    private val settingsFragment by lazy { SettingsFragment() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Create notification channels
        NotificationHelper.createNotificationChannels(this)
        
        // Initialize SharedPreferences
        prefs = getSharedPreferences("ai_responder_prefs", Context.MODE_PRIVATE)
        
        // Request notification permission on first launch (after prefs is initialized)
        requestNotificationPermission()
        
        database = AppDatabase.getInstance(this)

        initViews()
        setupBottomNavigation()

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(dashboardFragment, "dashboard")
        }
    }

    private fun initViews() {
        bottomNavigation = findViewById(R.id.bottomNavigation)
    }

    private fun setupBottomNavigation() {
        // Set up item selection listener with animation
        bottomNavigation.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_dashboard -> dashboardFragment
                R.id.nav_brain -> brainFragment
                R.id.nav_api -> apiFragment
                R.id.nav_rules -> rulesFragment
                R.id.nav_settings -> settingsFragment
                else -> dashboardFragment
            }

            val tag = when (item.itemId) {
                R.id.nav_dashboard -> "dashboard"
                R.id.nav_brain -> "brain"
                R.id.nav_api -> "api"
                R.id.nav_rules -> "rules"
                R.id.nav_settings -> "settings"
                else -> "dashboard"
            }

            loadFragment(fragment, tag)
            true
        }

        // Enable item animations
        bottomNavigation.itemIconTintList = ContextCompat.getColorStateList(this, R.color.bottom_nav_color)
        bottomNavigation.itemTextColor = ContextCompat.getColorStateList(this, R.color.bottom_nav_color)
    }

    private fun loadFragment(fragment: Fragment, tag: String) {
        // Check if fragment is already added
        val existingFragment = supportFragmentManager.findFragmentByTag(tag)

        supportFragmentManager.beginTransaction().apply {
            // Set custom animations
            setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out
            )

            // Hide all fragments
            supportFragmentManager.fragments.forEach { hideFragment(it) }

            if (existingFragment != null) {
                show(existingFragment)
            } else {
                add(R.id.fragmentContainer, fragment, tag)
            }

            commit()
        }
    }

    private fun hideFragment(fragment: Fragment) {
        if (fragment.isAdded) {
            supportFragmentManager.beginTransaction()
                .hide(fragment)
                .commit()
        }
    }

    private fun showFragment(fragment: Fragment) {
        if (fragment.isAdded) {
            supportFragmentManager.beginTransaction()
                .show(fragment)
                .commit()
        } else {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, fragment)
                .commit()
        }
    }

    fun setServiceEnabled(enabled: Boolean) {
        // Save state to SharedPreferences for persistence
        prefs.edit().putBoolean("service_enabled", enabled).apply()
        
        if (enabled) {
            startAutoReplyService()
        } else {
            stopAutoReplyService()
        }
        
        // Sync Quick Settings Tile
        try {
            val tileComponent = android.content.ComponentName(
                this,
                com.zai.autoresponder.service.MyTileService::class.java
            )
            android.service.quicksettings.TileService.requestListeningState(this, tileComponent)
        } catch (e: Exception) {
            // Tile service may not be available
        }
    }

    fun isServiceEnabled(): Boolean {
        // Load state from SharedPreferences
        return prefs.getBoolean("service_enabled", false)
    }

    fun getDatabase(): AppDatabase = database

    // Function to track when a reply is sent
    fun trackReply() {
        val prefs = getSharedPreferences("ai_responder_prefs", Context.MODE_PRIVATE)
        
        // Increment total replies
        val totalReplies = prefs.getInt("total_replies", 0) + 1
        prefs.edit().putInt("total_replies", totalReplies).apply()
        
        // Increment today's replies
        val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val todayKey = "replies_$todayDate"
        val todayReplies = prefs.getInt(todayKey, 0) + 1
        prefs.edit().putInt(todayKey, todayReplies).apply()
        
        // Update response time (simplified - just random for demo, in production would measure actual time)
        val avgTime = prefs.getInt("avg_response_time", 0)
        val newAvg = if (avgTime == 0) (5..15).random() else (avgTime + (5..15).random()) / 2
        prefs.edit().putInt("avg_response_time", newAvg).apply()
    }
    
    private fun startAutoReplyService() {
        createNotificationChannel()
        val intent = Intent(this, AutoReplyForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopAutoReplyService() {
        val intent = Intent(this, AutoReplyForegroundService::class.java)
        stopService(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "auto_reply_channel",
                "Auto Reply Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Notifications for auto reply service" }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val firstLaunch = prefs.getBoolean("first_launch", true)
            if (firstLaunch) {
                // Mark that we've asked for permission
                prefs.edit().putBoolean("first_launch", false).apply()
                
                // Request notification permission
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) 
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                        1001
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

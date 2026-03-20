package com.zai.autoresponder.fragments

import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.zai.autoresponder.MainActivity
import com.zai.autoresponder.R
import com.zai.autoresponder.data.AppDatabase
import com.zai.autoresponder.data.entity.NoticeBoardItem
import com.zai.autoresponder.data.entity.ReplyHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment() {

    private lateinit var statusToggle: LinearLayout
    private lateinit var statusIndicator: View
    private lateinit var statusText: TextView
    private lateinit var serviceSwitch: Switch
    private lateinit var serviceIconContainer: FrameLayout
    private lateinit var serviceIcon: ImageView
    private lateinit var serviceStatusText: TextView

    private lateinit var statTotal: TextView
    private lateinit var statToday: TextView
    private lateinit var statSpeed: TextView
    private lateinit var statApps: TextView

    // Card references for click listeners
    private lateinit var cardTotalReplies: LinearLayout
    private lateinit var cardToday: LinearLayout
    private lateinit var cardSpeed: LinearLayout
    private lateinit var cardApps: LinearLayout

    private lateinit var dashboardPlatformsList: LinearLayout
    
    // Analytics
    private lateinit var repliesChart: LineChart
    private lateinit var timeSavedText: TextView
    private lateinit var streakText: TextView

    // Notice Board
    private lateinit var dashboardNoticeEmptyState: LinearLayout
    private lateinit var tvDashboardNoticeCount: TextView
    private lateinit var dashboardNoticesRecyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var noticeAdapter: NoticeBoardAdapter

    private var floatAnimation: Animation? = null
    private var glowPulseAnimation: Animation? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupAnimations()
        setupServiceToggle()
        setupStatCardClicks()
        setupPlatforms()
        setupNoticeBoard()
        loadData()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh service status when returning to dashboard
        loadData()
    }

    private fun setupAnimations() {
        // Load float animation for bot icon
        floatAnimation = AnimationUtils.loadAnimation(context, R.anim.float_animation)
        serviceIcon.startAnimation(floatAnimation)
        
        // Load glow pulse animation for status indicator
        glowPulseAnimation = AnimationUtils.loadAnimation(context, R.anim.glow_pulse)
    }

    private fun initViews(view: View) {
        statusToggle = view.findViewById(R.id.statusToggle)
        statusIndicator = view.findViewById(R.id.statusIndicator)
        statusText = view.findViewById(R.id.statusText)
        serviceSwitch = view.findViewById(R.id.serviceSwitch)
        serviceIconContainer = view.findViewById(R.id.serviceIconContainer)
        serviceIcon = view.findViewById(R.id.serviceIcon)
        serviceStatusText = view.findViewById(R.id.serviceStatusText)

        statTotal = view.findViewById(R.id.statTotal)
        statToday = view.findViewById(R.id.statToday)
        statSpeed = view.findViewById(R.id.statSpeed)
        statApps = view.findViewById(R.id.statApps)

        // Get card references
        cardTotalReplies = view.findViewById(R.id.cardTotalReplies)
        cardToday = view.findViewById(R.id.cardToday)
        cardSpeed = view.findViewById(R.id.cardSpeed)
        cardApps = view.findViewById(R.id.cardApps)

        dashboardPlatformsList = view.findViewById(R.id.dashboardPlatformsList)
        
        // Analytics
        repliesChart = view.findViewById(R.id.repliesChart)
        timeSavedText = view.findViewById(R.id.timeSavedText)
        streakText = view.findViewById(R.id.streakText)

        // Notice Board
        dashboardNoticeEmptyState = view.findViewById(R.id.dashboardNoticeEmptyState)
        tvDashboardNoticeCount = view.findViewById(R.id.tvDashboardNoticeCount)
        dashboardNoticesRecyclerView = view.findViewById(R.id.rvDashboardNotices)
    }

    private fun setupServiceToggle() {
        statusToggle.setOnClickListener {
            requireView().performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            val newState = !serviceSwitch.isChecked
            
            // Check permission before enabling
            if (newState && !isNotificationAccessEnabled()) {
                // Permission not granted, request it
                requestNotificationAccess()
                return@setOnClickListener
            }
            
            serviceSwitch.isChecked = newState
        }

        serviceSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Check permission before enabling
            if (isChecked && !isNotificationAccessEnabled()) {
                // Permission not granted, request it and reset switch
                requestNotificationAccess()
                serviceSwitch.isChecked = false
                return@setOnCheckedChangeListener
            }
            
            (activity as? MainActivity)?.setServiceEnabled(isChecked)
            updateServiceStatus(isChecked)
        }
    }
    
    private fun isNotificationAccessEnabled(): Boolean {
        val pkgName = requireContext().packageName
        val flat = Settings.Secure.getString(
            requireContext().contentResolver,
            "enabled_notification_listeners"
        )
        return flat?.contains(pkgName) == true
    }
    
    private fun requestNotificationAccess() {
        Toast.makeText(
            requireContext(),
            "Notification Access is required for auto-reply to work",
            Toast.LENGTH_LONG
        ).show()
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            val intent = android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        }
    }

    private fun setupStatCardClicks() {
        cardTotalReplies.setOnClickListener {
            requireView().performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            showHistoryDialog("Total Replies", statTotal.text.toString())
        }

        cardToday.setOnClickListener {
            requireView().performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            showHistoryDialog("Today's Replies", statToday.text.toString())
        }

        cardSpeed.setOnClickListener {
            requireView().performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            showStatDetailsDialog("Average Speed", statSpeed.text.toString(), listOf<Any>())
        }

        cardApps.setOnClickListener {
            requireView().performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            // Get list of active platforms
            val prefs = requireContext().getSharedPreferences("ai_responder_prefs", Context.MODE_PRIVATE)
            val platforms = listOf(
                Pair("whatsapp", "WhatsApp"),
                Pair("messenger", "Messenger"),
                Pair("telegram", "Telegram"),
                Pair("facebook", "Facebook"),
                Pair("instagram", "Instagram")
            )
            val activePlatforms = platforms.filter { prefs.getBoolean("platform_${it.first}", false) }
            showStatDetailsDialog("Active Apps", statApps.text.toString(), activePlatforms)
        }
    }

    /**
     * Show history dialog with actual reply history from database
     */
    private fun showHistoryDialog(title: String, count: String) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_stat_details)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.window?.setDimAmount(0.7f)

        val dialogTitle = dialog.findViewById<TextView>(R.id.dialogTitle)
        val detailCount = dialog.findViewById<TextView>(R.id.detailCount)
        val detailLabel = dialog.findViewById<TextView>(R.id.detailLabel)
        val historyContainer = dialog.findViewById<LinearLayout>(R.id.historyContainer)
        val closeButton = dialog.findViewById<ImageButton>(R.id.closeButton)
        val blurryBackground = dialog.findViewById<View>(R.id.blurryBackground)

        dialogTitle.text = title
        detailCount.text = count
        detailLabel.text = "History"

        // Load history from database using coroutines
        historyContainer.removeAllViews()
        
        val mainActivity = requireActivity() as MainActivity
        val db = mainActivity.getDatabase()
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val historyList = db.replyHistoryDao().getRecentHistory(20).first()
                
                if (historyList.isEmpty()) {
                    val emptyView = TextView(requireContext()).apply {
                        text = "No reply history yet"
                        setTextColor(Color.parseColor("#948979"))
                        setPadding(32, 32, 32, 32)
                    }
                    historyContainer.addView(emptyView)
                } else {
                    for (history in historyList) {
                        val historyView = layoutInflater.inflate(R.layout.item_history, historyContainer, false)
                        val iconView = historyView.findViewById<ImageView>(R.id.itemIcon)
                        val titleText = historyView.findViewById<TextView>(R.id.itemTitle)
                        val messageText = historyView.findViewById<TextView>(R.id.itemMessage)
                        val timeText = historyView.findViewById<TextView>(R.id.itemTime)
                        
                        // Set platform icon
                        val iconRes = when (history.platform.lowercase()) {
                            "whatsapp" -> R.drawable.whatsapp
                            "messenger" -> R.drawable.messenger
                            "telegram" -> R.drawable.telegram
                            "facebook" -> R.drawable.facebook
                            "instagram" -> R.drawable.instagram
                            else -> R.drawable.ic_message_circle
                        }
                        iconView.setImageResource(iconRes)
                        
                        // Show original message and reply
                        titleText.text = history.platform.replaceFirstChar { it.uppercase() }
                        messageText.text = "In: ${history.originalMsg.take(30)}${if (history.originalMsg.length > 30) "..." else ""}\nOut: ${history.sentReply.take(30)}${if (history.sentReply.length > 30) "..." else ""}"
                        
                        // Show time
                        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        timeText.text = timeFormat.format(Date(history.createdAt))
                        
                        // Color based on AI or Quick Reply
                        messageText.setTextColor(
                            if (history.usedAI) Color.parseColor("#DFD0B8") 
                            else Color.parseColor("#4CAF50")
                        )
                        
                        // Add click listener for detail popup
                        historyView.setOnClickListener {
                            historyView.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                            showReplyDetailDialog(history)
                        }
                        
                        historyContainer.addView(historyView)
                    }
                }
            } catch (e: Exception) {
                val errorView = TextView(requireContext()).apply {
                    text = "Error loading history"
                    setTextColor(Color.parseColor("#f43f5e"))
                    setPadding(32, 32, 32, 32)
                }
                historyContainer.addView(errorView)
            }
        }

        // Handle blurry background click to dismiss
        blurryBackground.setOnClickListener {
            dialog.dismiss()
        }

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Show floating detail card with full reply information
     */
    private fun showReplyDetailDialog(history: ReplyHistory) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_reply_detail)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.window?.setDimAmount(0.0f)

        val blurryBackground = dialog.findViewById<View>(R.id.blurryBackground)
        // Click on blurry background to close dialog
        blurryBackground.setOnClickListener {
            dialog.dismiss()
        }
        val closeButton = dialog.findViewById<ImageButton>(R.id.closeButton)
        val platformIcon = dialog.findViewById<ImageView>(R.id.platformIcon)
        val platformName = dialog.findViewById<TextView>(R.id.platformName)
        val recipientName = dialog.findViewById<TextView>(R.id.recipientName)
        val replyTypeBadge = dialog.findViewById<TextView>(R.id.replyTypeBadge)
        val originalMessage = dialog.findViewById<TextView>(R.id.originalMessage)
        val aiReply = dialog.findViewById<TextView>(R.id.aiReply)
        val replyTime = dialog.findViewById<TextView>(R.id.replyTime)

        // Set platform icon
        val iconRes = when (history.platform.lowercase()) {
            "whatsapp" -> R.drawable.whatsapp
            "messenger" -> R.drawable.messenger
            "telegram" -> R.drawable.telegram
            "facebook" -> R.drawable.facebook
            "instagram" -> R.drawable.instagram
            else -> R.drawable.ic_message_circle
        }
        platformIcon.setImageResource(iconRes)
        platformName.text = history.platform.replaceFirstChar { it.uppercase() }

        // Set recipient (from triggerMatch if available)
        val recipient = history.triggerMatch ?: "Unknown"
        recipientName.text = "Sent to: $recipient"

        // Set reply type badge
        replyTypeBadge.text = if (history.usedAI) "AI" else "Quick"
        replyTypeBadge.setBackgroundResource(
            if (history.usedAI) R.drawable.glass_badge_background
            else R.drawable.glass_badge_quick_background
        )

        // Set messages
        originalMessage.text = history.originalMsg
        aiReply.text = history.sentReply

        // Set time
        val timeFormat = SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        replyTime.text = "Sent at ${timeFormat.format(Date(history.createdAt))}"

        // Handle blurry background click to dismiss
        blurryBackground.setOnClickListener {
            blurryBackground.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            dialog.dismiss()
        }

        closeButton.setOnClickListener {
            closeButton.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showStatDetailsDialog(title: String, count: String, items: List<Any>) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_stat_details)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.window?.setDimAmount(0.7f)

        val dialogTitle = dialog.findViewById<TextView>(R.id.dialogTitle)
        val detailCount = dialog.findViewById<TextView>(R.id.detailCount)
        val detailLabel = dialog.findViewById<TextView>(R.id.detailLabel)
        val historyContainer = dialog.findViewById<LinearLayout>(R.id.historyContainer)
        val closeButton = dialog.findViewById<ImageButton>(R.id.closeButton)
        val blurryBackground = dialog.findViewById<View>(R.id.blurryBackground)

        dialogTitle.text = title
        detailCount.text = count

        // Set label based on title
        detailLabel.text = when (title) {
            "Total Replies" -> "All Time"
            "Today's Replies" -> "Today"
            "Average Speed" -> "Average"
            "Active Apps" -> "Enabled"
            else -> "Total"
        }
        
        // Populate history container for Active Apps
        if (title == "Active Apps" && items.isNotEmpty()) {
            historyContainer.removeAllViews()
            for (item in items) {
                if (item is Pair<*, *>) {
                    val platformView = layoutInflater.inflate(R.layout.item_history, historyContainer, false)
                    val iconView = platformView.findViewById<ImageView>(R.id.itemIcon)
                    val titleText = platformView.findViewById<TextView>(R.id.itemTitle)
                    val messageText = platformView.findViewById<TextView>(R.id.itemMessage)
                    val timeText = platformView.findViewById<TextView>(R.id.itemTime)
                    
                    // Set icon based on platform
                    val iconRes = when (item.first) {
                        "whatsapp" -> R.drawable.whatsapp
                        "messenger" -> R.drawable.messenger
                        "telegram" -> R.drawable.telegram
                        "facebook" -> R.drawable.facebook
                        "instagram" -> R.drawable.instagram
                        else -> R.drawable.ic_message_circle
                    }
                    iconView.setImageResource(iconRes)
                    titleText.text = item.second.toString()
                    messageText.text = "Enabled"
                    messageText.setTextColor(Color.parseColor("#4CAF50"))
                    timeText.text = ""
                    
                    historyContainer.addView(platformView)
                }
            }
        } else {
            // Show placeholder for other stats
            historyContainer.removeAllViews()
            if (title != "Active Apps") {
                val emptyView = TextView(requireContext()).apply {
                    text = "No data yet"
                    setTextColor(Color.parseColor("#948979"))
                    setPadding(32, 32, 32, 32)
                }
                historyContainer.addView(emptyView)
            }
        }

        // Handle blurry background click to dismiss
        blurryBackground.setOnClickListener {
            dialog.dismiss()
        }

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateServiceStatus(enabled: Boolean) {
        if (enabled) {
            statusIndicator.setBackgroundResource(R.drawable.status_indicator_active)
            statusText.text = "ON"
            statusText.setTextColor(resources.getColor(R.color.green_400, null))
            serviceIconContainer.setBackgroundResource(R.drawable.service_icon_background_active)
            serviceIcon.setColorFilter(resources.getColor(R.color.green_400, null))
            serviceStatusText.text = "AI monitoring active"
            
            // Start glow pulse animation when service is ON
            glowPulseAnimation?.let { statusIndicator.startAnimation(it) }
        } else {
            statusIndicator.setBackgroundResource(R.drawable.status_indicator_inactive)
            statusText.text = "OFF"
            statusText.setTextColor(resources.getColor(R.color.foreground, null))
            serviceIconContainer.setBackgroundResource(R.drawable.service_icon_background_inactive)
            serviceIcon.setColorFilter(resources.getColor(R.color.accent_warm, null))
            serviceStatusText.text = "Service disabled"
            
            // Stop glow pulse animation when service is OFF
            statusIndicator.clearAnimation()
        }
    }

    private fun setupPlatforms() {
        val prefs = requireContext().getSharedPreferences("ai_responder_prefs", Context.MODE_PRIVATE)
        
        val platforms = listOf(
            Triple("whatsapp", R.drawable.whatsapp, "#25D366"),
            Triple("messenger", R.drawable.messenger, "#0099FF"),
            Triple("telegram", R.drawable.telegram, "#26A5E4"),
            Triple("facebook", R.drawable.facebook, "#1877F2"),
            Triple("instagram", R.drawable.instagram, "#E4405F")
        )

        dashboardPlatformsList.removeAllViews()
        
        // Count active platforms
        var activeCount = 0
        for ((platformKey, _, _) in platforms) {
            if (prefs.getBoolean("platform_$platformKey", false)) {
                activeCount++
            }
        }
        statApps.text = activeCount.toString()
        
        // Create platform items WITH switches for direct toggling
        for ((platformKey, icon, color) in platforms) {
            val platformView = layoutInflater.inflate(R.layout.item_platform_switch, dashboardPlatformsList, false)
            val iconView = platformView.findViewById<ImageView>(R.id.platformIcon)
            val nameView = platformView.findViewById<TextView>(R.id.platformName)
            val switch = platformView.findViewById<Switch>(R.id.platformSwitch)

            iconView.setImageResource(icon)
            nameView.text = platformKey.replaceFirstChar { it.uppercase() }
            
            // Load saved state from SharedPreferences
            val isEnabled = prefs.getBoolean("platform_$platformKey", false)
            switch.isChecked = isEnabled
            
            // Make switch interactive - toggles immediately and updates count
            switch.setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("platform_$platformKey", isChecked).apply()
                // Immediately update the count
                updateActiveCount()
            }

            dashboardPlatformsList.addView(platformView)
        }
    }
    
    private fun updateActiveCount() {
        val prefs = requireContext().getSharedPreferences("ai_responder_prefs", Context.MODE_PRIVATE)
        val platforms = listOf("whatsapp", "messenger", "telegram", "facebook", "instagram")
        var activeCount = 0
        for (platform in platforms) {
            if (prefs.getBoolean("platform_$platform", false)) {
                activeCount++
            }
        }
        statApps.text = activeCount.toString()
    }

    private fun setupNoticeBoard() {
        val mainActivity = requireActivity() as MainActivity
        val database = mainActivity.getDatabase()

        noticeAdapter = NoticeBoardAdapter(
            onItemClick = { notice ->
                lifecycleScope.launch {
                    database.noticeBoardDao().markAsRead(notice.id)
                }
            },
            onArchiveClick = { notice ->
                lifecycleScope.launch {
                    database.noticeBoardDao().archive(notice.id)
                    Toast.makeText(requireContext(), "Notice archived", Toast.LENGTH_SHORT).show()
                }
            },
            onDeleteClick = { notice ->
                lifecycleScope.launch {
                    database.noticeBoardDao().delete(notice)
                    Toast.makeText(requireContext(), "Notice deleted", Toast.LENGTH_SHORT).show()
                }
            }
        )

        dashboardNoticesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = noticeAdapter
        }

        // Observe notices
        lifecycleScope.launch {
            database.noticeBoardDao().getAllNotices().collectLatest { notices ->
                noticeAdapter.submitList(notices)
                dashboardNoticeEmptyState.visibility = if (notices.isEmpty()) View.VISIBLE else View.GONE
                dashboardNoticesRecyclerView.visibility = if (notices.isEmpty()) View.GONE else View.VISIBLE
                tvDashboardNoticeCount.text = if (notices.isEmpty()) "No notices" else "${notices.size} notice${if (notices.size > 1) "s" else ""}"
            }
        }
    }

    private fun loadData() {
        val mainActivity = activity as? MainActivity
        val isEnabled = mainActivity?.isServiceEnabled() ?: false
        serviceSwitch.isChecked = isEnabled
        updateServiceStatus(isEnabled)
        
        // Load stats from SharedPreferences
        val prefs = requireContext().getSharedPreferences("ai_responder_prefs", Context.MODE_PRIVATE)
        
        // Get total replies
        val totalReplies = prefs.getInt("total_replies", 0)
        statTotal.text = totalReplies.toString()
        
        // Get today's replies
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayKey = "replies_$todayDate"
        val todayReplies = prefs.getInt(todayKey, 0)
        statToday.text = todayReplies.toString()
        
        // Get average response time (in seconds)
        val avgResponseTime = prefs.getInt("avg_response_time", 0)
        statSpeed.text = if (avgResponseTime > 0) "${avgResponseTime}s" else "--"
        
        // Count active platforms - already done in setupPlatforms()
        // Just ensure the count is up to date
        val platforms = listOf("whatsapp", "messenger", "telegram", "facebook", "instagram")
        var activeCount = 0
        for (platform in platforms) {
            if (prefs.getBoolean("platform_$platform", false)) {
                activeCount++
            }
        }
        statApps.text = activeCount.toString()
        
        // Setup analytics chart
        setupChart(prefs)
    }
    
    private fun setupChart(prefs: SharedPreferences) {
        // Configure chart appearance
        repliesChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            setDrawGridBackground(false)
            setBackgroundColor(Color.TRANSPARENT)
            
            // X-Axis (Days of week)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.parseColor("#948979")
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"))
            }
            
            // Left Y-Axis
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#1A1F26")
                textColor = Color.parseColor("#948979")
                axisMinimum = 0f
            }
            
            // Right Y-Axis (disable)
            axisRight.isEnabled = false
            
            // Animation
            animateX(1000)
        }
        
        // Get database and fetch real data
        val mainActivity = requireActivity() as MainActivity
        val database = mainActivity.getDatabase()
        
        // Calculate start time for 7 days ago
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        
        // Observe real data from database
        lifecycleScope.launch {
            database.replyHistoryDao().getRecentReplies(sevenDaysAgo).collectLatest { replies ->
                // Count replies by day (Mon=0 to Sun=6)
                val dayCounts = IntArray(7) { 0 }
                val calendar = java.util.Calendar.getInstance()
                
                for (reply in replies) {
                    calendar.timeInMillis = reply.createdAt
                    // Calendar.DAY_OF_WEEK: Sunday=1, Monday=2, ... Saturday=7
                    // Convert to: Monday=0, Tuesday=1, ... Sunday=6
                    val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
                    val index = when (dayOfWeek) {
                        java.util.Calendar.MONDAY -> 0
                        java.util.Calendar.TUESDAY -> 1
                        java.util.Calendar.WEDNESDAY -> 2
                        java.util.Calendar.THURSDAY -> 3
                        java.util.Calendar.FRIDAY -> 4
                        java.util.Calendar.SATURDAY -> 5
                        java.util.Calendar.SUNDAY -> 6
                        else -> 0
                    }
                    dayCounts[index]++
                }
                
                // Create chart entries from real data
                val entries = listOf(
                    Entry(0f, dayCounts[0].toFloat()),  // Monday
                    Entry(1f, dayCounts[1].toFloat()),  // Tuesday
                    Entry(2f, dayCounts[2].toFloat()),  // Wednesday
                    Entry(3f, dayCounts[3].toFloat()),  // Thursday
                    Entry(4f, dayCounts[4].toFloat()),  // Friday
                    Entry(5f, dayCounts[5].toFloat()),  // Saturday
                    Entry(6f, dayCounts[6].toFloat())   // Sunday
                )
                
                val dataSet = LineDataSet(entries, "Replies").apply {
                    color = Color.parseColor("#DFD0B8")
                    setCircleColor(Color.parseColor("#DFD0B8"))
                    lineWidth = 3f
                    circleRadius = 4f
                    setDrawCircleHole(true)
                    circleHoleRadius = 2f
                    circleHoleColor = Color.parseColor("#0f1218")
                    setDrawFilled(true)
                    fillColor = Color.parseColor("#40DFD0B8")
                    fillAlpha = 50
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                    setDrawHighlightIndicators(true)
                    highLightColor = Color.parseColor("#DFD0B8")
                }
                
                repliesChart.data = LineData(dataSet)
                repliesChart.invalidate()
                
                // Calculate time saved (estimate 30 seconds per reply)
                val totalReplies = dayCounts.sum()
                val timeSavedMinutes = totalReplies * 0.5 // 30 seconds = 0.5 minutes
                timeSavedText.text = "${timeSavedMinutes.toInt()} min"
                
                // Calculate streak (consecutive days with at least 1 reply)
                var streak = 0
                for (i in dayCounts.indices.reversed()) {
                    if (dayCounts[i] > 0) streak++
                    else break
                }
                streakText.text = "$streak days"
            }
        }
    }

    // Data classes for dialog items
    data class HistoryItem(val sender: String, val message: String, val time: String)
    data class StatDetail(val label: String, val value: String)
    data class PlatformItem(val name: String, val status: String, val color: String, val iconRes: Int)
}

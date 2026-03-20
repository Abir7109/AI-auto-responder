package com.zai.autoresponder.service

import android.accessibilityservice.AccessibilityService
import android.content.SharedPreferences
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.zai.autoresponder.util.AIHelper
import com.zai.autoresponder.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * AccessibilityService that monitors screen content and automatically replies to messages
 * This is the same approach used by AutoResponder.ai
 */
class AutoReplyAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var aiHelper: AIHelper? = null
    private var prefs: SharedPreferences? = null
    
    // Track last processed message to avoid duplicates
    private var lastProcessedText: String = ""
    private var lastProcessedTime: Long = 0
    private val PROCESS_TIMEOUT = 5000L // 5 seconds
    
    // Track if we're currently typing a reply
    private var isTypingReply = false
    
    // Track the last known message to detect new messages
    private var lastKnownMessage: String = ""

    override fun onCreate() {
        super.onCreate()
        aiHelper = AIHelper(applicationContext)
        prefs = getSharedPreferences("ai_responder_prefs", MODE_PRIVATE)
        Log.d(TAG, "AutoReplyAccessibilityService created")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "AccessibilityService connected and ready")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        
        // Only process window state changes (when user opens a chat)
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return
        }

        val packageName = event.packageName?.toString() ?: return
        
        // Check if this is from a supported platform
        val platform = getPlatform(packageName) ?: return
        
        // Check if service is enabled
        val serviceEnabled = prefs?.getBoolean("service_enabled", false) ?: return
        if (!serviceEnabled) return
        
        // Check if platform is enabled
        if (!isPlatformEnabled(platform)) return
        
        // Check DND
        val dndEnabled = prefs?.getBoolean("dnd_enabled", false) ?: false
        if (dndEnabled && isInDNDPeriod()) {
            return
        }
        
        // Skip if we're currently typing a reply
        if (isTypingReply) return
        
        // Get the root window to analyze current screen content
        val rootNode = rootInActiveWindow ?: return
        
        try {
            // Check if this is a chat screen by looking for message bubbles
            val messages = extractChatMessages(rootNode, platform)
            
            if (messages.isEmpty()) {
                return
            }
            
            // Get the most recent message (last one in the list)
            val latestMessage = messages.lastOrNull() ?: ""
            
            // Skip if no new message or it's our own reply
            if (latestMessage.isBlank() || latestMessage == lastKnownMessage) {
                return
            }
            
            // Skip if this is our own message (the reply we just sent)
            if (latestMessage.startsWith("Auto-reply:") || 
                latestMessage.contains("I am an AI assistant")) {
                return
            }
            
            // Check for duplicates
            val currentTime = System.currentTimeMillis()
            if (latestMessage == lastProcessedText && 
                (currentTime - lastProcessedTime) < PROCESS_TIMEOUT) {
                return
            }
            
            lastKnownMessage = latestMessage
            lastProcessedText = latestMessage
            lastProcessedTime = currentTime
            
            Log.d(TAG, "New message detected from $platform: $latestMessage")
            
            // Generate and send reply
            scope.launch {
                try {
                    processAndReply(platform, latestMessage, rootNode)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing: ${e.message}")
                }
            }
        } finally {
            rootNode.recycle()
        }
    }
    
    private suspend fun processAndReply(platform: String, messageText: String, rootNode: AccessibilityNodeInfo) {
        // Generate AI response
        val replyMessage = generateReply(messageText)
        
        Log.d(TAG, "Generated reply: $replyMessage")
        
        // Find the text input field and send button
        val inputField = findInputField(rootNode, platform)
        
        if (inputField != null) {
            // Find send button
            val sendButton = findSendButton(rootNode, platform)
            
            if (sendButton != null) {
                // Type the reply and send
                typeAndSendReply(inputField, sendButton, replyMessage)
                updateReplyCounts()
                Log.d(TAG, "Auto-reply sent to $platform")
            } else {
                Log.w(TAG, "Could not find send button for $platform")
                // Try with any clickable button
                tryFindAndSendAny(inputField, rootNode, replyMessage)
            }
            
            inputField.recycle()
        } else {
            Log.w(TAG, "Could not find input field for $platform")
        }
    }
    
    private fun findInputField(rootNode: AccessibilityNodeInfo, platform: String): AccessibilityNodeInfo? {
        // Try common input field IDs based on platform
        val inputIds = when (platform) {
            Constants.PLATFORM_WHATSAPP -> listOf(
                "com.whatsapp:id/entry",
                "com.whatsapp:id/caption",
                "com.whatsapp.w4b:id/entry"
            )
            Constants.PLATFORM_MESSENGER -> listOf(
                "com.facebook.orca:id/qsearch_input",
                "com.facebook.orca:id/search_input",
                "com.facebook.mlite:id/messenger_text_input_field"
            )
            Constants.PLATFORM_TELEGRAM -> listOf(
                "org.telegram.messenger:id/input_field",
                "org.telegram.plus:id/input_field"
            )
            else -> emptyList()
        }
        
        // First try by ID
        for (id in inputIds) {
            val found = rootNode.findAccessibilityNodeInfosByViewId(id)
            if (found.isNotEmpty()) {
                Log.d(TAG, "Found input field by ID: $id")
                return found[0]
            }
        }
        
        // Fallback: find by text field characteristics - look for editable text fields
        return findEditableTextField(rootNode)
    }
    
    private fun findEditableTextField(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Check if this node is an editable text field
        if (node.isEditable && node.isFocusable) {
            return node
        }
        
        // Check for EditText class
        val className = node.className?.toString() ?: ""
        if (className.contains("EditText") || className.contains("TextField")) {
            if (node.isEditable) {
                return node
            }
        }
        
        // Check children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findEditableTextField(child)
            if (result != null) {
                return result
            }
            child.recycle()
        }
        
        return null
    }
    
    private fun findSendButton(rootNode: AccessibilityNodeInfo, platform: String): AccessibilityNodeInfo? {
        // Try common send button IDs
        val sendIds = when (platform) {
            Constants.PLATFORM_WHATSAPP -> listOf(
                "com.whatsapp:id/send",
                "com.whatsapp.w4b:id/send"
            )
            Constants.PLATFORM_MESSENGER -> listOf(
                "com.facebook.orca:id/send_button"
            )
            Constants.PLATFORM_TELEGRAM -> listOf(
                "org.telegram.messenger:id/send_button"
            )
            else -> emptyList()
        }
        
        for (id in sendIds) {
            val found = rootNode.findAccessibilityNodeInfosByViewId(id)
            if (found.isNotEmpty()) {
                Log.d(TAG, "Found send button by ID: $id")
                return found[0]
            }
        }
        
        // Fallback: find button by content description
        val sendDescriptions = listOf("send", "send message", "reply", "🢃")  // arrow down is WhatsApp send
        for (desc in sendDescriptions) {
            val found = rootNode.findAccessibilityNodeInfosByText(desc)
            for (node in found) {
                if (node.isClickable) {
                    return node
                }
                node.recycle()
            }
        }
        
        // Last resort: find send button by location (right side of screen, bottom area)
        return findSendButtonByLocation(rootNode)
    }
    
    private fun findSendButtonByLocation(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        
        // Look for clickable buttons in the bottom-right area of screen
        if (node.isClickable && 
            (node.className?.contains("Button") == true || 
             node.className?.contains("ImageButton") == true)) {
            
            // Check if button is in bottom-right quadrant (typical send button position)
            if (bounds.left > 400 && bounds.top > 800) {
                return node
            }
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findSendButtonByLocation(child)
            if (result != null) {
                return result
            }
        }
        
        return null
    }
    
    private suspend fun typeAndSendReply(
        inputField: AccessibilityNodeInfo, 
        sendButton: AccessibilityNodeInfo, 
        message: String
    ) {
        isTypingReply = true
        
        try {
            // Focus on input field
            inputField.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            delay(100)
            
            // Clear existing text first (if any)
            val arguments = android.os.Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, message)
            inputField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            
            // Wait for text to appear
            delay(500)
            
            // Click send button
            val clicked = sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.d(TAG, "Send button clicked: $clicked")
            
            // Wait a bit after sending
            delay(1000)
            
            Log.d(TAG, "Message sent successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error typing/sending: ${e.message}")
        } finally {
            delay(500)
            isTypingReply = false
        }
    }
    
    private suspend fun tryFindAndSendAny(
        inputField: AccessibilityNodeInfo, 
        rootNode: AccessibilityNodeInfo,
        message: String
    ) {
        // Try to find any clickable element in the bottom area
        val sendButton = findSendButtonByLocation(rootNode)
        
        if (sendButton != null) {
            typeAndSendReply(inputField, sendButton, message)
        } else {
            Log.w(TAG, "No send button found - message typed but cannot send automatically")
            // Just type the message, user will need to tap send
            isTypingReply = false
        }
    }
    
    private fun extractChatMessages(rootNode: AccessibilityNodeInfo, platform: String): List<String> {
        val messages = mutableListOf<String>()
        
        // Different platforms have different UI structures
        when (platform) {
            Constants.PLATFORM_WHATSAPP -> extractWhatsAppMessages(rootNode, messages)
            Constants.PLATFORM_MESSENGER -> extractMessengerMessages(rootNode, messages)
            Constants.PLATFORM_TELEGRAM -> extractTelegramMessages(rootNode, messages)
            else -> extractGenericMessages(rootNode, messages)
        }
        
        return messages
    }
    
    private fun extractWhatsAppMessages(node: AccessibilityNodeInfo, messages: MutableList<String>) {
        // WhatsApp uses specific IDs for message containers
        val messageContainerIds = listOf(
            "android:id/list",
            "com.whatsapp:id/list",
            "com.whatsapp:id/conversations_list"
        )
        
        // Try to find message list first
        var messageList: AccessibilityNodeInfo? = null
        for (id in messageContainerIds) {
            val found = node.findAccessibilityNodeInfosByViewId(id)
            if (found.isNotEmpty()) {
                messageList = found[0]
                break
            }
        }
        
        val searchNode = messageList ?: node
        
        // Find all text views that contain messages
        extractTextFromNode(searchNode, messages, isIncoming = true)
    }
    
    private fun extractMessengerMessages(node: AccessibilityNodeInfo, messages: MutableList<String>) {
        extractTextFromNode(node, messages, isIncoming = true)
    }
    
    private fun extractTelegramMessages(node: AccessibilityNodeInfo, messages: MutableList<String>) {
        extractTextFromNode(node, messages, isIncoming = true)
    }
    
    private fun extractGenericMessages(node: AccessibilityNodeInfo, messages: MutableList<String>) {
        extractTextFromNode(node, messages, isIncoming = true)
    }
    
    private fun extractTextFromNode(
        node: AccessibilityNodeInfo, 
        messages: MutableList<String>,
        isIncoming: Boolean
    ) {
        val text = node.text?.toString()
        val contentDesc = node.contentDescription?.toString()
        
        // Filter out UI elements and get actual message text
        val validText = when {
            !text.isNullOrBlank() && text.length > 3 && 
                !isUIElement(text) -> text.trim()
            !contentDesc.isNullOrBlank() && contentDesc.length > 3 &&
                !isUIElement(contentDesc) -> contentDesc.trim()
            else -> null
        }
        
        if (validText != null) {
            // Avoid duplicates
            if (!messages.contains(validText)) {
                messages.add(validText)
            }
        }
        
        // Recursively check children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            extractTextFromNode(child, messages, isIncoming)
            child.recycle()
        }
    }
    
    private fun isUIElement(text: String): Boolean {
        // Skip common UI elements that aren't messages
        val uiPatterns = listOf(
            "menu", "settings", "search", "call", "video", "attach",
            "emoji", "camera", "microphone", "send", "reply", "forward",
            "delete", "copy", "info", "profile", "status", "online",
            "typing", "last seen", "minutes", "hours", "yesterday",
            "pm", "am", ":", "🢃", "✓", "✓✓"  // WhatsApp status indicators
        )
        
        val lowerText = text.lowercase()
        return uiPatterns.any { lowerText.contains(it) } || 
               text.length < 4 ||
               text.matches(Regex("^[0-9:]+$"))  // Just numbers or time
    }
    
    private suspend fun generateReply(originalMessage: String): String {
        return try {
            aiHelper?.generateResponse(originalMessage) ?: getDefaultReply()
        } catch (e: Exception) {
            Log.e(TAG, "AI generation failed: ${e.message}")
            getDefaultReply()
        }
    }
    
    private fun getDefaultReply(): String {
        val persona = prefs?.getString("ai_persona", "Professional") ?: "Professional"
        return when (persona) {
            "Professional" -> "Thank you for your message. I'll respond shortly."
            "Friendly" -> "Hey! Got your message, will get back to you soon! 😊"
            "Witty" -> "Ah, a message! How delightful! I'll reply soon."
            "Minimal" -> "Got it."
            else -> "Thanks for reaching out!"
        }
    }
    
    private fun updateReplyCounts() {
        prefs?.let { p ->
            // Update total replies
            val totalReplies = p.getInt("total_replies", 0) + 1
            p.edit().putInt("total_replies", totalReplies).apply()
            
            // Update today's replies
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = dateFormat.format(Date())
            val todayKey = "replies_$today"
            val todayReplies = p.getInt(todayKey, 0) + 1
            p.edit().putInt(todayKey, todayReplies).apply()
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Accessibility service destroyed")
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
    
    private fun isPlatformEnabled(platform: String): Boolean {
        val prefs = prefs ?: return false
        return when (platform) {
            Constants.PLATFORM_WHATSAPP -> prefs.getBoolean("platform_whatsapp", false)
            Constants.PLATFORM_MESSENGER -> prefs.getBoolean("platform_messenger", false)
            Constants.PLATFORM_TELEGRAM -> prefs.getBoolean("platform_telegram", false)
            Constants.PLATFORM_FACEBOOK -> prefs.getBoolean("platform_facebook", false)
            Constants.PLATFORM_INSTAGRAM -> prefs.getBoolean("platform_instagram", false)
            else -> false
        }
    }
    
    private fun isInDNDPeriod(): Boolean {
        val prefs = prefs ?: return false
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

    companion object {
        private const val TAG = "AutoReplyAccessSvc"
    }
}

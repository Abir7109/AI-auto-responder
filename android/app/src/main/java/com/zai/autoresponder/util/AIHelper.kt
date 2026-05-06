package com.zai.autoresponder.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * AI Helper class for generating responses using various AI providers
 * Supports: OpenAI, Anthropic (Claude), Groq, Gemini
 * 
 * Now includes RAG-Lite (Retrieval-Augmented Generation) for personalized responses
 * by injecting user profile and knowledge snippets into the prompt.
 */
class AIHelper(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val prefs = context.getSharedPreferences("ai_responder_prefs", Context.MODE_PRIVATE)
    private var database: com.zai.autoresponder.data.AppDatabase? = null

    private fun getDatabase(): com.zai.autoresponder.data.AppDatabase {
        if (database == null) {
            database = com.zai.autoresponder.data.AppDatabase.getInstance(context)
        }
        return database!!
    }

    /**
     * Generate AI response based on incoming message
     * Now uses RAG-Lite approach with user profile and knowledge
     * 
     * @param isFirstMessage true if this is the first message from this contact
     * @param contactName the name of the person messaging
     */
    suspend fun generateResponse(incomingMessage: String, isFirstMessage: Boolean = false, contactName: String = ""): String = withContext(Dispatchers.IO) {
        try {
            // Check for reels - special response
            val messageLower = incomingMessage.lowercase()
            if (messageLower.contains("reel") || messageLower.contains(" reels") || messageLower.contains("reels")) {
                return@withContext "Thanks for sharing! Sir will come soon and see your reels 😊"
            }
            
            val apiKey = prefs.getString("api_key", "") ?: ""
            val persona = prefs.getString("ai_persona", "Professional") ?: "Professional"
            
            if (apiKey.isEmpty()) {
                Log.w(TAG, "No API key configured")
                return@withContext getDefaultResponse(persona)
            }

            // Build RAG-Lite context from user profile and knowledge snippets
            val userContext = buildUserContext(isFirstMessage, contactName, incomingMessage)
            val systemPrompt = buildSystemPrompt(persona, userContext, isFirstMessage, contactName)

            val endpoint = getEndpoint(apiKey)
            val response = when {
                apiKey.startsWith("sk-ant") -> callAnthropic(apiKey, incomingMessage, systemPrompt)
                apiKey.startsWith("gsk_") -> callOpenAICompatible(
                    apiKey, incomingMessage, systemPrompt,
                    "https://api.groq.com/openai/v1/chat/completions",
                    "llama-3.3-70b-versatile"
                )
                apiKey.startsWith("AIza") -> callGemini(apiKey, incomingMessage, systemPrompt)
                apiKey.startsWith("sk-") -> callOpenAICompatible(
                    apiKey, incomingMessage, systemPrompt,
                    "https://api.openai.com/v1/chat/completions",
                    "gpt-4o-mini"
                )
                else -> callOpenAICompatible(
                    apiKey, incomingMessage, systemPrompt,
                    "https://api.openai.com/v1/chat/completions",
                    "gpt-4o-mini"
                )
            }
            
            if (response.isNotEmpty()) response else getDefaultResponse(persona)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating AI response: ${e.message}")
            getDefaultResponse(prefs.getString("ai_persona", "Professional") ?: "Professional")
        }
    }

    /**
     * Extract important information from message and save to Notice Board
     * Detects: reminders, meetings, phone numbers, important requests
     */
    suspend fun checkAndSaveToNoticeBoard(
        message: String,
        contactName: String,
        contactNumber: String? = null
    ) {
        withContext(Dispatchers.IO) {
            try {
                val lowerMessage = message.lowercase()
                var noticeType: String? = null
                var noticeContent: String? = null

                // Check for phone numbers in message
                val phoneRegex = "(\\+?[0-9]{1,3}[-.\\s]?)?(\\()?\\d{3,4}(\\))?[-.\\s]?\\d{3,4}[-.\\s]?\\d{3,4}".toRegex()
                val foundPhone = phoneRegex.find(message)?.value

                // Detect meeting/schedule keywords
                val meetingKeywords = listOf(
                    "meeting", "meet", "schedule", "scheduled", "appointment",
                    "tomorrow", "today", "monday", "tuesday", "wednesday",
                    "thursday", "friday", "saturday", "sunday", "at 1", "at 2",
                    "at 3", "at 4", "at 5", "at 6", "at 7", "at 8", "at 9", "at 10",
                    "at 11", "at 12", "pm", "am", "o'clock", "call me", "call back"
                )
                val reminderKeywords = listOf(
                    "remind me", "reminder", "don't forget", "remember to",
                    "please remember", "important", "urgent", "asap",
                    "when you get", "once you", "get back to me"
                )
                
                // New important categories
                val addressKeywords = listOf("address", "location", "street", "road", "house", "building", "area")
                val emailKeywords = listOf("email", "mail me", "gmail", "send to")
                val dateKeywords = listOf("birthday", "anniversary", "dob", "date of birth", "celebration")
                val taskKeywords = listOf("task", "assignment", "deadline", "project", "work to do", "todo")
                val giftKeywords = listOf("gift", "present", "surprise", "buying", "order")
                val medicalKeywords = listOf("medicine", "doctor", "appointment", "health", "prescription", "hospital", "clinic")
                val financialKeywords = listOf("payment", "money", "transaction", "bank", "account", "transfer", "bill", "invoice", "price", "cost")
                val travelKeywords = listOf("flight", "train", "bus", "ticket", "booking", "travel", "trip", "vacation", "holiday")
                val workKeywords = listOf("office", "client", "boss", "manager", "colleague", "meeting", "report", "presentation")

                when {
                    // Phone number shared
                    foundPhone != null && (lowerMessage.contains("number") ||
                        lowerMessage.contains("call") ||
                        lowerMessage.contains("phone") ||
                        lowerMessage.contains("contact") ||
                        lowerMessage.contains("reach") ||
                        lowerMessage.contains("my number") ||
                        lowerMessage.contains("save")) -> {
                        noticeType = "number_shared"
                        noticeContent = "Phone number shared: $foundPhone"
                    }

                    // Meeting/Schedule detected
                    meetingKeywords.any { lowerMessage.contains(it) } &&
                    (lowerMessage.contains("meet") || lowerMessage.contains("call") ||
                     lowerMessage.contains("schedule") || lowerMessage.contains("when") ||
                     lowerMessage.contains("at") || lowerMessage.contains("tomorrow") ||
                     lowerMessage.contains("today")) -> {
                        noticeType = "meeting"
                        // Extract the relevant part of the message
                        noticeContent = extractMeetingInfo(message)
                    }

                    // Reminder/Call back request
                    reminderKeywords.any { lowerMessage.contains(it) } -> {
                        noticeType = if (lowerMessage.contains("call") || lowerMessage.contains("call back")) {
                            "call_back"
                        } else {
                            "reminder"
                        }
                        noticeContent = extractReminderInfo(message)
                    }

                    // Explicit important request
                    lowerMessage.contains("important") ||
                    lowerMessage.contains("urgent") ||
                    lowerMessage.contains("asap") ||
                    lowerMessage.contains("please remember") -> {
                        noticeType = "important"
                        noticeContent = message.take(200)
                    }
                    
                    // Address detected
                    addressKeywords.any { lowerMessage.contains(it) } -> {
                        noticeType = "address"
                        noticeContent = "Address mentioned: ${message.take(200)}"
                    }
                    
                    // Email detected
                    emailKeywords.any { lowerMessage.contains(it) } && lowerMessage.contains("@") -> {
                        noticeType = "email"
                        val emailRegex = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}".toRegex()
                        val email = emailRegex.find(message)?.value ?: message.take(100)
                        noticeContent = "Email: $email"
                    }
                    
                    // Date/Birthday detected
                    dateKeywords.any { lowerMessage.contains(it) } -> {
                        noticeType = "date"
                        noticeContent = "Important date: ${message.take(200)}"
                    }
                    
                    // Task/Work detected
                    taskKeywords.any { lowerMessage.contains(it) } || workKeywords.any { lowerMessage.contains(it) } -> {
                        noticeType = "task"
                        noticeContent = "Task/Work: ${message.take(200)}"
                    }
                    
                    // Gift detected
                    giftKeywords.any { lowerMessage.contains(it) } -> {
                        noticeType = "gift"
                        noticeContent = "Gift related: ${message.take(200)}"
                    }
                    
                    // Medical/Health detected
                    medicalKeywords.any { lowerMessage.contains(it) } -> {
                        noticeType = "medical"
                        noticeContent = "Medical/Health: ${message.take(200)}"
                    }
                    
                    // Financial detected
                    financialKeywords.any { lowerMessage.contains(it) } -> {
                        noticeType = "financial"
                        noticeContent = "Financial: ${message.take(200)}"
                    }
                    
                    // Travel detected
                    travelKeywords.any { lowerMessage.contains(it) } -> {
                        noticeType = "travel"
                        noticeContent = "Travel: ${message.take(200)}"
                    }

                    else -> {
                        // No important info detected
                    }
                }

                // Save to notice board if we detected something
                if (noticeType != null && noticeContent != null) {
                    saveToNoticeBoard(
                        contactName = contactName,
                        contactNumber = contactNumber ?: foundPhone,
                        noticeType = noticeType,
                        noticeContent = noticeContent,
                        originalMessage = message
                    )
                    Log.d(TAG, "Saved notice to board: $noticeType - $noticeContent")
                } else {
                    // No important info detected, do nothing
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking notice board: ${e.message}")
            }
        }
        Unit
    }

    private fun extractMeetingInfo(message: String): String {
        // Try to extract meeting time/date info
        val keywords = listOf("monday", "tuesday", "wednesday", "thursday", "friday",
            "saturday", "sunday", "tomorrow", "today", "pm", "am", "at ", "o'clock")
        
        for (keyword in keywords) {
            val index = message.lowercase().indexOf(keyword)
            if (index != -1) {
                val start = maxOf(0, index - 30)
                val end = minOf(message.length, index + 50)
                return message.substring(start, end).trim()
            }
        }
        return message.take(100)
    }

    private fun extractReminderInfo(message: String): String {
        // Extract the key reminder info
        val removeWords = listOf("please", "remember to", "remind me", "don't forget", "can you")
        var result = message
        for (word in removeWords) {
            result = result.replace(word, "", ignoreCase = true)
        }
        return result.trim().take(150)
    }

    private suspend fun saveToNoticeBoard(
        contactName: String,
        contactNumber: String?,
        noticeType: String,
        noticeContent: String,
        originalMessage: String
    ) {
        val noticeItem = com.zai.autoresponder.data.entity.NoticeBoardItem(
            id = UUID.randomUUID().toString(),
            contactName = contactName,
            contactNumber = contactNumber,
            noticeType = noticeType,
            noticeContent = noticeContent,
            originalMessage = originalMessage,
            isRead = false,
            isArchived = false
        )
        
        getDatabase().noticeBoardDao().insert(noticeItem)
        
        // Show notification that notice was saved
        try {
            NotificationHelper.showNoticeSavedNotification(context, noticeType, contactName)
        } catch (e: Exception) {
            // Ignore notification errors
        }
    }

    /**
     * Build user context from profile and knowledge snippets
     * This is the RAG-Lite approach - retrieving relevant info and injecting into prompt
     */
    private fun buildUserContext(isFirstMessage: Boolean, contactName: String, incomingMessage: String): String {
        val db = com.zai.autoresponder.data.AppDatabase.getInstance(context)
        
        // Get user profile
        val profile = try {
            kotlinx.coroutines.runBlocking {
                db.userProfileDao().getProfileSync()
            }
        } catch (e: Exception) {
            null
        }

        // Get knowledge snippets
        val snippets = try {
            kotlinx.coroutines.runBlocking {
                db.knowledgeSnippetDao().getAllActiveSnippetsSync()
            }
        } catch (e: Exception) {
            emptyList()
        }

        val contextBuilder = StringBuilder()

        // Always add creator info as default knowledge
        contextBuilder.append("\n=== FACTS ABOUT THE CREATOR (RM ABIR) ===\n")
        contextBuilder.append("- RM Abir is the creator and developer of this AI Auto-Responder app.\n")
        contextBuilder.append("- He's a talented developer who built this app to help people automate their WhatsApp, Messenger, Telegram, Facebook, and Instagram replies using artificial intelligence.\n")
        contextBuilder.append("- If someone asks about RM Abir, the creator, or who made this app, respond that my boss is RM Abir, the talented developer behind this AI Auto-Responder app.\n")
        contextBuilder.append("- For contact inquiries, tell them: My boss is RM Abir. You can reach him on WhatsApp at +8801919069898 or connect with him on Facebook at facebook.com/rmabir or Instagram at instagram.com/rmabir.\n")

        // Add identity info if available
        if (profile != null) {
            contextBuilder.append("\n=== USER IDENTITY ===\n")
            if (profile.name.isNotBlank()) {
                contextBuilder.append("Name: ${profile.name}\n")
            }
            if (profile.profession.isNotBlank()) {
                contextBuilder.append("Profession: ${profile.profession}\n")
            }
            if (profile.bio.isNotBlank()) {
                contextBuilder.append("Bio: ${profile.bio}\n")
            }
            if (profile.hobbies.isNotBlank()) {
                contextBuilder.append("Hobbies: ${profile.hobbies}\n")
            }
            if (profile.location.isNotBlank()) {
                contextBuilder.append("Location: ${profile.location}\n")
            }
            if (profile.age > 0) {
                contextBuilder.append("Age: ${profile.age}\n")
            }
            
            // Only include availability on first message OR if contact asks about it
            val messageLower = incomingMessage.lowercase()
            val availabilityKeywords = listOf("available", "free", "busy", "occupied", "meeting", "when free", "are you free", "what are you doing", "working", "vacation", "leave")
            val isAskingAboutAvailability = availabilityKeywords.any { messageLower.contains(it) }
            
            // Check if this contact has been informed before (for non-first messages)
            val contactedPrefs = context.getSharedPreferences("contact_availability", Context.MODE_PRIVATE)
            val contactKey = contactName.ifBlank { "unknown" }
            val hasBeenInformed = contactedPrefs.getBoolean("informed_$contactKey", false)
            
            // Show availability if:
            // 1. It's the first message from this contact, OR
            // 2. Contact is asking about availability, OR
            // 3. Contact has already been informed (show to remind them)
            val shouldShowAvailability = isFirstMessage || isAskingAboutAvailability || hasBeenInformed
            
            if (profile.availability.isNotBlank() && shouldShowAvailability) {
                contextBuilder.append("Current Availability: ${profile.availability}\n")
                // Mark this contact as informed
                contactedPrefs.edit().putBoolean("informed_$contactKey", true).apply()
            }
        }

        // Add knowledge snippets
        if (snippets.isNotEmpty()) {
            contextBuilder.append("\n=== FACTS ABOUT USER ===\n")
            snippets.forEach { snippet ->
                contextBuilder.append("- ${snippet.content}\n")
            }
        }

        return contextBuilder.toString()
    }

    /**
     * Build system prompt with RAG-Lite context
     * @param isFirstMessage true if this is the first message from this contact
     * @param contactName the name of the person messaging (for personalized greeting)
     */
    private fun buildSystemPrompt(persona: String, userContext: String, isFirstMessage: Boolean = false, contactName: String = ""): String {
        val basePrompt = getSystemPrompt(persona)
        
        // Get profile for custom behavior
        val db = com.zai.autoresponder.data.AppDatabase.getInstance(context)
        val profile = try {
            kotlinx.coroutines.runBlocking {
                db.userProfileDao().getProfileSync()
            }
        } catch (e: Exception) {
            null
        }

        // Get response length setting
        val responseLength = profile?.responseLength ?: "medium"
        val lengthInstructions = when (responseLength) {
            "short" -> "Keep responses very brief (1-2 sentences)."
            "long" -> "Provide detailed responses when needed."
            else -> "Keep responses concise but complete."
        }

        val userName = profile?.name?.ifBlank { "the user" } ?: "the user"
        
        // Track first message using SharedPreferences for persistence across app restarts
        val contactPrefs = context.getSharedPreferences("contact_tracking", Context.MODE_PRIVATE)
        val contactKey = contactName.ifBlank { "unknown" }
        val isActuallyFirst = isFirstMessage || !contactPrefs.getBoolean("messaged_before_$contactKey", false)
        
        // Mark this contact as having messaged before
        contactPrefs.edit().putBoolean("messaged_before_$contactKey", true).apply()
        
        // Only introduce with name on first message or when asked
        val nameIntro = if (isActuallyFirst) {
            """- "Hi! I'm $userName's personal assistant. I'll let him know you messaged!"
- "Hello! This is $userName's assistant. He's here to help."
- "Hi there! As $userName's assistant, I can tell you he's just helping out."""
        } else {
            """- "Hi! Thanks for reaching out. I'll let him know you messaged!"
- "Hello! Got your message, I'll pass it along to him!"
- "Thanks for messaging! I'll notify him right away."""
        }

        return """
$basePrompt

IMPORTANT - YOU ARE A PERSONAL AI ASSISTANT:
You are representing $userName as their PERSONAL AI ASSISTANT/RECEPTIONIST.
When someone messages, reply as their assistant, NOT as an AI.

$userContext

STRICT RULES:
1. Reply in FIRST PERSON ("I", "me", "my") as if you ARE the user's assistant
2. DON'T always mention the user's name - just act as their assistant naturally
3. Only mention $userName's name if it's the FIRST MESSAGE from this person OR if they ask who you are
4. Use casual, natural phrases like "I'll let him know", "Got your message", "Thanks for reaching out"
5. Only mention availability if the person explicitly asks about it
6. Be polite, professional, and helpful
7. $lengthInstructions
8. Never say you're an AI or chatbot - you are their personal assistant

Example replies (use naturally, don't force these):
$nameIntro

Now reply to the following message as $userName's personal assistant:""".trimIndent()
    }

    private fun getEndpoint(apiKey: String): String = when {
        apiKey.startsWith("sk-ant") -> "https://api.anthropic.com/v1/messages"
        apiKey.startsWith("gsk_") -> "https://api.groq.com/openai/v1/chat/completions"
        apiKey.startsWith("AIza") -> "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
        apiKey.startsWith("sk-") -> "https://api.openai.com/v1/chat/completions"
        else -> "https://api.openai.com/v1/chat/completions"
    }

    private fun getSystemPrompt(persona: String): String = when (persona) {
        "Professional" -> "You are a professional assistant. Respond formally and concisely. Keep responses brief and to the point."
        "Friendly" -> "You are a friendly assistant. Respond warmly and casually, like chatting with a friend. Use a conversational tone."
        "Witty" -> "You are a witty assistant. Respond with humor and charm while being helpful. Keep it light and engaging."
        "Minimal" -> "You are a minimal assistant. Respond with only the essential information. Be extremely brief and direct."
        else -> "You are a helpful assistant. Respond naturally and concisely."
    }

    private fun callOpenAICompatible(
        apiKey: String,
        message: String,
        systemPrompt: String,
        endpoint: String,
        model: String
    ): String {
        val fullPrompt = systemPrompt
        
        val jsonBody = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", fullPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", "Reply to this message: \"$message\"")
                })
            })
            put("max_tokens", 150)
            put("temperature", 0.7)
        }

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "API call failed: ${response.code}")
                return@use ""
            }
            
            val responseBody = response.body?.string() ?: ""
            val json = JSONObject(responseBody)
            val choices = json.getJSONArray("choices")
            if (choices.length() > 0) {
                choices.getJSONObject(0).getJSONObject("message").getString("content")
            } else {
                ""
            }
        }
    }

    private fun callAnthropic(apiKey: String, message: String, systemPrompt: String): String {
        val fullPrompt = systemPrompt
        
        val jsonBody = JSONObject().apply {
            put("model", "claude-3-haiku-20240307")
            put("max_tokens", 150)
            put("system", fullPrompt)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", "Reply to this message: \"$message\"")
                })
            })
        }

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "Anthropic API call failed: ${response.code}")
                return@use ""
            }
            
            val responseBody = response.body?.string() ?: ""
            val json = JSONObject(responseBody)
            val content = json.getJSONArray("content")
            if (content.length() > 0) {
                content.getJSONObject(0).getString("text")
            } else {
                ""
            }
        }
    }

    private fun callGemini(apiKey: String, message: String, systemPrompt: String): String {
        val fullPrompt = "$systemPrompt\n\nReply to this message: \"$message\""
        
        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", fullPrompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("maxOutputTokens", 150)
                put("temperature", 0.7)
            })
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "Gemini API call failed: ${response.code}")
                return@use ""
            }
            
            val responseBody = response.body?.string() ?: ""
            val json = JSONObject(responseBody)
            val candidates = json.getJSONArray("candidates")
            if (candidates.length() > 0) {
                val content = candidates.getJSONObject(0).getJSONObject("content")
                val parts = content.getJSONArray("parts")
                if (parts.length() > 0) {
                    parts.getJSONObject(0).getString("text")
                } else {
                    ""
                }
            } else {
                ""
            }
        }
    }

    private fun getDefaultResponse(persona: String): String = when (persona) {
        "Professional" -> "Thank you for your message. I'll respond shortly."
        "Friendly" -> "Hey! Got your message, will get back to you soon! 😊"
        "Witty" -> "Ah, a message! How delightful! I'll reply post-haste."
        "Minimal" -> "Got it."
        else -> "Thanks for reaching out!"
    }

    companion object {
        private const val TAG = "AIHelper"
    }
}

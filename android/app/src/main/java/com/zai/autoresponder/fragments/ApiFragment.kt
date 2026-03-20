package com.zai.autoresponder.fragments

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.zai.autoresponder.R
import kotlinx.coroutines.*
import org.json.JSONArray
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ApiFragment : Fragment() {

    private lateinit var apiKeyInput: EditText
    private lateinit var testMessageInput: EditText
    private lateinit var testAiButton: Button
    private lateinit var testResponseContainer: LinearLayout
    private lateinit var testResponseText: TextView
    private lateinit var testProgressBar: ProgressBar
    
    // API Profile views
    private lateinit var apiProfileContainer: LinearLayout
    private lateinit var apiKeyInputContainer: LinearLayout
    private lateinit var profileNameText: TextView
    private lateinit var profileModelText: TextView
    private lateinit var changeApiText: TextView
    
    // Critical Alerts
    private lateinit var criticalAlertsSwitch: Switch
    private lateinit var urgencyKeywordsContainer: LinearLayout
    private lateinit var urgencyChipGroup: ChipGroup
    
    private lateinit var prefs: SharedPreferences
    private val defaultUrgencyKeywords = listOf("emergency", "help", "urgent", "asap", "911")
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_api, container, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupApiSettings()
    }

    private fun initViews(view: View) {
        apiKeyInput = view.findViewById(R.id.apiKeyInput)
        testMessageInput = view.findViewById(R.id.testMessageInput)
        testAiButton = view.findViewById(R.id.testAiButton)
        testResponseContainer = view.findViewById(R.id.testResponseContainer)
        testResponseText = view.findViewById(R.id.testResponseText)
        testProgressBar = view.findViewById(R.id.testProgressBar)
        
        // API Profile
        apiProfileContainer = view.findViewById(R.id.apiProfileContainer)
        apiKeyInputContainer = view.findViewById(R.id.apiKeyInputContainer)
        profileNameText = view.findViewById(R.id.profileNameText)
        profileModelText = view.findViewById(R.id.profileModelText)
        changeApiText = view.findViewById(R.id.changeApiText)
        
        // Critical Alerts
        criticalAlertsSwitch = view.findViewById(R.id.criticalAlertsSwitch)
        urgencyKeywordsContainer = view.findViewById(R.id.urgencyKeywordsContainer)
        urgencyChipGroup = view.findViewById(R.id.urgencyChipGroup)
    }

    private fun setupApiSettings() {
        prefs = requireContext().getSharedPreferences("ai_responder_prefs", Context.MODE_PRIVATE)
        
        // Check if API is saved and show profile
        updateApiProfileVisibility()
        
        // Load saved settings
        loadSettings()
        
        // Save API button
        view?.findViewById<Button>(R.id.saveApiButton)?.setOnClickListener {
            saveApiSettings()
        }
        
        // Guide button
        view?.findViewById<Button>(R.id.guideButton)?.setOnClickListener {
            showGuideDialog()
        }
        
        // Change API click
        changeApiText.setOnClickListener {
            showApiKeyInput()
        }

        // Critical alerts switch
        criticalAlertsSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("critical_alerts_enabled", isChecked).apply()
            urgencyKeywordsContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
            Toast.makeText(context, if (isChecked) "Critical Alerts Enabled" else "Critical Alerts Disabled", Toast.LENGTH_SHORT).show()
        }

        testAiButton.setOnClickListener {
            testAIResponse()
        }
    }

    private fun updateApiProfileVisibility() {
        val savedApiKey = prefs.getString("api_key", null)
        
        if (savedApiKey != null && savedApiKey.isNotEmpty()) {
            // Show profile, hide input
            apiProfileContainer.visibility = View.VISIBLE
            apiKeyInputContainer.visibility = View.GONE
            
            // Detect provider from API key
            val provider = detectProvider(savedApiKey)
            profileNameText.text = "API Connected"
            profileModelText.text = "$provider - Ready to respond"
        } else {
            // Show input, hide profile
            apiProfileContainer.visibility = View.GONE
            apiKeyInputContainer.visibility = View.VISIBLE
        }
    }
    
    private fun showApiKeyInput() {
        apiProfileContainer.visibility = View.GONE
        apiKeyInputContainer.visibility = View.VISIBLE
    }

    // Auto-detect which AI provider the API key belongs to
    private fun detectProvider(apiKey: String): String {
        return when {
            apiKey.startsWith("sk-ant") -> "Anthropic Claude"
            apiKey.startsWith("gsk_") -> "Groq"
            apiKey.startsWith("AIza") -> "Google Gemini"
            apiKey.startsWith("github_pat") -> "GitHub Models"
            apiKey.startsWith("azure_") -> "Azure OpenAI"
            apiKey.startsWith("sk-") -> "OpenAI"
            else -> "AI Provider"
        }
    }

    private fun getApiUrl(apiKey: String): String {
        return when {
            apiKey.startsWith("sk-ant") -> "https://api.anthropic.com/v1/messages"
            apiKey.startsWith("gsk_") -> "https://api.groq.com/openai/v1/chat/completions"
            apiKey.startsWith("AIza") -> "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
            apiKey.startsWith("sk-") -> "https://api.openai.com/v1/chat/completions"
            else -> "https://api.openai.com/v1/chat/completions"
        }
    }

    private fun getModel(apiKey: String): String {
        return when {
            apiKey.startsWith("sk-ant") -> "claude-3-haiku-20240307"
            apiKey.startsWith("gsk_") -> "llama-3.3-70b-versatile"
            apiKey.startsWith("AIza") -> "gemini-2.0-flash"
            apiKey.startsWith("sk-") -> "gpt-4o-mini"
            else -> "gpt-4o-mini"
        }
    }

    private fun saveApiSettings() {
        val apiKey = apiKeyInput.text.toString().trim()
        
        if (apiKey.isEmpty()) {
            Toast.makeText(context, "Please paste your API key", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Save the API key
        prefs.edit().putString("api_key", apiKey).apply()
        
        // Clear input field
        apiKeyInput.text?.clear()
        
        // Update UI to show profile
        updateApiProfileVisibility()
        
        val provider = detectProvider(apiKey)
        Toast.makeText(context, "Connected to $provider!", Toast.LENGTH_SHORT).show()
    }

    private fun loadSettings() {
        // Load critical alerts
        val criticalAlertsEnabled = prefs.getBoolean("critical_alerts_enabled", false)
        criticalAlertsSwitch.isChecked = criticalAlertsEnabled
        urgencyKeywordsContainer.visibility = if (criticalAlertsEnabled) View.VISIBLE else View.GONE
        
        // Load urgency keywords
        loadUrgencyKeywords()
    }
    
    private fun loadUrgencyKeywords() {
        urgencyChipGroup.removeAllViews()
        val keywordsJson = prefs.getString("urgency_keywords", null)
        val keywords = if (keywordsJson != null) {
            JSONArray(keywordsJson).let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            }
        } else {
            defaultUrgencyKeywords
        }
        
        for (keyword in keywords) {
            addKeywordChip(keyword)
        }
    }
    
    private fun addKeywordChip(keyword: String) {
        val chip = Chip(requireContext()).apply {
            text = keyword
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                removeKeyword(keyword)
            }
        }
        urgencyChipGroup.addView(chip)
    }
    
    private fun removeKeyword(keyword: String) {
        val keywords = mutableListOf<String>()
        for (i in 0 until urgencyChipGroup.childCount) {
            val chip = urgencyChipGroup.getChildAt(i) as? Chip
            chip?.text?.toString()?.let {
                if (it != keyword) keywords.add(it)
            }
        }
        saveKeywords(keywords)
        urgencyChipGroup.removeAllViews()
        keywords.forEach { addKeywordChip(it) }
    }
    
    private fun saveKeywords(keywords: List<String>) {
        val arr = JSONArray()
        keywords.forEach { arr.put(it) }
        prefs.edit().putString("urgency_keywords", arr.toString()).apply()
    }
    
    fun checkForUrgency(message: String): Boolean {
        if (!prefs.getBoolean("critical_alerts_enabled", false)) return false
        
        val keywordsJson = prefs.getString("urgency_keywords", null)
        val keywords = if (keywordsJson != null) {
            JSONArray(keywordsJson).let { arr ->
                (0 until arr.length()).map { arr.getString(it).lowercase() }
            }
        } else {
            defaultUrgencyKeywords
        }
        
        val lowerMessage = message.lowercase()
        return keywords.any { lowerMessage.contains(it) }
    }

    private fun testAIResponse() {
        val message = testMessageInput.text.toString().trim()
        if (message.isEmpty()) {
            Toast.makeText(context, "Please enter a test message", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if API is configured
        val apiKey = prefs.getString("api_key", null)
        
        if (apiKey == null || apiKey.isEmpty()) {
            Toast.makeText(context, "Please connect an API key first", Toast.LENGTH_SHORT).show()
            return
        }

        testAiButton.isEnabled = false
        testAiButton.text = "Generating..."
        testProgressBar.visibility = View.VISIBLE
        testResponseContainer.visibility = View.GONE

        scope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    callAI(apiKey, message)
                }
                
                testResponseContainer.visibility = View.VISIBLE
                testResponseText.text = response
            } catch (e: Exception) {
                testResponseContainer.visibility = View.VISIBLE
                testResponseText.text = "Error: ${e.message}\n\nMake sure your API key is valid."
            } finally {
                testAiButton.isEnabled = true
                testAiButton.text = "Test Response"
                testProgressBar.visibility = View.GONE
            }
        }
    }
    
    // Show the API guide dialog
    private fun showGuideDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_api_guide)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.window?.setDimAmount(0.7f)
        
        dialog.findViewById<View>(R.id.blurryBackground)?.setOnClickListener { 
            dialog.dismiss() 
        }
        dialog.findViewById<ImageButton>(R.id.closeButton)?.setOnClickListener { 
            dialog.dismiss() 
        }
        
        dialog.show()
    }
    
    private fun callAI(apiKey: String, userMessage: String): String {
        val provider = detectProvider(apiKey)
        
        return when {
            apiKey.startsWith("sk-ant") -> callAnthropic(apiKey, userMessage)
            apiKey.startsWith("gsk_") -> callOpenAICompatible(apiKey, userMessage, "https://api.groq.com/openai/v1/chat/completions", "llama-3.3-70b-versatile")
            apiKey.startsWith("AIza") -> callGemini(apiKey, userMessage)
            apiKey.startsWith("sk-") -> callOpenAICompatible(apiKey, userMessage, "https://api.openai.com/v1/chat/completions", "gpt-4o-mini")
            else -> callOpenAICompatible(apiKey, userMessage, "https://api.openai.com/v1/chat/completions", "gpt-4o-mini")
        }
    }
    
    private fun callOpenAICompatible(apiKey: String, userMessage: String, apiUrl: String, model: String): String {
        val url = URL(apiUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Authorization", "Bearer $apiKey")
        connection.doOutput = true
        
        val requestBody = """
            {
                "model": "$model",
                "messages": [{"role": "user", "content": "$userMessage"}],
                "max_tokens": 500
            }
        """.trimIndent()
        
        OutputStreamWriter(connection.outputStream).use { writer ->
            writer.write(requestBody)
        }
        
        val responseCode = connection.responseCode
        if (responseCode == 200) {
            val response = connection.inputStream.bufferedReader().readText()
            val jsonResponse = org.json.JSONObject(response)
            val choices = jsonResponse.getJSONArray("choices")
            if (choices.length() > 0) {
                return choices.getJSONObject(0).getJSONObject("message").getString("content")
            }
            return "No response generated"
        } else {
            val errorResponse = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
            throw Exception("API Error ($responseCode): $errorResponse")
        }
    }
    
    private fun callAnthropic(apiKey: String, userMessage: String): String {
        val url = URL("https://api.anthropic.com/v1/messages")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("x-api-key", apiKey)
        connection.setRequestProperty("anthropic-version", "2023-06-01")
        connection.doOutput = true
        
        val requestBody = """
            {
                "model": "claude-3-haiku-20240307",
                "max_tokens": 500,
                "messages": [{"role": "user", "content": "$userMessage"}]
            }
        """.trimIndent()
        
        OutputStreamWriter(connection.outputStream).use { writer ->
            writer.write(requestBody)
        }
        
        val responseCode = connection.responseCode
        if (responseCode == 200) {
            val response = connection.inputStream.bufferedReader().readText()
            val jsonResponse = org.json.JSONObject(response)
            return jsonResponse.getJSONObject("content").getString("text")
        } else {
            val errorResponse = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
            throw Exception("Claude API Error ($responseCode): $errorResponse")
        }
    }
    
    private fun callGemini(apiKey: String, userMessage: String): String {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        
        val requestBody = """
            {
                "contents": [{"parts": [{"text": "$userMessage"}]}]
            }
        """.trimIndent()
        
        OutputStreamWriter(connection.outputStream).use { writer ->
            writer.write(requestBody)
        }
        
        val responseCode = connection.responseCode
        if (responseCode == 200) {
            val response = connection.inputStream.bufferedReader().readText()
            val jsonResponse = org.json.JSONObject(response)
            val candidates = jsonResponse.getJSONArray("candidates")
            if (candidates.length() > 0) {
                return candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
            }
            return "No response generated"
        } else {
            val errorResponse = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
            throw Exception("Gemini API Error ($responseCode): $errorResponse")
        }
    }
}

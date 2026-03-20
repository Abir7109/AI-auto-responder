package com.zai.autoresponder.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.zai.autoresponder.R
import org.json.JSONArray
import org.json.JSONObject

class RulesFragment : Fragment() {

    private lateinit var triggerInput: EditText
    private lateinit var responseInput: EditText
    private lateinit var responseTypeQuick: TextView
    private lateinit var responseTypeAI: TextView
    private lateinit var quickReplyContainer: LinearLayout
    private lateinit var createRuleButton: Button
    private lateinit var rulesListContainer: LinearLayout
    
    private lateinit var prefs: SharedPreferences
    private var responseType = "quick"
    
    // Store rules
    private val rules = mutableListOf<Rule>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_rules, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRules()
    }

    private fun initViews(view: View) {
        triggerInput = view.findViewById(R.id.triggerInput)
        responseInput = view.findViewById(R.id.responseInput)
        responseTypeQuick = view.findViewById(R.id.responseTypeQuick)
        responseTypeAI = view.findViewById(R.id.responseTypeAI)
        quickReplyContainer = view.findViewById(R.id.quickReplyContainer)
        createRuleButton = view.findViewById(R.id.createRuleButton)
        rulesListContainer = view.findViewById(R.id.rulesListContainer)
    }

    private fun setupRules() {
        prefs = requireContext().getSharedPreferences("ai_responder_prefs", Context.MODE_PRIVATE)
        
        // Load saved rules
        loadRules()
        
        responseTypeQuick.setOnClickListener {
            responseType = "quick"
            responseTypeQuick.setBackgroundResource(R.drawable.response_type_selected_background)
            responseTypeQuick.setTextColor(resources.getColor(R.color.primary_foreground, null))
            responseTypeAI.setBackgroundResource(R.drawable.glass_button_background)
            responseTypeAI.setTextColor(resources.getColor(R.color.foreground, null))
            quickReplyContainer.visibility = View.VISIBLE
        }

        responseTypeAI.setOnClickListener {
            responseType = "ai"
            responseTypeAI.setBackgroundResource(R.drawable.response_type_selected_background)
            responseTypeAI.setTextColor(resources.getColor(R.color.primary_foreground, null))
            responseTypeQuick.setBackgroundResource(R.drawable.glass_button_background)
            responseTypeQuick.setTextColor(resources.getColor(R.color.foreground, null))
            quickReplyContainer.visibility = View.GONE
        }

        createRuleButton.setOnClickListener {
            createRuleButton.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            createRule()
        }
    }
    
    private fun loadRules() {
        rules.clear()
        
        val rulesJson = prefs.getString("saved_rules", "[]") ?: "[]"
        val rulesArray = JSONArray(rulesJson)
        
        for (i in 0 until rulesArray.length()) {
            val ruleObj = rulesArray.getJSONObject(i)
            rules.add(Rule(
                trigger = ruleObj.getString("trigger"),
                response = ruleObj.getString("response"),
                type = ruleObj.getString("type")
            ))
        }
        
        updateRulesList()
    }
    
    private fun saveRules() {
        val rulesArray = JSONArray()
        
        for (rule in rules) {
            val ruleObj = JSONObject()
            ruleObj.put("trigger", rule.trigger)
            ruleObj.put("response", rule.response)
            ruleObj.put("type", rule.type)
            rulesArray.put(ruleObj)
        }
        
        prefs.edit().putString("saved_rules", rulesArray.toString()).apply()
    }
    
    private fun updateRulesList() {
        rulesListContainer.removeAllViews()
        
        if (rules.isEmpty()) {
            val emptyText = TextView(context).apply {
                text = "No rules yet. Create your first rule above."
                setTextColor(resources.getColor(R.color.text_secondary, null))
                textSize = 14f
                setPadding(32, 32, 32, 32)
            }
            rulesListContainer.addView(emptyText)
            return
        }
        
        for ((index, rule) in rules.withIndex()) {
            val ruleView = layoutInflater.inflate(R.layout.item_rule, rulesListContainer, false)
            
            val ruleTrigger = ruleView.findViewById<TextView>(R.id.ruleTrigger)
            val ruleResponse = ruleView.findViewById<TextView>(R.id.ruleResponse)
            val ruleType = ruleView.findViewById<TextView>(R.id.ruleType)
            val deleteButton = ruleView.findViewById<ImageButton>(R.id.deleteRuleButton)
            
            ruleTrigger.text = "Trigger: ${rule.trigger}"
            ruleResponse.text = "Response: ${if (rule.response.length > 50) rule.response.substring(0, 50) + "..." else rule.response}"
            ruleType.text = if (rule.type == "ai") "🤖 AI Response" else "⚡ Quick Reply"
            
            deleteButton.setOnClickListener {
                deleteRule(index)
            }
            
            rulesListContainer.addView(ruleView)
        }
    }
    
    private fun deleteRule(index: Int) {
        rules.removeAt(index)
        saveRules()
        updateRulesList()
        Toast.makeText(context, "Rule deleted", Toast.LENGTH_SHORT).show()
    }
    
    fun shouldAllowContact(contactName: String): Boolean {
        // Contact filtering has been removed - allow all contacts
        return true
    }

    private fun createRule() {
        val trigger = triggerInput.text.toString().trim()
        val response = responseInput.text.toString().trim()

        if (trigger.isEmpty()) {
            Toast.makeText(context, "Please enter a trigger", Toast.LENGTH_SHORT).show()
            return
        }

        if (responseType == "quick" && response.isEmpty()) {
            Toast.makeText(context, "Please enter a response", Toast.LENGTH_SHORT).show()
            return
        }

        // Add new rule
        val newRule = Rule(
            trigger = trigger,
            response = if (responseType == "quick") response else "AI Response",
            type = responseType
        )
        rules.add(newRule)
        
        // Save to preferences
        saveRules()
        
        // Update the list
        updateRulesList()
        
        // Clear inputs
        triggerInput.text?.clear()
        responseInput.text?.clear()
        
        Toast.makeText(context, "Rule created successfully!", Toast.LENGTH_SHORT).show()
    }
    
    // Data class for Rule
    data class Rule(
        val trigger: String,
        val response: String,
        val type: String // "quick" or "ai"
    )
}

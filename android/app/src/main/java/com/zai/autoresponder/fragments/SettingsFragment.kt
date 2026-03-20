package com.zai.autoresponder.fragments

import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.fragment.app.Fragment
import com.zai.autoresponder.R
import java.text.SimpleDateFormat
import java.util.*

class SettingsFragment : Fragment() {

    private lateinit var personaSpinner: Spinner
    private lateinit var delaySeekBar: SeekBar
    private lateinit var delayValueText: TextView
    
    // DND Mode
    private lateinit var dndSwitch: Switch
    private lateinit var startTimeText: TextView
    private lateinit var endTimeText: TextView
    private lateinit var startTimeContainer: LinearLayout
    private lateinit var endTimeContainer: LinearLayout
    private lateinit var timeSelectionContainer: LinearLayout
    private lateinit var dndStatusText: TextView
    
    private lateinit var prefs: SharedPreferences
    
    private var startHour = 9  // 9 AM default
    private var startMinute = 0
    private var endHour = 22  // 10 PM default
    private var endMinute = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupSettings()
    }

    private fun initViews(view: View) {
        personaSpinner = view.findViewById(R.id.personaSpinner)
        delaySeekBar = view.findViewById(R.id.delaySeekBar)
        delayValueText = view.findViewById(R.id.delayValueText)
        
        // DND Mode views
        dndSwitch = view.findViewById(R.id.dndSwitch)
        startTimeText = view.findViewById(R.id.startTimeText)
        endTimeText = view.findViewById(R.id.endTimeText)
        startTimeContainer = view.findViewById(R.id.startTimeContainer)
        endTimeContainer = view.findViewById(R.id.endTimeContainer)
        timeSelectionContainer = view.findViewById(R.id.timeSelectionContainer)
        dndStatusText = view.findViewById(R.id.dndStatusText)
    }

    private fun setupSettings() {
        prefs = requireContext().getSharedPreferences("ai_responder_prefs", Context.MODE_PRIVATE)
        
        // Setup AI Persona spinner
        setupPersonaSpinner()
        
        // Load DND settings
        loadDNDSettings()
        
        // Setup DND switch listener
        dndSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dnd_enabled", isChecked).apply()
            updateDNDUI()
            Toast.makeText(context, if (isChecked) "DND Mode Enabled" else "DND Mode Disabled", Toast.LENGTH_SHORT).show()
        }
        
        // Setup time pickers
        startTimeContainer.setOnClickListener {
            showTimePicker(true)
        }
        
        endTimeContainer.setOnClickListener {
            showTimePicker(false)
        }
        
        delaySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                delayValueText.text = "$progress seconds"
                prefs.edit().putInt("reply_delay", progress).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        view?.findViewById<Button>(R.id.tileButton)?.setOnClickListener {
            view?.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            requestAddTile()
        }

        view?.findViewById<Button>(R.id.permissionButton)?.setOnClickListener {
            view?.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            requestNotificationPermission()
        }

        // Setup About button
        view?.findViewById<Button>(R.id.aboutButton)?.setOnClickListener {
            view?.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            showAboutDialog()
        }
    }
    
    private fun setupPersonaSpinner() {
        val personas = arrayOf("Professional", "Friendly", "Witty", "Minimal")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            personas
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        personaSpinner.adapter = adapter
        
        // Load saved persona
        val savedPersona = prefs.getString("ai_persona", "Professional")
        val personaIndex = personas.indexOf(savedPersona)
        if (personaIndex >= 0) {
            personaSpinner.setSelection(personaIndex)
        }
        
        // Save persona when selection changes
        personaSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit().putString("ai_persona", personas[position]).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun loadDNDSettings() {
        val dndEnabled = prefs.getBoolean("dnd_enabled", false)
        startHour = prefs.getInt("dnd_start_hour", 9)
        startMinute = prefs.getInt("dnd_start_minute", 0)
        endHour = prefs.getInt("dnd_end_hour", 22)
        endMinute = prefs.getInt("dnd_end_minute", 0)
        
        dndSwitch.isChecked = dndEnabled
        updateTimeTexts()
        updateDNDUI()
    }
    
    private fun updateTimeTexts() {
        startTimeText.text = formatTime(startHour, startMinute)
        endTimeText.text = formatTime(endHour, endMinute)
    }
    
    private fun formatTime(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return format.format(calendar.time)
    }
    
    private fun updateDNDUI() {
        val isEnabled = dndSwitch.isChecked
        timeSelectionContainer.visibility = if (isEnabled) View.VISIBLE else View.GONE
        
        if (isEnabled) {
            val isWithinSchedule = isWithinSchedule()
            dndStatusText.visibility = View.VISIBLE
            if (isWithinSchedule) {
                dndStatusText.text = "🤖 Bot is active during this schedule"
                dndStatusText.setTextColor(resources.getColor(R.color.accent_sand, null))
            } else {
                dndStatusText.text = "😴 Bot is sleeping (outside schedule)"
                dndStatusText.setTextColor(resources.getColor(R.color.text_secondary, null))
            }
        } else {
            dndStatusText.visibility = View.GONE
        }
    }
    
    fun isWithinSchedule(): Boolean {
        if (!prefs.getBoolean("dnd_enabled", false)) return true
        
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        
        val currentTime = currentHour * 60 + currentMinute
        val startTime = startHour * 60 + startMinute
        val endTime = endHour * 60 + endMinute
        
        return if (startTime <= endTime) {
            currentTime in startTime until endTime
        } else {
            // Crosses midnight
            currentTime >= startTime || currentTime < endTime
        }
    }
    
    private fun showTimePicker(isStartTime: Boolean) {
        val hour = if (isStartTime) startHour else endHour
        val minute = if (isStartTime) startMinute else endMinute
        
        TimePickerDialog(
            requireContext(),
            R.style.Theme_AIAutoResponder_TimePicker,
            { _, selectedHour, selectedMinute ->
                if (isStartTime) {
                    startHour = selectedHour
                    startMinute = selectedMinute
                    prefs.edit()
                        .putInt("dnd_start_hour", selectedHour)
                        .putInt("dnd_start_minute", selectedMinute)
                        .apply()
                } else {
                    endHour = selectedHour
                    endMinute = selectedMinute
                    prefs.edit()
                        .putInt("dnd_end_hour", selectedHour)
                        .putInt("dnd_end_minute", selectedMinute)
                        .apply()
                }
                updateTimeTexts()
                updateDNDUI()
            },
            hour,
            minute,
            false
        ).show()
    }

    private fun showAboutDialog() {
        val dialog = Dialog(requireContext(), R.style.Theme_AIAutoResponder_Dialog)
        dialog.setContentView(R.layout.dialog_about)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Apply scale-in animation
        val scaleInAnimation = AnimationUtils.loadAnimation(context, R.anim.scale_in)
        dialog.window?.decorView?.startAnimation(scaleInAnimation)

        // Apply float animation to the app logo
        val floatAnimation = AnimationUtils.loadAnimation(context, R.anim.float_animation)
        dialog.findViewById<ImageView>(R.id.appLogo)?.startAnimation(floatAnimation)

        // Close button
        dialog.findViewById<ImageButton>(R.id.closeButton)?.setOnClickListener {
            dialog.dismiss()
        }

        // Email button
        dialog.findViewById<FrameLayout>(R.id.emailButton)?.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:rahikulmakhtum147@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "AI Auto-Responder")
            }
            startActivity(Intent.createChooser(emailIntent, "Send Email"))
        }

        // WhatsApp button
        dialog.findViewById<FrameLayout>(R.id.whatsappButton)?.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/"))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
            }
        }

        // Facebook button
        dialog.findViewById<FrameLayout>(R.id.facebookButton)?.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://facebook.com"))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Facebook not installed", Toast.LENGTH_SHORT).show()
            }
        }

        // Instagram button
        dialog.findViewById<FrameLayout>(R.id.instagramButton)?.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com"))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Instagram not installed", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun requestAddTile() {
        try {
            // Open Quick Settings tiles settings
            val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
            startActivity(intent)
            Toast.makeText(context, "Go to Display > Quick Settings to add 'AI Responder' tile", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open Settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        }
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = android.provider.Settings.Secure.getString(
            requireContext().contentResolver,
            android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains("com.zai.autoresponder") == true
    }
    
    private fun requestAccessibilityPermission() {
        val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }
}

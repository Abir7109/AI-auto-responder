package com.zai.autoresponder.fragments

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.zai.autoresponder.MainActivity
import com.zai.autoresponder.R
import com.zai.autoresponder.data.AppDatabase
import com.zai.autoresponder.data.entity.KnowledgeSnippet
import com.zai.autoresponder.data.entity.NoticeBoardItem
import com.zai.autoresponder.data.entity.UserProfile
import com.zai.autoresponder.databinding.DialogAddSnippetBinding
import com.zai.autoresponder.databinding.FragmentBrainBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

class BrainFragment : Fragment() {

    private var _binding: FragmentBrainBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: AppDatabase
    private lateinit var adapter: KnowledgeSnippetAdapter
    private lateinit var noticeAdapter: NoticeBoardAdapter

    // Pre-defined availability options
    private val availabilityPresets = mapOf(
        R.id.chipAvailable to "Available",
        R.id.chipBusy to "Busy",
        R.id.chipMeeting to "In a meeting",
        R.id.chipWorking to "Working remotely"
    )



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = (requireActivity() as MainActivity).getDatabase()

        setupRecyclerView()
        setupClickListeners()
        setupAvailabilityChips()
        observeData()
    }

    private fun setupRecyclerView() {
        adapter = KnowledgeSnippetAdapter { snippet ->
            // Delete snippet
            lifecycleScope.launch {
                database.knowledgeSnippetDao().delete(snippet)
                Toast.makeText(requireContext(), "Fact deleted", Toast.LENGTH_SHORT).show()
            }
        }

        binding.rvSnippets.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@BrainFragment.adapter
        }

        // Setup Notice Board RecyclerView
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

        binding.rvNotices.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@BrainFragment.noticeAdapter
        }
    }

    private fun setupClickListeners() {
        // Save Profile Button
        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }

        // Edit Profile Button (on profile card)
        binding.editProfileBtn.setOnClickListener {
            showProfileForm()
        }

        // Entire profile card is clickable
        binding.cardProfile.setOnClickListener {
            showProfileForm()
        }

        // Cancel Edit Button
        binding.cancelEditBtn.setOnClickListener {
            // Reload profile data and hide form
            observeData()
        }



        // Save Behavior Button
        binding.btnSaveBehavior.setOnClickListener {
            saveBehavior()
        }

        // Add Snippet Button
        binding.btnAddSnippet.setOnClickListener {
            showAddSnippetDialog()
        }

        // Response length chips
        binding.chipGroupLength.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val selectedId = checkedIds[0]
                val length = when (selectedId) {
                    R.id.chipShort -> "short"
                    R.id.chipMedium -> "medium"
                    R.id.chipLong -> "long"
                    else -> "medium"
                }
                lifecycleScope.launch {
                    database.userProfileDao().updateResponseLength(length)
                }
            }
        }
    }



    private fun setupAvailabilityChips() {
        // Set up preset availability chips
        for ((chipId, text) in availabilityPresets) {
            val chip = binding.root.findViewById<Chip>(chipId)
            chip?.setOnClickListener {
                binding.etAvailability.setText(text)
                // Clear custom chip selection when preset is chosen
                binding.chipCustomAvailability.isChecked = false
            }
        }

        // Custom availability chip
        binding.chipCustomAvailability.setOnClickListener {
            // Focus on the custom input field
            binding.etAvailability.requestFocus()
        }
    }

    private fun observeData() {
        // Observe profile
        lifecycleScope.launch {
            database.userProfileDao().getProfile().collectLatest { profile ->
                if (profile != null) {
                    updateProfileUI(profile)
                } else {
                    // Create default profile
                    database.userProfileDao().insertOrUpdate(UserProfile())
                    // Add default creator knowledge snippets
                    addDefaultCreatorSnippets(database)
                }
            }
        }

        // Observe snippets
        lifecycleScope.launch {
            database.knowledgeSnippetDao().getAllActiveSnippets().collectLatest { snippets ->
                adapter.submitList(snippets)
                binding.tvSnippetCount.text = snippets.size.toString()
                binding.emptyState.visibility = if (snippets.isEmpty()) View.VISIBLE else View.GONE
                binding.rvSnippets.visibility = if (snippets.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        // Observe Notice Board
        lifecycleScope.launch {
            database.noticeBoardDao().getAllNotices().collectLatest { notices ->
                noticeAdapter.submitList(notices)
                binding.noticeEmptyState.visibility = if (notices.isEmpty()) View.VISIBLE else View.GONE
                binding.rvNotices.visibility = if (notices.isEmpty()) View.GONE else View.VISIBLE
                binding.tvNoticeCount.text = if (notices.isEmpty()) "No notices" else "${notices.size} notice${if (notices.size > 1) "s" else ""}"
            }
        }
    }

    private fun updateProfileUI(profile: UserProfile) {
        // Check if profile is complete
        val isProfileComplete = profile.name.isNotBlank() && profile.profession.isNotBlank()

        // Update profile card
        binding.tvProfileStatus.text = if (isProfileComplete) "Tap to edit" else "Tap to set up"
        binding.cardName.text = if (isProfileComplete) profile.name else "Set up your profile"
        
        // Fill form fields
        binding.etName.setText(profile.name)
        binding.etProfession.setText(profile.profession)
        binding.etBio.setText(profile.bio)
        binding.etHobbies.setText(profile.hobbies)
        binding.etLocation.setText(profile.location)
        binding.etAge.setText(if (profile.age > 0) profile.age.toString() else "")
        binding.etAvailability.setText(profile.availability)

        // Set avatar initial
        val initial = if (profile.name.isNotBlank()) profile.name.first().uppercase() else "?"
        binding.avatarInitial.text = initial

        // Update response length chips
        when (profile.responseLength) {
            "short" -> binding.chipShort.isChecked = true
            "medium" -> binding.chipMedium.isChecked = true
            "long" -> binding.chipLong.isChecked = true
            else -> binding.chipMedium.isChecked = true
        }

        // Update availability chip selection
        updateAvailabilityChipSelection(profile.availability)
    }

    private fun showProfileForm() {
        binding.profileFormContainer.visibility = View.VISIBLE
    }

    private fun updateAvailabilityChipSelection(availability: String) {
        // Clear all chips first
        binding.chipAvailable.isChecked = false
        binding.chipBusy.isChecked = false
        binding.chipMeeting.isChecked = false
        binding.chipWorking.isChecked = false
        binding.chipCustomAvailability.isChecked = false

        // Check if current availability matches a preset
        val matchedPreset = availabilityPresets.entries.find { it.value.equals(availability, ignoreCase = true) }
        
        if (matchedPreset != null) {
            val chip = binding.root.findViewById<Chip>(matchedPreset.key)
            chip?.isChecked = true
        } else if (availability.isNotBlank()) {
            // Custom availability - check the custom chip
            binding.chipCustomAvailability.isChecked = true
        }
    }

    private fun saveProfile() {
        val name = binding.etName.text.toString().trim()
        val profession = binding.etProfession.text.toString().trim()
        val bio = binding.etBio.text.toString().trim()
        val hobbies = binding.etHobbies.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()
        val ageStr = binding.etAge.text.toString().trim()
        val age = ageStr.toIntOrNull() ?: 0

        // Validate
        if (name.isBlank()) {
            Toast.makeText(requireContext(), "Please enter your name", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val profile = database.userProfileDao().getProfileSync() ?: UserProfile()
            
            val updatedProfile = profile.copy(
                name = name,
                profession = profession,
                bio = bio,
                hobbies = hobbies,
                location = location,
                age = age,
                updatedAt = System.currentTimeMillis()
            )
            
            database.userProfileDao().insertOrUpdate(updatedProfile)
            
            // Hide the form
            binding.profileFormContainer.visibility = View.GONE
            
            // Update profile card display
            updateProfileCard(updatedProfile)
            
            Toast.makeText(requireContext(), "Profile saved!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateProfileCard(profile: UserProfile) {
        // Update avatar initial
        if (profile.name.isNotBlank()) {
            binding.avatarInitial.text = profile.name.first().uppercase()
        }
        
        // Update name and status
        binding.cardName.text = if (profile.name.isNotBlank()) profile.name else "Set up your profile"
        binding.tvProfileStatus.text = if (profile.profession.isNotBlank()) {
            profile.profession
        } else if (profile.availability.isNotBlank()) {
            profile.availability
        } else {
            "Tap to customize"
        }
    }

    private fun saveBehavior() {
        val availability = binding.etAvailability.text.toString().trim()
        val length = when (binding.chipGroupLength.checkedChipId) {
            R.id.chipShort -> "short"
            R.id.chipLong -> "long"
            else -> "medium"
        }

        lifecycleScope.launch {
            val profile = database.userProfileDao().getProfileSync() ?: UserProfile()
            
            val updatedProfile = profile.copy(
                availability = availability,
                responseLength = length,
                updatedAt = System.currentTimeMillis()
            )
            
            database.userProfileDao().insertOrUpdate(updatedProfile)
            
            // Show "Active" status indicator
            binding.tvBehaviorStatus.visibility = View.VISIBLE
            
            Toast.makeText(requireContext(), "Behavior saved and active!", Toast.LENGTH_SHORT).show()
            
            // Update profile card with new availability
            updateProfileCard(updatedProfile)
        }
    }

    private fun showAddSnippetDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        
        val dialogBinding = DialogAddSnippetBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnAdd.setOnClickListener {
            val keyword = dialogBinding.etKeyword.text.toString().trim()
            val content = dialogBinding.etContent.text.toString().trim()
            
            if (keyword.isBlank() || content.isBlank()) {
                Toast.makeText(requireContext(), "Please fill in both fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val category = when (dialogBinding.chipGroupCategory.checkedChipId) {
                R.id.chipWork -> "work"
                R.id.chipPersonal -> "personal"
                R.id.chipAvailability -> "availability"
                else -> "general"
            }

            val snippet = KnowledgeSnippet(
                id = UUID.randomUUID().toString(),
                keyword = keyword,
                content = content,
                category = category,
                isActive = true
            )

            lifecycleScope.launch {
                database.knowledgeSnippetDao().insert(snippet)
                Toast.makeText(requireContext(), "Fact added!", Toast.LENGTH_SHORT).show()
            }

            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Add default creator knowledge snippets when app is first launched
     */
    private suspend fun addDefaultCreatorSnippets(database: AppDatabase) {
        val creatorSnippets = listOf(
            KnowledgeSnippet(
                id = "creator_rm_abir",
                keyword = "RM ABIR",
                content = "RM Abir is the creator and developer of this AI Auto-Responder app. He's a talented developer who built this app to help people automate their WhatsApp, Messenger, Telegram, Facebook, and Instagram replies using artificial intelligence.",
                category = "creator",
                isActive = true
            ),
            KnowledgeSnippet(
                id = "creator_who",
                keyword = "who created you",
                content = "This app was created by RM Abir. He's an amazing developer who built this AI-powered auto-responder to help automate messaging across multiple platforms.",
                category = "creator",
                isActive = true
            ),
            KnowledgeSnippet(
                id = "creator_ai",
                keyword = "creator of this ai",
                content = "The AI in this app was developed by RM Abir. He's the founder and developer of this wonderful auto-responder app.",
                category = "creator",
                isActive = true
            ),
            KnowledgeSnippet(
                id = "social_media",
                keyword = "social media",
                content = "You can connect with RM Abir on:\n📱 Facebook: facebook.com/rmabir\n📸 Instagram: instagram.com/rmabir\n💬 WhatsApp: +1 234 567 890",
                category = "creator",
                isActive = true
            ),
            KnowledgeSnippet(
                id = "contact_info",
                keyword = "contact",
                content = "To contact RM Abir:\n📱 Facebook: facebook.com/rmabir\n📸 Instagram: instagram.com/rmabir\n💬 WhatsApp: +1 234 567 890",
                category = "creator",
                isActive = true
            )
        )

        for (snippet in creatorSnippets) {
            database.knowledgeSnippetDao().insert(snippet)
        }
    }
}

/**
 * Adapter for knowledge snippets RecyclerView
 */
class KnowledgeSnippetAdapter(
    private val onDeleteClick: (KnowledgeSnippet) -> Unit
) : androidx.recyclerview.widget.ListAdapter<KnowledgeSnippet, KnowledgeSnippetAdapter.ViewHolder>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<KnowledgeSnippet>() {
        override fun areItemsTheSame(oldItem: KnowledgeSnippet, newItem: KnowledgeSnippet) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: KnowledgeSnippet, newItem: KnowledgeSnippet) = oldItem == newItem
    }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = com.zai.autoresponder.databinding.ItemKnowledgeSnippetBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: com.zai.autoresponder.databinding.ItemKnowledgeSnippetBinding
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

        fun bind(snippet: KnowledgeSnippet) {
            binding.tvKeyword.text = snippet.keyword.uppercase()
            binding.tvContent.text = snippet.content

            // Set category icon
            val icon = when (snippet.category) {
                "work" -> "💼"
                "personal" -> "🏠"
                "availability" -> "🕐"
                else -> "📌"
            }
            binding.tvCategoryIcon.text = icon

            binding.btnDelete.setOnClickListener {
                onDeleteClick(snippet)
            }
        }
    }
}

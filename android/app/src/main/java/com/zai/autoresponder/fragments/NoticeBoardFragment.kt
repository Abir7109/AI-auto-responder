package com.zai.autoresponder.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.zai.autoresponder.MainActivity
import com.zai.autoresponder.R
import com.zai.autoresponder.data.AppDatabase
import com.zai.autoresponder.data.entity.NoticeBoardItem
import com.zai.autoresponder.databinding.FragmentNoticeBoardBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NoticeBoardFragment : Fragment() {

    private var _binding: FragmentNoticeBoardBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: AppDatabase
    private lateinit var adapter: NoticeBoardAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoticeBoardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = (requireActivity() as MainActivity).getDatabase()

        setupRecyclerView()
        observeData()
    }

    private fun setupRecyclerView() {
        adapter = NoticeBoardAdapter(
            onItemClick = { notice ->
                // Mark as read
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
            adapter = this@NoticeBoardFragment.adapter
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            database.noticeBoardDao().getAllNotices().collectLatest { notices ->
                adapter.submitList(notices)
                binding.emptyState.visibility = if (notices.isEmpty()) View.VISIBLE else View.GONE
                binding.rvNotices.visibility = if (notices.isEmpty()) View.GONE else View.VISIBLE
                binding.tvNoticeCount.text = notices.size.toString()
            }
        }
        
        // Observe unread count
        lifecycleScope.launch {
            database.noticeBoardDao().getUnreadCount().collectLatest { count ->
                binding.tvUnreadCount.text = if (count > 0) "$count unread" else "All read"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * Adapter for Notice Board RecyclerView
 */
class NoticeBoardAdapter(
    private val onItemClick: (NoticeBoardItem) -> Unit,
    private val onArchiveClick: (NoticeBoardItem) -> Unit,
    private val onDeleteClick: (NoticeBoardItem) -> Unit
) : androidx.recyclerview.widget.ListAdapter<NoticeBoardItem, NoticeBoardAdapter.ViewHolder>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<NoticeBoardItem>() {
        override fun areItemsTheSame(oldItem: NoticeBoardItem, newItem: NoticeBoardItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: NoticeBoardItem, newItem: NoticeBoardItem) = oldItem == newItem
    }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = com.zai.autoresponder.databinding.ItemNoticeBoardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: com.zai.autoresponder.databinding.ItemNoticeBoardBinding
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

        fun bind(notice: NoticeBoardItem) {
            binding.tvContactName.text = notice.contactName
            binding.tvNoticeContent.text = notice.noticeContent
            binding.tvOriginalMessage.text = notice.originalMessage
            
            // Set icon based on notice type
            val icon = when (notice.noticeType) {
                "reminder" -> "⏰"
                "meeting" -> "📅"
                "call_back" -> "📞"
                "number_shared" -> "📱"
                "important" -> "⭐"
                "address" -> "📍"
                "email" -> "📧"
                "date" -> "🎂"
                "task" -> "📋"
                "gift" -> "🎁"
                "medical" -> "🏥"
                "financial" -> "💰"
                "travel" -> "✈️"
                "work" -> "💼"
                else -> "📝"
            }
            binding.tvNoticeIcon.text = icon
            
            // Set type label
            val typeLabel = when (notice.noticeType) {
                "reminder" -> "Reminder"
                "meeting" -> "Meeting"
                "call_back" -> "Call Back"
                "number_shared" -> "Number Shared"
                "important" -> "Important"
                "address" -> "Address"
                "email" -> "Email"
                "date" -> "Important Date"
                "task" -> "Task"
                "gift" -> "Gift"
                "medical" -> "Medical"
                "financial" -> "Financial"
                "travel" -> "Travel"
                "work" -> "Work"
                else -> "Notice"
            }
            binding.tvNoticeType.text = typeLabel
            
            // Set read status
            if (!notice.isRead) {
                binding.root.alpha = 1.0f
                binding.unreadIndicator.visibility = View.VISIBLE
            } else {
                binding.root.alpha = 0.7f
                binding.unreadIndicator.visibility = View.GONE
            }
            
            // Contact number if available
            if (!notice.contactNumber.isNullOrBlank()) {
                binding.tvContactNumber.visibility = View.VISIBLE
                binding.tvContactNumber.text = notice.contactNumber
            } else {
                binding.tvContactNumber.visibility = View.GONE
            }

            binding.root.setOnClickListener { onItemClick(notice) }
            binding.btnArchive.setOnClickListener { onArchiveClick(notice) }
            binding.btnDelete.setOnClickListener { onDeleteClick(notice) }
        }
    }
}

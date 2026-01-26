package com.example.swapit.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.swapit.data.ChatRepository
import com.example.swapit.databinding.FragmentChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ValueEventListener

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private var conversationId: String = ""
    private var messagesListener: ValueEventListener? = null
    private var availabilityListener: ValueEventListener? = null

    private lateinit var adapter: ChatAdapter
    private var myUid: String = ""
    private var isItemAvailable: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        conversationId = requireArguments().getString("conversationId").orEmpty()
        myUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

        adapter = ChatAdapter(myUid)

        binding.rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter

        ChatRepository.markConversationSeen(conversationId)

        availabilityListener = ChatRepository.listenItemAvailability(conversationId) { available ->
            isItemAvailable = available
            applyAvailabilityUi()
        }

        messagesListener = ChatRepository.listenMessages(conversationId) { list ->
            adapter.submit(list)
            if (list.isNotEmpty()) binding.rvMessages.scrollToPosition(list.size - 1)

            val last = list.lastOrNull()
            if (last != null && last.senderUid != myUid) {
                ChatRepository.markConversationSeen(conversationId)
            }
        }

        binding.btnSend.setOnClickListener {
            if (!isItemAvailable) return@setOnClickListener

            val text = binding.etMessage.text?.toString()?.trim().orEmpty()
            if (text.isBlank()) return@setOnClickListener

            binding.btnSend.isEnabled = false
            ChatRepository.sendMessage(
                conversationId = conversationId,
                text = text,
                onSuccess = {
                    binding.etMessage.setText("")
                    binding.btnSend.isEnabled = true
                    ChatRepository.markConversationSeen(conversationId)
                },
                onFailure = {
                    binding.btnSend.isEnabled = true
                }
            )
        }

        applyAvailabilityUi()
    }

    private fun applyAvailabilityUi() {
        binding.tvItemUnavailable.isVisible = !isItemAvailable
        binding.etMessage.isEnabled = isItemAvailable
        binding.btnSend.isEnabled = isItemAvailable
    }

    override fun onDestroyView() {
        super.onDestroyView()

        messagesListener?.let { l ->
            if (conversationId.isNotBlank()) ChatRepository.stopListening(conversationId, l)
        }
        availabilityListener?.let { l ->
            if (conversationId.isNotBlank()) ChatRepository.stopListeningItemAvailability(conversationId, l)
        }

        messagesListener = null
        availabilityListener = null
        _binding = null
    }
}

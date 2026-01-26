package com.example.swapit.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.swapit.R
import com.example.swapit.data.ChatRepository
import com.example.swapit.databinding.FragmentConversationsBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.ValueEventListener

class ConversationsFragment : Fragment() {

    private var _binding: FragmentConversationsBinding? = null
    private val binding get() = _binding!!

    private var listener: ValueEventListener? = null
    private lateinit var adapter: ConversationsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentConversationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = ConversationsAdapter(
            onClick = { row ->
                findNavController().navigate(
                    R.id.chatFragment,
                    bundleOf(
                        "conversationId" to row.conversationId,
                        "itemId" to row.itemId,
                        "itemTitle" to row.itemTitle
                    )
                )
            },
            onLongClick = { row ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete conversation?")
                    .setMessage("This will delete the chat for both users.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        ChatRepository.deleteConversation(
                            conversationId = row.conversationId,
                            onSuccess = {
                                Snackbar.make(binding.root, "Conversation deleted", Snackbar.LENGTH_SHORT).show()
                            },
                            onFailure = {
                                Snackbar.make(binding.root, "Failed to delete conversation", Snackbar.LENGTH_SHORT).show()
                            }
                        )
                    }
                    .show()
            }
        )

        binding.rvConversations.layoutManager = LinearLayoutManager(requireContext())
        binding.rvConversations.adapter = adapter

        listener = ChatRepository.listenMyConversations { list ->
            adapter.submit(list)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.let { ChatRepository.stopListeningMyConversations(it) }
        listener = null
        _binding = null
    }
}

package com.example.swapit.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.swapit.R
import com.example.swapit.databinding.RowConversationBinding

class ConversationsAdapter(
    private val onClick: (ConversationRow) -> Unit,
    private val onLongClick: (ConversationRow) -> Unit
) : RecyclerView.Adapter<ConversationsAdapter.VH>() {

    private val items = mutableListOf<ConversationRow>()

    class VH(val binding: RowConversationBinding) : RecyclerView.ViewHolder(binding.root)

    fun submit(list: List<ConversationRow>) {
        items.clear()
        items.addAll(list.sortedByDescending { it.lastMessageAt })
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = RowConversationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = items[position]

        holder.binding.tvName.text = row.otherNickname
        holder.binding.tvItemTitle.text = row.itemTitle.ifBlank { "Item" }
        holder.binding.tvLast.text = if (row.lastMessage.isBlank()) "Say hi 👋" else row.lastMessage

        holder.binding.ivItem.load(row.itemImageUrl) {
            placeholder(R.drawable.ic_image_placeholder)
            error(R.drawable.ic_image_placeholder)
            crossfade(true)
        }

        holder.binding.root.setOnClickListener { onClick(row) }
        holder.binding.root.setOnLongClickListener {
            onLongClick(row)
            true
        }
    }

    override fun getItemCount(): Int = items.size
}

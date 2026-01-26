package com.example.swapit.ui.chat

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.swapit.R
import com.example.swapit.data.ChatMessage
import com.example.swapit.databinding.RowMessageBinding

class ChatAdapter(private val myUid: String) : RecyclerView.Adapter<ChatAdapter.VH>() {

    private val items = mutableListOf<ChatMessage>()

    class VH(val binding: RowMessageBinding) : RecyclerView.ViewHolder(binding.root)

    fun submit(list: List<ChatMessage>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = RowMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val msg = items[position]
        holder.binding.tvMsg.text = msg.text

        val lp = holder.binding.tvMsg.layoutParams as FrameLayout.LayoutParams
        val mine = msg.senderUid == myUid

        if (mine) {
            lp.gravity = Gravity.END
            holder.binding.tvMsg.setBackgroundResource(R.drawable.bg_msg_mine)
            holder.binding.tvMsg.setTextColor(Color.WHITE)
        } else {
            lp.gravity = Gravity.START
            holder.binding.tvMsg.setBackgroundResource(R.drawable.bg_msg_other)
            holder.binding.tvMsg.setTextColor(Color.BLACK)
        }

        holder.binding.tvMsg.layoutParams = lp
    }

    override fun getItemCount(): Int = items.size
}

package com.example.swapit.ui.myitems

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.swapit.data.Item
import com.example.swapit.databinding.RowMyItemBinding

class MyItemsAdapter(
    private val items: List<Item>,
    private val onItemClick: (String) -> Unit,
    private val onEditClick: (Item) -> Unit,
    private val onDeleteClick: (Item) -> Unit
) : RecyclerView.Adapter<MyItemsAdapter.VH>() {

    inner class VH(val binding: RowMyItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = RowMyItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        holder.binding.tvTitle.text = item.title
        holder.binding.tvCategory.text = item.category
        holder.binding.tvLocation.text = item.location?.label.orEmpty()

        holder.binding.ivImage.load(item.imageUrls.firstOrNull()) { crossfade(true) }

        holder.binding.root.setOnClickListener { onItemClick(item.id) }
        holder.binding.btnEdit.setOnClickListener { onEditClick(item) }
        holder.binding.btnDelete.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount(): Int = items.size
}

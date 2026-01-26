package com.example.swapit.ui.additem

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.swapit.databinding.RowEditableImageBinding

class EditImagesAdapter(
    private val images: MutableList<EditableImage>,
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<EditImagesAdapter.VH>() {

    inner class VH(val binding: RowEditableImageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = RowEditableImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val image = images[position]

        when (image) {
            is EditableImage.Existing -> holder.binding.ivThumb.load(image.url) { crossfade(true) }
            is EditableImage.New -> holder.binding.ivThumb.setImageURI(image.uri)
        }

        holder.binding.btnRemove.visibility = android.view.View.VISIBLE
        holder.binding.btnRemove.bringToFront()

        holder.binding.btnRemove.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onRemove(pos)
        }
    }


    override fun getItemCount(): Int = images.size
}

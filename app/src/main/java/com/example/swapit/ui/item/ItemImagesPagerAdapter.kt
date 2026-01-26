package com.example.swapit.ui.item

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.swapit.R

class ItemImagesPagerAdapter(
    private val urls: List<String>
) : RecyclerView.Adapter<ItemImagesPagerAdapter.VH>() {

    class VH(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val iv = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_pager_image, parent, false) as ImageView
        return VH(iv)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.imageView.load(urls[position])
    }

    override fun getItemCount(): Int = urls.size
}

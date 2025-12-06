package com.DASTAK.i230613_i230658_i230736.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.DASTAK.i230613_i230658_i230736.R
import com.DASTAK.i230613_i230658_i230736.models.listing

class ListingAdapter(
    private val items: List<listing>,
    private val itemClick: ((listing) -> Unit)? = null
) : RecyclerView.Adapter<ListingAdapter.ListingVH>() {

    inner class ListingVH(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.imgListing)
        val title: TextView = view.findViewById(R.id.tvTitle)
        val subtitle: TextView = view.findViewById(R.id.tvSubtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListingVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_listing, parent, false)
        return ListingVH(view)
    }

    override fun onBindViewHolder(holder: ListingVH, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.subtitle.text = item.subtitle
        holder.img.setImageResource(item.imageRes)
        holder.itemView.setOnClickListener { itemClick?.invoke(item) }
    }

    override fun getItemCount(): Int = items.size
}

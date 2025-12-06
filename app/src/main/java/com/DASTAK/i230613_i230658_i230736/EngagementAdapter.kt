package com.DASTAK.i230613_i230658_i230736.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.DASTAK.i230613_i230658_i230736.R
import com.DASTAK.i230613_i230658_i230736.models.Engagement
import com.DASTAK.i230613_i230658_i230736.utils.ImageUtils

class EngagementAdapter(
    private val items: List<Engagement>,
    private val itemClick: ((Engagement) -> Unit)? = null
) : RecyclerView.Adapter<EngagementAdapter.EngagementVH>() {

    inner class EngagementVH(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.imgEngagement)
        val title: TextView = view.findViewById(R.id.tvEngTitle)
        val whenText: TextView = view.findViewById(R.id.tvEngWhen)
        val count: TextView = view.findViewById(R.id.tvEngCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EngagementVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_engagement, parent, false)
        return EngagementVH(view)
    }

    override fun onBindViewHolder(holder: EngagementVH, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.whenText.text = item.whenText
        holder.count.text = item.attendeesText

        // Load image from Base64 or use resource
        if (item.imageBase64.isNotEmpty()) {
            val bitmap = ImageUtils.base64ToBitmap(item.imageBase64)
            if (bitmap != null) {
                holder.img.setImageBitmap(bitmap)
            } else {
                holder.img.setImageResource(R.drawable.grid_icon)
            }
        } else if (item.imageRes != 0) {
            holder.img.setImageResource(item.imageRes)
        } else {
            holder.img.setImageResource(R.drawable.grid_icon)
        }

        holder.itemView.setOnClickListener { itemClick?.invoke(item) }
    }

    override fun getItemCount(): Int = items.size
}
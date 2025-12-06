package com.DASTAK.i230613_i230658_i230736.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.DASTAK.i230613_i230658_i230736.R
import com.DASTAK.i230613_i230658_i230736.models.Engagement

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
        holder.img.setImageResource(item.imageRes)
        holder.itemView.setOnClickListener { itemClick?.invoke(item) }
    }

    override fun getItemCount(): Int = items.size
}

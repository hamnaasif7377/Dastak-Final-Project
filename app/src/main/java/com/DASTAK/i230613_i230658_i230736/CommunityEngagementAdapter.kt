package com.DASTAK.i230613_i230658_i230736

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView

data class CommunityEngagement(
    val eventId: Int,
    val eventName: String,
    val eventPoster: String?,
    val organizationName: String,
    val organizationImage: String?,
    val registrationDate: String,
    val status: String
)

class CommunityEngagementAdapter(
    private val engagements: MutableList<CommunityEngagement>,
    private val onItemClick: (CommunityEngagement) -> Unit
) : RecyclerView.Adapter<CommunityEngagementAdapter.EngagementViewHolder>() {

    class EngagementViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Use orgImage for BOTH event poster AND organization image
        // Since your layout only has orgImage, we'll use it for the event poster
        val orgImage: CircleImageView = view.findViewById(R.id.orgImage)
        val eventName: TextView = view.findViewById(R.id.eventName)
        val orgName: TextView = view.findViewById(R.id.orgName)
        val statusBadge: TextView = view.findViewById(R.id.statusBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EngagementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_community_engagement, parent, false)
        return EngagementViewHolder(view)
    }

    override fun onBindViewHolder(holder: EngagementViewHolder, position: Int) {
        val engagement = engagements[position]
        val context = holder.itemView.context

        holder.eventName.text = engagement.eventName
        holder.orgName.text = engagement.organizationName
        holder.statusBadge.text = engagement.status.capitalize()

        // Set status badge color
        when (engagement.status.lowercase()) {
            "accepted" -> {
                holder.statusBadge.setBackgroundResource(R.drawable.badge_accepted)
                holder.statusBadge.setTextColor(context.getColor(android.R.color.white))
            }
            "pending" -> {
                holder.statusBadge.setBackgroundResource(R.drawable.badge_pending)
                holder.statusBadge.setTextColor(context.getColor(android.R.color.black))
            }
            else -> {
                holder.statusBadge.setBackgroundResource(R.drawable.badge_default)
                holder.statusBadge.setTextColor(context.getColor(android.R.color.white))
            }
        }

        // PRIORITY 1: Try to load event poster first
        // PRIORITY 2: Fall back to organization image if no poster
        val imageToLoad = if (!engagement.eventPoster.isNullOrEmpty()) {
            // Use event poster
            android.util.Log.d("EngagementAdapter", "Loading event poster: ${engagement.eventPoster}")
            engagement.eventPoster
        } else if (!engagement.organizationImage.isNullOrEmpty()) {
            // Fall back to organization image
            android.util.Log.d("EngagementAdapter", "No poster, using org image: ${engagement.organizationImage}")
            engagement.organizationImage
        } else {
            null
        }

        if (imageToLoad != null) {
            val imageUrl = if (imageToLoad.startsWith("http")) {
                imageToLoad
            } else {
                Constants.BASE_URL + "uploads/" + imageToLoad
            }

            android.util.Log.d("EngagementAdapter", "Final image URL: $imageUrl")

            Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.pfp)
                .error(R.drawable.pfp)
                .centerCrop()
                .into(holder.orgImage)
        } else {
            android.util.Log.d("EngagementAdapter", "No image available, using placeholder")
            holder.orgImage.setImageResource(R.drawable.pfp)
        }

        holder.itemView.setOnClickListener {
            onItemClick(engagement)
        }
    }

    override fun getItemCount() = engagements.size

    fun updateEngagements(newEngagements: List<CommunityEngagement>) {
        android.util.Log.d("EngagementAdapter", "Updating ${newEngagements.size} engagements")
        newEngagements.forEachIndexed { index, eng ->
            android.util.Log.d("EngagementAdapter", "[$index] ${eng.eventName} - Poster: ${eng.eventPoster}, Org: ${eng.organizationImage}")
        }
        engagements.clear()
        engagements.addAll(newEngagements)
        notifyDataSetChanged()
    }
}
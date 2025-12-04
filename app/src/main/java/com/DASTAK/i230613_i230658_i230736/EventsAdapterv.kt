package com.DASTAK.i230613_i230658_i230736

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.DASTAK.i230613_i230658_i230736.R
import com.bumptech.glide.load.engine.DiskCacheStrategy
import java.text.SimpleDateFormat
import java.util.*

class EventsAdapterv(
    private var events: MutableList<Event>,
    private val onEventClick: (Event) -> Unit
) : RecyclerView.Adapter<EventsAdapterv.EventViewHolder>() {

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val eventImage: ImageView = itemView.findViewById(R.id.eventImage)
        val eventTitle: TextView = itemView.findViewById(R.id.eventTitle)
        val eventLocation: TextView = itemView.findViewById(R.id.eventLocation)
        val eventOrganization: TextView = itemView.findViewById(R.id.eventOrganization)
        val eventDate: TextView = itemView.findViewById(R.id.eventDate)
        val btnViewDetails: Button = itemView.findViewById(R.id.btnViewDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event_card_v, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]

        holder.eventTitle.text = event.event_name
        holder.eventLocation.text = event.event_location
        holder.eventOrganization.text = event.organizer.name
        holder.eventDate.text = formatDate(event.event_date)

        // Load event image using Glide
        if (!event.poster_image.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(event.poster_image)
                .placeholder(R.drawable.alkhidmat) // Use your placeholder
                .error(R.drawable.alkhidmat)
                .centerCrop()
                .into(holder.eventImage)
        } else {
            holder.eventImage.setImageResource(R.drawable.alkhidmat)
        }

        // Click listener for View Details button
        holder.btnViewDetails.setOnClickListener {
            onEventClick(event)
        }

        // Optional: Make entire card clickable
        holder.itemView.setOnClickListener {
            onEventClick(event)
        }
    }

    override fun getItemCount(): Int = events.size

    fun updateEvents(newEvents: List<Event>) {
        events.clear()
        events.addAll(newEvents)
        notifyDataSetChanged()
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString
        }
    }
}

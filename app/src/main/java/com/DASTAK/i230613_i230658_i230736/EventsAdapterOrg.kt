package com.DASTAK.i230613_i230658_i230736

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import java.text.SimpleDateFormat
import java.util.*

class EventsAdapterOrg(
    private var events: MutableList<Event>,
    private val onEditClick: (Event) -> Unit,
    private val onDeleteClick: (Event) -> Unit,
    private val onViewClick: (Event) -> Unit
) : RecyclerView.Adapter<EventsAdapterOrg.EventViewHolder>() {

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val eventImage: ImageView = itemView.findViewById(R.id.eventImage)
        val eventTitle: TextView = itemView.findViewById(R.id.eventTitle)
        val eventLocation: TextView = itemView.findViewById(R.id.eventLocation)
        val eventDate: TextView = itemView.findViewById(R.id.eventDate)
        val eventParticipants: TextView = itemView.findViewById(R.id.eventParticipants)
        val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
        val btnViewDetails: Button = itemView.findViewById(R.id.btnViewDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event_card_org, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]

        holder.eventTitle.text = event.event_name
        holder.eventLocation.text = event.event_location
        holder.eventDate.text = formatDate(event.event_date)
        holder.eventParticipants.text = "${event.participant_count} participants"

        // Load event poster image
        if (!event.poster_image.isNullOrEmpty()) {
            val imageUrl = Constants.BASE_URL + event.poster_image

            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_menu)
                .error(R.drawable.ic_menu)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.eventImage)
        } else {
            holder.eventImage.setImageResource(R.drawable.ic_menu)
        }

        // Button click listeners
        holder.btnEdit.setOnClickListener {
            onEditClick(event)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(event)
        }

        holder.btnViewDetails.setOnClickListener {
            onViewClick(event)
        }
    }

    override fun getItemCount(): Int = events.size

    fun updateEvents(newEvents: List<Event>) {
        events.clear()
        events.addAll(newEvents)
        notifyDataSetChanged()
    }

    fun removeEvent(eventId: Int) {
        val position = events.indexOfFirst { it.event_id == eventId }
        if (position != -1) {
            events.removeAt(position)
            notifyItemRemoved(position)
        }
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
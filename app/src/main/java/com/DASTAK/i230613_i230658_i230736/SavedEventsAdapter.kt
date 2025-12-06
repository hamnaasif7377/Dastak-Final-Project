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

class SavedEventsAdapter(
    private var events: MutableList<Event>,
    private val onEventClick: (Event) -> Unit,
    private val onRemoveClick: (Event) -> Unit
) : RecyclerView.Adapter<SavedEventsAdapter.SavedEventViewHolder>() {

    inner class SavedEventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val eventImage: ImageView = itemView.findViewById(R.id.eventImage)
        val eventTitle: TextView = itemView.findViewById(R.id.eventTitle)
        val eventLocation: TextView = itemView.findViewById(R.id.eventLocation)
        val eventOrganization: TextView = itemView.findViewById(R.id.eventOrganization)
        val eventDate: TextView = itemView.findViewById(R.id.eventDate)
        val btnJoin: Button = itemView.findViewById(R.id.btnJoin)
        val btnRemove: Button = itemView.findViewById(R.id.btnRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedEventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.saved_event_row_item, parent, false)
        return SavedEventViewHolder(view)
    }

    override fun onBindViewHolder(holder: SavedEventViewHolder, position: Int) {
        val event = events[position]

        holder.eventTitle.text = event.event_name
        holder.eventLocation.text = event.event_location
        holder.eventOrganization.text = event.organizer.name
        holder.eventDate.text = formatDate(event.event_date)

        // Load event image
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

        // Join button - opens event details
        holder.btnJoin.setOnClickListener {
            onEventClick(event)
        }

        // Remove button - removes from favorites
        holder.btnRemove.setOnClickListener {
            onRemoveClick(event)
        }

        // Card click - also opens details
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

    fun removeEvent(event: Event) {
        val position = events.indexOf(event)
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
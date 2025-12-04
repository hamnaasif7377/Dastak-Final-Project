package com.DASTAK.i230613_i230658_i230736

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import java.text.SimpleDateFormat
import java.util.*

class EventsAdapter(
    private var events: MutableList<Event>,
    private val onEventClick: (Event) -> Unit
) : RecyclerView.Adapter<EventsAdapter.EventViewHolder>() {

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val eventTitle: TextView = itemView.findViewById(R.id.eventCardTitle)
        val eventDate: TextView = itemView.findViewById(R.id.eventCardDate)
        val eventDescription: TextView = itemView.findViewById(R.id.eventCardDescription)
        val eventImage: ImageView = itemView.findViewById(R.id.eventCardImage)

        fun bind(event: Event) {
            eventTitle.text = event.event_name
            eventDate.text = formatDate(event.event_date)
            eventDescription.text = event.event_description

            // Load event poster image
            if (!event.poster_image.isNullOrEmpty()) {
                val imageUrl = Constants.BASE_URL + event.poster_image

                Glide.with(itemView.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_menu) // Use a placeholder drawable
                    .error(R.drawable.ic_menu) // Use an error drawable
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(eventImage)
            } else {
                eventImage.setImageResource(R.drawable.ic_menu) // Default image
            }

            // Click listener
            itemView.setOnClickListener {
                onEventClick(event)
            }
        }

        private fun formatDate(dateString: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                date?.let { outputFormat.format(it) } ?: dateString
            } catch (e: Exception) {
                dateString
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event_card, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount(): Int = events.size

    fun updateEvents(newEvents: List<Event>) {
        events.clear()
        events.addAll(newEvents)
        notifyDataSetChanged()
    }

    fun addEvent(event: Event) {
        events.add(0, event) // Add to beginning
        notifyItemInserted(0)
    }
}
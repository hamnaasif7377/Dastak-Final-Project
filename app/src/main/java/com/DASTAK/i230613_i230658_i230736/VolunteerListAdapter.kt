package com.DASTAK.i230613_i230658_i230736

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class VolunteerListAdapter(
    private val volunteers: MutableList<VolunteerRegistration>,
    private val onRemove: (Int, Int) -> Unit  // (registrationId, position)
) : RecyclerView.Adapter<VolunteerListAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val volunteerImage: ImageView = view.findViewById(R.id.volunteerImage)
        val volunteerName: TextView = view.findViewById(R.id.volunteerName)
        val eventName: TextView = view.findViewById(R.id.eventName)
        val volunteerEmail: TextView = view.findViewById(R.id.volunteerEmail)
        val btnRemove: Button = view.findViewById(R.id.btnRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_volunteer_registration, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val registration = volunteers[position]
        val volunteer = registration.volunteer

        holder.volunteerName.text = volunteer.name
        holder.eventName.text = "Registered for: ${registration.eventName}"
        holder.volunteerEmail.text = volunteer.email

        // Load volunteer profile image
        if (volunteer.profileImage != null && volunteer.profileImage.isNotEmpty()) {
            val imageUrl = Constants.BASE_URL + volunteer.profileImage
            Glide.with(holder.volunteerImage.context)
                .load(imageUrl)
                .placeholder(R.drawable.image8)
                .error(R.drawable.image8)
                .circleCrop()
                .into(holder.volunteerImage)
        } else {
            Glide.with(holder.volunteerImage.context)
                .load(R.drawable.image8)
                .circleCrop()
                .into(holder.volunteerImage)
        }

        // Remove button click
        holder.btnRemove.setOnClickListener {
            onRemove(registration.registrationId, position)
        }
    }

    override fun getItemCount(): Int = volunteers.size

    fun removeItem(position: Int) {
        volunteers.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, volunteers.size)
    }
}
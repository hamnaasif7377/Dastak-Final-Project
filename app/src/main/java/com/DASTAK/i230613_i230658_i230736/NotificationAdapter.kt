package com.DASTAK.i230613_i230658_i230736

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class NotificationAdapter(
    private val notifications: MutableList<Notification>,
    private val onAccept: (Int, Int) -> Unit,  // (registrationId, position)
    private val onReject: (Int, Int) -> Unit   // (registrationId, position)
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    inner class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userAvatar: ImageView = view.findViewById(R.id.userAvatar)
        val notificationText: TextView = view.findViewById(R.id.notificationText)
        val btnAccept: Button? = view.findViewById(R.id.btnAccept)
        val btnReject: Button? = view.findViewById(R.id.btnReject)
        val buttonContainer: LinearLayout? = view.findViewById(R.id.buttonContainer)
    }

    override fun getItemViewType(position: Int): Int {
        return when (notifications[position].notificationType) {
            "registration_request" -> VIEW_TYPE_REQUEST
            else -> VIEW_TYPE_RESPONSE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val layoutId = when (viewType) {
            VIEW_TYPE_REQUEST -> R.layout.item_notification_request
            else -> R.layout.item_notification_response
        }

        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]

        holder.notificationText.text = notification.message

        // Load sender profile image (organization logo or volunteer photo)
        if (notification.sender?.profileImage != null && notification.sender.profileImage.isNotEmpty()) {
            val imageUrl = Constants.BASE_URL + notification.sender.profileImage

            Glide.with(holder.userAvatar.context)
                .load(imageUrl)
                .placeholder(R.drawable.image8)
                .error(R.drawable.image8)
                .circleCrop() // Makes the image circular
                .into(holder.userAvatar)
        } else {
            // Use default image if no profile image
            Glide.with(holder.userAvatar.context)
                .load(R.drawable.image8)
                .circleCrop()
                .into(holder.userAvatar)
        }

        // Handle request notifications (with Accept/Reject buttons)
        if (notification.notificationType == "registration_request") {
            // Check if this request has already been responded to
            if (notification.registrationStatus != null && notification.registrationStatus != "pending") {
                // Request has been responded to
                if (notification.registrationStatus == "accepted") {
                    // Show "Accepted" button only, disable Reject button
                    holder.btnAccept?.apply {
                        text = "Accepted"
                        isEnabled = false
                        alpha = 0.6f
                    }
                    holder.btnReject?.visibility = View.GONE
                } else if (notification.registrationStatus == "rejected") {
                    // Hide both buttons or show "Rejected"
                    holder.btnAccept?.visibility = View.GONE
                    holder.btnReject?.apply {
                        text = "Rejected"
                        isEnabled = false
                        alpha = 0.6f
                    }
                }
            } else {
                // Request is still pending - show both buttons as active
                holder.btnAccept?.apply {
                    text = "Accept"
                    isEnabled = true
                    alpha = 1.0f
                    visibility = View.VISIBLE
                    setOnClickListener {
                        notification.registrationId?.let { regId ->
                            onAccept(regId, position)
                        }
                    }
                }

                holder.btnReject?.apply {
                    text = "Reject"
                    isEnabled = true
                    alpha = 1.0f
                    visibility = View.VISIBLE
                    setOnClickListener {
                        notification.registrationId?.let { regId ->
                            onReject(regId, position)
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = notifications.size

    fun removeItem(position: Int) {
        notifications.removeAt(position)
        notifyItemRemoved(position)
    }

    fun updateItem(position: Int, notification: Notification) {
        notifications[position] = notification
        notifyItemChanged(position)
    }

    fun updateRegistrationStatus(position: Int, newStatus: String) {
        if (position >= 0 && position < notifications.size) {
            val notification = notifications[position]
            val updatedNotification = notification.copy(registrationStatus = newStatus)
            notifications[position] = updatedNotification
            notifyItemChanged(position)
        }
    }

    companion object {
        private const val VIEW_TYPE_REQUEST = 1
        private const val VIEW_TYPE_RESPONSE = 2
    }
}
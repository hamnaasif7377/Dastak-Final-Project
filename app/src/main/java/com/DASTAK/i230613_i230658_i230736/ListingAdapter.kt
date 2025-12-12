package com.DASTAK.i230613_i230658_i230736

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

data class Listing(
    val listingId: Int,
    val listingName: String,
    val donationDate: String,
    val listingImage: String?
)

class ListingAdapter(
    private val listings: MutableList<Listing>
) : RecyclerView.Adapter<ListingAdapter.ListingViewHolder>() {

    class ListingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val listingImage: ImageView = view.findViewById(R.id.listingImage)
        val listingName: TextView = view.findViewById(R.id.listingName)
        val listingDate: TextView = view.findViewById(R.id.listingDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_listing, parent, false)
        return ListingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ListingViewHolder, position: Int) {
        val listing = listings[position]

        holder.listingName.text = listing.listingName

        // Format date for display
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val date = inputFormat.parse(listing.donationDate)
            holder.listingDate.text = date?.let { outputFormat.format(it) } ?: listing.donationDate
        } catch (e: Exception) {
            holder.listingDate.text = listing.donationDate
        }

        // Load image
        if (!listing.listingImage.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(Constants.BASE_URL + listing.listingImage)
                .placeholder(R.drawable.grid_icon)
                .error(R.drawable.grid_icon)
                .centerCrop()
                .into(holder.listingImage)
        } else {
            holder.listingImage.setImageResource(R.drawable.grid_icon)
        }
    }

    override fun getItemCount() = listings.size

    fun updateListings(newListings: List<Listing>) {
        listings.clear()
        listings.addAll(newListings)
        notifyDataSetChanged()
    }
}
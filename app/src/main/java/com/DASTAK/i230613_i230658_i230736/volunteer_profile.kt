package com.DASTAK.i230613_i230658_i230736

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.DASTAK.i230613_i230658_i230736.adapters.EngagementAdapter
import com.DASTAK.i230613_i230658_i230736.adapters.ListingAdapter
import com.DASTAK.i230613_i230658_i230736.databinding.ActivityVolunteerProfileBinding
import com.DASTAK.i230613_i230658_i230736.models.Engagement
import com.DASTAK.i230613_i230658_i230736.models.listing

class volunteer_profile : AppCompatActivity() {

    // ViewBinding instance
    private lateinit var binding: ActivityVolunteerProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // edge-to-edge if you want full-bleed content behind system bars
        enableEdgeToEdge()

        // inflate binding and set content view
        binding = ActivityVolunteerProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // apply system bar paddings to the root container (id = main in layout)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupListings()
        setupEngagements()
        setupClicks()
    }

    private fun setupListings() {
        // sample data - replace with your real data source
        val listings = listOf(
            listing("Bedsheet", "Size: 38", R.drawable.bedsheeet),
            listing("T-shirts", "Size: Medium — Quantity: 2", R.drawable.tshirt),
            listing("Blanket", "Size: Double bed — Quantity: 12", R.drawable.blanket)
        )

        val adapter = ListingAdapter(listings) { listing ->
            // TODO: handle listing click (open details, share, etc.)
        }

        binding.recyclerListings.apply {
            layoutManager = LinearLayoutManager(this@volunteer_profile, LinearLayoutManager.VERTICAL, false)
            isNestedScrollingEnabled = false
            setHasFixedSize(false)
            this.adapter = adapter
        }
    }

    private fun setupEngagements() {
        val engagements = listOf(
            Engagement("Flood donation day", "Sun, Dec 1 at 8 AM", "123 attendees", R.drawable.flood_donation),
            Engagement("Beach Cleanup", "Sat, Dec 7 at 9 AM", "42 attendees", R.drawable.flood_donation)
        )

        val adapter = EngagementAdapter(engagements) { engagement ->
            // TODO: handle engagement click
        }

        binding.recyclerEngagements.apply {
            layoutManager = LinearLayoutManager(this@volunteer_profile, LinearLayoutManager.VERTICAL, false)
            isNestedScrollingEnabled = false
            setHasFixedSize(false)
            this.adapter = adapter
        }
    }

    private fun setupClicks() {
        binding.backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.menuButton.setOnClickListener {
            // TODO: show menu / navigation drawer
        }
    }
}

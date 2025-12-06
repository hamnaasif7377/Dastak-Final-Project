package com.DASTAK.i230613_i230658_i230736

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.DASTAK.i230613_i230658_i230736.adapters.EngagementAdapter
import com.DASTAK.i230613_i230658_i230736.adapters.ListingAdapter
import com.DASTAK.i230613_i230658_i230736.databinding.ActivityVolunteerProfileBinding
import com.DASTAK.i230613_i230658_i230736.models.Engagement
import com.DASTAK.i230613_i230658_i230736.models.listing
import com.google.firebase.database.*

class volunteer_profile : AppCompatActivity() {

    private lateinit var binding: ActivityVolunteerProfileBinding
    private lateinit var database: FirebaseDatabase

    private val listingsList = mutableListOf<listing>()
    private val engagementsList = mutableListOf<Engagement>()

    private lateinit var listingAdapter: ListingAdapter
    private lateinit var engagementAdapter: EngagementAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityVolunteerProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        database = FirebaseDatabase.getInstance()

        setupListings()
        setupEngagements()
        setupClicks()

        // Fetch data from Firebase
        fetchListings()
        fetchEngagements()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning from AddEngagement
        fetchListings()
        fetchEngagements()
    }

    private fun setupListings() {
        listingAdapter = ListingAdapter(listingsList) { listing ->
            // TODO: handle listing click
        }

        binding.recyclerListings.apply {
            layoutManager = LinearLayoutManager(this@volunteer_profile, LinearLayoutManager.VERTICAL, false)
            isNestedScrollingEnabled = false
            setHasFixedSize(false)
            adapter = listingAdapter
        }
    }

    private fun setupEngagements() {
        engagementAdapter = EngagementAdapter(engagementsList) { engagement ->
            // TODO: handle engagement click
        }

        binding.recyclerEngagements.apply {
            layoutManager = LinearLayoutManager(this@volunteer_profile, LinearLayoutManager.VERTICAL, false)
            isNestedScrollingEnabled = false
            setHasFixedSize(false)
            adapter = engagementAdapter
        }
    }

    private fun setupClicks() {
        binding.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.menuButton.setOnClickListener {
            // TODO: show menu / navigation drawer
        }

        // Click listeners for add buttons
        binding.addListingButton.setOnClickListener {
            startActivity(Intent(this, AddEngagement::class.java))
        }

        binding.addListingButton2.setOnClickListener {
            startActivity(Intent(this, AddEngagement::class.java))
        }
    }

    private fun fetchListings() {
        val listingsRef = database.reference.child("listings")

        listingsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listingsList.clear()

                for (listingSnapshot in snapshot.children) {
                    val listing = listingSnapshot.getValue(listing::class.java)
                    listing?.let { listingsList.add(it) }
                }

                // Sort by timestamp (newest first)
                listingsList.sortByDescending { it.timestamp }
                listingAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun fetchEngagements() {
        val engagementsRef = database.reference.child("engagements")

        engagementsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                engagementsList.clear()

                for (engagementSnapshot in snapshot.children) {
                    val engagement = engagementSnapshot.getValue(Engagement::class.java)
                    engagement?.let { engagementsList.add(it) }
                }

                // Sort by timestamp (newest first)
                engagementsList.sortByDescending { it.timestamp }
                engagementAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }
}
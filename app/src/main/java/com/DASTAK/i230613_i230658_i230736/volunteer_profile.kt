package com.DASTAK.i230613_i230658_i230736

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.DASTAK.i230613_i230658_i230736.adapters.EngagementAdapter
import com.DASTAK.i230613_i230658_i230736.adapters.ListingAdapter
import com.DASTAK.i230613_i230658_i230736.databinding.ActivityVolunteerProfileBinding
import com.DASTAK.i230613_i230658_i230736.models.Engagement
import com.DASTAK.i230613_i230658_i230736.User
import com.DASTAK.i230613_i230658_i230736.models.listing
import com.DASTAK.i230613_i230658_i230736.utils.ImageUtils
import com.google.firebase.database.*

class volunteer_profile : AppCompatActivity() {

    private lateinit var binding: ActivityVolunteerProfileBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var drawerLayout: DrawerLayout

    private var userId = "user123" // Replace with actual user ID from Firebase Auth

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
        drawerLayout = binding.drawerLayout

        setupListings()
        setupEngagements()
        setupClicks()
        setupDrawer()

        initializeUserIfNeeded()

        // Fetch data from Firebase
        fetchUserProfile()
        fetchListings()
        fetchEngagements()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning
        fetchUserProfile()
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

        // Open drawer when menu button is clicked
        binding.menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Click listeners for add buttons
        binding.addListingButton.setOnClickListener {
            startActivity(Intent(this, AddEngagement::class.java))
        }

        binding.addListingButton2.setOnClickListener {
            startActivity(Intent(this, AddEngagement::class.java))
        }
    }

    private fun setupDrawer() {
        // Find drawer menu items
        val menuHome = findViewById<android.widget.LinearLayout>(R.id.menu_home)
        val menuEditProfile = findViewById<android.widget.LinearLayout>(R.id.menu_edit_profile)
        val menuNotifications = findViewById<android.widget.LinearLayout>(R.id.menu_notifications)
        val menuLogout = findViewById<android.widget.LinearLayout>(R.id.menu_logout)

        menuHome.setOnClickListener {
            drawerLayout.closeDrawers()
        }

        menuEditProfile.setOnClickListener {
            drawerLayout.closeDrawers()
            val intent = Intent(this, Edit_profile_volunteer::class.java)
            startActivity(intent)
        }

        menuNotifications.setOnClickListener {
            drawerLayout.closeDrawers()
            val intent = Intent(this , NotificationActivity::class.java)
            startActivity(intent)
        }

        menuLogout.setOnClickListener {
            drawerLayout.closeDrawers()
            val intent = Intent(this, login_v::class.java)
            startActivity(intent)
        }
    }

    private fun fetchUserProfile() {
        val userRef = database.reference.child("users").child(userId)

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                user?.let { updateProfileUI(it) }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun updateProfileUI(user: User) {
        // Update main profile
        binding.userName.text = user.username.ifEmpty { "User" }
        binding.userTagline.text = user.location.ifEmpty { "Location not set" }
        binding.userDescription.text = "${user.contributions} contributions"

        // Update drawer profile
        val drawerUserName = findViewById<android.widget.TextView>(R.id.drawerUserName)
        drawerUserName.text = user.username.ifEmpty { "User" }

        // Load profile image
        if (user.profileImageBase64.isNotEmpty()) {
            val bitmap = ImageUtils.base64ToBitmap(user.profileImageBase64)
            bitmap?.let {
                binding.profileImage.setImageBitmap(it)
                val drawerProfileImage = findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.drawerProfileImage)
                drawerProfileImage.setImageBitmap(it)
            }
        }
    }

    private fun fetchListings() {
        val listingsRef = database.reference.child("listings")
            .orderByChild("userId").equalTo(userId)

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
            .orderByChild("userId").equalTo(userId)

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


    private fun initializeUserIfNeeded() {
        val userRef = database.reference.child("users").child(userId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    // Create default user
                    val defaultUser = User(
                        id = userId,
                        name = "Aina Fatima",
                        username = "AinaFatima",
                        email = "aina@example.com",
                        location = "Rawalpindi, Punjab, Pakistan",
                        contributions = 0
                    )
                    userRef.setValue(defaultUser)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })

    }


}
package com.DASTAK.i230613_i230658_i230736

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONArray
import org.json.JSONObject

class VolunteerHomeActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var menuButton: ImageView
    private lateinit var profileImage: CircleImageView
    private lateinit var userName: TextView
    private lateinit var userTagline: TextView
    private lateinit var userDescription: TextView

    private lateinit var recyclerListings: RecyclerView
    private lateinit var recyclerEngagements: RecyclerView
    private lateinit var listingAdapter: ListingAdapter
    private lateinit var engagementAdapter: CommunityEngagementAdapter

    private val EDIT_PROFILE_REQUEST = 100
    private val ADD_LISTING_REQUEST = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_volunteer_home)

        initializeViews()
        loadUserData()
        setupRecyclerViews()
        setupDrawerMenu()
        setupUIElements()

        // Load data
        loadListings()
        loadCommunityEngagements()
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("VolunteerHome", "=== onResume called - Reloading data ===")
        loadCommunityEngagements()
        loadListings()
        loadContributionsCount()
    }

    private fun initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout)
        menuButton = findViewById(R.id.menuButton)
        profileImage = findViewById(R.id.profileImage)
        userName = findViewById(R.id.userName)
        userTagline = findViewById(R.id.userTagline)
        userDescription = findViewById(R.id.userDescription)
        recyclerListings = findViewById(R.id.recyclerListings)
        recyclerEngagements = findViewById(R.id.recyclerEngagements)

        menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerViews() {
        listingAdapter = ListingAdapter(mutableListOf())
        recyclerListings.apply {
            layoutManager = LinearLayoutManager(this@VolunteerHomeActivity)
            adapter = listingAdapter
            isNestedScrollingEnabled = false
        }

        engagementAdapter = CommunityEngagementAdapter(mutableListOf()) { engagement ->
            Toast.makeText(this, "Event: ${engagement.eventName}", Toast.LENGTH_SHORT).show()
        }
        recyclerEngagements.apply {
            layoutManager = LinearLayoutManager(this@VolunteerHomeActivity)
            adapter = engagementAdapter
            isNestedScrollingEnabled = false
        }

        android.util.Log.d("VolunteerHome", "RecyclerViews initialized")
    }

    private fun loadUserData() {
        val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)
        val name = sharedPref.getString("name", "User")
        val location = sharedPref.getString("location", "Location not set")
        val profileImageUrl = sharedPref.getString("profile_image", "")

        android.util.Log.d("VolunteerHome", "Loading user data for user_id: $userId")

        userName.text = name
        userTagline.text = location
        loadContributionsCount()

        if (!profileImageUrl.isNullOrEmpty() && profileImageUrl != "") {
            Glide.with(this)
                .load(Constants.BASE_URL + "uploads/" + profileImageUrl)
                .placeholder(R.drawable.pfp)
                .error(R.drawable.pfp)
                .into(profileImage)
        } else {
            profileImage.setImageResource(R.drawable.pfp)
        }
    }

    private fun loadContributionsCount() {
        val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)

        val url = Constants.BASE_URL + "get_contributions_count.php?user_id=$userId"
        val queue = Volley.newRequestQueue(this)

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        val count = json.getInt("count")
                        userDescription.text = "$count contributions"
                    } else {
                        userDescription.text = "0 contributions"
                    }
                } catch (e: Exception) {
                    android.util.Log.e("VolunteerHome", "Error parsing contributions", e)
                    userDescription.text = "0 contributions"
                }
            },
            { error ->
                android.util.Log.e("VolunteerHome", "Network error loading contributions", error)
                userDescription.text = "0 contributions"
            }
        )

        queue.add(request)
    }

    private fun loadListings() {
        val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)

        val url = Constants.BASE_URL + "get_listings.php?user_id=$userId"
        val queue = Volley.newRequestQueue(this)

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        val listingsArray = json.getJSONArray("listings")
                        val listings = parseListings(listingsArray)
                        listingAdapter.updateListings(listings)
                    } else {
                        listingAdapter.updateListings(emptyList())
                    }
                } catch (e: Exception) {
                    android.util.Log.e("VolunteerHome", "Error parsing listings", e)
                    listingAdapter.updateListings(emptyList())
                }
            },
            { error ->
                android.util.Log.e("VolunteerHome", "Network error loading listings", error)
                listingAdapter.updateListings(emptyList())
            }
        )

        queue.add(request)
    }

    private fun parseListings(jsonArray: JSONArray): List<Listing> {
        val listings = mutableListOf<Listing>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            listings.add(
                Listing(
                    listingId = obj.getInt("listing_id"),
                    listingName = obj.getString("listing_name"),
                    donationDate = obj.getString("donation_date"),
                    listingImage = if (obj.isNull("listing_image")) null else obj.getString("listing_image")
                )
            )
        }
        return listings
    }

    private fun loadCommunityEngagements() {
        val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)

        if (userId == -1) {
            android.util.Log.e("VolunteerHome", "Invalid user_id: -1")
            Toast.makeText(this, "Error: Invalid user ID", Toast.LENGTH_LONG).show()
            return
        }

        val url = Constants.BASE_URL + "get_accepted_events.php?user_id=$userId"
        val queue = Volley.newRequestQueue(this)

        android.util.Log.d("VolunteerHome", "=== LOADING ENGAGEMENTS ===")
        android.util.Log.d("VolunteerHome", "User ID: $userId")
        android.util.Log.d("VolunteerHome", "URL: $url")

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    android.util.Log.d("VolunteerHome", "=== RAW RESPONSE ===")
                    android.util.Log.d("VolunteerHome", response)

                    val json = JSONObject(response)
                    val status = json.getString("status")

                    android.util.Log.d("VolunteerHome", "Status: $status")

                    // Log debug info if available
                    if (json.has("debug")) {
                        val debug = json.getJSONObject("debug")
                        android.util.Log.d("VolunteerHome", "=== DEBUG INFO ===")
                        android.util.Log.d("VolunteerHome", "Total registrations: ${debug.optInt("total_registrations", 0)}")
                        android.util.Log.d("VolunteerHome", "Accepted count: ${debug.optInt("accepted_count", 0)}")

                        if (debug.has("all_registrations")) {
                            android.util.Log.d("VolunteerHome", "All registrations: ${debug.getJSONArray("all_registrations")}")
                        }
                    }

                    if (status == "success") {
                        val engagementsArray = json.getJSONArray("engagements")
                        val count = json.getInt("count")

                        android.util.Log.d("VolunteerHome", "Engagements count from server: $count")
                        android.util.Log.d("VolunteerHome", "Engagements array: $engagementsArray")

                        val engagements = parseEngagements(engagementsArray)

                        android.util.Log.d("VolunteerHome", "Parsed ${engagements.size} engagements")

                        engagementAdapter.updateEngagements(engagements)

                        runOnUiThread {
                            if (engagements.isEmpty()) {
                                Toast.makeText(this, "No accepted events found", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Loaded ${engagements.size} accepted events", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        val message = json.optString("message", "Unknown error")
                        android.util.Log.e("VolunteerHome", "Server error: $message")
                        engagementAdapter.updateEngagements(emptyList())

                        runOnUiThread {
                            Toast.makeText(this, "Server error: $message", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("VolunteerHome", "=== PARSE ERROR ===")
                    android.util.Log.e("VolunteerHome", "Error: ${e.message}")
                    android.util.Log.e("VolunteerHome", "Stack trace:", e)
                    android.util.Log.e("VolunteerHome", "Response was: $response")

                    engagementAdapter.updateEngagements(emptyList())

                    runOnUiThread {
                        Toast.makeText(this, "Parse error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            { error ->
                android.util.Log.e("VolunteerHome", "=== NETWORK ERROR ===")
                android.util.Log.e("VolunteerHome", "Error: ${error.message}")

                error.networkResponse?.let {
                    android.util.Log.e("VolunteerHome", "Status code: ${it.statusCode}")
                    val responseBody = String(it.data, Charsets.UTF_8)
                    android.util.Log.e("VolunteerHome", "Response body: $responseBody")
                }

                engagementAdapter.updateEngagements(emptyList())

                runOnUiThread {
                    Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        )

        queue.add(request)
    }

    private fun parseEngagements(jsonArray: JSONArray): List<CommunityEngagement> {
        val engagements = mutableListOf<CommunityEngagement>()

        android.util.Log.d("VolunteerHome", "=== PARSING ENGAGEMENTS ===")
        android.util.Log.d("VolunteerHome", "Array length: ${jsonArray.length()}")

        for (i in 0 until jsonArray.length()) {
            try {
                val obj = jsonArray.getJSONObject(i)

                android.util.Log.d("VolunteerHome", "Raw JSON item $i: $obj")

                val eventPoster = if (obj.isNull("event_poster")) null else obj.getString("event_poster")
                val orgImage = if (obj.isNull("organization_image")) null else obj.getString("organization_image")

                android.util.Log.d("VolunteerHome", "Event poster: $eventPoster")
                android.util.Log.d("VolunteerHome", "Org image: $orgImage")

                val engagement = CommunityEngagement(
                    eventId = obj.getInt("event_id"),
                    eventName = obj.getString("event_name"),
                    eventPoster = eventPoster,
                    organizationName = obj.getString("organization_name"),
                    organizationImage = orgImage,
                    registrationDate = obj.getString("registration_date"),
                    status = obj.getString("status")
                )

                engagements.add(engagement)
                android.util.Log.d("VolunteerHome", "✓ Parsed: ${engagement.eventName} (poster=${engagement.eventPoster != null})")

            } catch (e: Exception) {
                android.util.Log.e("VolunteerHome", "✗ Error parsing engagement at index $i: ${e.message}", e)
            }
        }

        android.util.Log.d("VolunteerHome", "Total parsed: ${engagements.size} engagements")
        return engagements
    }

    private fun setupUIElements() {
        findViewById<ImageView>(R.id.addListingButton).setOnClickListener {
            val intent = Intent(this, AddListingActivity::class.java)
            startActivityForResult(intent, ADD_LISTING_REQUEST)
        }

        findViewById<androidx.cardview.widget.CardView>(R.id.addListingCard2).visibility = android.view.View.GONE
    }

    private fun setupDrawerMenu() {
        findViewById<LinearLayout>(R.id.menu_home).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        findViewById<LinearLayout>(R.id.menu_edit_profile).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivityForResult(intent, EDIT_PROFILE_REQUEST)
        }

        findViewById<LinearLayout>(R.id.menu_browse_activities).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            openBrowseActivities()
        }

        findViewById<LinearLayout>(R.id.menu_my_contributions).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        findViewById<LinearLayout>(R.id.menu_saved).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            try {
                val intent = Intent(this, SavedEvents::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error opening Saved Activities: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        findViewById<LinearLayout>(R.id.menu_notifications).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            val intent = Intent(this, NotificationActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.menu_logout).setOnClickListener {
            logoutUser()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            EDIT_PROFILE_REQUEST -> {
                if (resultCode == Activity.RESULT_OK) {
                    loadUserData()
                }
            }
            ADD_LISTING_REQUEST -> {
                if (resultCode == Activity.RESULT_OK) {
                    loadListings()
                    loadContributionsCount()
                }
            }
        }
    }

    private fun openBrowseActivities() {
        val intent = Intent(this, BrowseActivitiesActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }

    private fun logoutUser() {
        val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            clear()
            apply()
        }

        val intent = Intent(this, RoleActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finishAffinity()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            finishAffinity()
        }
    }
}
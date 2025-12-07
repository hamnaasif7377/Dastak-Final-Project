package com.DASTAK.i230613_i230658_i230736

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
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
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.database.*
import org.json.JSONObject

class volunteer_profile : AppCompatActivity() {

    private lateinit var binding: ActivityVolunteerProfileBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var requestQueue: RequestQueue

    private var userIdInt = 0
    private val TAG = "VolunteerProfile"

    private val listingsList = mutableListOf<listing>()
    private val engagementsList = mutableListOf<Engagement>()

    private lateinit var listingAdapter: ListingAdapter
    private lateinit var engagementAdapter: EngagementAdapter

    // ✅ Activity Result Launcher for Edit Profile
    private val editProfileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d(TAG, "Refreshing profile after edit")
            fetchUserProfileFromFirebase()
        }
    }

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
        requestQueue = Volley.newRequestQueue(this)
        drawerLayout = binding.drawerLayout

        val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)

        userIdInt = try {
            sharedPref.getString("user_id", "0")?.toIntOrNull() ?: 0
        } catch (e: Exception) {
            sharedPref.getInt("user_id", 0)
        }

        Log.d(TAG, "Current User ID: $userIdInt")

        if (userIdInt == 0) {
            val intent = Intent(this, login_v::class.java)
            startActivity(intent)
            finishAffinity()
            return
        }

        setupListings()
        setupEngagements()
        setupClicks()
        setupDrawer()

        fetchUserProfileFromFirebase()
        fetchListings()
        fetchEngagements()
    }

    override fun onResume() {
        super.onResume()
        fetchUserProfileFromFirebase()
        fetchListings()
        fetchEngagements()
    }

    private fun setupListings() {
        listingAdapter = ListingAdapter(listingsList) { listing ->
            Log.d(TAG, "Listing clicked: ${listing.id}")
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
            Log.d(TAG, "Engagement clicked: ${engagement.id}")
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
            drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.addListingButton.setOnClickListener {
            startActivity(Intent(this, AddEngagement::class.java))
        }

        binding.addListingButton2.setOnClickListener {
            startActivity(Intent(this, AddEngagement::class.java))
        }
    }

    private fun setupDrawer() {
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
            editProfileLauncher.launch(intent)  // ✅ Use launcher to get result
        }

        menuNotifications.setOnClickListener {
            drawerLayout.closeDrawers()
            val intent = Intent(this, NotificationActivity::class.java)
            startActivity(intent)
        }

        menuLogout.setOnClickListener {
            drawerLayout.closeDrawers()
            val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putBoolean("isLoggedIn", false)
                remove("user_id")
                remove("email")
                remove("name")
                apply()
            }
            val intent = Intent(this, login_v::class.java)
            startActivity(intent)
            finishAffinity()
        }
    }

    private fun fetchUserProfileFromFirebase() {
        val userRef = database.reference.child("users").child(userIdInt.toString())

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val user = snapshot.getValue(User::class.java)
                    user?.let {
                        Log.d(TAG, "User found in Firebase: ${it.name}, username: ${it.username}")
                        updateProfileUI(
                            it.username.ifEmpty { it.name },
                            it.email,
                            it.location,
                            it.profileImageBase64
                        )

                        // ✅ Update cache with fresh data
                        val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putString("name", it.name)
                            putString("email", it.email)
                            putString("location", it.location)
                            putString("profile_image", it.profileImageBase64)
                            apply()
                        }
                    }
                } else {
                    Log.d(TAG, "User not found in Firebase, fetching from MySQL")
                    fetchUserProfileFromMySQL()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Firebase error: ${error.message}")
                fetchUserProfileFromMySQL()
            }
        })
    }

    private fun fetchUserProfileFromMySQL() {
        val url = Constants.BASE_URL + "get_organization_profile.php?user_id=$userIdInt"

        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    val status = json.getString("status")

                    if (status == "success") {
                        val user = json.getJSONObject("user")
                        val name = user.getString("name")
                        val email = user.getString("email")
                        val location = user.optString("location", "Not set")
                        val profileImage = user.optString("profile_image", "")

                        Log.d(TAG, "User loaded from MySQL: $name")
                        updateProfileUI(name, email, location, profileImage)
                        syncUserToFirebase(name, email, location, profileImage)

                        val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putString("name", name)
                            putString("email", email)
                            putString("location", location)
                            putString("profile_image", profileImage)
                            apply()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "MySQL fetch error: ${e.message}")
                    updateProfileUIFromCache()
                }
            },
            { error ->
                Log.e(TAG, "Network error: ${error.message}")
                updateProfileUIFromCache()
            }
        )

        requestQueue.add(stringRequest)
    }

    private fun syncUserToFirebase(name: String, email: String, location: String, profileImage: String) {
        val userData = hashMapOf<String, Any>(
            "id" to userIdInt,  // Int
            "name" to name,
            "username" to name,
            "email" to email,
            "password" to "",
            "location" to location,
            "profileImageBase64" to profileImage,
            "contributions" to 0,  // Int
            "timestamp" to System.currentTimeMillis()  // Long
        )

        database.reference.child("users").child(userIdInt.toString())
            .setValue(userData)  // ✅ Use setValue for new users
            .addOnSuccessListener {
                Log.d(TAG, "User synced to Firebase successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to sync user: ${e.message}")
            }
    }

    private fun updateProfileUIFromCache() {
        val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        val name = sharedPref.getString("name", "User") ?: "User"
        val email = sharedPref.getString("email", "") ?: ""
        val location = sharedPref.getString("location", "Location not set") ?: "Location not set"
        val profileImage = sharedPref.getString("profile_image", "") ?: ""

        updateProfileUI(name, email, location, profileImage)
    }

    private fun updateProfileUI(name: String, email: String, location: String, profileImage: String) {
        binding.userName.text = name.ifEmpty { "User" }
        binding.userTagline.text = location.ifEmpty { "Location not set" }
        binding.userDescription.text = email

        val drawerUserName = findViewById<android.widget.TextView>(R.id.drawerUserName)
        drawerUserName.text = name.ifEmpty { "User" }

        if (profileImage.isNotEmpty()) {
            val bitmap = ImageUtils.base64ToBitmap(profileImage)
            bitmap?.let {
                binding.profileImage.setImageBitmap(it)
                val drawerProfileImage = findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.drawerProfileImage)
                drawerProfileImage.setImageBitmap(it)
            }
        }
    }

    private fun fetchListings() {
        val listingsRef = database.reference.child("listings")

        listingsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listingsList.clear()

                for (listingSnapshot in snapshot.children) {
                    try {
                        val listing = listingSnapshot.getValue(listing::class.java)
                        listing?.let {
                            val itemUserId = it.getUserIdAsInt()
                            Log.d(TAG, "Found listing: ${it.id}, userId: $itemUserId, currentUserId: $userIdInt")

                            if (itemUserId == userIdInt) {
                                listingsList.add(it)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing listing: ${e.message}")
                    }
                }

                Log.d(TAG, "Total listings for user $userIdInt: ${listingsList.size}")
                listingsList.sortByDescending { it.timestamp }
                listingAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error fetching listings: ${error.message}")
            }
        })
    }

    private fun fetchEngagements() {
        val engagementsRef = database.reference.child("engagements")

        engagementsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                engagementsList.clear()

                for (engagementSnapshot in snapshot.children) {
                    try {
                        val engagement = engagementSnapshot.getValue(Engagement::class.java)
                        engagement?.let {
                            val itemUserId = it.getUserIdAsInt()
                            Log.d(TAG, "Found engagement: ${it.id}, userId: $itemUserId, currentUserId: $userIdInt")

                            if (itemUserId == userIdInt) {
                                engagementsList.add(it)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing engagement: ${e.message}")
                    }
                }

                Log.d(TAG, "Total engagements for user $userIdInt: ${engagementsList.size}")
                engagementsList.sortByDescending { it.timestamp }
                engagementAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error fetching engagements: ${error.message}")
            }
        })
    }
}
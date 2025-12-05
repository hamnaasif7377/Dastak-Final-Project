package com.DASTAK.i230613_i230658_i230736

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide

class organizationProfile : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var btnMenu: ImageView
    private lateinit var btnBack: ImageView
    private lateinit var btnMyEvents: Button
    private lateinit var bannerImage: ImageView
    private lateinit var foundationName: TextView
    private lateinit var locationText: TextView
    private lateinit var contactInfo: TextView
    private lateinit var descriptionText: TextView
    private lateinit var operationLocationsText: TextView
    private lateinit var programsText: TextView

    // Drawer menu items
    private lateinit var menuHome: LinearLayout
    private lateinit var menuEditProfile: LinearLayout
    private lateinit var menuCreateOpportunity: LinearLayout
    private lateinit var menuNotifications: LinearLayout
    private lateinit var menuLogout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_organization_profile)

        // Initialize views
        drawerLayout = findViewById(R.id.drawer_layout)
        btnMenu = findViewById(R.id.btnMenu)
        btnBack = findViewById(R.id.btnBack)
        btnMyEvents = findViewById(R.id.btnMyEvents)
        bannerImage = findViewById(R.id.bannerImage)
        foundationName = findViewById(R.id.foundationName)
        locationText = findViewById(R.id.location)
        contactInfo = findViewById(R.id.contactInfo)
        descriptionText = findViewById(R.id.organizationDescription)
        operationLocationsText = findViewById(R.id.operationLocationsList)
        programsText = findViewById(R.id.programsList)

        // Initialize drawer menu items
        menuHome = findViewById(R.id.menu_home)
        menuEditProfile = findViewById(R.id.menu_edit_profile)
        menuCreateOpportunity = findViewById(R.id.menu_create_opportunity)
        menuNotifications = findViewById(R.id.menu_notifications)
        menuLogout = findViewById(R.id.menu_logout)

        // Load profile data
        loadProfileData()

        // Open drawer when menu button is clicked
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(androidx.core.view.GravityCompat.START)
        }

        // Back button functionality
        btnBack.setOnClickListener {
            finish()
        }

        // My Events button
        btnMyEvents.setOnClickListener {
            val intent = Intent(this, EventsActivity::class.java)
            startActivity(intent)
        }

        // Drawer menu item click listeners
        menuHome.setOnClickListener {
            drawerLayout.closeDrawers()
        }

        menuEditProfile.setOnClickListener {
            drawerLayout.closeDrawers()
            val intent = Intent(this, EditProfileO::class.java)
            startActivity(intent)
        }

        menuCreateOpportunity.setOnClickListener {
            drawerLayout.closeDrawers()
            val intent = Intent(this, createOpportunityorg::class.java)
            startActivity(intent)
        }

        menuNotifications.setOnClickListener {
            drawerLayout.closeDrawers()
            val intent = Intent(this, NotificationActivity::class.java)
            startActivity(intent)
        }

        menuLogout.setOnClickListener {
            drawerLayout.closeDrawers()
            val sharedPref = getSharedPreferences("userPrefs", MODE_PRIVATE)
            sharedPref.edit().clear().apply()

            val intent = Intent(this, login_o::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload profile data when returning from edit page
        loadProfileData()
    }

    private fun loadProfileData() {
        val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        val name = sharedPref.getString("name", "Organization Name")
        val email = sharedPref.getString("email", "")
        val location = sharedPref.getString("location", "Location not set")
        val description = sharedPref.getString("description", "")
        val operationLocation = sharedPref.getString("operation_location", "")
        val programs = sharedPref.getString("programs", "")
        val profileImage = sharedPref.getString("profile_image", "")

        // Update UI
        foundationName.text = name
        locationText.text = location

        // Display email instead of "Contact info"
        if (!email.isNullOrEmpty()) {
            contactInfo.text = email
        }

        // Update description
        if (!description.isNullOrEmpty()) {
            descriptionText.text = description
        }

        // Update operation locations
        if (!operationLocation.isNullOrEmpty()) {
            operationLocationsText.text = formatListWithBullets(operationLocation)
        }

        // Update programs
        if (!programs.isNullOrEmpty()) {
            programsText.text = formatProgramsList(programs)
        }

        // Load profile image
        if (!profileImage.isNullOrEmpty()) {
            val imageUrl = Constants.BASE_URL + "uploads/profiles/" + profileImage
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.alkhidmat)
                .error(R.drawable.alkhidmat)
                .into(bannerImage)
        }
    }

    /**
     * Format multi-line text with bullet points
     */
    private fun formatListWithBullets(text: String): String {
        return text.split("\n")
            .filter { it.isNotBlank() }
            .joinToString("\n") { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("•")) trimmed else "• $trimmed"
            }
    }

    /**
     * Format programs list for display
     */
    private fun formatProgramsList(programs: String): String {
        return programs.split("\n")
            .filter { it.isNotBlank() }
            .joinToString("\n\n") { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("•")) trimmed else "• $trimmed"
            }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(androidx.core.view.GravityCompat.START)) {
            drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
package com.DASTAK.i230613_i230658_i230736

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout

class organizationProfile : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var btnMenu: ImageView
    private lateinit var btnBack: ImageView
    private lateinit var btnMyEvents: Button

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

        // Initialize drawer menu items
        menuHome = findViewById(R.id.menu_home)
        menuEditProfile = findViewById(R.id.menu_edit_profile)
        menuCreateOpportunity = findViewById(R.id.menu_create_opportunity)
        menuNotifications = findViewById(R.id.menu_notifications)
        menuLogout = findViewById(R.id.menu_logout)


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
            // Navigate to My Events activity
            val intent = Intent(this, EventsActivity::class.java)
            startActivity(intent)
        }

        // Drawer menu item click listeners
        menuHome.setOnClickListener {
            drawerLayout.closeDrawers()
            // Navigate to home or stay on current page
        }

        menuEditProfile.setOnClickListener {
            drawerLayout.closeDrawers()
            // Navigate to Edit Profile activity
            // val intent = Intent(this, EditProfileActivity::class.java)
            // startActivity(intent)
        }

        menuCreateOpportunity.setOnClickListener {
            drawerLayout.closeDrawers()
            // Navigate to Create Opportunity activity
            val intent = Intent(this, createOpportunityorg::class.java)
            startActivity(intent)
        }

        menuNotifications.setOnClickListener {
            drawerLayout.closeDrawers()
            // Navigate to Notifications activity
            // val intent = Intent(this, NotificationsActivity::class.java)
            // startActivity(intent)
        }

        menuLogout.setOnClickListener {
            drawerLayout.closeDrawers()
            // Logout logic - clear session and go to login
            // Clear any saved session data
            val sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE)
            sharedPref.edit().clear().apply()

            // Navigate to login screen
            val intent = Intent(this, login_o::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onBackPressed() {
        // Close drawer if open, otherwise perform normal back action
        if (drawerLayout.isDrawerOpen(androidx.core.view.GravityCompat.START)) {
            drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
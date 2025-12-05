package com.DASTAK.i230613_i230658_i230736

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout

class VolunteerHomeActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var btnMenu: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_volunteer_home)

        initializeViews()
        setupDrawerMenu()
        setupButtons()
    }

    private fun initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout)
        btnMenu = findViewById(R.id.btnMenu)

        // Menu button to open drawer
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun setupButtons() {
        // Browse Activities Button (main screen)
        findViewById<Button>(R.id.btnBrowseActivities).setOnClickListener {
            openBrowseActivities()
        }

        // My Contributions Button
        findViewById<Button>(R.id.btnMyContributions).setOnClickListener {
            // TODO: Open My Contributions Activity
            // val intent = Intent(this, MyContributionsActivity::class.java)
            // startActivity(intent)
        }

        // Saved Activities Button
        findViewById<Button>(R.id.btnSavedActivities).setOnClickListener {
            // TODO: Open Saved Activities Activity
            // val intent = Intent(this, SavedActivitiesActivity::class.java)
            // startActivity(intent)
        }
    }

    private fun setupDrawerMenu() {
        val navView = findViewById<LinearLayout>(R.id.nav_view)

        // Home - Close drawer (already on home)
        findViewById<LinearLayout>(R.id.menu_home).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        // Edit Profile
        findViewById<LinearLayout>(R.id.menu_edit_profile).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            // TODO: Open Edit Profile Activity
            // val intent = Intent(this, EditProfileActivity::class.java)
            // startActivity(intent)
        }

        // Browse Activities
        findViewById<LinearLayout>(R.id.menu_browse_activities).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            openBrowseActivities()
        }

        // Saved
        findViewById<LinearLayout>(R.id.menu_saved).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            // TODO: Open Saved Activities
        }

        // Notifications
        findViewById<LinearLayout>(R.id.menu_notifications).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            val intent = Intent(this, NotificationActivity::class.java)
            startActivity(intent)
        }

        // My Contributions
        findViewById<LinearLayout>(R.id.menu_my_contributions).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            // TODO: Open My Contributions Activity
        }

        // Logout
        findViewById<LinearLayout>(R.id.menu_logout).setOnClickListener {
            logoutUser()
        }
    }

    private fun openBrowseActivities() {
        val intent = Intent(this, BrowseActivitiesActivity::class.java)
        startActivity(intent)
    }

    private fun logoutUser() {
        val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            clear() // Clear all data
            apply()
        }

        val intent = Intent(this, RoleActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
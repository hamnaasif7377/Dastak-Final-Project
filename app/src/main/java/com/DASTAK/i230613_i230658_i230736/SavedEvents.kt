package com.DASTAK.i230613_i230658_i230736

import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class SavedEvents : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnBack: ImageView
    private lateinit var btnMenu: ImageView
    private lateinit var searchInput: EditText
    private lateinit var btnFilterDate: LinearLayout
    private lateinit var btnFilterOrganization: LinearLayout
    private lateinit var btnFilterLocation: LinearLayout

    private lateinit var savedEventsAdapter: SavedEventsAdapter
    private lateinit var progressDialog: ProgressDialog
    private lateinit var offlineSyncManager: OfflineSyncManager

    private val client = OkHttpClient()
    private val API_BASE_URL = Constants.BASE_URL

    private var userId: Int = -1

    private var allEvents = mutableListOf<Event>()
    private var selectedDate: String? = null
    private var selectedOrganization: String? = null
    private var selectedLocation: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_events)

        val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        userId = sharedPref.getInt("user_id", -1)


        if (userId == -1) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        offlineSyncManager = OfflineSyncManager(this)

        initializeViews()
        setupDrawerMenu()
        setupRecyclerView()
        setupSearch()
        setupFilters()

        if (NetworkStateReceiver.isNetworkAvailable(this)) {
            syncPendingOperations()
        }

        loadSavedEvents()
    }

    private fun initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout)
        recyclerView = findViewById(R.id.recyclerViewEvents)
        btnBack = findViewById(R.id.btnBack)
        btnMenu = findViewById(R.id.btnMenu)
        searchInput = findViewById(R.id.searchInput)
        btnFilterDate = findViewById(R.id.btnFilterDate)
        btnFilterOrganization = findViewById(R.id.btnFilterOrganization)
        btnFilterLocation = findViewById(R.id.btnFilterLocation)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Loading saved events...")
        progressDialog.setCancelable(false)

        // Back button - navigate to VolunteerHomeActivity
        btnBack.setOnClickListener {
            val intent = Intent(this, VolunteerHomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        // Menu button - open drawer
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun setupDrawerMenu() {
        // Home Menu Item
        findViewById<LinearLayout>(R.id.menu_home).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            val intent = Intent(this, VolunteerHomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        // Edit Profile
        findViewById<LinearLayout>(R.id.menu_edit_profile).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            Toast.makeText(this, "Edit Profile - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        // Browse Activities
        findViewById<LinearLayout>(R.id.menu_browse_activities).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            val intent = Intent(this, BrowseActivitiesActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        // Saved Events - already on this page
        findViewById<LinearLayout>(R.id.menu_saved).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        // Notifications
        findViewById<LinearLayout>(R.id.menu_notifications).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            val intent = Intent(this, NotificationActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        // My Contributions
        findViewById<LinearLayout>(R.id.menu_my_contributions).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            Toast.makeText(this, "My Contributions - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        // Logout
        findViewById<LinearLayout>(R.id.menu_logout).setOnClickListener {
            logoutUser()
        }
    }

    private fun logoutUser() {
        val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            clear()
            apply()
        }

        val intent = Intent(this, RoleActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }

    private fun setupRecyclerView() {
        savedEventsAdapter = SavedEventsAdapter(
            mutableListOf(),
            onEventClick = { event ->
                val intent = Intent(this, OpportunityDetailActivity::class.java)
                intent.putExtra("event_id", event.event_id)
                startActivity(intent)
            },
            onRemoveClick = { event ->
                showRemoveConfirmation(event)
            }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@SavedEvents)
            adapter = savedEventsAdapter
        }
    }

    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterEvents(s.toString().trim())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupFilters() {
        btnFilterDate.setOnClickListener { showDatePicker() }
        btnFilterOrganization.setOnClickListener { showOrganizationFilter() }
        btnFilterLocation.setOnClickListener { showLocationFilter() }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        val dialog = DatePickerDialog(
            this,
            { _, y, m, d ->
                cal.set(y, m, d)
                selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                applyFilters()
                Toast.makeText(this, "Filtered by date: $selectedDate", Toast.LENGTH_SHORT).show()
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        )
        dialog.show()
    }

    private fun showOrganizationFilter() {
        val list = allEvents.map { it.organizer.name }.distinct().toTypedArray()
        if (list.isEmpty()) {
            Toast.makeText(this, "No organizations available", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Filter by Organization")
            .setItems(list) { _, index ->
                selectedOrganization = list[index]
                applyFilters()
                Toast.makeText(this, "Filtered by: $selectedOrganization", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Clear Filter") { _, _ ->
                selectedOrganization = null
                applyFilters()
            }
            .show()
    }

    private fun showLocationFilter() {
        val list = allEvents.map { it.event_location }.distinct().toTypedArray()
        if (list.isEmpty()) {
            Toast.makeText(this, "No locations available", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Filter by Location")
            .setItems(list) { _, index ->
                selectedLocation = list[index]
                applyFilters()
                Toast.makeText(this, "Filtered by: $selectedLocation", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Clear Filter") { _, _ ->
                selectedLocation = null
                applyFilters()
            }
            .show()
    }

    private fun filterEvents(search: String) {
        var filtered = if (search.isEmpty()) {
            allEvents
        } else {
            allEvents.filter {
                it.event_name.contains(search, ignoreCase = true) ||
                        it.event_location.contains(search, ignoreCase = true) ||
                        it.organizer.name.contains(search, ignoreCase = true) ||
                        it.event_description.contains(search, ignoreCase = true)
            }
        }
        applyFiltersOnList(filtered)
    }

    private fun applyFilters() {
        filterEvents(searchInput.text.toString().trim())
    }

    private fun applyFiltersOnList(list: List<Event>) {
        var filtered = list.toMutableList()

        selectedDate?.let { date ->
            filtered = filtered.filter { it.event_date == date }.toMutableList()
        }

        selectedOrganization?.let { org ->
            filtered = filtered.filter { it.organizer.name == org }.toMutableList()
        }

        selectedLocation?.let { loc ->
            filtered = filtered.filter { it.event_location == loc }.toMutableList()
        }

        savedEventsAdapter.updateEvents(filtered)

        if (filtered.isEmpty()) {
            Toast.makeText(this, "No activities match your filters", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSavedEvents() {
        val isOnline = NetworkStateReceiver.isNetworkAvailable(this)

        if (!isOnline) {
            loadSavedEventsOffline()
            return
        }

        progressDialog.show()

        val url = "${API_BASE_URL}get_saved_events.php?user_id=$userId"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressDialog.dismiss()
                    loadSavedEventsOffline()
                    Toast.makeText(
                        this@SavedEvents,
                        "Loading offline saved events",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    progressDialog.dismiss()

                    try {
                        if (responseBody.isNullOrEmpty()) {
                            Toast.makeText(
                                this@SavedEvents,
                                "Empty response from server",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@runOnUiThread
                        }

                        Log.d("SavedEvents", "Response: $responseBody")

                        val jsonResponse = JSONObject(responseBody)
                        val status = jsonResponse.getString("status")

                        if (status == "success") {
                            val eventsArray = jsonResponse.getJSONArray("events")
                            val eventsList = mutableListOf<Event>()

                            for (i in 0 until eventsArray.length()) {
                                val eventObj = eventsArray.getJSONObject(i)
                                val organizerObj = eventObj.getJSONObject("organizer")

                                val event = Event(
                                    event_id = eventObj.getInt("event_id"),
                                    event_name = eventObj.getString("event_name"),
                                    event_location = eventObj.getString("event_location"),
                                    event_date = eventObj.getString("event_date"),
                                    event_time = if (eventObj.isNull("event_time")) null else eventObj.getString("event_time"),
                                    event_description = eventObj.getString("event_description"),
                                    poster_image = if (eventObj.isNull("poster_image")) null else eventObj.getString("poster_image"),
                                    volunteer_tasks = if (eventObj.isNull("volunteer_tasks")) null else eventObj.getString("volunteer_tasks"),
                                    things_to_bring = if (eventObj.isNull("things_to_bring")) null else eventObj.getString("things_to_bring"),
                                    meeting_point = if (eventObj.isNull("meeting_point")) null else eventObj.getString("meeting_point"),
                                    contact_info = if (eventObj.isNull("contact_info")) null else eventObj.getString("contact_info"),
                                    status = eventObj.getString("status"),
                                    participant_count = eventObj.getInt("participant_count"),
                                    created_at = eventObj.getString("created_at"),
                                    organizer = Organizer(
                                        user_id = organizerObj.getInt("user_id"),
                                        name = organizerObj.getString("name"),
                                        profile_image = if (organizerObj.isNull("profile_image")) null else organizerObj.getString("profile_image")
                                    )
                                    ,
                                    latitude = eventObj.optDouble("latitude", 0.0),
                                    longitude = eventObj.optDouble("longitude", 0.0)

                                )

                                eventsList.add(event)
                            }

                            allEvents.clear()
                            allEvents.addAll(eventsList)

                            savedEventsAdapter.updateEvents(allEvents)

                            if (eventsList.isEmpty()) {
                                Toast.makeText(
                                    this@SavedEvents,
                                    "No saved events yet. Start browsing!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    this@SavedEvents,
                                    "Loaded ${eventsList.size} saved events",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        } else {
                            val message = jsonResponse.optString("message", "Failed to load saved events")
                            Toast.makeText(this@SavedEvents, message, Toast.LENGTH_SHORT).show()
                        }

                    } catch (e: Exception) {
                        Toast.makeText(
                            this@SavedEvents,
                            "Error parsing data: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.e("SavedEvents", "Parse error", e)
                        Log.e("SavedEvents", "Response was: $responseBody")
                    }
                }
            }
        })
    }

    private fun showRemoveConfirmation(event: Event) {
        AlertDialog.Builder(this)
            .setTitle("Remove from Favorites")
            .setMessage("Are you sure you want to remove '${event.event_name}' from your saved events?")
            .setPositiveButton("Remove") { _, _ ->
                removeEventFromFavorites(event)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun removeEventFromFavorites(event: Event) {
        val formBody = FormBody.Builder()
            .add("user_id", userId.toString())
            .add("event_id", event.event_id.toString())
            .add("action", "unsave")
            .build()

        val request = Request.Builder()
            .url("${API_BASE_URL}save_event.php")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@SavedEvents,
                        "Network error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    try {
                        val jsonResponse = JSONObject(responseBody ?: "{}")
                        val status = jsonResponse.getString("status")
                        val message = jsonResponse.getString("message")

                        if (status == "success") {
                            allEvents.remove(event)
                            savedEventsAdapter.removeEvent(event)
                            Toast.makeText(this@SavedEvents, message, Toast.LENGTH_SHORT).show()
                            applyFilters()
                        } else {
                            Toast.makeText(this@SavedEvents, message, Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@SavedEvents,
                            "Error: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e("SavedEvents", "Error removing event", e)
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (NetworkStateReceiver.isNetworkAvailable(this)) {
            syncPendingOperations()
        }
        loadSavedEvents()
    }

    private fun loadSavedEventsOffline() {
        val cachedEvents = offlineSyncManager.getSavedEventsFromCache(userId)

        allEvents.clear()
        allEvents.addAll(cachedEvents)
        savedEventsAdapter.updateEvents(allEvents)

        if (cachedEvents.isEmpty()) {
            Toast.makeText(
                this,
                "No saved events available offline. Connect to internet to sync your saved events.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                this,
                "Showing ${cachedEvents.size} saved events (offline)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun syncPendingOperations() {
        if (!offlineSyncManager.hasPendingOperations()) return

        offlineSyncManager.syncWithServer(API_BASE_URL) { success, count ->
            runOnUiThread {
                if (success && count > 0) {
                    Toast.makeText(
                        this,
                        "Synced $count offline changes",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            val intent = Intent(this, VolunteerHomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }
}
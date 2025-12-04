package com.DASTAK.i230613_i230658_i230736

import android.app.DatePickerDialog
import android.app.ProgressDialog
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class BrowseActivitiesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnBack: ImageView
    private lateinit var btnMenu: ImageView
    private lateinit var searchInput: EditText
    private lateinit var btnFilterDate: LinearLayout
    private lateinit var btnFilterOrganization: LinearLayout
    private lateinit var btnFilterLocation: LinearLayout

    private lateinit var activitiesAdapter: EventsAdapterv
    private lateinit var progressDialog: ProgressDialog

    private val client = OkHttpClient()
    private val API_BASE_URL = Constants.BASE_URL

    // Store all events for filtering
    private var allEvents = mutableListOf<Event>()
    private var filteredEvents = mutableListOf<Event>()

    // Filter values
    private var selectedDate: String? = null
    private var selectedOrganization: String? = null
    private var selectedLocation: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse_activities)

        initializeViews()
        setupRecyclerView()
        setupSearchBar()
        setupFilters()
        loadAllActivities()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewActivities)
        btnBack = findViewById(R.id.btnBack)
        btnMenu = findViewById(R.id.btnMenu)
        searchInput = findViewById(R.id.searchInput)
        btnFilterDate = findViewById(R.id.btnFilterDate)
        btnFilterOrganization = findViewById(R.id.btnFilterOrganization)
        btnFilterLocation = findViewById(R.id.btnFilterLocation)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Loading activities...")
        progressDialog.setCancelable(false)

        // Back button
        btnBack.setOnClickListener {
            finish()
        }

        // Menu button
        btnMenu.setOnClickListener {
            // TODO: Open menu/drawer
            Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        activitiesAdapter = EventsAdapterv(mutableListOf()) { event ->
            // Handle event click - open event details
            Toast.makeText(this, "Clicked: ${event.event_name}", Toast.LENGTH_SHORT).show()
            // TODO: Open event details activity
            // val intent = Intent(this, EventDetailsActivity::class.java)
            // intent.putExtra("event_id", event.event_id)
            // startActivity(intent)
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@BrowseActivitiesActivity)
            adapter = activitiesAdapter
        }
    }

    private fun setupSearchBar() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                filterEvents(query)
            }
        })
    }

    private fun setupFilters() {
        // Date Filter
        btnFilterDate.setOnClickListener {
            showDatePicker()
        }

        // Organization Filter
        btnFilterOrganization.setOnClickListener {
            showOrganizationFilter()
        }

        // Location Filter
        btnFilterLocation.setOnClickListener {
            showLocationFilter()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                selectedDate = dateFormat.format(calendar.time)
                applyFilters()
                Toast.makeText(this, "Filtered by date: $selectedDate", Toast.LENGTH_SHORT).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun showOrganizationFilter() {
        // Get unique organizations from all events
        val organizations = allEvents.map { it.organizer.name }.distinct().toTypedArray()

        if (organizations.isEmpty()) {
            Toast.makeText(this, "No organizations available", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Filter by Organization")
            .setItems(organizations) { _, which ->
                selectedOrganization = organizations[which]
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
        // Get unique locations from all events
        val locations = allEvents.map { it.event_location }.distinct().toTypedArray()

        if (locations.isEmpty()) {
            Toast.makeText(this, "No locations available", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Filter by Location")
            .setItems(locations) { _, which ->
                selectedLocation = locations[which]
                applyFilters()
                Toast.makeText(this, "Filtered by: $selectedLocation", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Clear Filter") { _, _ ->
                selectedLocation = null
                applyFilters()
            }
            .show()
    }

    private fun filterEvents(searchQuery: String) {
        filteredEvents.clear()

        if (searchQuery.isEmpty()) {
            filteredEvents.addAll(allEvents)
        } else {
            filteredEvents.addAll(
                allEvents.filter {
                    it.event_name.contains(searchQuery, ignoreCase = true) ||
                            it.event_location.contains(searchQuery, ignoreCase = true) ||
                            it.organizer.name.contains(searchQuery, ignoreCase = true) ||
                            it.event_description.contains(searchQuery, ignoreCase = true)
                }
            )
        }

        applyFilters()
    }

    private fun applyFilters() {
        var filtered = if (filteredEvents.isEmpty()) allEvents else filteredEvents

        // Apply date filter
        if (selectedDate != null) {
            //filtered = filtered.filter { it.event_date == selectedDate }
        }

        // Apply organization filter
        if (selectedOrganization != null) {
            //filtered = filtered.filter { it.organizer.name == selectedOrganization }
        }

        // Apply location filter
        if (selectedLocation != null) {
           // filtered = filtered.filter { it.event_location == selectedLocation }
        }

        activitiesAdapter.updateEvents(filtered)

        if (filtered.isEmpty()) {
            Toast.makeText(this, "No activities match your filters", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadAllActivities() {
        progressDialog.show()

        // Get ALL upcoming events
        val url = "${API_BASE_URL}getevents.php?status=upcoming&limit=100"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@BrowseActivitiesActivity,
                        "Network error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("BrowseActivities", "Network error", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    progressDialog.dismiss()

                    try {
                        if (responseBody.isNullOrEmpty()) {
                            Toast.makeText(
                                this@BrowseActivitiesActivity,
                                "Empty response from server",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@runOnUiThread
                        }

                        Log.d("BrowseActivities", "Response: $responseBody")

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
                                    event_description = eventObj.getString("event_description"),
                                    poster_image = if (eventObj.isNull("poster_image")) null
                                    else eventObj.getString("poster_image"),
                                    status = eventObj.getString("status"),
                                    participant_count = eventObj.getInt("participant_count"),
                                    created_at = eventObj.getString("created_at"),
                                    organizer = Organizer(
                                        user_id = organizerObj.getInt("user_id"),
                                        name = organizerObj.getString("name"),
                                        profile_image = if (organizerObj.isNull("profile_image")) null
                                        else organizerObj.getString("profile_image")
                                    )
                                )

                                eventsList.add(event)
                            }

                            allEvents.clear()
                            allEvents.addAll(eventsList)
                            filteredEvents.clear()
                            filteredEvents.addAll(eventsList)

                            activitiesAdapter.updateEvents(filteredEvents)

                            if (eventsList.isEmpty()) {
                                Toast.makeText(
                                    this@BrowseActivitiesActivity,
                                    "No upcoming activities found.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    this@BrowseActivitiesActivity,
                                    "Found ${eventsList.size} activities",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        } else {
                            val message = jsonResponse.optString("message", "Failed to load activities")
                            Toast.makeText(
                                this@BrowseActivitiesActivity,
                                message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    } catch (e: Exception) {
                        Toast.makeText(
                            this@BrowseActivitiesActivity,
                            "Error parsing data: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.e("BrowseActivities", "Parse error", e)
                        Log.e("BrowseActivities", "Response was: $responseBody")
                    }
                }
            }
        })
    }
}
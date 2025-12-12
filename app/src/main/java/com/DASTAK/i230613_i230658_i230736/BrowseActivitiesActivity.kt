package com.DASTAK.i230613_i230658_i230736

import android.app.DatePickerDialog
import android.app.ProgressDialog
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
    private lateinit var offlineSyncManager: OfflineSyncManager

    private val client = OkHttpClient()
    private val API_BASE_URL = Constants.BASE_URL

    // List of ALL events returned from server
    private var allEvents = mutableListOf<Event>()

    // Active filters
    private var selectedDate: String? = null
    private var selectedOrganization: String? = null
    private var selectedLocation: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse_activities)

        offlineSyncManager = OfflineSyncManager(this)

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

        btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        activitiesAdapter = EventsAdapterv(mutableListOf()) { event ->
            val intent = Intent(this, OpportunityDetailActivity::class.java)
            intent.putExtra("event_id", event.event_id)
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = activitiesAdapter
    }

    // -------------------------------
    // SEARCH BAR
    // -------------------------------
    private fun setupSearchBar() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterEvents(s.toString().trim())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    // -------------------------------
    // FILTER BUTTONS
    // -------------------------------
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
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        )
        dialog.show()
    }

    private fun showOrganizationFilter() {
        val list = allEvents.map { it.organizer.name }.distinct().toTypedArray()
        if (list.isEmpty()) return

        AlertDialog.Builder(this)
            .setTitle("Filter by Organization")
            .setItems(list) { _, index ->
                selectedOrganization = list[index]
                applyFilters()
            }
            .setNeutralButton("Clear") { _, _ ->
                selectedOrganization = null
                applyFilters()
            }
            .show()
    }

    private fun showLocationFilter() {
        val list = allEvents.map { it.event_location }.distinct().toTypedArray()
        if (list.isEmpty()) return

        AlertDialog.Builder(this)
            .setTitle("Filter by Location")
            .setItems(list) { _, index ->
                selectedLocation = list[index]
                applyFilters()
            }
            .setNeutralButton("Clear") { _, _ ->
                selectedLocation = null
                applyFilters()
            }
            .show()
    }

    // -------------------------------
    // FILTER LOGIC
    // -------------------------------
    private fun filterEvents(search: String) {
        var filtered = if (search.isEmpty()) {
            allEvents
        } else {
            allEvents.filter {
                it.event_name.contains(search, true) ||
                        it.event_location.contains(search, true) ||
                        it.event_description.contains(search, true) ||
                        it.organizer.name.contains(search, true)
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

        activitiesAdapter.updateEvents(filtered)

        if (filtered.isEmpty()) {
            Toast.makeText(this, "No activities match filters", Toast.LENGTH_SHORT).show()
        }
    }

    // -------------------------------
    // LOAD EVENTS FROM SERVER (WITH OFFLINE SUPPORT)
    // -------------------------------
    private fun loadAllActivities() {
        // Check if network is available
        if (!NetworkStateReceiver.isNetworkAvailable(this)) {
            // Load from cache in offline mode
            loadFromCache()
            return
        }

        // Try to load from server
        progressDialog.show()

        val url = "${API_BASE_URL}getevents.php?status=upcoming&limit=100"
        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressDialog.dismiss()
                    // On failure, try to load from cache
                    Log.e("BrowseActivities", "Network error, loading from cache", e)
                    loadFromCache()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                runOnUiThread {
                    progressDialog.dismiss()

                    try {
                        val json = JSONObject(body!!)
                        if (json.getString("status") != "success") {
                            Toast.makeText(this@BrowseActivitiesActivity, "Failed to load events", Toast.LENGTH_SHORT).show()
                            loadFromCache()
                            return@runOnUiThread
                        }

                        val arr = json.getJSONArray("events")
                        val list = mutableListOf<Event>()

                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val org = obj.getJSONObject("organizer")

                            val event = Event(
                                event_id = obj.getInt("event_id"),
                                event_name = obj.getString("event_name"),
                                event_location = obj.getString("event_location"),
                                event_date = obj.getString("event_date"),
                                event_time = if (obj.isNull("event_time")) null else obj.getString("event_time"),
                                event_description = obj.getString("event_description"),
                                poster_image = if (obj.isNull("poster_image")) null else obj.getString("poster_image"),
                                volunteer_tasks = if (obj.isNull("volunteer_tasks")) null else obj.getString("volunteer_tasks"),
                                things_to_bring = if (obj.isNull("things_to_bring")) null else obj.getString("things_to_bring"),
                                meeting_point = if (obj.isNull("meeting_point")) null else obj.getString("meeting_point"),
                                contact_info = if (obj.isNull("contact_info")) null else obj.getString("contact_info"),
                                status = obj.getString("status"),
                                participant_count = obj.getInt("participant_count"),
                                created_at = obj.getString("created_at"),
                                organizer = Organizer(
                                    user_id = org.getInt("user_id"),
                                    name = org.getString("name"),
                                    profile_image = if (org.isNull("profile_image")) null else org.getString("profile_image")
                                ),
                                latitude = obj.getDouble("latitude"),
                                longitude = obj.getDouble("longitude")
                            )

                            list.add(event)
                            // Cache each event for offline use
                            offlineSyncManager.cacheEvent(event)
                        }

                        allEvents = list
                        activitiesAdapter.updateEvents(allEvents)

                    } catch (e: Exception) {
                        Log.e("BrowseActivities", "JSON ERROR", e)
                        loadFromCache()
                    }
                }
            }
        })
    }

    private fun loadFromCache() {
        val cachedEvents = offlineSyncManager.getCachedEvents()

        if (cachedEvents.isNotEmpty()) {
            allEvents = cachedEvents.toMutableList()
            activitiesAdapter.updateEvents(allEvents)
            Toast.makeText(
                this,
                "Showing ${cachedEvents.size} cached activities (Offline Mode)",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                this,
                "No cached activities available. Connect to the internet to browse activities.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
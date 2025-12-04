package com.DASTAK.i230613_i230658_i230736

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class EventsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnAddEvents: Button
    private lateinit var btnBack: ImageView
    private lateinit var eventsAdapter: EventsAdapter
    private lateinit var progressDialog: ProgressDialog

    private val client = OkHttpClient()
    private var userId: Int = -1

    // Your server URL
    private val API_BASE_URL = Constants.BASE_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_events)

        // Get user ID from SharedPreferences
        val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        userId = sharedPref.getInt("user_id", -1)

        if (userId == -1) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        setupRecyclerView()
        loadEvents()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewEvents)
        btnAddEvents = findViewById(R.id.btnAddEvents)
        btnBack = findViewById(R.id.btnBack)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Loading events...")
        progressDialog.setCancelable(false)

        // Back button
        btnBack.setOnClickListener {
            finish()
        }

        // Add Events button
        btnAddEvents.setOnClickListener {
            val intent = Intent(this, createOpportunityorg::class.java)
            startActivityForResult(intent, REQUEST_CODE_ADD_EVENT)
        }
    }

    private fun setupRecyclerView() {
        eventsAdapter = EventsAdapter(mutableListOf()) { event ->
            // Handle event click - you can open event details here
            Toast.makeText(this, "Clicked: ${event.event_name}", Toast.LENGTH_SHORT).show()
            // TODO: Open event details activity
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@EventsActivity)
            adapter = eventsAdapter
        }
    }

    private fun loadEvents() {
        progressDialog.show()

        // Build URL with user_id filter to get only this organization's events
        val url = "${API_BASE_URL}getevents.php?user_id=$userId"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@EventsActivity,
                        "Network error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("EventsActivity", "Network error", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    progressDialog.dismiss()

                    try {
                        if (responseBody.isNullOrEmpty()) {
                            Toast.makeText(
                                this@EventsActivity,
                                "Empty response from server",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@runOnUiThread
                        }

                        Log.d("EventsActivity", "Response: $responseBody")

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

                            eventsAdapter.updateEvents(eventsList)

                            if (eventsList.isEmpty()) {
                                Toast.makeText(
                                    this@EventsActivity,
                                    "No events found. Create your first event!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        } else {
                            val message = jsonResponse.optString("message", "Failed to load events")
                            Toast.makeText(this@EventsActivity, message, Toast.LENGTH_SHORT).show()
                        }

                    } catch (e: Exception) {
                        Toast.makeText(
                            this@EventsActivity,
                            "Error parsing data: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.e("EventsActivity", "Parse error", e)
                        Log.e("EventsActivity", "Response was: $responseBody")
                    }
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_ADD_EVENT && resultCode == RESULT_OK) {
            // Event was posted successfully, reload the list
            Toast.makeText(this, "Event posted! Refreshing list...", Toast.LENGTH_SHORT).show()
            loadEvents()
        }
    }

    companion object {
        private const val REQUEST_CODE_ADD_EVENT = 100
    }
}
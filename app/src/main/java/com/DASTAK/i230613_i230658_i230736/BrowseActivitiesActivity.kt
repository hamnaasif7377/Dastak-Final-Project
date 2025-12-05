package com.DASTAK.i230613_i230658_i230736

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class BrowseActivitiesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnBack: ImageView
    private lateinit var activitiesAdapter: EventsAdapterv
    private lateinit var progressDialog: ProgressDialog

    private val client = OkHttpClient()

    // Your server URL
    private val API_BASE_URL = Constants.BASE_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse_activities)

        initializeViews()
        setupRecyclerView()
        loadAllActivities()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewActivities)
        btnBack = findViewById(R.id.btnBack)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Loading activities...")
        progressDialog.setCancelable(false)

        // Back button
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        activitiesAdapter = EventsAdapterv(mutableListOf()) { event ->
            // Handle event click - open event details
            Toast.makeText(this, "Clicked: ${event.event_name}", Toast.LENGTH_SHORT).show()
            // TODO: Open event details activity where volunteer can register
             val intent = Intent(this, OpportunityDetailActivity::class.java)
             intent.putExtra("event_id", event.event_id)
             startActivity(intent)
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@BrowseActivitiesActivity)
            adapter = activitiesAdapter
        }
    }

    private fun loadAllActivities() {
        progressDialog.show()

        // Get ALL upcoming events (no user_id filter, status=upcoming)
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
                                    event_time = if (eventObj.isNull("event_time")) null else eventObj.getString("event_time"),
                                    event_description = eventObj.getString("event_description"),
                                    poster_image = if (eventObj.isNull("poster_image")) null
                                    else eventObj.getString("poster_image"),
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
                                        profile_image = if (organizerObj.isNull("profile_image")) null
                                        else organizerObj.getString("profile_image")
                                    )
                                )

                                eventsList.add(event)
                            }

                            activitiesAdapter.updateEvents(eventsList)

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
                            Toast.makeText(this@BrowseActivitiesActivity, message, Toast.LENGTH_SHORT).show()
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
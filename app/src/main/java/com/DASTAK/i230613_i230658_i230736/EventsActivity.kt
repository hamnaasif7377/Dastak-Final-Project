package com.DASTAK.i230613_i230658_i230736

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
    private lateinit var btnMenu: ImageView
    private lateinit var eventsAdapter: EventsAdapterOrg
    private lateinit var progressDialog: ProgressDialog

    private val client = OkHttpClient()
    private var userId: Int = -1

    private val API_BASE_URL = Constants.BASE_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_events)

        // Load user ID
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
        btnMenu = findViewById(R.id.btnMenu)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Loading events...")
        progressDialog.setCancelable(false)

        btnBack.setOnClickListener { finish() }

        btnMenu.setOnClickListener {
            Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show()
        }

        btnAddEvents.setOnClickListener {
            val intent = Intent(this, createOpportunityorg::class.java)
            startActivityForResult(intent, REQUEST_CODE_ADD_EVENT)
        }
    }

    private fun setupRecyclerView() {
        eventsAdapter = EventsAdapterOrg(
            mutableListOf(),
            onEditClick = { event ->
                val intent = Intent(this, createOpportunityorg::class.java)
                intent.putExtra("MODE", "EDIT")
                intent.putExtra("event_id", event.event_id)
                intent.putExtra("event_name", event.event_name)
                intent.putExtra("event_location", event.event_location)
                intent.putExtra("event_date", event.event_date)
                intent.putExtra("event_description", event.event_description)
                intent.putExtra("poster_image", event.poster_image)
                startActivityForResult(intent, REQUEST_CODE_EDIT_EVENT)
            },
            onDeleteClick = { event ->
                showDeleteConfirmation(event)
            },
            onViewClick = { event ->
                val intent = Intent(this, OpportunityDetailActivity::class.java)
                intent.putExtra("EVENT_ID", event.event_id)
                startActivity(intent)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = eventsAdapter
    }

    private fun showDeleteConfirmation(event: Event) {
        AlertDialog.Builder(this)
            .setTitle("Delete Event")
            .setMessage("Are you sure you want to delete '${event.event_name}'?")
            .setPositiveButton("Delete") { _, _ ->
                deleteEvent(event)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteEvent(event: Event) {
        progressDialog.setMessage("Deleting event...")
        progressDialog.show()

        val url = "${API_BASE_URL}deleteevent.php"

        val formBody = FormBody.Builder()
            .add("event_id", event.event_id.toString())
            .add("user_id", userId.toString())
            .build()

        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(this@EventsActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()

                runOnUiThread {
                    progressDialog.dismiss()
                    try {
                        if (body.isNullOrEmpty()) {
                            Toast.makeText(this@EventsActivity, "Empty server response", Toast.LENGTH_SHORT).show()
                            return@runOnUiThread
                        }

                        val json = JSONObject(body)
                        val status = json.getString("status")
                        val message = json.getString("message")

                        Toast.makeText(this@EventsActivity, message, Toast.LENGTH_SHORT).show()

                        if (status == "success") {
                            eventsAdapter.removeEvent(event.event_id)
                        }

                    } catch (e: Exception) {
                        Toast.makeText(this@EventsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun loadEvents() {
        progressDialog.show()

        val url = "${API_BASE_URL}getevents.php?user_id=$userId"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(this@EventsActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()

                runOnUiThread {
                    progressDialog.dismiss()
                    try {
                        if (body.isNullOrEmpty()) {
                            Toast.makeText(this@EventsActivity, "Empty response", Toast.LENGTH_SHORT).show()
                            return@runOnUiThread
                        }

                        val json = JSONObject(body)
                        val status = json.getString("status")

                        if (status == "success") {
                            val eventsArray = json.getJSONArray("events")
                            val list = mutableListOf<Event>()

                            for (i in 0 until eventsArray.length()) {
                                val eventObj = eventsArray.getJSONObject(i)
                                val orgObj = eventObj.getJSONObject("organizer")

                                list.add(
                                    Event(
                                        event_id = eventObj.getInt("event_id"),
                                        event_name = eventObj.getString("event_name"),
                                        event_location = eventObj.getString("event_location"),
                                        event_date = eventObj.getString("event_date"),
                                        event_time = eventObj.optString("event_time", null),
                                        event_description = eventObj.getString("event_description"),
                                        poster_image = eventObj.optString("poster_image", null),
                                        volunteer_tasks = eventObj.optString("volunteer_tasks", null),
                                        things_to_bring = eventObj.optString("things_to_bring", null),
                                        meeting_point = eventObj.optString("meeting_point", null),
                                        contact_info = eventObj.optString("contact_info", null),
                                        status = eventObj.getString("status"),
                                        participant_count = eventObj.getInt("participant_count"),
                                        created_at = eventObj.getString("created_at"),
                                        organizer = Organizer(
                                            user_id = orgObj.getInt("user_id"),
                                            name = orgObj.getString("name"),
                                            profile_image = orgObj.optString("profile_image", null)
                                        )
                                    )
                                )
                            }

                            eventsAdapter.updateEvents(list)

                            if (list.isEmpty()) {
                                Toast.makeText(this@EventsActivity, "No events found", Toast.LENGTH_SHORT).show()
                            }

                        } else {
                            Toast.makeText(this@EventsActivity, json.optString("message", "Failed"), Toast.LENGTH_SHORT).show()
                        }

                    } catch (e: Exception) {
                        Toast.makeText(this@EventsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if ((requestCode == REQUEST_CODE_ADD_EVENT || requestCode == REQUEST_CODE_EDIT_EVENT) && resultCode == RESULT_OK) {
            Toast.makeText(this, "Event saved! Refreshing...", Toast.LENGTH_SHORT).show()
            loadEvents()
        }
    }

    companion object {
        private const val REQUEST_CODE_ADD_EVENT = 100
        private const val REQUEST_CODE_EDIT_EVENT = 101
    }
}

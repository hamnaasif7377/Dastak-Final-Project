package com.DASTAK.i230613_i230658_i230736

import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class OpportunityDetailActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var btnMenu: ImageView
    private lateinit var eventDetailImage: ImageView
    private lateinit var eventDetailTitle: TextView
    private lateinit var eventDetailOrganization: TextView
    private lateinit var eventDetailLocation: TextView
    private lateinit var eventDetailDateTime: TextView
    private lateinit var eventDetailOverview: TextView
    private lateinit var eventDetailTasks: TextView
    private lateinit var eventDetailBring: TextView
    private lateinit var eventDetailMeetingPoint: TextView
    private lateinit var eventDetailContact: TextView
    private lateinit var btnRegister: Button

    private lateinit var progressDialog: ProgressDialog
    private val client = OkHttpClient()
    private val API_BASE_URL = Constants.BASE_URL

    private var eventId: Int = -1
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_opportunity_detail)

        // Get event_id from intent
        eventId = intent.getIntExtra("event_id", -1)

        if (eventId == -1) {
            Toast.makeText(this, "Error: Event not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Get user_id from SharedPreferences
        val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        userId = sharedPref.getInt("user_id", -1)

        initializeViews()
        loadEventDetails()
    }

    private fun initializeViews() {
        btnBack = findViewById(R.id.btnBack)
        btnMenu = findViewById(R.id.btnMenu)
        eventDetailImage = findViewById(R.id.eventDetailImage)
        eventDetailTitle = findViewById(R.id.eventDetailTitle)
        eventDetailOrganization = findViewById(R.id.eventDetailOrganization)
        eventDetailLocation = findViewById(R.id.eventDetailLocation)
        eventDetailDateTime = findViewById(R.id.eventDetailDateTime)
        eventDetailOverview = findViewById(R.id.eventDetailOverview)
        eventDetailTasks = findViewById(R.id.eventDetailTasks)
        eventDetailBring = findViewById(R.id.eventDetailBring)
        eventDetailMeetingPoint = findViewById(R.id.eventDetailMeetingPoint)
        eventDetailContact = findViewById(R.id.eventDetailContact)
        btnRegister = findViewById(R.id.btnRegister)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Loading event details...")
        progressDialog.setCancelable(false)

        // Back button
        btnBack.setOnClickListener {
            finish()
        }

        // Menu button
        btnMenu.setOnClickListener {
            // TODO: Open menu
            Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show()
        }

        // Register button
        btnRegister.setOnClickListener {
            registerForEvent()
        }
    }

    private fun loadEventDetails() {
        progressDialog.show()

        val url = "${API_BASE_URL}get_event_detail.php?event_id=$eventId"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@OpportunityDetailActivity,
                        "Network error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("OpportunityDetail", "Network error", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    progressDialog.dismiss()

                    try {
                        if (responseBody.isNullOrEmpty()) {
                            Toast.makeText(
                                this@OpportunityDetailActivity,
                                "Empty response from server",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@runOnUiThread
                        }

                        Log.d("OpportunityDetail", "Response: $responseBody")

                        val jsonResponse = JSONObject(responseBody)
                        val status = jsonResponse.getString("status")

                        if (status == "success") {
                            val eventObj = jsonResponse.getJSONObject("event")
                            displayEventDetails(eventObj)
                        } else {
                            val message = jsonResponse.optString("message", "Failed to load event details")
                            Toast.makeText(this@OpportunityDetailActivity, message, Toast.LENGTH_LONG).show()
                            finish()
                        }

                    } catch (e: Exception) {
                        Toast.makeText(
                            this@OpportunityDetailActivity,
                            "Error parsing data: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.e("OpportunityDetail", "Parse error", e)
                        Log.e("OpportunityDetail", "Response was: $responseBody")
                    }
                }
            }
        })
    }

    private fun displayEventDetails(eventObj: JSONObject) {
        try {
            val organizerObj = eventObj.getJSONObject("organizer")

            // Basic info
            eventDetailTitle.text = eventObj.getString("event_name")
            eventDetailOrganization.text = organizerObj.getString("name")
            eventDetailLocation.text = eventObj.getString("event_location")

            // Date and time
            val date = eventObj.getString("event_date")
            val time = if (eventObj.isNull("event_time")) null else eventObj.getString("event_time")
            eventDetailDateTime.text = formatDateTime(date, time)

            // Overview (description)
            eventDetailOverview.text = eventObj.getString("event_description")

            // Volunteer tasks
            if (!eventObj.isNull("volunteer_tasks") && eventObj.getString("volunteer_tasks").isNotEmpty()) {
                eventDetailTasks.text = eventObj.getString("volunteer_tasks")
            } else {
                eventDetailTasks.text = "Tasks will be assigned on the day of the event."
            }

            // Things to bring
            if (!eventObj.isNull("things_to_bring") && eventObj.getString("things_to_bring").isNotEmpty()) {
                eventDetailBring.text = eventObj.getString("things_to_bring")
            } else {
                eventDetailBring.text = "No specific items required."
            }

            // Meeting point
            if (!eventObj.isNull("meeting_point") && eventObj.getString("meeting_point").isNotEmpty()) {
                eventDetailMeetingPoint.text = eventObj.getString("meeting_point")
            } else {
                eventDetailMeetingPoint.text = "Meeting point will be shared after registration."
            }

            // Contact info
            if (!eventObj.isNull("contact_info") && eventObj.getString("contact_info").isNotEmpty()) {
                eventDetailContact.text = eventObj.getString("contact_info")
            } else {
                val email = organizerObj.getString("email")
                eventDetailContact.text = email
            }

            // Load event poster image
            if (!eventObj.isNull("poster_image")) {
                val posterImage = eventObj.getString("poster_image")
                if (posterImage.isNotEmpty()) {
                    val imageUrl = Constants.BASE_URL + posterImage

                    Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.flood_relief)
                        .error(R.drawable.flood_relief)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(eventDetailImage)
                }
            }

        } catch (e: Exception) {
            Log.e("OpportunityDetail", "Error displaying details", e)
            Toast.makeText(this, "Error displaying event details", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatDateTime(dateString: String, timeString: String?): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            val formattedDate = outputFormat.format(date ?: Date())

            if (timeString != null && timeString.isNotEmpty()) {
                "$formattedDate · $timeString"
            } else {
                formattedDate
            }
        } catch (e: Exception) {
            if (timeString != null && timeString.isNotEmpty()) {
                "$dateString · $timeString"
            } else {
                dateString
            }
        }
    }

    private fun registerForEvent() {
        if (userId == -1) {
            Toast.makeText(this, "Please login to register", Toast.LENGTH_SHORT).show()
            return
        }

        // TODO: Implement registration API call
        Toast.makeText(this, "Registration feature coming soon!", Toast.LENGTH_SHORT).show()

        // This will be implemented in the next step
        // For now, just show a success message
    }
}
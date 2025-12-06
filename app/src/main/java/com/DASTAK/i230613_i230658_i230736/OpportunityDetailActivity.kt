package com.DASTAK.i230613_i230658_i230736

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
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
    private lateinit var btnFavorite: ImageView
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
    private lateinit var offlineSyncManager: OfflineSyncManager

    private var eventId: Int = -1
    private var userId: Int = -1
    private var isSaved: Boolean = false
    private var currentEvent: Event? = null
    private var registrationStatus: String = "none"

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

        // Initialize offline sync manager
        offlineSyncManager = OfflineSyncManager(this)

        initializeViews()
        loadEventDetails()
    }

    private fun initializeViews() {
        btnBack = findViewById(R.id.btnBack)
        btnMenu = findViewById(R.id.btnMenu)
        btnFavorite = findViewById(R.id.btnFavorite)
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
            Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show()
        }

        // Favorite button
        btnFavorite.setOnClickListener {
            toggleFavorite()
        }

        // Register button
        btnRegister.setOnClickListener {
            registerForEvent()
        }
    }

    private fun loadEventDetails() {
        // Check if network is available
        if (!NetworkStateReceiver.isNetworkAvailable(this)) {
            // Load from cache in offline mode
            loadEventFromCache()
            return
        }

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
                    Log.e("OpportunityDetail", "Network error, loading from cache", e)
                    // On failure, try to load from cache
                    loadEventFromCache()
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
                            loadEventFromCache()
                            return@runOnUiThread
                        }

                        Log.d("OpportunityDetail", "Response: $responseBody")

                        val jsonResponse = JSONObject(responseBody)
                        val status = jsonResponse.getString("status")

                        if (status == "success") {
                            val eventObj = jsonResponse.getJSONObject("event")
                            displayEventDetails(eventObj)
                            // Check if event is saved
                            checkIfEventIsSaved()
                            // Check registration status
                            checkRegistrationStatus()
                        } else {
                            val message = jsonResponse.optString("message", "Failed to load event details")
                            Toast.makeText(this@OpportunityDetailActivity, message, Toast.LENGTH_SHORT).show()
                            loadEventFromCache()
                        }

                    } catch (e: Exception) {
                        Toast.makeText(
                            this@OpportunityDetailActivity,
                            "Error parsing data: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e("OpportunityDetail", "Parse error", e)
                        Log.e("OpportunityDetail", "Response was: $responseBody")
                        loadEventFromCache()
                    }
                }
            }
        })
    }

    private fun loadEventFromCache() {
        val cachedEvent = offlineSyncManager.getCachedEvent(eventId)

        if (cachedEvent != null) {
            currentEvent = cachedEvent
            displayEventFromCache(cachedEvent)

            // Check if event is saved locally
            isSaved = offlineSyncManager.isEventSavedLocally(eventId)
            updateFavoriteIcon()

            Toast.makeText(
                this,
                "Showing cached event (Offline Mode)",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                this,
                "Event not available offline. Please connect to the internet.",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    private fun displayEventFromCache(event: Event) {
        // Basic info
        eventDetailTitle.text = event.event_name
        eventDetailOrganization.text = event.organizer.name
        eventDetailLocation.text = event.event_location

        // Date and time
        eventDetailDateTime.text = formatDateTime(event.event_date, event.event_time)

        // Overview (description)
        eventDetailOverview.text = event.event_description

        // Volunteer tasks
        if (!event.volunteer_tasks.isNullOrEmpty()) {
            eventDetailTasks.text = event.volunteer_tasks
        } else {
            eventDetailTasks.text = "Tasks will be assigned on the day of the event."
        }

        // Things to bring
        if (!event.things_to_bring.isNullOrEmpty()) {
            eventDetailBring.text = event.things_to_bring
        } else {
            eventDetailBring.text = "No specific items required."
        }

        // Meeting point
        if (!event.meeting_point.isNullOrEmpty()) {
            eventDetailMeetingPoint.text = event.meeting_point
        } else {
            eventDetailMeetingPoint.text = "Meeting point will be shared after registration."
        }

        // Contact info
        if (!event.contact_info.isNullOrEmpty()) {
            eventDetailContact.text = event.contact_info
        } else {
            eventDetailContact.text = "Contact info not available"
        }

        // Load event poster image
        if (!event.poster_image.isNullOrEmpty()) {
            val imageUrl = Constants.BASE_URL + event.poster_image

            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.flood_relief)
                .error(R.drawable.flood_relief)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(eventDetailImage)
        } else {
            eventDetailImage.setImageResource(R.drawable.flood_relief)
        }
    }

    private fun displayEventDetails(eventObj: JSONObject) {
        try {
            val organizerObj = eventObj.getJSONObject("organizer")

            // Create Event object
            currentEvent = Event(
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
            )

            // Cache the event for offline access
            currentEvent?.let { offlineSyncManager.cacheEvent(it) }

            // Display using the Event object
            displayEventFromCache(currentEvent!!)

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

        progressDialog.setMessage("Sending registration request...")
        progressDialog.show()

        val formBody = FormBody.Builder()
            .add("volunteer_id", userId.toString())
            .add("event_id", eventId.toString())
            .build()

        val request = Request.Builder()
            .url("${API_BASE_URL}register_for_event.php")
            .post(formBody)
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
                    Log.e("OpportunityDetail", "Registration error", e)
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

                        Log.d("OpportunityDetail", "Registration response: $responseBody")

                        val jsonResponse = JSONObject(responseBody)
                        val status = jsonResponse.getString("status")
                        val message = jsonResponse.getString("message")

                        if (status == "success") {
                            Toast.makeText(
                                this@OpportunityDetailActivity,
                                message,
                                Toast.LENGTH_LONG
                            ).show()

                            // Navigate to confirmation if RegisterConfirmation activity exists
                            try {
                                val intent = Intent(
                                    this@OpportunityDetailActivity,
                                    RegisterConfirmation::class.java
                                )
                                startActivity(intent)
                            } catch (e: Exception) {
                                Log.d("OpportunityDetail", "RegisterConfirmation activity not found")
                            }

                            registrationStatus = "pending"
                            updateRegisterButton()

                        } else {
                            Toast.makeText(
                                this@OpportunityDetailActivity,
                                message,
                                Toast.LENGTH_LONG
                            ).show()
                        }

                    } catch (e: Exception) {
                        Toast.makeText(
                            this@OpportunityDetailActivity,
                            "Error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.e("OpportunityDetail", "Parse error", e)
                    }
                }
            }
        })
    }

    private fun checkRegistrationStatus() {
        if (userId == -1) return

        val url = "${API_BASE_URL}check_registration_status.php?event_id=$eventId&volunteer_id=$userId"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OpportunityDetail", "Failed to check registration status", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    try {
                        if (!responseBody.isNullOrEmpty()) {
                            val jsonResponse = JSONObject(responseBody)
                            if (jsonResponse.getString("status") == "success") {
                                registrationStatus = jsonResponse.getString("registration_status")
                                updateRegisterButton()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("OpportunityDetail", "Error checking registration status", e)
                    }
                }
            }
        })
    }

    private fun updateRegisterButton() {
        when (registrationStatus) {
            "pending" -> {
                btnRegister.text = "Request Pending"
                btnRegister.isEnabled = false
                btnRegister.alpha = 0.6f
            }
            "accepted" -> {
                btnRegister.text = "Registered"
                btnRegister.isEnabled = false
                btnRegister.alpha = 0.6f
            }
            "rejected" -> {
                btnRegister.text = "Request Rejected"
                btnRegister.isEnabled = false
                btnRegister.alpha = 0.6f
            }
            else -> {
                btnRegister.text = "Register"
                btnRegister.isEnabled = true
                btnRegister.alpha = 1.0f
            }
        }
    }

    private fun checkIfEventIsSaved() {
        if (userId == -1) return // Not logged in, skip check

        // First check local cache
        isSaved = offlineSyncManager.isEventSavedLocally(eventId)
        updateFavoriteIcon()

        // If online, verify with server
        if (!NetworkStateReceiver.isNetworkAvailable(this)) {
            return
        }

        val url = "${API_BASE_URL}checkifeventsaved.php?user_id=$userId&event_id=$eventId"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OpportunityDetail", "Failed to check saved status", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    try {
                        if (!responseBody.isNullOrEmpty()) {
                            val jsonResponse = JSONObject(responseBody)
                            val status = jsonResponse.getString("status")

                            if (status == "success") {
                                isSaved = jsonResponse.getBoolean("is_saved")
                                updateFavoriteIcon()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("OpportunityDetail", "Error checking saved status", e)
                    }
                }
            }
        })
    }

    private fun toggleFavorite() {
        if (userId == -1) {
            Toast.makeText(this, "Please login to save events", Toast.LENGTH_SHORT).show()
            return
        }

        val action = if (isSaved) "unsave" else "save"
        val isOnline = NetworkStateReceiver.isNetworkAvailable(this)

        if (!isOnline) {
            // Queue the operation for later sync
            if (action == "save") {
                offlineSyncManager.addPendingSave(userId, eventId)
                isSaved = true
                Toast.makeText(
                    this,
                    "Event saved offline. Will sync when online.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                offlineSyncManager.addPendingUnsave(userId, eventId)
                isSaved = false
                Toast.makeText(
                    this,
                    "Event unsaved offline. Will sync when online.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            updateFavoriteIcon()
            return
        }

        // Online - send to server immediately
        val formBody = FormBody.Builder()
            .add("user_id", userId.toString())
            .add("event_id", eventId.toString())
            .add("action", action)
            .build()

        val request = Request.Builder()
            .url("${API_BASE_URL}togglefavorite.php")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    // Queue for offline sync on failure
                    if (action == "save") {
                        offlineSyncManager.addPendingSave(userId, eventId)
                        isSaved = true
                    } else {
                        offlineSyncManager.addPendingUnsave(userId, eventId)
                        isSaved = false
                    }
                    updateFavoriteIcon()
                    Toast.makeText(
                        this@OpportunityDetailActivity,
                        "Queued for sync when online",
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
                            isSaved = jsonResponse.getBoolean("is_saved")

                            // Update local cache immediately
                            if (isSaved) {
                                offlineSyncManager.addPendingSave(userId, eventId)
                            } else {
                                offlineSyncManager.addPendingUnsave(userId, eventId)
                            }

                            updateFavoriteIcon()
                            Toast.makeText(this@OpportunityDetailActivity, message, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@OpportunityDetailActivity, message, Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        // If parsing fails, queue for offline sync
                        if (action == "save") {
                            offlineSyncManager.addPendingSave(userId, eventId)
                            isSaved = true
                        } else {
                            offlineSyncManager.addPendingUnsave(userId, eventId)
                            isSaved = false
                        }
                        updateFavoriteIcon()
                        Toast.makeText(
                            this@OpportunityDetailActivity,
                            "Saved locally. Will sync when online.",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e("OpportunityDetail", "Error toggling favorite", e)
                    }
                }
            }
        })
    }

    private fun updateFavoriteIcon() {
        if (isSaved) {
            btnFavorite.setImageResource(R.drawable.ic_favorite_filled)
        } else {
            btnFavorite.setImageResource(R.drawable.ic_favorite_border)
        }
    }

    override fun onResume() {
        super.onResume()
        // Sync pending operations if back online
        if (NetworkStateReceiver.isNetworkAvailable(this) &&
            offlineSyncManager.hasPendingOperations()) {
            offlineSyncManager.syncWithServer(API_BASE_URL) { success, count ->
                runOnUiThread {
                    if (success && count > 0) {
                        Toast.makeText(
                            this,
                            "Synced $count offline changes",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Refresh saved status after sync
                        checkIfEventIsSaved()
                    }
                }
            }
        }
    }
}
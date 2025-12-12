package com.DASTAK.i230613_i230658_i230736

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
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
import org.osmdroid.config.Configuration
import org.osmdroid.views.MapView
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

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
    private lateinit var mapView: MapView

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

        offlineSyncManager = OfflineSyncManager(this)

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

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
        mapView = findViewById(R.id.eventMapView)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Loading event details...")
        progressDialog.setCancelable(false)

        btnBack.setOnClickListener { finish() }
        btnMenu.setOnClickListener { Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show() }
        btnFavorite.setOnClickListener { toggleFavorite() }
        btnRegister.setOnClickListener { registerForEvent() }
    }

    private fun loadEventDetails() {
        progressDialog.show()

        val request = Request.Builder()
            .url("$API_BASE_URL/get_event_detail.php?event_id=$eventId")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(this@OpportunityDetailActivity, "Failed to load event", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (body.isNullOrEmpty()) {
                    runOnUiThread {
                        progressDialog.dismiss()
                        Toast.makeText(this@OpportunityDetailActivity, "Empty response", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                try {
                    val json = JSONObject(body)
                    if (json.getString("status") == "success") {
                        val eventJson = json.getJSONObject("event")
                        currentEvent = Event.fromJson(eventJson)
                        runOnUiThread {
                            progressDialog.dismiss()
                            displayEventDetails(currentEvent!!)
                            setupMap(currentEvent!!)
                        }
                    } else {
                        runOnUiThread {
                            progressDialog.dismiss()
                            Toast.makeText(this@OpportunityDetailActivity, json.getString("message"), Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        progressDialog.dismiss()
                        Log.e("OpportunityDetail", "JSON parsing error", e)
                        Toast.makeText(this@OpportunityDetailActivity, "Error parsing event details", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun setupMap(event: Event) {
        mapView.setMultiTouchControls(true)

        val lat = event.latitude ?: 33.6844
        val lon = event.longitude ?: 73.0479

        if (lat != null && lon != null) {

            val startPoint = GeoPoint(lat, lon)

            mapView.controller.setZoom(15.0)
            mapView.controller.setCenter(startPoint)

            // Add marker
            val marker = Marker(mapView)
            marker.position = startPoint
            marker.title = event.event_name
            mapView.overlays.add(marker)

        } else {
            Toast.makeText(this, "Invalid coordinates", Toast.LENGTH_SHORT).show()
        }
    }



    private fun displayEventDetails(event: Event) {
        eventDetailTitle.text = event.event_name
        eventDetailOrganization.text = event.organizer.name
        eventDetailLocation.text = event.event_location
        eventDetailDateTime.text = formatDateTime(event.event_date, event.event_time)
        eventDetailOverview.text = event.event_description
        eventDetailTasks.text = event.volunteer_tasks ?: "Tasks will be assigned on the day of the event."
        eventDetailBring.text = event.things_to_bring ?: "No specific items required."
        eventDetailMeetingPoint.text = event.meeting_point ?: "Meeting point will be shared after registration."
        eventDetailContact.text = event.contact_info ?: "Contact info not available."

        if (!event.poster_image.isNullOrEmpty()) {
            Glide.with(this)
                .load(Constants.BASE_URL + event.poster_image)
                .placeholder(R.drawable.flood_relief)
                .error(R.drawable.flood_relief)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(eventDetailImage)
        } else {
            eventDetailImage.setImageResource(R.drawable.flood_relief)
        }
    }

    private fun formatDateTime(dateString: String, timeString: String?): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            val formattedDate = outputFormat.format(date ?: Date())
            if (!timeString.isNullOrEmpty()) "$formattedDate · $timeString" else formattedDate
        } catch (e: Exception) {
            if (!timeString.isNullOrEmpty()) "$dateString · $timeString" else dateString
        }
    }

    private fun registerForEvent() {
        if (userId == -1) {
            Toast.makeText(this, "Please login to register", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading dialog
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
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    progressDialog.dismiss()
                    try {
                        val json = JSONObject(responseBody ?: "{}")
                        val status = json.getString("status")
                        val message = json.getString("message")

                        if (status == "success") {
                            registrationStatus = "pending"
                            updateRegisterButton()

                            // Show success toast
                            Toast.makeText(
                                this@OpportunityDetailActivity,
                                "Request sent for registration",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Navigate to RegisterConfirmationActivity
                            val intent = Intent(this@OpportunityDetailActivity, RegisterConfirmation::class.java)
                            intent.putExtra("event_id", eventId)
                            intent.putExtra("event_name", currentEvent?.event_name)
                            startActivity(intent)

                        } else {
                            Toast.makeText(
                                this@OpportunityDetailActivity,
                                "Error: $message",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Log.e("OpportunityDetail", "Registration parse error", e)
                        Toast.makeText(
                            this@OpportunityDetailActivity,
                            "Error processing registration",
                            Toast.LENGTH_SHORT
                        ).show()
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
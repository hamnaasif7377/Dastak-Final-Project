package com.DASTAK.i230613_i230658_i230736

import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class NotificationActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateText: TextView
    private lateinit var adapter: NotificationAdapter
    private lateinit var progressDialog: ProgressDialog

    private val client = OkHttpClient()
    private val API_BASE_URL = Constants.BASE_URL
    private val notifications = mutableListOf<Notification>()
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        userId = sharedPref.getInt("user_id", -1)

        if (userId == -1) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        loadNotifications()
    }

    private fun initializeViews() {
        btnBack = findViewById(R.id.btnBack)
        recyclerView = findViewById(R.id.notificationsRecyclerView)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Loading notifications...")
        progressDialog.setCancelable(false)

        btnBack.setOnClickListener {
            finish()
        }

        adapter = NotificationAdapter(
            notifications,
            onAccept = { registrationId, position -> respondToRegistration(registrationId, "accept", position) },
            onReject = { registrationId, position -> respondToRegistration(registrationId, "reject", position) }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadNotifications() {
        progressDialog.show()

        val url = "${API_BASE_URL}get_notifications.php?user_id=$userId"

        Log.d("NotificationActivity", "Loading notifications from: $url")

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@NotificationActivity,
                        "Network error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("NotificationActivity", "Network error", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    progressDialog.dismiss()

                    try {
                        if (responseBody.isNullOrEmpty()) {
                            Toast.makeText(
                                this@NotificationActivity,
                                "Empty response from server",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@runOnUiThread
                        }

                        Log.d("NotificationActivity", "Response code: ${response.code}")
                        Log.d("NotificationActivity", "Response body: $responseBody")

                        // Check if response starts with HTML (common error indicator)
                        if (responseBody.trim().startsWith("<")) {
                            Log.e("NotificationActivity", "Received HTML instead of JSON")
                            Toast.makeText(
                                this@NotificationActivity,
                                "Server error: Received HTML instead of JSON. Check PHP file.",
                                Toast.LENGTH_LONG
                            ).show()
                            return@runOnUiThread
                        }

                        val jsonResponse = JSONObject(responseBody)
                        val status = jsonResponse.getString("status")

                        if (status == "success") {
                            val notificationsArray = jsonResponse.getJSONArray("notifications")
                            val count = jsonResponse.getInt("count")

                            Log.d("NotificationActivity", "Found $count notifications")

                            parseNotifications(notificationsArray)
                        } else {
                            val message = jsonResponse.optString("message", "Failed to load notifications")
                            Toast.makeText(this@NotificationActivity, message, Toast.LENGTH_LONG).show()
                            Log.e("NotificationActivity", "Server returned error: $message")
                        }

                    } catch (e: Exception) {
                        Toast.makeText(
                            this@NotificationActivity,
                            "Error parsing data: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.e("NotificationActivity", "Parse error", e)
                        Log.e("NotificationActivity", "Response was: $responseBody")
                    }
                }
            }
        })
    }

    private fun parseNotifications(jsonArray: JSONArray) {
        notifications.clear()

        for (i in 0 until jsonArray.length()) {
            try {
                val notifObj = jsonArray.getJSONObject(i)

                val sender = if (!notifObj.isNull("sender")) {
                    val senderObj = notifObj.getJSONObject("sender")
                    NotificationSender(
                        userId = senderObj.getInt("user_id"),
                        name = senderObj.getString("name"),
                        profileImage = if (senderObj.isNull("profile_image")) null
                        else senderObj.getString("profile_image"),
                        role = if (senderObj.isNull("role")) null
                        else senderObj.getString("role")
                    )
                } else null

                val notification = Notification(
                    notificationId = notifObj.getInt("notification_id"),
                    notificationType = notifObj.getString("notification_type"),
                    title = notifObj.getString("title"),
                    message = notifObj.getString("message"),
                    isRead = notifObj.getBoolean("is_read"),
                    createdAt = notifObj.getString("created_at"),
                    eventId = if (notifObj.isNull("event_id")) null else notifObj.getInt("event_id"),
                    eventName = if (notifObj.isNull("event_name")) null else notifObj.getString("event_name"),
                    registrationId = if (notifObj.isNull("registration_id")) null else notifObj.getInt("registration_id"),
                    registrationStatus = if (notifObj.isNull("registration_status")) null else notifObj.getString("registration_status"),
                    sender = sender
                )

                notifications.add(notification)

                Log.d("NotificationActivity", "Parsed notification: ${notification.title}")

            } catch (e: Exception) {
                Log.e("NotificationActivity", "Error parsing notification at index $i", e)
            }
        }

        adapter.notifyDataSetChanged()

        if (notifications.isEmpty()) {
            Toast.makeText(this, "No notifications", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Loaded ${notifications.size} notifications", Toast.LENGTH_SHORT).show()
        }
    }

    private fun respondToRegistration(registrationId: Int, action: String, position: Int) {
        progressDialog.setMessage("Processing...")
        progressDialog.show()

        val formBody = FormBody.Builder()
            .add("registration_id", registrationId.toString())
            .add("organizer_id", userId.toString())
            .add("action", action)
            .build()

        val request = Request.Builder()
            .url("${API_BASE_URL}respond_to_registration.php")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@NotificationActivity,
                        "Network error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("NotificationActivity", "Response error", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    progressDialog.dismiss()

                    try {
                        if (responseBody.isNullOrEmpty()) {
                            Toast.makeText(
                                this@NotificationActivity,
                                "Empty response from server",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@runOnUiThread
                        }

                        Log.d("NotificationActivity", "Response: $responseBody")

                        val jsonResponse = JSONObject(responseBody)
                        val status = jsonResponse.getString("status")
                        val message = jsonResponse.getString("message")

                        if (status == "success") {
                            Toast.makeText(
                                this@NotificationActivity,
                                message,
                                Toast.LENGTH_LONG
                            ).show()

                            // Update the button state without reloading all notifications
                            val newStatus = if (action == "accept") "accepted" else "rejected"
                            adapter.updateRegistrationStatus(position, newStatus)
                        } else {
                            Toast.makeText(
                                this@NotificationActivity,
                                message,
                                Toast.LENGTH_LONG
                            ).show()
                        }

                    } catch (e: Exception) {
                        Toast.makeText(
                            this@NotificationActivity,
                            "Error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.e("NotificationActivity", "Parse error", e)
                    }
                }
            }
        })
    }
}
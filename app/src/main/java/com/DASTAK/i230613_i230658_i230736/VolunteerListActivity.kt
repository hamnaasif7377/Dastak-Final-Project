package com.DASTAK.i230613_i230658_i230736

import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class VolunteerListActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var btnMenu: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VolunteerListAdapter
    private lateinit var progressDialog: ProgressDialog

    private val client = OkHttpClient()
    private val API_BASE_URL = Constants.BASE_URL
    private val volunteers = mutableListOf<VolunteerRegistration>()
    private var organizerId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_volunteer_list)

        val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        organizerId = sharedPref.getInt("user_id", -1)

        if (organizerId == -1) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        loadVolunteers()
    }

    private fun initializeViews() {
        btnBack = findViewById(R.id.btnBack)
        btnMenu = findViewById(R.id.btnMenu)
        recyclerView = findViewById(R.id.recyclerViewVolunteers)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Loading volunteers...")
        progressDialog.setCancelable(false)

        btnBack.setOnClickListener {
            finish()
        }

        btnMenu.setOnClickListener {
            // Optional: Open menu
        }

        adapter = VolunteerListAdapter(
            volunteers,
            onRemove = { registrationId, position ->
                showRemoveConfirmation(registrationId, position)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadVolunteers() {
        progressDialog.show()

        val url = "${API_BASE_URL}get_volunteer_list.php?organizer_id=$organizerId"

        Log.d("VolunteerList", "Loading from: $url")
        Log.d("VolunteerList", "Organizer ID: $organizerId")

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@VolunteerListActivity,
                        "Network error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("VolunteerList", "Network error", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    progressDialog.dismiss()

                    try {
                        if (responseBody.isNullOrEmpty()) {
                            Toast.makeText(
                                this@VolunteerListActivity,
                                "Empty response from server",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@runOnUiThread
                        }

                        Log.d("VolunteerList", "Response code: ${response.code}")
                        Log.d("VolunteerList", "Response length: ${responseBody.length}")
                        Log.d("VolunteerList", "Response first 200 chars: ${responseBody.take(200)}")

                        if (responseBody.trim().startsWith("<")) {
                            Log.e("VolunteerList", "Full HTML response: $responseBody")
                            Toast.makeText(
                                this@VolunteerListActivity,
                                "Server error: Check PHP file for errors. See logs.",
                                Toast.LENGTH_LONG
                            ).show()
                            return@runOnUiThread
                        }

                        val jsonResponse = JSONObject(responseBody)
                        val status = jsonResponse.getString("status")

                        if (status == "success") {
                            val volunteersArray = jsonResponse.getJSONArray("volunteers")
                            val count = jsonResponse.getInt("count")

                            Log.d("VolunteerList", "Found $count volunteers")

                            parseVolunteers(volunteersArray)
                        } else {
                            val message = jsonResponse.optString("message", "Failed to load volunteers")
                            Toast.makeText(this@VolunteerListActivity, message, Toast.LENGTH_LONG).show()
                            Log.e("VolunteerList", "Server returned error: $message")
                        }

                    } catch (e: Exception) {
                        Toast.makeText(
                            this@VolunteerListActivity,
                            "Error parsing data: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.e("VolunteerList", "Parse error", e)
                        Log.e("VolunteerList", "Response was: $responseBody")
                    }
                }
            }
        })
    }

    private fun parseVolunteers(jsonArray: JSONArray) {
        volunteers.clear()

        for (i in 0 until jsonArray.length()) {
            try {
                val regObj = jsonArray.getJSONObject(i)
                val volunteerObj = regObj.getJSONObject("volunteer")

                val registration = VolunteerRegistration(
                    registrationId = regObj.getInt("registration_id"),
                    eventId = regObj.getInt("event_id"),
                    eventName = regObj.getString("event_name"),
                    eventDate = regObj.getString("event_date"),
                    eventTime = if (regObj.isNull("event_time")) null else regObj.getString("event_time"),
                    eventLocation = regObj.getString("event_location"),
                    volunteer = VolunteerInfo(
                        volunteerId = volunteerObj.getInt("volunteer_id"),
                        name = volunteerObj.getString("name"),
                        email = volunteerObj.getString("email"),
                        phone = if (volunteerObj.isNull("phone")) null else volunteerObj.getString("phone"),
                        profileImage = if (volunteerObj.isNull("profile_image")) null else volunteerObj.getString("profile_image")
                    ),
                    registeredAt = regObj.getString("registered_at"),
                    status = regObj.getString("status")
                )

                volunteers.add(registration)

            } catch (e: Exception) {
                Log.e("VolunteerList", "Error parsing volunteer at index $i", e)
            }
        }

        adapter.notifyDataSetChanged()

        if (volunteers.isEmpty()) {
            Toast.makeText(this, "No registered volunteers yet", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Loaded ${volunteers.size} volunteers", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRemoveConfirmation(registrationId: Int, position: Int) {
        val volunteer = volunteers[position]

        AlertDialog.Builder(this)
            .setTitle("Remove Volunteer")
            .setMessage("Are you sure you want to remove ${volunteer.volunteer.name} from ${volunteer.eventName}?")
            .setPositiveButton("Remove") { _, _ ->
                removeVolunteer(registrationId, position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun removeVolunteer(registrationId: Int, position: Int) {
        progressDialog.setMessage("Removing volunteer...")
        progressDialog.show()

        val formBody = FormBody.Builder()
            .add("registration_id", registrationId.toString())
            .add("organizer_id", organizerId.toString())
            .build()

        val request = Request.Builder()
            .url("${API_BASE_URL}remove_volunteer.php")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@VolunteerListActivity,
                        "Network error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("VolunteerList", "Remove error", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    progressDialog.dismiss()

                    try {
                        if (responseBody.isNullOrEmpty()) {
                            Toast.makeText(
                                this@VolunteerListActivity,
                                "Empty response from server",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@runOnUiThread
                        }

                        Log.d("VolunteerList", "Remove response: $responseBody")

                        val jsonResponse = JSONObject(responseBody)
                        val status = jsonResponse.getString("status")
                        val message = jsonResponse.getString("message")

                        if (status == "success") {
                            Toast.makeText(
                                this@VolunteerListActivity,
                                message,
                                Toast.LENGTH_LONG
                            ).show()

                            // Remove from list
                            adapter.removeItem(position)
                        } else {
                            Toast.makeText(
                                this@VolunteerListActivity,
                                message,
                                Toast.LENGTH_LONG
                            ).show()
                        }

                    } catch (e: Exception) {
                        Toast.makeText(
                            this@VolunteerListActivity,
                            "Error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.e("VolunteerList", "Parse error", e)
                    }
                }
            }
        })
    }
}
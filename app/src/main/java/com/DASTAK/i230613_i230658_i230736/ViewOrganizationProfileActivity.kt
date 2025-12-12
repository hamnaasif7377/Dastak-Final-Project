package com.DASTAK.i230613_i230658_i230736

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class ViewOrganizationProfileActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var bannerImage: ImageView
    private lateinit var foundationName: TextView
    private lateinit var locationText: TextView
    private lateinit var contactInfo: TextView
    private lateinit var descriptionText: TextView
    private lateinit var operationLocationsText: TextView
    private lateinit var programsText: TextView
    private lateinit var btnViewEvents: Button

    private lateinit var progressDialog: ProgressDialog
    private val client = OkHttpClient()
    private val API_BASE_URL = Constants.BASE_URL

    private var organizationId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_organization_profile)

        // Get organization_id from intent
        organizationId = intent.getIntExtra("organization_id", -1)

        if (organizationId == -1) {
            Toast.makeText(this, "Error: Organization not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        loadOrganizationProfile()
    }

    private fun initializeViews() {
        btnBack = findViewById(R.id.btnBack)
        bannerImage = findViewById(R.id.bannerImage)
        foundationName = findViewById(R.id.foundationName)
        locationText = findViewById(R.id.location)
        contactInfo = findViewById(R.id.contactInfo)
        descriptionText = findViewById(R.id.organizationDescription)
        operationLocationsText = findViewById(R.id.operationLocationsList)
        programsText = findViewById(R.id.programsList)


        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Loading organization profile...")
        progressDialog.setCancelable(false)

        // Back button
        btnBack.setOnClickListener {
            finish()
        }

    }

    private fun loadOrganizationProfile() {
        progressDialog.show()

        val url = "${API_BASE_URL}get_organization_profile.php?user_id=$organizationId"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@ViewOrganizationProfileActivity,
                        "Network error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("ViewOrgProfile", "Network error", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    progressDialog.dismiss()

                    try {
                        if (responseBody.isNullOrEmpty()) {
                            Toast.makeText(
                                this@ViewOrganizationProfileActivity,
                                "Empty response from server",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@runOnUiThread
                        }

                        Log.d("ViewOrgProfile", "Response: $responseBody")

                        val jsonResponse = JSONObject(responseBody)
                        val status = jsonResponse.getString("status")

                        if (status == "success") {
                            val userObj = jsonResponse.getJSONObject("user")
                            displayOrganizationProfile(userObj)
                        } else {
                            val message = jsonResponse.optString("message", "Failed to load organization profile")
                            Toast.makeText(
                                this@ViewOrganizationProfileActivity,
                                message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    } catch (e: Exception) {
                        Toast.makeText(
                            this@ViewOrganizationProfileActivity,
                            "Error parsing data: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e("ViewOrgProfile", "Parse error", e)
                        Log.e("ViewOrgProfile", "Response was: $responseBody")
                    }
                }
            }
        })
    }

    private fun displayOrganizationProfile(userObj: JSONObject) {
        try {
            // Organization name
            val name = userObj.getString("name")
            foundationName.text = name

            // Email/Contact info
            val email = if (userObj.isNull("email")) "" else userObj.getString("email")
            if (email.isNotEmpty()) {
                contactInfo.text = email
            } else {
                contactInfo.text = "Contact info not available"
            }

            // Location
            val location = if (userObj.isNull("location")) "Location not set" else userObj.getString("location")
            locationText.text = location

            // Description
            val description = if (userObj.isNull("description")) "" else userObj.getString("description")
            if (description.isNotEmpty()) {
                descriptionText.text = description
            } else {
                descriptionText.text = "No description available"
            }

            // Operation locations
            val operationLocation = if (userObj.isNull("operation_location")) "" else userObj.getString("operation_location")
            if (operationLocation.isNotEmpty()) {
                operationLocationsText.text = formatListWithBullets(operationLocation)
            } else {
                operationLocationsText.text = "Operation locations not specified"
            }

            // Programs
            val programs = if (userObj.isNull("programs")) "" else userObj.getString("programs")
            if (programs.isNotEmpty()) {
                programsText.text = formatProgramsList(programs)
            } else {
                programsText.text = "Programs information not available"
            }

            // Profile/Banner image
            val profileImage = if (userObj.isNull("profile_image")) "" else userObj.getString("profile_image")
            if (profileImage.isNotEmpty()) {
                val imageUrl = Constants.BASE_URL + "uploads/profiles/" + profileImage

                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.mainpho)
                    .error(R.drawable.mainpho)
                    .into(bannerImage)
            } else {
                bannerImage.setImageResource(R.drawable.mainpho)
            }

        } catch (e: Exception) {
            Log.e("ViewOrgProfile", "Error displaying profile", e)
            Toast.makeText(this, "Error displaying organization profile", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatListWithBullets(text: String): String {
        return text.split("\n")
            .filter { it.isNotBlank() }
            .joinToString("\n") { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("•")) trimmed else "• $trimmed"
            }
    }

    private fun formatProgramsList(programs: String): String {
        return programs.split("\n")
            .filter { it.isNotBlank() }
            .joinToString("\n\n") { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("•")) trimmed else "• $trimmed"
            }
    }
}
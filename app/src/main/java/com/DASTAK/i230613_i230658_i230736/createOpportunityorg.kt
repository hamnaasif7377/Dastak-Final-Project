package com.DASTAK.i230613_i230658_i230736

import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class createOpportunityorg : AppCompatActivity() {

    private lateinit var eventNameInput: EditText
    private lateinit var eventLocationInput: EditText
    private lateinit var dateInput: EditText
    private lateinit var eventDescriptionInput: EditText
    private lateinit var btnAddPoster: Button
    private lateinit var btnPostEvent: Button
    private lateinit var backButton: ImageView
    private lateinit var datePickerIcon: ImageView

    private lateinit var sharedPreferences: SharedPreferences
    private var userId: Int = -1

    private var selectedDate: Calendar = Calendar.getInstance()
    private var posterImageUri: Uri? = null
    private var posterImageFile: File? = null

    private val client = OkHttpClient()
    private lateinit var progressDialog: ProgressDialog

    // Your API base URL - CHANGE THIS TO YOUR SERVER
    private val API_BASE_URL = Constants.BASE_URL

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            posterImageUri = result.data?.data
            posterImageUri?.let {
                posterImageFile = getFileFromUri(it)
                Toast.makeText(this, "Event poster selected!", Toast.LENGTH_SHORT).show()
                btnAddPoster.text = "Poster Added ✓"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_opportunityorg)

        // Get user info from SharedPreferences
        sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        userId = sharedPreferences.getInt("user_id", -1)

        if (userId == -1) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        setupListeners()
    }

    private fun initializeViews() {
        eventNameInput = findViewById(R.id.eventNameInput)
        eventLocationInput = findViewById(R.id.eventLocationInput)
        dateInput = findViewById(R.id.dateInput)
        eventDescriptionInput = findViewById(R.id.eventDescriptionInput)
        btnAddPoster = findViewById(R.id.btnAddPoster)
        btnPostEvent = findViewById(R.id.btnPostEvent)
        backButton = findViewById(R.id.backButton)
        datePickerIcon = findViewById(R.id.datePickerIcon)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Posting event...")
        progressDialog.setCancelable(false)

        // Make date input non-editable (only through picker)
        dateInput.isFocusable = false
        dateInput.isClickable = true
    }

    private fun setupListeners() {
        backButton.setOnClickListener { finish() }

        dateInput.setOnClickListener { showDatePicker() }
        datePickerIcon.setOnClickListener { showDatePicker() }

        btnAddPoster.setOnClickListener { openImagePicker() }
        btnPostEvent.setOnClickListener { validateAndPostEvent() }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedDate.set(selectedYear, selectedMonth, selectedDay)
                updateDateDisplay()
            },
            year, month, day
        )

        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("dd - MM - yyyy", Locale.getDefault())
        dateInput.setText(dateFormat.format(selectedDate.time))
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private fun getFileFromUri(uri: Uri): File? {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(cacheDir, "temp_poster_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(tempFile)

            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()

            return tempFile
        } catch (e: Exception) {
            Log.e("CreateOpportunity", "Error getting file from URI", e)
            return null
        }
    }

    private fun validateAndPostEvent() {
        val eventName = eventNameInput.text.toString().trim()
        val eventLocation = eventLocationInput.text.toString().trim()
        val eventDate = dateInput.text.toString().trim()
        val eventDescription = eventDescriptionInput.text.toString().trim()

        when {
            eventName.isEmpty() -> {
                eventNameInput.error = "Event name is required"
                eventNameInput.requestFocus()
                return
            }
            eventLocation.isEmpty() -> {
                eventLocationInput.error = "Event location is required"
                eventLocationInput.requestFocus()
                return
            }
            eventDate.isEmpty() -> {
                Toast.makeText(this, "Please select an event date", Toast.LENGTH_SHORT).show()
                return
            }
            eventDescription.isEmpty() -> {
                eventDescriptionInput.error = "Event description is required"
                eventDescriptionInput.requestFocus()
                return
            }
        }

        postEventToServer(eventName, eventLocation, eventDate, eventDescription)
    }

    private fun postEventToServer(name: String, location: String, date: String, description: String) {
        progressDialog.show()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("user_id", userId.toString())
            .addFormDataPart("event_name", name)
            .addFormDataPart("event_location", location)
            .addFormDataPart("event_date", date)
            .addFormDataPart("event_description", description)

        // Add poster image if selected
        posterImageFile?.let { file ->
            val mediaType = "image/jpeg".toMediaTypeOrNull()
            requestBody.addFormDataPart(
                "poster_image",
                file.name,
                file.asRequestBody(mediaType)
            )
        }

        val request = Request.Builder()
            .url("${API_BASE_URL}postevent.php")
            .post(requestBody.build())
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@createOpportunityorg,
                        "Network error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("CreateOpportunity", "Network error", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    progressDialog.dismiss()

                    // Debug: Log the raw response
                    Log.d("CreateOpportunity", "Raw Response: $responseBody")

                    try {
                        // Check if response is actually JSON
                        if (responseBody == null || responseBody.trim().startsWith("<")) {
                            Toast.makeText(
                                this@createOpportunityorg,
                                "Server error - check PHP file and logs. Response starts with: ${responseBody?.take(100)}",
                                Toast.LENGTH_LONG
                            ).show()
                            Log.e("CreateOpportunity", "Non-JSON response: $responseBody")
                            return@runOnUiThread
                        }

                        val jsonResponse = JSONObject(responseBody)
                        val status = jsonResponse.getString("status")
                        val message = jsonResponse.getString("message")

                        if (status == "success") {
                            Toast.makeText(
                                this@createOpportunityorg,
                                message,
                                Toast.LENGTH_LONG
                            ).show()

                            // Clean up temp file
                            posterImageFile?.delete()

                            // Return to previous activity
                            setResult(RESULT_OK)
                            finish()
                        } else {
                            Toast.makeText(
                                this@createOpportunityorg,
                                message,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@createOpportunityorg,
                            "Parse Error: ${e.message}\nCheck logcat for details",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.e("CreateOpportunity", "Parse error", e)
                        Log.e("CreateOpportunity", "Response was: $responseBody")
                    }
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up temp file if exists
        posterImageFile?.delete()
    }
}
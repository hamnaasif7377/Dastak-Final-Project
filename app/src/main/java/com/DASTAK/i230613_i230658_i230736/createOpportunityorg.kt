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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
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

    private var isEditMode = false
    private var editEventId: Int = -1
    private var existingPosterImage: String? = null

    private val client = OkHttpClient()
    private lateinit var progressDialog: ProgressDialog

    private val API_BASE_URL = Constants.BASE_URL

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

        sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        userId = sharedPreferences.getInt("user_id", -1)

        if (userId == -1) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val mode = intent.getStringExtra("MODE")
        if (mode == "EDIT") {
            isEditMode = true
            editEventId = intent.getIntExtra("event_id", -1)
        }

        initializeViews()

        if (isEditMode) {
            loadEventDataForEdit()
        }

        setupListeners()
    }


    private fun loadEventDataForEdit() {
        supportActionBar?.title = "Edit Event"

        // Get data from intent
        val eventName = intent.getStringExtra("event_name")
        val eventLocation = intent.getStringExtra("event_location")
        val eventDate = intent.getStringExtra("event_date") // Format: yyyy-MM-dd
        val eventDescription = intent.getStringExtra("event_description")
        existingPosterImage = intent.getStringExtra("poster_image")

        // Pre-fill form fields
        eventNameInput.setText(eventName)
        eventLocationInput.setText(eventLocation)
        eventDescriptionInput.setText(eventDescription)

        // Convert date from MySQL format to display format
        eventDate?.let {
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd - MM - yyyy", Locale.getDefault())
                val date = inputFormat.parse(it)
                dateInput.setText(outputFormat.format(date ?: Date()))
            } catch (e: Exception) {
                dateInput.setText(it) // Fallback to original format
            }
        }

        // Load existing poster image
        existingPosterImage?.let { posterPath ->
            val imageUrl = Constants.BASE_URL + posterPath
            Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .placeholder(R.drawable.ic_menu)
                .error(R.drawable.ic_menu)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(object : com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
                    override fun onResourceReady(
                        resource: android.graphics.Bitmap,
                        transition: com.bumptech.glide.request.transition.Transition<in android.graphics.Bitmap>?
                    ) {
                        btnAddPoster.background = android.graphics.drawable.BitmapDrawable(resources, resource)
                        btnAddPoster.text = "Poster Added ✓"
                    }

                    override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                        // Optional
                    }
                })
        }

        btnPostEvent.text = "Update Event"
    }
    private fun updateEvent() {
        val eventName = eventNameInput.text.toString().trim()
        val eventLocation = eventLocationInput.text.toString().trim()
        val eventDate = dateInput.text.toString().trim()
        val eventDescription = eventDescriptionInput.text.toString().trim()

        if (eventName.isEmpty() || eventLocation.isEmpty() ||
            eventDate.isEmpty() || eventDescription.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        progressDialog.setMessage("Updating event...")
        progressDialog.show()

        val url = "${Constants.BASE_URL}updateevent.php"

        // Convert date to MySQL format before sending
        val mysqlDate = convertDateToMySQLFormat(eventDate)

        val formBodyBuilder = FormBody.Builder()
            .add("event_id", editEventId.toString())
            .add("user_id", userId.toString())
            .add("event_name", eventName)
            .add("event_location", eventLocation)
            .add("event_date", mysqlDate) // Use converted date
            .add("event_description", eventDescription)

        if (posterImageUri == null && !existingPosterImage.isNullOrEmpty()) {
            formBodyBuilder.add("existing_poster_image", existingPosterImage!!)
        }

        val formBody = formBodyBuilder.build()

        val request = Request.Builder()
            .url(url)
            .post(formBody)
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
                    Log.e("UpdateEvent", "Network error", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    progressDialog.dismiss()

                    try {
                        if (!responseBody.isNullOrEmpty()) {
                            Log.d("UpdateEvent", "Response: $responseBody")

                            // Check if response is HTML/XML error instead of JSON
                            if (responseBody.trim().startsWith("<")) {
                                Toast.makeText(
                                    this@createOpportunityorg,
                                    "Server error - check PHP logs. Response starts with: ${responseBody.take(100)}",
                                    Toast.LENGTH_LONG
                                ).show()
                                Log.e("UpdateEvent", "Non-JSON response: $responseBody")
                                return@runOnUiThread
                            }

                            val jsonResponse = JSONObject(responseBody)
                            val status = jsonResponse.getString("status")
                            val message = jsonResponse.getString("message")

                            Toast.makeText(this@createOpportunityorg, message, Toast.LENGTH_LONG).show()

                            if (status == "success") {
                                setResult(RESULT_OK)
                                finish()
                            }
                        } else {
                            Toast.makeText(this@createOpportunityorg, "Empty response", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@createOpportunityorg,
                            "Error: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e("UpdateEvent", "Parse error", e)
                        Log.e("UpdateEvent", "Response was: $responseBody")
                    }
                }
            }
        })
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

        dateInput.isFocusable = false
        dateInput.isClickable = true
    }

    private fun setupListeners() {
        backButton.setOnClickListener { finish() }

        dateInput.setOnClickListener { showDatePicker() }
        datePickerIcon.setOnClickListener { showDatePicker() }

        btnAddPoster.setOnClickListener { openImagePicker() }
        btnPostEvent.setOnClickListener {
            if (isEditMode) {
                updateEvent()
            } else {
                validateAndPostEvent()
            }
        }
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

        // Convert date to MySQL format
        val mysqlDate = convertDateToMySQLFormat(date)

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("user_id", userId.toString())
            .addFormDataPart("event_name", name)
            .addFormDataPart("event_location", location)
            .addFormDataPart("event_date", mysqlDate) // Use converted date
            .addFormDataPart("event_description", description)

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

                Log.e("CreateOpportunity", "Full Response Code: ${response.code}")
                Log.e("CreateOpportunity", "Full Response Body: $responseBody")

                runOnUiThread {
                    progressDialog.dismiss()
                    Log.d("CreateOpportunity", "Raw Response: $responseBody")

                    try {
                        if (responseBody == null || responseBody.trim().startsWith("<")) {
                            Toast.makeText(
                                this@createOpportunityorg,
                                "Server error - check PHP file. Response: ${responseBody?.take(50)}",
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

                            posterImageFile?.delete()
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
                            "Parse Error: ${e.message}",
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
        posterImageFile?.delete()
    }

    // Add this method to fix the date format before sending to server
    private fun convertDateToMySQLFormat(dateString: String): String {
        return try {
            // Input format: "dd - MM - yyyy" (e.g., "06 - 12 - 2025")
            val inputFormat = SimpleDateFormat("dd - MM - yyyy", Locale.getDefault())
            // Output format: "yyyy-MM-dd" (MySQL format)
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            Log.e("DateConversion", "Error converting date: $dateString", e)
            dateString // Return original if conversion fails
        }
    }

}
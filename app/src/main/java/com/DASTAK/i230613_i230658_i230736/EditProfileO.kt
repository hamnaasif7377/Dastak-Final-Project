package com.DASTAK.i230613_i230658_i230736

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

class EditProfileO : AppCompatActivity() {

    private lateinit var profileImage: ImageView
    private lateinit var nameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var descriptionInput: EditText
    private lateinit var operationLocationInput: EditText
    private lateinit var locationInput: EditText
    private lateinit var programsInput: EditText
    private lateinit var btnDone: Button
    private lateinit var backArrow: ImageView

    private var selectedImageUri: Uri? = null
    private var userId: Int = 0

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                profileImage.setImageURI(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile_o)

        // Initialize views
        profileImage = findViewById(R.id.profileImage)
        nameInput = findViewById(R.id.nameInput)
        passwordInput = findViewById(R.id.usernameInput)
        descriptionInput = findViewById(R.id.newPasswordInput)
        operationLocationInput = findViewById(R.id.confirmPasswordInput)
        locationInput = findViewById(R.id.locationInput)
        programsInput = findViewById(R.id.listprog)
        btnDone = findViewById(R.id.btnDone)
        backArrow = findViewById(R.id.backArrow)

        // Get user ID from SharedPreferences
        val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        userId = sharedPref.getInt("user_id", 0)

        if (userId == 0) {
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load existing profile data
        loadProfileData()

        // Profile image click to select new image
        profileImage.setOnClickListener {
            openImagePicker()
        }

        // Back button
        backArrow.setOnClickListener {
            finish()
        }

        // Done button - save changes
        btnDone.setOnClickListener {
            updateProfile()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun loadProfileData() {
        val url = Constants.BASE_URL + "get_organization_profile.php?user_id=$userId"
        val queue = Volley.newRequestQueue(this)

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.getString("status") == "success") {
                        val user = json.getJSONObject("user")

                        nameInput.setText(user.optString("name", ""))
                        descriptionInput.setText(user.optString("description", ""))
                        operationLocationInput.setText(user.optString("operation_location", ""))
                        locationInput.setText(user.optString("location", ""))
                        programsInput.setText(user.optString("programs", ""))

                        // Load profile image if exists
                        val profileImagePath = user.optString("profile_image", "")
                        if (profileImagePath.isNotEmpty()) {
                            // You can use Glide or Picasso to load the image
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { error ->
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
        )

        queue.add(request)
    }

    private fun updateProfile() {
        val name = nameInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        val description = descriptionInput.text.toString().trim()
        val operationLocation = operationLocationInput.text.toString().trim()
        val location = locationInput.text.toString().trim()
        val programs = programsInput.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, "Organization name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val url = Constants.BASE_URL + "update_organization_profile.php"
        val queue = Volley.newRequestQueue(this)

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    val status = json.getString("status")
                    val message = json.getString("message")

                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()

                    if (status == "success") {
                        // Update SharedPreferences
                        val user = json.getJSONObject("user")
                        val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putString("name", user.getString("name"))
                            if (user.has("profile_image")) {
                                putString("profile_image", user.getString("profile_image"))
                            }
                            if (user.has("description")) {
                                putString("description", user.getString("description"))
                            }
                            if (user.has("operation_location")) {
                                putString("operation_location", user.getString("operation_location"))
                            }
                            if (user.has("location")) {
                                putString("location", user.getString("location"))
                            }
                            if (user.has("programs")) {
                                putString("programs", user.getString("programs"))
                            }
                            apply()
                        }

                        // Return to profile page
                        val intent = Intent(this, organizationProfile::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        startActivity(intent)
                        finish()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                Toast.makeText(this, "Network Error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = hashMapOf(
                    "user_id" to userId.toString(),
                    "name" to name,
                    "description" to description,
                    "operation_location" to operationLocation,
                    "location" to location,
                    "programs" to programs
                )

                if (password.isNotEmpty()) {
                    params["new_password"] = password
                }

                return params
            }

            override fun getBody(): ByteArray {
                return if (selectedImageUri != null) {
                    createMultipartBody()
                } else {
                    super.getBody()
                }
            }

            override fun getBodyContentType(): String {
                return if (selectedImageUri != null) {
                    "multipart/form-data; boundary=$boundary"
                } else {
                    super.getBodyContentType()
                }
            }
        }

        queue.add(request)
    }

    private val boundary = "----WebKitFormBoundary${System.currentTimeMillis()}"

    private fun createMultipartBody(): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val writer = outputStream.writer()

        // Add text fields
        addFormField(writer, "user_id", userId.toString())
        addFormField(writer, "name", nameInput.text.toString())
        addFormField(writer, "description", descriptionInput.text.toString())
        addFormField(writer, "operation_location", operationLocationInput.text.toString())
        addFormField(writer, "location", locationInput.text.toString())
        addFormField(writer, "programs", programsInput.text.toString())

        val password = passwordInput.text.toString()
        if (password.isNotEmpty()) {
            addFormField(writer, "new_password", password)
        }

        // Add image file
        selectedImageUri?.let { uri ->
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
                val imageBytes = byteArrayOutputStream.toByteArray()

                writer.append("--$boundary\r\n")
                writer.append("Content-Disposition: form-data; name=\"profile_image\"; filename=\"profile.jpg\"\r\n")
                writer.append("Content-Type: image/jpeg\r\n\r\n")
                writer.flush()
                outputStream.write(imageBytes)
                writer.append("\r\n")
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        writer.append("--$boundary--\r\n")
        writer.flush()
        writer.close()

        return outputStream.toByteArray()
    }

    private fun addFormField(writer: java.io.Writer, name: String, value: String) {
        writer.append("--$boundary\r\n")
        writer.append("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
        writer.append(value)
        writer.append("\r\n")
    }
}
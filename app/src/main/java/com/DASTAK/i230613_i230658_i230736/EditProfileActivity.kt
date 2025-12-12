package com.DASTAK.i230613_i230658_i230736

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class EditProfileActivity : AppCompatActivity() {

    private lateinit var profileImage: ImageView
    private lateinit var nameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var locationInput: EditText
    private lateinit var btnDone: Button
    private lateinit var backArrow: ImageView

    private var selectedImageBitmap: Bitmap? = null
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        initializeViews()
        loadUserData()
        setupClickListeners()
    }

    private fun initializeViews() {
        profileImage = findViewById(R.id.profileImage)
        nameInput = findViewById(R.id.nameInput)
        emailInput = findViewById(R.id.email)
        passwordInput = findViewById(R.id.usernameInput)
        locationInput = findViewById(R.id.locationInput)
        btnDone = findViewById(R.id.btnDone)
        backArrow = findViewById(R.id.backArrow)
    }

    private fun loadUserData() {
        val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        val name = sharedPref.getString("name", "")
        val email = sharedPref.getString("email", "")
        val location = sharedPref.getString("location", "")

        nameInput.setText(name)
        emailInput.setText(email)
        locationInput.setText(location)

        // Load profile image if exists
        // TODO: Load image from URL or local storage
    }

    private fun setupClickListeners() {
        backArrow.setOnClickListener {
            finish()
        }

        profileImage.setOnClickListener {
            openImagePicker()
        }

        btnDone.setOnClickListener {
            updateProfile()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri: Uri? = data.data
            try {
                selectedImageBitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                profileImage.setImageBitmap(selectedImageBitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateProfile() {
        val name = nameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        val location = locationInput.text.toString().trim()

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and email are required", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)

        if (userId == -1) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
            return
        }

        val url = Constants.BASE_URL + "update_profile.php"
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
                        with(sharedPref.edit()) {
                            putString("name", name)
                            putString("email", email)
                            putString("location", location)
                            if (json.has("profile_image")) {
                                putString("profile_image", json.getString("profile_image"))
                            }
                            apply()
                        }

                        // Return to home with success flag
                        val intent = Intent()
                        intent.putExtra("profile_updated", true)
                        setResult(Activity.RESULT_OK, intent)
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
                val params = HashMap<String, String>()
                params["user_id"] = userId.toString()
                params["name"] = name
                params["email"] = email
                params["location"] = location

                // Only include password if it's not empty
                if (password.isNotEmpty()) {
                    params["password"] = password
                }

                // Convert image to Base64 if selected
                selectedImageBitmap?.let {
                    params["profile_image"] = bitmapToBase64(it)
                }

                return params
            }
        }

        queue.add(request)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}
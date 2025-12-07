package com.DASTAK.i230613_i230658_i230736

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.DASTAK.i230613_i230658_i230736.User
import com.DASTAK.i230613_i230658_i230736.utils.ImageUtils
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView

class Edit_profile_volunteer : AppCompatActivity() {

    private val database = FirebaseDatabase.getInstance()
    private var userIdInt = 0  // ✅ Changed from hardcoded String to Int
    private val TAG = "EditProfileVolunteer"

    private lateinit var profileImageView: CircleImageView
    private lateinit var profilePictureFrame: FrameLayout
    private lateinit var nameEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var locationsEditText: EditText
    private lateinit var passwordToggleButton: ImageButton
    private lateinit var doneButton: Button
    private lateinit var backButton: ImageButton
    private lateinit var menuButton: ImageButton

    private var profileImageUri: Uri? = null
    private var isPasswordVisible = false
    private var currentUser: User? = null

    private val profileImagePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            profileImageUri = result.data?.data
            profileImageView.setImageURI(profileImageUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile_volunteer)

        // ✅ Get the actual logged-in user ID
        val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        userIdInt = try {
            sharedPref.getString("user_id", "0")?.toIntOrNull() ?: 0
        } catch (e: Exception) {
            sharedPref.getInt("user_id", 0)
        }

        Log.d(TAG, "Editing profile for user ID: $userIdInt")

        if (userIdInt == 0) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupClicks()
        loadUserData()
    }

    private fun initViews() {
        profileImageView = findViewById(R.id.profileImageView)
        profilePictureFrame = findViewById(R.id.add_image)
        nameEditText = findViewById(R.id.nameEditText)
        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        locationsEditText = findViewById(R.id.locationsEditText)
        passwordToggleButton = findViewById(R.id.passwordToggleButton)
        doneButton = findViewById(R.id.doneButton)
        backButton = findViewById(R.id.backButton)
        menuButton = findViewById(R.id.menuButton)
    }

    private fun setupClicks() {
        backButton.setOnClickListener {
            finish()
        }

        profilePictureFrame.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            profileImagePicker.launch(intent)
        }

        doneButton.setOnClickListener {
            saveProfile()
        }
    }

    private fun loadUserData() {
        val userRef = database.reference.child("users").child(userIdInt.toString())

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    currentUser = snapshot.getValue(User::class.java)
                    currentUser?.let { user ->
                        Log.d(TAG, "Loaded user data: ${user.name}")
                        nameEditText.setText(user.name)
                        usernameEditText.setText(user.username.ifEmpty { user.name })
                        locationsEditText.setText(user.location)
                        passwordEditText.setText(user.password)

                        if (user.profileImageBase64.isNotEmpty()) {
                            val bitmap = ImageUtils.base64ToBitmap(user.profileImageBase64)
                            bitmap?.let { profileImageView.setImageBitmap(it) }
                        }
                    }
                } else {
                    Log.d(TAG, "User not found in Firebase, loading from cache")
                    loadFromCache()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load profile: ${error.message}")
                Toast.makeText(this@Edit_profile_volunteer, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadFromCache() {
        val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        nameEditText.setText(sharedPref.getString("name", ""))
        usernameEditText.setText(sharedPref.getString("name", ""))
        locationsEditText.setText(sharedPref.getString("location", ""))

        val profileImage = sharedPref.getString("profile_image", "")
        if (!profileImage.isNullOrEmpty()) {
            val bitmap = ImageUtils.base64ToBitmap(profileImage)
            bitmap?.let { profileImageView.setImageBitmap(it) }
        }
    }

    private fun saveProfile() {
        val name = nameEditText.text.toString().trim()
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val location = locationsEditText.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show()
            return
        }

        if (username.isEmpty()) {
            Toast.makeText(this, "Username is required", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading
        doneButton.isEnabled = false
        doneButton.text = "Saving..."

        // Get existing profile image or new one
        var imageBase64 = currentUser?.profileImageBase64 ?: ""

        // If user selected a new image
        if (profileImageUri != null) {
            val newImageBase64 = ImageUtils.uriToBase64(this, profileImageUri!!)
            if (newImageBase64 != null) {
                imageBase64 = newImageBase64
            }
        }

        // Prepare user data
        val userUpdates = hashMapOf(
            "id" to userIdInt,  // Stored as Int
            "name" to name,
            "username" to username,
            "password" to password,
            "email" to (currentUser?.email ?: ""),
            "location" to location,
            "profileImageBase64" to imageBase64,
            "contributions" to (currentUser?.getContributionsAsInt() ?: 0),
            "timestamp" to System.currentTimeMillis()  // Stored as Long
        )

        Log.d(TAG, "Saving profile updates for user $userIdInt")

        // Update in Firebase
        database.reference.child("users").child(userIdInt.toString())
            .setValue(userUpdates)  // ✅ Using setValue instead of updateChildren
            .addOnSuccessListener {
                Log.d(TAG, "Profile updated successfully in Firebase")

                // ✅ Update SharedPreferences cache
                val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putString("name", name)
                    putString("location", location)
                    putString("profile_image", imageBase64)
                    apply()
                }

                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()

                // ✅ Return to profile with result code to trigger refresh
                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to update: ${e.message}")
                Toast.makeText(this, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
                doneButton.isEnabled = true
                doneButton.text = "Done"
            }
    }
}
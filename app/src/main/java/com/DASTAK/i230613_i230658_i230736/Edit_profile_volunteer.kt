package com.DASTAK.i230613_i230658_i230736

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
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
    private var userId = "user123" // Replace with actual user ID from Firebase Auth

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

        // Done button - save changes
        doneButton.setOnClickListener {
            saveProfile()
        }
    }

    private fun loadUserData() {
        val userRef = database.reference.child("users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentUser = snapshot.getValue(User::class.java)
                currentUser?.let { user ->
                    // Populate fields
                    nameEditText.setText(user.name)
                    usernameEditText.setText(user.username)
                    locationsEditText.setText(user.location)
                    passwordEditText.setText(user.password)

                    // Load profile image
                    if (user.profileImageBase64.isNotEmpty()) {
                        val bitmap = ImageUtils.base64ToBitmap(user.profileImageBase64)
                        bitmap?.let { profileImageView.setImageBitmap(it) }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Edit_profile_volunteer, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveProfile() {
        val name = nameEditText.text.toString().trim()
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val location = locationsEditText.text.toString().trim()

        if (username.isEmpty()) {
            Toast.makeText(this, "Username is required", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading
        doneButton.isEnabled = false
        doneButton.text = "Saving..."

        // Prepare user data
        val userUpdates = mutableMapOf<String, Any>()
        userUpdates["name"] = name
        userUpdates["username"] = username
        userUpdates["password"] = password
        userUpdates["location"] = location
        userUpdates["timestamp"] = System.currentTimeMillis()

        // Handle profile image if changed
        if (profileImageUri != null) {
            val imageBase64 = ImageUtils.uriToBase64(this, profileImageUri!!)
            if (imageBase64 != null) {
                userUpdates["profileImageBase64"] = imageBase64
            }
        }

        // Update in Firebase
        database.reference.child("users").child(userId)
            .updateChildren(userUpdates)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
                doneButton.isEnabled = true
                doneButton.text = "Done"
            }
    }


}

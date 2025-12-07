package com.DASTAK.i230613_i230658_i230736

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.database.FirebaseDatabase
import org.json.JSONObject

class signup_v : AppCompatActivity() {

    private lateinit var usernameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var btnRegister: Button
    private val database = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup_v)

        usernameInput = findViewById(R.id.usernameInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        btnRegister = findViewById(R.id.btnRegister)

        btnRegister.setOnClickListener {
            signupUser()
        }
    }

    private fun signupUser() {
        val name = usernameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val url = Constants.BASE_URL + "signupvolunteer.php"
        val queue: RequestQueue = Volley.newRequestQueue(this)

        val stringRequest = object : StringRequest(
            Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    val status = json.getString("status")
                    val message = json.getString("message")
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()

                    if (status == "success") {
                        val userIdInt = if (json.has("user_id")) {
                            json.getInt("user_id")
                        } else {
                            System.currentTimeMillis().toInt()
                        }

                        saveVolunteerToFirebase(userIdInt, name, email, password)

                        usernameInput.text.clear()
                        emailInput.text.clear()
                        passwordInput.text.clear()

                        Toast.makeText(this, "Account created! Redirecting to login...", Toast.LENGTH_SHORT).show()

                        Thread.sleep(1000)
                        val intent = Intent(this@signup_v, login_v::class.java)
                        startActivity(intent)
                        finish()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Invalid response from server", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "name" to name,
                    "email" to email,
                    "password" to password,
                    "role" to "volunteer"
                )
            }
        }

        queue.add(stringRequest)
    }

    private fun saveVolunteerToFirebase(userIdInt: Int, name: String, email: String, password: String) {
        val volunteerData = hashMapOf(
            "id" to userIdInt,
            "userId" to userIdInt,
            "name" to name,
            "username" to name,
            "email" to email,
            "password" to password,
            "location" to "Not set",
            "profileImageBase64" to "",
            "contributions" to 0,
            "timestamp" to System.currentTimeMillis(),
            "role" to "volunteer"
        )

        database.reference.child("users").child(userIdInt.toString())
            .setValue(volunteerData)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile saved to database", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Warning: Offline sync may be limited", Toast.LENGTH_SHORT).show()
            }
    }
}
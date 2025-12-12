package com.DASTAK.i230613_i230658_i230736

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class login_v : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var btnLogin: Button
    private lateinit var signupLink: TextView
    //private lateinit var logoutBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ✅ Correct volunteer layout
        setContentView(R.layout.activity_login_v)

        val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)
        val savedRole = sharedPref.getString("role", "")

        // Already logged in → open correct home screen
        if (isLoggedIn) {
            val intent = when (savedRole) {
                "volunteer" -> Intent(this, VolunteerHomeActivity::class.java)
                "organization" -> Intent(this, OrgHomeActivity::class.java)
                else -> Intent(this, RoleActivity::class.java)
            }
            startActivity(intent)
            finishAffinity()
        }

        // Initialize views
        emailInput = findViewById(R.id.usernameInput)
        passwordInput = findViewById(R.id.passwordInput)
        btnLogin = findViewById(R.id.login)
        signupLink = findViewById(R.id.signup)


        // Signup link → volunteer signup page
        signupLink.setOnClickListener {
            val intent = Intent(this, signup_v::class.java)
            startActivity(intent)
        }

        btnLogin.setOnClickListener {
            loginUser()
        }


    }

    private fun loginUser() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val url = Constants.BASE_URL + "login.php"
        val queue: RequestQueue = Volley.newRequestQueue(this)

        val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        val selectedRole = sharedPref.getString("role", "") // role clicked earlier

        val request = object : StringRequest(
            Request.Method.POST, url,
            { result ->

                try {
                    val json = JSONObject(result)
                    val status = json.getString("status")
                    val message = json.getString("message")

                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()

                    if (status == "success") {
                        val userObj = json.getJSONObject("user")
                        val accountRole = userObj.getString("role")

                        // Role validation
                        if (selectedRole == accountRole) {
                            with(sharedPref.edit()) {
                                putBoolean("isLoggedIn", true)
                                putInt("user_id", userObj.getInt("user_id"))
                                putString("name", userObj.getString("name"))
                                putString("email", userObj.getString("email"))
                                putString("role", accountRole)
                                putString("profile_image", userObj.getString("profile_image"))
                                apply()
                            }

                            // ✅ THIS IS THE FIX - Initialize FCM after login
                            FCMTokenService.initializeFCM(this)

                            val intent = when (accountRole) {
                                "volunteer" -> Intent(this, VolunteerHomeActivity::class.java)
                                "organization" -> Intent(this, OrgHomeActivity::class.java)
                                else -> Intent(this, RoleActivity::class.java)
                            }
                            startActivity(intent)
                            finishAffinity()
                        } else {
                            Toast.makeText(
                                this,
                                "Access Denied! This is a $accountRole account, not $selectedRole!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Invalid response from server", Toast.LENGTH_LONG).show()
                }

            },
            { error ->
                Toast.makeText(this, "Network Error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "email" to email,
                    "password" to password
                )
            }
        }

        queue.add(request)
    }

    private fun logoutUser() {
        val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("isLoggedIn", false)
            remove("email")
            apply()
        }

        Toast.makeText(this, "Logged Out", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, login_v::class.java)
        startActivity(intent)
        finishAffinity()
    }
}

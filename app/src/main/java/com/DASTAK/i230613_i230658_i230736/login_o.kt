package com.DASTAK.i230613_i230658_i230736

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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


class login_o : AppCompatActivity() {
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var btnLogin: Button
    private lateinit var signupLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ⚠ FIXED: Use correct organization login layout
        setContentView(R.layout.activity_login_o)

        val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)
        val savedRole = sharedPref.getString("role", "")

        // If already logged in → Open correct home dashboard directly
        if (isLoggedIn) {
            Handler(Looper.getMainLooper()).post {
                val intent = when (savedRole) {
                    "volunteer" -> Intent(this, VolunteerHomeActivity::class.java)
                    "organization" -> Intent(this, OrgHomeActivity::class.java)
                    else -> Intent(this, RoleActivity::class.java)
                }
                startActivity(intent)
                finishAffinity()
            }
        }

        // Initialize inputs
        emailInput = findViewById(R.id.usernameInput)
        passwordInput = findViewById(R.id.passwordInput)
        btnLogin = findViewById(R.id.login)
        signupLink = findViewById(R.id.signup)

        // Signup link → organization signup page
        signupLink.setOnClickListener {
            val intent = Intent(this, signup_o::class.java)
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
            { response ->
                try {
                    val json = JSONObject(response)
                    val status = json.getString("status")


                    if (status == "success") {
                        val userObj = json.getJSONObject("user")  // ← get the nested object
                        val accountRole = userObj.getString("role")

                        // Role validation
                        if (selectedRole == accountRole) {
                            with(sharedPref.edit()) {
                                putBoolean("isLoggedIn", true)
                                putString("email", email)
                                putString("role", accountRole) // save the role
                                apply()
                            }

                            val intent = when (accountRole) {
                                "volunteer" -> Intent(this, VolunteerHomeActivity::class.java)
                                "organization" -> Intent(this, organizationProfile::class.java)
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
                    Toast.makeText(this, "Server error / invalid response", Toast.LENGTH_LONG).show()
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
}
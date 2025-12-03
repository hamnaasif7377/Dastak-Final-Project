package com.DASTAK.i230613_i230658_i230736

import android.content.Intent
import android.os.Bundle
import android.util.Log
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

class signup_o : AppCompatActivity() {

    private lateinit var nameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var btnSignup: Button
    private lateinit var loginLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup_o)

        nameInput = findViewById(R.id.usernameInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        btnSignup = findViewById(R.id.btnRegister)
        loginLink = findViewById(R.id.loginInstead)

        // Go to login page if user clicks login link
        loginLink.setOnClickListener {
            val intent = Intent(this, login_v::class.java)
            startActivity(intent)
        }

        btnSignup.setOnClickListener {
            registerOrganization()
        }
    }

    private fun registerOrganization() {
        val name = nameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if(name.isEmpty() || email.isEmpty() || password.isEmpty()){
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val url = Constants.BASE_URL + "signuporganization.php"
        val queue: RequestQueue = Volley.newRequestQueue(this)

        val stringRequest = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    // Log the raw response for debugging
                    Log.d("ServerResponse", "Raw response: $response")

                    val json = JSONObject(response)
                    val status = json.getString("status")
                    val message = json.getString("message")

                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()

                    if(status == "success"){
                        // Go back to login page after successful signup
                        val intent = Intent(this@signup_o, login_o::class.java)
                        startActivity(intent)
                        finish()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("ParseError", "Failed to parse: $response", e)
                    Toast.makeText(this, "Invalid response from server. Check logs.", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                Log.e("NetworkError", "Error: ${error.message}", error)
                val errorMsg = error.networkResponse?.let {
                    "Error ${it.statusCode}: ${String(it.data)}"
                } ?: "Network error: ${error.message}"
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            }
        ){
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["name"] = name
                params["email"] = email
                params["password"] = password
                return params
            }
        }

        queue.add(stringRequest)
    }
}
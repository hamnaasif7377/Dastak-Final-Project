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
import org.json.JSONObject

class signup_v : AppCompatActivity() {

    private lateinit var usernameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var btnRegister: Button

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
                        // Optional: clear fields
                        usernameInput.text.clear()
                        emailInput.text.clear()
                        passwordInput.text.clear()
                        FCMTokenService.initializeFCM(this)
                        // Redirect to login activity
                        val intent = Intent(this@signup_v, login_v::class.java)
                        startActivity(intent)
                        finish() // optional: remove signup activity from back stack
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
                val params = HashMap<String, String>()
                params["name"] = name
                params["email"] = email
                params["password"] = password
                params["role"] = "volunteer"
                return params
            }
        }

        queue.add(stringRequest)
    }
}
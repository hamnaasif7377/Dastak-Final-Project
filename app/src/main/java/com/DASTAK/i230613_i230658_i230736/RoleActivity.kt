package com.DASTAK.i230613_i230658_i230736

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class RoleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_role)

        val volunteerBtn = findViewById<Button>(R.id.volunteer)
        val orgBtn = findViewById<Button>(R.id.org)

        volunteerBtn.setOnClickListener {
            saveRole("volunteer")
            val intent = Intent(this, login_v::class.java)
            startActivity(intent)
        }

        orgBtn.setOnClickListener {
            saveRole("organization")
            val intent = Intent(this, login_o::class.java)
            startActivity(intent)
        }
    }

    private fun saveRole(role: String) {
        val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("role", role)
            apply()
        }
    }
}

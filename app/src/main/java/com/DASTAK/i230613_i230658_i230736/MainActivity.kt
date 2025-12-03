package com.DASTAK.i230613_i230658_i230736

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)
        val savedRole = sharedPref.getString("role", "")

        Handler(Looper.getMainLooper()).postDelayed({
            if (isLoggedIn) {
                // Go directly to respective home
                val intent = when (savedRole) {
                    "volunteer" -> Intent(this, VolunteerHomeActivity::class.java)
                    "organization" -> Intent(this, organizationProfile::class.java)
                    else -> Intent(this, intro::class.java)
                }
                startActivity(intent)
                finish()
            } else {
                val intent = Intent(this, intro::class.java)
                startActivity(intent)
                finish()
            }
        }, 3000)
    }
}

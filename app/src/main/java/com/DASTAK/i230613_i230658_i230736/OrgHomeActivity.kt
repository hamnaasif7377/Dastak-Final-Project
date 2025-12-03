package com.DASTAK.i230613_i230658_i230736

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class OrgHomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_org_home)

        val logoutBtn = findViewById<Button>(R.id.logout)
        logoutBtn.setOnClickListener {
            logoutUser()
        }


    }

    private fun logoutUser() {
        val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("isLoggedIn", false)
            remove("email")
            remove("role")
            apply()

        }

        val intent = Intent(this, RoleActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }

}
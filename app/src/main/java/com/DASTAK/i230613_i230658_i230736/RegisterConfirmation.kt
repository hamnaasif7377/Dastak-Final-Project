package com.DASTAK.i230613_i230658_i230736

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class RegisterConfirmation : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var btnMenu: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_confirmation)

        initializeViews()
    }

    private fun initializeViews() {
        btnBack = findViewById(R.id.btnBack)
        btnMenu = findViewById(R.id.btnMenu)

        // Back button - return to previous screen
        btnBack.setOnClickListener {
            finish()
        }

        // Menu button
        btnMenu.setOnClickListener {
            // Optional: Open menu if needed
            finish()
        }
    }
}
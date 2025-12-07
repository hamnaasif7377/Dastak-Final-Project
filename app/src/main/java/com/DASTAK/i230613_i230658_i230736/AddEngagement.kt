package com.DASTAK.i230613_i230658_i230736

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.DASTAK.i230613_i230658_i230736.databinding.ActivityAddEngagementBinding
import com.DASTAK.i230613_i230658_i230736.models.Engagement
import com.DASTAK.i230613_i230658_i230736.models.listing
import com.DASTAK.i230613_i230658_i230736.utils.ImageUtils
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class AddEngagement : AppCompatActivity() {

    private lateinit var binding: ActivityAddEngagementBinding
    private val database = FirebaseDatabase.getInstance()

    private var listingImageUri: Uri? = null
    private var engagementImageUri: Uri? = null
    private var selectedListingDate: String = ""
    private var selectedEngagementDate: String = ""
    private var userIdInt = 0
    // Image pickers
    private val listingImagePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            listingImageUri = result.data?.data
            binding.uploadListing.setImageURI(listingImageUri)
        }
    }

    private val engagementImagePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            engagementImageUri = result.data?.data
            binding.uploadEngagement.setImageURI(engagementImageUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEngagementBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val sharedPref = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        userIdInt = try {
            sharedPref.getString("user_id", "0")?.toIntOrNull() ?: 0
        } catch (e: Exception) {
            sharedPref.getInt("user_id", 0)
        }

        setupDatePickers()
        setupImageUploads()
        setupButtons()

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun setupDatePickers() {
        val calendar = Calendar.getInstance()

        // Listing date picker
        binding.datepicker1.init(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ) { _, year, month, day ->
            selectedListingDate = String.format("%02d/%02d/%d", day, month + 1, year)
        }

        // Engagement date picker
        binding.datepicker2.init(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ) { _, year, month, day ->
            selectedEngagementDate = String.format("%02d/%02d/%d", day, month + 1, year)
        }
    }

    private fun setupImageUploads() {
        binding.uploadListing.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            listingImagePicker.launch(intent)
        }

        binding.uploadEngagement.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            engagementImagePicker.launch(intent)
        }
    }

    private fun setupButtons() {
        binding.btnAddItems.setOnClickListener {
            addListing()
        }

        binding.btnAddEngagement.setOnClickListener {
            addEngagement()
        }
    }

    private fun addListing() {
        val itemName = binding.itemname.text.toString().trim()
        val quantity = binding.itemquantity.text.toString().trim()

        if (itemName.isEmpty()) {
            Toast.makeText(this, "Please enter item name", Toast.LENGTH_SHORT).show()
            return
        }

        if (quantity.isEmpty()) {
            Toast.makeText(this, "Please enter quantity", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedListingDate.isEmpty()) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
            return
        }

        if (listingImageUri == null) {
            Toast.makeText(this, "Please upload an image", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading
        binding.btnAddItems.isEnabled = false
        binding.btnAddItems.text = "Adding..."

        // Convert image to Base64
        val imageBase64 = ImageUtils.uriToBase64(this, listingImageUri!!)

        if (imageBase64 == null) {
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show()
            binding.btnAddItems.isEnabled = true
            binding.btnAddItems.text = "Add Listing"
            return
        }

        saveListing(itemName, quantity, imageBase64)
    }

    private fun saveListing(itemName: String, quantity: String, imageBase64: String) {
        val listingId = database.reference.child("listings").push().key ?: return

        val listingItem = listing(
            id = listingId,
            userId = userIdInt,  // ✅ Changed to use Int
            title = itemName,
            subtitle = "Quantity: $quantity",
            imageBase64 = imageBase64,
            date = selectedListingDate,
            timestamp = System.currentTimeMillis()
        )

        database.reference.child("listings").child(listingId).setValue(listingItem)
            .addOnSuccessListener {
                Toast.makeText(this, "Listing added successfully!", Toast.LENGTH_SHORT).show()
                clearListingFields()
                binding.btnAddItems.isEnabled = true
                binding.btnAddItems.text = "Add Listing"
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to add listing: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnAddItems.isEnabled = true
                binding.btnAddItems.text = "Add Listing"
            }
    }

    private fun addEngagement() {
        val eventName = binding.eventname.text.toString().trim()
        val eventPlace = binding.eventplace.text.toString().trim()

        if (eventName.isEmpty()) {
            Toast.makeText(this, "Please enter event name", Toast.LENGTH_SHORT).show()
            return
        }

        if (eventPlace.isEmpty()) {
            Toast.makeText(this, "Please enter event place", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedEngagementDate.isEmpty()) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
            return
        }

        if (engagementImageUri == null) {
            Toast.makeText(this, "Please upload an image", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading
        binding.btnAddEngagement.isEnabled = false
        binding.btnAddEngagement.text = "Adding..."

        // Convert image to Base64
        val imageBase64 = ImageUtils.uriToBase64(this, engagementImageUri!!)

        if (imageBase64 == null) {
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show()
            binding.btnAddEngagement.isEnabled = true
            binding.btnAddEngagement.text = "Add Engagement"
            return
        }

        saveEngagement(eventName, eventPlace, imageBase64)
    }

    private fun saveEngagement(eventName: String, eventPlace: String, imageBase64: String) {
        val engagementId = database.reference.child("engagements").push().key ?: return

        val engagement = Engagement(
            id = engagementId,
            userId = userIdInt,  // ✅ Changed to use Int
            title = eventName,
            place = eventPlace,
            whenText = selectedEngagementDate,
            attendeesText = "0 attendees",
            imageBase64 = imageBase64,
            date = selectedEngagementDate,
            timestamp = System.currentTimeMillis()
        )

        database.reference.child("engagements").child(engagementId).setValue(engagement)
            .addOnSuccessListener {
                Toast.makeText(this, "Engagement added successfully!", Toast.LENGTH_SHORT).show()
                clearEngagementFields()
                binding.btnAddEngagement.isEnabled = true
                binding.btnAddEngagement.text = "Add Engagement"
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to add engagement: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnAddEngagement.isEnabled = true
                binding.btnAddEngagement.text = "Add Engagement"
            }
    }

    private fun clearListingFields() {
        binding.itemname.text?.clear()
        binding.itemquantity.text?.clear()
        listingImageUri = null
        binding.uploadListing.setImageResource(R.drawable.grid_icon)
    }

    private fun clearEngagementFields() {
        binding.eventname.text?.clear()
        binding.eventplace.text?.clear()
        engagementImageUri = null
        binding.uploadEngagement.setImageResource(R.drawable.grid_icon)
    }
}


package com.emergency.sevaapp.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.emergency.sevaapp.R

class ContactUsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_us)

        // Phone Number Click Listener
        val phoneLayout = findViewById<LinearLayout>(R.id.phone_layout) // ID of the phone layout

        phoneLayout.setOnClickListener {    
            val phoneIntent = Intent(Intent.ACTION_DIAL)
            phoneIntent.data = Uri.parse("tel:18002330000") // Replace with your phone number
            startActivity(phoneIntent)
        }

        // Website Click Listener
        val websiteLayout = findViewById<LinearLayout>(R.id.webLayout) // ID of the website layout
        websiteLayout.setOnClickListener {
            val websiteUrl = "https://www.emergencyseva.ai" // Replace with your website URL
            val websiteIntent = Intent(Intent.ACTION_VIEW)
            websiteIntent.data = Uri.parse(websiteUrl)
            try {
                startActivity(websiteIntent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "No browser found to open the website", Toast.LENGTH_SHORT).show()
            }
        }

        // Email Click Listener
        val emailLayout = findViewById<LinearLayout>(R.id.email_layout) // ID of the email layout
        emailLayout.setOnClickListener{
            val emailIntent = Intent(Intent.ACTION_SENDTO)
            emailIntent.data = Uri.parse("mailto:instaaid@emergencyseva.ai") // Replace with your email
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Customer Support") // Optional subject
            startActivity(Intent.createChooser(emailIntent, "Send Email"))
        }

        // Back Button Listener
        val backButton = findViewById<ImageView>(R.id.back_button)
        backButton.setOnClickListener {
            onBackPressed() // Navigate back to the previous screen
        }

        // Address Click Listener
        val addressLayout = findViewById<LinearLayout>(R.id.address_layout) // ID of the address layout
        addressLayout.setOnClickListener {
            // Latitude and Longitude of the address
            val latitude = 21.1588511 // Example latitude for Nagpur
            val longitude = 79.0830123 // Example longitude for Nagpur
            // Create Intent with geo URI
            val addressIntent = Intent(Intent.ACTION_VIEW)
            addressIntent.data = Uri.parse("geo:$latitude,$longitude") // Use latitude and longitude
            startActivity(addressIntent)
        }
    }
}

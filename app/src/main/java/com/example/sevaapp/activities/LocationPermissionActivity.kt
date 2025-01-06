package com.example.sevaapp.activities

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.location.LocationManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sevaapp.R

class LocationPermissionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_permission)

        findViewById<Button>(R.id.btn_turn_on_location).setOnClickListener {
            // Open location settings
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }

        findViewById<Button>(R.id.not_now_Button).setOnClickListener {
            navigateToMainActivity()
            Toast.makeText(this@LocationPermissionActivity,"Please turn on your location",Toast.LENGTH_SHORT).show()

        }
    }

    override fun onResume() {
        super.onResume()
        if (isLocationEnabled()) {
            navigateToMainActivity() // Navigate if location is enabled
        }
    }

    /**
     * Function to check if location services are enabled
     */
    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Function to navigate to MainActivity
     */
    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish() // Close LocationPermissionActivity
    }
}

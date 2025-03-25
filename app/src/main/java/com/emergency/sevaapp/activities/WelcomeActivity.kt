package com.emergency.sevaapp.activities

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.emergency.sevaapp.R

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        // Check internet connectivity before proceeding
//        if (!isInternetAvailable()) {
//            showNoInternetMessage()
//            return
//        }

        // Check if the user is logged in
        if (isUserLoggedIn()) {
            navigateToActivity(MainActivity::class.java)
            return
        }

        // Set the welcome screen layout if the user is not logged in
        setContentView(R.layout.activity_welcome)

        val terms = findViewById<TextView>(R.id.terms)
        val text = "To continue, you agree to our Terms and Conditions"
        val spannableString = SpannableString(text)
        val highlightColor = ContextCompat.getColor(this, R.color.highlight_color)
        val colorSpan = ForegroundColorSpan(highlightColor) // Change to your desired color
        // Apply color only to "Terms and Conditions"
        spannableString.setSpan(colorSpan, text.indexOf("Terms and Conditions"),
            text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        terms.text = spannableString

        terms.setOnClickListener {
            navigateToActivity(TermConditionActivity::class.java)
        }

        findViewById<Button>(R.id.getStarted).setOnClickListener {
            navigateToActivity(LoginActivity::class.java)
        }
    }

    private fun isUserLoggedIn(): Boolean {
        // Check if the user is logged in using shared preferences
        return getSharedPreferences("AppPrefs", MODE_PRIVATE)
            .getBoolean("is_logged_in", false) // Ensure the key matches your login state saving logic
    }

    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
        finish() // Prevent navigating back to WelcomeActivity
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun showNoInternetMessage() {
        // Use Snackbar to show the offline message at the bottom
        val rootView = findViewById<View>(android.R.id.content) // Root view of the current activity
        Snackbar.make(rootView, "Seems like you are offline. Please check your connection and try again.", Snackbar.LENGTH_LONG)
            .setAction("Dismiss") {
                // You can also close the app or go back if the user presses Dismiss
                finish()
            }
            .show()
    }
}

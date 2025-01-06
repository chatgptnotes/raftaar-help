package com.example.sevaapp.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.sevaapp.R

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if the user is logged in and navigate accordingly
        if (isUserLoggedIn()) {
            navigateToActivity(MainActivity::class.java)
            return
        }

        setContentView(R.layout.activity_welcome)

        findViewById<Button>(R.id.getStarted).setOnClickListener {
            navigateToActivity(LoginActivity::class.java)
//            navigateToActivity(MainActivity::class.java)
        }
    }

    private fun isUserLoggedIn(): Boolean {
        return getSharedPreferences("AppPrefs", MODE_PRIVATE)
            .getBoolean("isLoggedIn", false)
    }

    private fun navigateToActivity(activityClass: Class<*>) {
        startActivity(Intent(this, activityClass))
        finish() // Prevent navigating back to WelcomeActivity
    }
}

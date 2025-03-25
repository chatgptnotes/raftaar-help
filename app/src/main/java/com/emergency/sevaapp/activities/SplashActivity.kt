package com.emergency.sevaapp.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageView
import com.emergency.sevaapp.R

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logoImageView = findViewById<ImageView>(R.id.logoImageView)

        // Add fade-in animation to the logo
        val fadeIn = AlphaAnimation(0.0f, 1.0f).apply {
            duration = 1500 // 1.5 seconds for fade-in
            fillAfter = true
        }

        logoImageView.startAnimation(fadeIn)

        // Add listener to start navigation after fade-in animation
        fadeIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}

            override fun onAnimationEnd(animation: Animation) {
                navigateToNextScreen() // Navigate only after animation ends
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
    }

    private fun navigateToNextScreen() {
        // Check login status and navigate accordingly
        val nextActivity = if (isUserLoggedIn()) MainActivity::class.java else WelcomeActivity::class.java
        startActivity(Intent(this, nextActivity))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out) // Add transition animation
        finish()
    }

    private fun isUserLoggedIn(): Boolean {
        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        return sharedPreferences.getBoolean("is_logged_in", false) // Default is false
    }

    private fun navigateToActivity(activityClass: Class<*>) {
        startActivity(Intent(this, activityClass))
        finish() // Prevent returning to this activity
    }
}

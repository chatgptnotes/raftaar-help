package com.raftaar.emergencyy.activities

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.raftaar.emergencyy.R

class EmergencyAssistanceActivity : AppCompatActivity() {

    private lateinit var sosButton: MaterialCardView
    private lateinit var aiAssistantCard: View
    private lateinit var trustedContactsCard: View
    private lateinit var emergencyDirectoryCard: View
    private lateinit var myProfileCard: View
    private lateinit var startVoiceChatButton: MaterialButton
    private lateinit var pulseAnimation: android.view.animation.Animation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_assistance)

        initializeViews()
        setupFeatureCards()
        setupClickListeners()
        setupPulseAnimation()
    }

    private fun initializeViews() {
        sosButton = findViewById(R.id.sosButtonContainer)
        aiAssistantCard = findViewById(R.id.aiAssistantCard)
        trustedContactsCard = findViewById(R.id.trustedContactsCard)
        emergencyDirectoryCard = findViewById(R.id.emergencyDirectoryCard)
        myProfileCard = findViewById(R.id.myProfileCard)
        startVoiceChatButton = findViewById(R.id.startVoiceChatButton)
        
        // Set blood red color for SOS button
        sosButton.setCardBackgroundColor(getColor(R.color.blood_red))
    }

    private fun setupPulseAnimation() {
        pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse)
        sosButton.startAnimation(pulseAnimation)
    }

    private fun setupFeatureCards() {
        // AI Assistant Card
        aiAssistantCard.apply {
            findViewById<ImageView>(R.id.featureIcon).setBackgroundResource(R.drawable.ic_chat)
            findViewById<TextView>(R.id.featureTitle).text = "AI Assistant"
            findViewById<TextView>(R.id.featureDescription).text = 
                "Get instant help and guidance from our AI"
        }

        // Trusted Contacts Card
        trustedContactsCard.apply {
            findViewById<ImageView>(R.id.featureIcon).setBackgroundResource(R.drawable.ic_contacts)
            findViewById<TextView>(R.id.featureTitle).text = "Trusted Contacts"
            findViewById<TextView>(R.id.featureDescription).text = 
                "Manage your emergency contact list"
        }

        // Emergency Directory Card
        emergencyDirectoryCard.apply {
            findViewById<ImageView>(R.id.featureIcon).setBackgroundResource(R.drawable.ic_location)
            findViewById<TextView>(R.id.featureTitle).text = "Emergency Directory"
            findViewById<TextView>(R.id.featureDescription).text = 
                "Find local emergency services"
        }

        // My Profile Card
        myProfileCard.apply {
            findViewById<ImageView>(R.id.featureIcon).setBackgroundResource(R.drawable.ic_profile)
            findViewById<TextView>(R.id.featureTitle).text = "My Profile"
            findViewById<TextView>(R.id.featureDescription).text = 
                "Update your personal information"
        }
    }

    private fun setupClickListeners() {
        // SOS Button
        sosButton.setOnClickListener {
            // Redirect to emergency seva website
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://admin.emergencyseva.in/public/emergency-sewa"))
            startActivity(intent)
        }

        // Feature Cards
        aiAssistantCard.setOnClickListener {
            // TODO: Navigate to AI Assistant
        }

        trustedContactsCard.setOnClickListener {
            // TODO: Navigate to Trusted Contacts
        }

        emergencyDirectoryCard.setOnClickListener {
            startActivity(Intent(this, EmergencyDirectoryActivity::class.java))
        }

        myProfileCard.setOnClickListener {
            // TODO: Navigate to My Profile
        }

        // Voice Chat
        startVoiceChatButton.setOnClickListener {
            // TODO: Start voice chat
        }
    }

    override fun onPause() {
        super.onPause()
        sosButton.clearAnimation()
    }

    override fun onResume() {
        super.onResume()
        sosButton.startAnimation(pulseAnimation)
    }
}
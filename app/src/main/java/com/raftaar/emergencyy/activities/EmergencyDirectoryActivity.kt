package com.raftaar.emergencyy.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.raftaar.emergencyy.R
import com.raftaar.emergencyy.adapters.EmergencyServiceAdapter
import com.raftaar.emergencyy.models.EmergencyService

class EmergencyDirectoryActivity : AppCompatActivity() {

    private lateinit var serviceTypeDropdown: AutoCompleteTextView
    private lateinit var stateDropdown: AutoCompleteTextView
    private lateinit var districtDropdown: AutoCompleteTextView
    private lateinit var servicesRecyclerView: RecyclerView
    private lateinit var serviceAdapter: EmergencyServiceAdapter

    private val serviceTypes = listOf("All Types", "Ambulance", "Police", "Fire", "Other")
    private val states = listOf("All States", "Delhi", "Maharashtra", "Karnataka", "Tamil Nadu")
    private val districts = listOf("All Districts", "New Delhi", "Mumbai", "Bangalore", "Chennai")

    private val emergencyServices = listOf(
        EmergencyService("All India Emergency Services", "112", "National Emergency Number", "All - All", true),
        EmergencyService("Police Emergency", "100", "National Police Emergency", "All - All", true),
        EmergencyService("Fire Emergency", "101", "National Fire Emergency", "All - All", true),
        EmergencyService("Medical Emergency (Ambulance)", "108", "National Ambulance Service", "All - All", true),
        EmergencyService("Delhi Police Control Room", "+91-11-23490000", "Delhi Police Headquarters, ITO", "Delhi - New Delhi", true),
        EmergencyService("AIIMS Emergency", "+91-11-26588500", "AIIMS, Ansari Nagar, New Delhi", "Delhi - New Delhi", true),
        EmergencyService("Mumbai Police Control", "+91-22-22621855", "Mumbai Police HQ, Crawford Market", "Maharashtra - Mumbai", true),
        EmergencyService("KEM Hospital Emergency", "+91-22-24107000", "KEM Hospital, Parel, Mumbai", "Maharashtra - Mumbai", true)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_directory)

        initializeViews()
        setupDropdowns()
        setupRecyclerView()
        setupEmergencyCards()
    }

    private fun initializeViews() {
        serviceTypeDropdown = findViewById(R.id.serviceTypeDropdown)
        stateDropdown = findViewById(R.id.stateDropdown)
        districtDropdown = findViewById(R.id.districtDropdown)
        servicesRecyclerView = findViewById(R.id.servicesRecyclerView)
    }

    private fun setupDropdowns() {
        serviceTypeDropdown.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, serviceTypes))
        stateDropdown.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, states))
        districtDropdown.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, districts))

        // Add filters
        serviceTypeDropdown.setOnItemClickListener { _, _, position, _ ->
            filterServices()
        }

        stateDropdown.setOnItemClickListener { _, _, position, _ ->
            filterServices()
        }

        districtDropdown.setOnItemClickListener { _, _, position, _ ->
            filterServices()
        }
    }

    private fun setupRecyclerView() {
        serviceAdapter = EmergencyServiceAdapter(emergencyServices) { service ->
            // Handle click on service - make a phone call
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${service.phoneNumber}")
            }
            startActivity(intent)
        }

        servicesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@EmergencyDirectoryActivity)
            adapter = serviceAdapter
        }
    }

    private fun setupEmergencyCards() {
        // Set click listeners for emergency number cards
        findViewById<MaterialCardView>(R.id.allEmergencyCard).setOnClickListener {
            dialEmergencyNumber("112")
        }

        findViewById<MaterialCardView>(R.id.policeCard).setOnClickListener {
            dialEmergencyNumber("100")
        }

        findViewById<MaterialCardView>(R.id.fireCard).setOnClickListener {
            dialEmergencyNumber("101")
        }

        findViewById<MaterialCardView>(R.id.ambulanceCard).setOnClickListener {
            dialEmergencyNumber("108")
        }
    }

    private fun filterServices() {
        val selectedType = serviceTypeDropdown.text.toString()
        val selectedState = stateDropdown.text.toString()
        val selectedDistrict = districtDropdown.text.toString()

        val filteredList = emergencyServices.filter { service ->
            val matchesType = selectedType == "All Types" || 
                service.name.contains(selectedType, ignoreCase = true)
            val location = service.location.split(" - ")
            val matchesState = selectedState == "All States" || 
                location[0] == selectedState
            val matchesDistrict = selectedDistrict == "All Districts" || 
                location.getOrNull(1) == selectedDistrict

            matchesType && matchesState && matchesDistrict
        }

        serviceAdapter.updateServices(filteredList)
    }

    private fun dialEmergencyNumber(number: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$number")
        }
        startActivity(intent)
    }
} 
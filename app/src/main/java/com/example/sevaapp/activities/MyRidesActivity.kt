package com.example.sevaapp.activities

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.sevaapp.R
import com.example.sevaapp.adapter.RidesAdapter
import com.example.sevaapp.models.Ride
import org.json.JSONArray
import org.json.JSONObject

class MyRidesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var ridesAdapter: RidesAdapter
    private val ridesList = mutableListOf<Ride>()
    private var searchEditText: EditText? = null // Declare as a class-level variable
    private lateinit var noDataLayout: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_rides)

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerViewRides)
        recyclerView.layoutManager = LinearLayoutManager(this)
        noDataLayout = findViewById(R.id.noDataLayout)

        // Fetch rides and set up the adapter

        val searchIcon = findViewById<ImageView>(R.id.search_icon)

        // Assuming a back button exists in your layout
        val topBar = findViewById<ConstraintLayout>(R.id.top_bar)
        val titleTextView = findViewById<TextView>(R.id.title_text)

        val backButton = findViewById<ImageView>(R.id.back_button)
        backButton.setOnClickListener {
            // Finish the current activity and go back to the previous one
            onBackPressed()
        }

        // Fetch data from API
        fetchRidesFromApi()

        searchIcon.setOnClickListener {
            // Remove the title TextView and hide the back button
            titleTextView.visibility = View.GONE
            backButton.visibility = View.INVISIBLE

            // Dynamically create an EditText for the search bar
            searchEditText = EditText(this).apply {
                id = View.generateViewId()
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = 16
                    marginEnd = 16
                }
                hint = "Search rides..."
                inputType = InputType.TYPE_CLASS_TEXT
            }

            // Add the EditText to the ConstraintLayout
            topBar.addView(searchEditText)

            // Set up constraints for the EditText
            val constraintSet = ConstraintSet()
            constraintSet.clone(topBar)
            constraintSet.connect(searchEditText!!.id, ConstraintSet.START, topBar.id, ConstraintSet.START, 16)
            constraintSet.connect(searchEditText!!.id, ConstraintSet.END, searchIcon.id, ConstraintSet.START, 16)
            constraintSet.connect(searchEditText!!.id, ConstraintSet.TOP, topBar.id, ConstraintSet.TOP)
            constraintSet.connect(searchEditText!!.id, ConstraintSet.BOTTOM, topBar.id, ConstraintSet.BOTTOM)
            constraintSet.applyTo(topBar)

            // Request focus for the search bar
            searchEditText!!.requestFocus()

            // Add TextWatcher to filter rides as the user types
            searchEditText!!.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                    filterRides(s.toString()) // Call the filtering function with the input text
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }

    private fun fetchRidesFromApi() {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        val sharedPreferences = getSharedPreferences("RideDetails", Context.MODE_PRIVATE)
        val savedRidesJson = sharedPreferences.getString("rides", "[]")

        try {
            val savedRidesArray = JSONArray(savedRidesJson)
            ridesList.clear()
            if (savedRidesArray.length() > 0) {
                for (i in 0 until savedRidesArray.length()) {
                    val rideObject = savedRidesArray.getJSONObject(i)
                    ridesList.add(
                        Ride(
                            date = rideObject.getString("date"),
                            time = rideObject.getString("time"),
                            status = rideObject.getString("status"),
                            vehicle = rideObject.getString("vehicle"),
                            crm = "N/A",
                            pickupLocation = rideObject.getString("address"),
                            dropLocation = rideObject.getString("city"),
                            profilePicture = ""
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        ridesAdapter = RidesAdapter(ridesList) { position ->
            deleteRide(position)
        }
        recyclerView.adapter = ridesAdapter

        toggleNoDataLayout() // Update visibility
        progressBar.visibility = View.GONE
    }

    private fun deleteRide(position: Int) {
        val rideToDelete = ridesList[position]

        SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
            .setTitleText("Are you sure?")
            .setContentText("Do you want to delete this ride?")
            .setConfirmText("Yes")
            .setCancelText("No")
            .setConfirmClickListener { sweetAlertDialog ->
                sweetAlertDialog.dismissWithAnimation()

                // Remove the ride from the list
                ridesList.removeAt(position)

                // Notify the adapter about the item removed
                ridesAdapter.notifyItemRemoved(position)

                // Handle the empty state
                if (ridesList.isEmpty()) {
                    ridesAdapter.notifyDataSetChanged() // Refresh adapter for empty state
                    toggleNoDataLayout() // Show "No Data" layout
                }

                // Update SharedPreferences to reflect the changes
                val sharedPreferences = getSharedPreferences("RideDetails", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()

                val updatedRidesArray = JSONArray()
                for (ride in ridesList) {
                    val rideObject = JSONObject().apply {
                        put("date", ride.date)
                        put("time", ride.time)
                        put("status", ride.status)
                        put("vehicle", ride.vehicle)
                        put("address", ride.pickupLocation)
                        put("city", ride.dropLocation)
                        put("profilePicture", ride.profilePicture)
                    }
                    updatedRidesArray.put(rideObject)
                }

                editor.putString("rides", updatedRidesArray.toString())
                editor.apply()

                // Display success message
                SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                    .setTitleText("Deleted!")
                    .setContentText("The ride has been successfully deleted.")
                    .setConfirmText("OK")
                    .show()
            }
            .setCancelClickListener { sweetAlertDialog ->
                sweetAlertDialog.dismissWithAnimation()
            }
            .show()
    }

    private fun toggleNoDataLayout() {
        if (ridesList.isEmpty()) {
            noDataLayout.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            noDataLayout.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    override fun onBackPressed() {
        if (searchEditText != null) {
            // Remove the search EditText and restore the original UI elements
            val topBar = findViewById<ConstraintLayout>(R.id.top_bar)
            val titleTextView = findViewById<TextView>(R.id.title_text)
            val backButton = findViewById<ImageView>(R.id.back_button)

            topBar.removeView(searchEditText)
            searchEditText = null // Clear the reference
            titleTextView.visibility = View.VISIBLE
            backButton.visibility = View.VISIBLE
        } else {
            // Default behavior
            super.onBackPressed()
        }
    }
}

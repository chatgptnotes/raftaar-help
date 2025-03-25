package com.emergency.sevaapp.activities

import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.pedant.SweetAlert.SweetAlertDialog
import com.emergency.sevaapp.R
import com.emergency.sevaapp.adapter.RidesAdapter
import com.emergency.sevaapp.apiclient.RetrofitClient
import com.emergency.sevaapp.models.Ride
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyRidesActivity : AppCompatActivity() {
    private var isInitialFetch = true // Flag for initial fetch
    private lateinit var handler: Handler
    private val DELAY: Long = 1000 // Time in milliseconds for the refresh interval (5 seconds)
    private lateinit var recyclerView: RecyclerView
    private lateinit var ridesAdapter: RidesAdapter
    private var rideList: MutableList<Ride> = mutableListOf()
    private var filteredRidesList: MutableList<Ride> = mutableListOf()
//    private var searchEditText: EditText? = null
    private lateinit var noDataLayout: View
    private lateinit var progressBar: ProgressBar // Declare ProgressBar
    private lateinit var titleTextView: TextView
    private lateinit var backButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_rides)

        // Initialize ProgressBar and other views
        progressBar = findViewById(R.id.progressBar)
        titleTextView = findViewById(R.id.title_text)
        backButton = findViewById(R.id.back_button)
        // Set up the back button functionality
        backButton.setOnClickListener {
            // Handle the back button click
            onBackPressed()
        }

        // Set up RecyclerView
        recyclerView = findViewById(R.id.recyclerViewRides)
        recyclerView.layoutManager = LinearLayoutManager(this)
        ridesAdapter = RidesAdapter(filteredRidesList) { position ->
            // Handle ride removal on item click
            filteredRidesList.removeAt(position)
            ridesAdapter.notifyItemRemoved(position)
        }
        recyclerView.adapter = ridesAdapter

        // Initialize no data layout
        noDataLayout = findViewById(R.id.noDataLayout)

        // Set up search functionality
//        val searchIcon = findViewById<ImageView>(R.id.search_icon)
//        searchIcon.setOnClickListener {
//            // Show EditText for search when clicked
////            showSearchField()
//        }

        // Fetch ride data
        fetchRidesData()

        // Start periodic data refresh
        startPeriodicRefresh()

        if (!isInternetAvailable()) {
            // Hide progress bar and show "No Internet" message
            progressBar.visibility = View.GONE
            SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("No Internet Connection ")
                .setContentText("Failed to fetch Rides")
                .setConfirmText("OK")
                .show()
            return // Skip the rest of the function if there's no internet
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
        return activeNetwork?.isConnected == true
    }

    private fun startPeriodicRefresh() {
        handler = Handler(Looper.getMainLooper())

        // Define a Runnable task to fetch rides data every 5 seconds
        val refreshRunnable = object : Runnable {
            override fun run() {
                fetchRidesData() // Refresh the data
                handler.postDelayed(this, DELAY) // Call this function every 5 seconds
            }
        }

        // Start the periodic task
        handler.post(refreshRunnable)
    }

    private fun stopPeriodicRefresh() {
        // Stop the handler from executing the periodic task
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop the periodic task when the activity is destroyed
        stopPeriodicRefresh()
    }

    private fun fetchRidesData() {
        // Show the ProgressBar while fetching data only for the initial fetch
        if (isInitialFetch) {
            progressBar.visibility = View.VISIBLE
        }

        // Check if there is an internet connection
        // Get the phone number from SharedPreferences
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val phoneNumber = sharedPref.getString("cached_phone", "") ?: ""

        if (phoneNumber.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Make the API call using the fetched phone number
                    val response = RetrofitClient.instance.getBooking(phoneNumber)
                    if (response.isSuccessful && response.body() != null) {
                        val rides = response.body()!!.data

                        if (rides.isEmpty()) {
                            // No rides available
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@MyRidesActivity, "No Rides Available", Toast.LENGTH_SHORT).show()
                                progressBar.visibility = View.GONE
                            }
                        } else {
                            // Reverse the list to show in descending order
                            val reversedRides = rides.toMutableList().apply {
                                reverse()
                            }
                            withContext(Dispatchers.Main) {
                                rideList.clear()
                                rideList.addAll(reversedRides) // Set the reversed list
                                filteredRidesList.clear()
                                filteredRidesList.addAll(reversedRides) // Initialize filtered list with reversed data
                                ridesAdapter.notifyDataSetChanged()
                                toggleNoDataLayout()
                                isInitialFetch = false // Mark the initial fetch as complete
                                progressBar.visibility = View.GONE // Hide ProgressBar after initial fetch
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MyRidesActivity, "No data found", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
//                        Toast.makeText(this@MyRidesActivity, "Failed to fetch rides", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    if (isInitialFetch) {
                        withContext(Dispatchers.Main) {
                            progressBar.visibility = View.GONE // Hide ProgressBar after the initial fetch is done
                        }
                    }
                }
            }
        } else {
            Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleNoDataLayout() {
        if (filteredRidesList.isEmpty()) {
            noDataLayout.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            noDataLayout.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
}
package com.raftaar.emergencyy.activities

import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.raftaar.emergencyy.R
import com.raftaar.emergencyy.adapter.RidesAdapter
import com.raftaar.emergencyy.apiclient.RetrofitClient
import com.raftaar.emergencyy.models.Ride
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
    private lateinit var noDataLayout: View
    private lateinit var noInternetLayout: View // Declare no internet layout here
    private lateinit var progressBar: ProgressBar
    private lateinit var titleTextView: TextView
    private lateinit var backButton: ImageView
    // Declare lastFetchedRides to track the previous rides data
    private var lastFetchedRides: MutableList<Ride> = mutableListOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_rides)

        // Initialize ProgressBar and other views
        progressBar = findViewById(R.id.progressBar)
        titleTextView = findViewById(R.id.title_text)
        backButton = findViewById(R.id.back_button)
        noDataLayout = findViewById(R.id.noDataLayout)
        noInternetLayout = findViewById(R.id.no_internet_layout) // Initialize No Internet layout
        // Set up the back button functionality
        backButton.setOnClickListener {
            onBackPressed()
        }

        // Set up RecyclerView
        recyclerView = findViewById(R.id.recyclerViewRides)
        recyclerView.layoutManager = LinearLayoutManager(this)
        ridesAdapter = RidesAdapter(filteredRidesList) { position ->
            filteredRidesList.removeAt(position)
            ridesAdapter.notifyItemRemoved(position)
        }
        recyclerView.adapter = ridesAdapter

        // Fetch ride data
        if (!isInternetAvailable()) {
            showNoInternetScreen()
        } else {
            fetchRidesData()
            startPeriodicRefresh()
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
        return activeNetwork?.isConnected == true
    }

    private fun showNoInternetScreen() {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.GONE
        noInternetLayout.visibility = View.VISIBLE  // Show the No Internet Layout
    }

    private fun showNoDataScreen() {
        noDataLayout.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
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
        // Only stop handler if it's initialized
        if (::handler.isInitialized) {
            // Stop the handler from executing the periodic task
            handler.removeCallbacksAndMessages(null)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop the periodic task when the activity is destroyed
        stopPeriodicRefresh()
    }

    private fun fetchRidesData() {
        if (isInitialFetch) {
            progressBar.visibility = View.VISIBLE
        }

        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val phoneNumber = sharedPref.getString("cached_phone", "") ?: ""

        if (phoneNumber.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = RetrofitClient.instance.getBooking(phoneNumber)

                    // Log the response body for debugging
                    Log.d("API Response", response.body().toString())

                    if (response.isSuccessful && response.body() != null) {
                        val rides = response.body()!!.data

                        // Check if the rides data is empty
                        if (rides.isEmpty()) {
                            withContext(Dispatchers.Main) {
                                // Show "No Data Found" screen if no rides available
                                Toast.makeText(this@MyRidesActivity, "No Rides Available", Toast.LENGTH_SHORT).show()
                                progressBar.visibility = View.GONE
                                showNoDataScreen()
                            }
                        } else {
                            val reversedRides = rides.toMutableList().apply { reverse() }

                            withContext(Dispatchers.Main) {
                                // Check if the rides data has changed
                                if (reversedRides != lastFetchedRides) {
                                    rideList.clear()
                                    rideList.addAll(reversedRides)
                                    filteredRidesList.clear()
                                    filteredRidesList.addAll(reversedRides)
                                    ridesAdapter.notifyDataSetChanged()

                                    lastFetchedRides.clear()
                                    lastFetchedRides.addAll(reversedRides)

                                    toggleNoDataLayout() // Hide the "No Data" layout if there are rides
                                }
                                isInitialFetch = false
                                progressBar.visibility = View.GONE
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MyRidesActivity, "No data found", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MyRidesActivity, "Error fetching data", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    if (isInitialFetch) {
                        withContext(Dispatchers.Main) {
                            progressBar.visibility = View.GONE
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
package com.raftaar.emergencyy.activities

import android.Manifest
import android.app.PendingIntent
import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import cn.pedant.SweetAlert.SweetAlertDialog
import com.bumptech.glide.Glide
import com.raftaar.emergencyy.R
import com.raftaar.emergencyy.apiclient.RetrofitClient
import com.raftaar.emergencyy.models.BookingDetails
import com.raftaar.emergencyy.models.BookingResponse
import com.raftaar.emergencyy.models.ProfileResponse
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.navigation.NavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    //  private val CHANNEL_ID = "raftaar_seva_notifications"
    val CHANNEL_ID = "raftaar_seva_notifications"
//  val gifUrl = "https://i.giphy.com/media/v1.Y2lkPTc5MGI3NjExdWJmNzJ0MmNpMGw0djhzZXZidG9mcXJ3cW5mdmRtdzd6ajhxbjBqaCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/wSnOQErP2oRnk4RB36/giphy.gif"

    // UI Components
    private lateinit var currentLocationField: EditText
    private lateinit var latitudeField: EditText
    private lateinit var longitudeField: EditText
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var progressDialog: ProgressDialog
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var youtube: ImageView
    private lateinit var facebook: ImageView
    private lateinit var twitter: ImageView
    private lateinit var instagram: ImageView
    private lateinit var navView: NavigationView

    // Data Variables
    private var updatedLatitude: Double = 0.0
    private var updatedLongitude: Double = 0.0
    private var updatedAddress: String = ""
    private var updatedCity: String = ""
    private var updatedPincode: String = ""

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val AUTOCOMPLETE_REQUEST_CODE = 1

//const val NOTIFICATION_INTERVAL = 4 * 24 * 60 * 60 * 1000L // 4 days
        const val NOTIFICATION_INTERVAL = 10 * 1000L // 4 days // 50 sec
//      const val NOTIFICATION_INTERVAL = 1 * 24 * 60 * 60 * 1000L // 1 days
//        const val NOTIFICATION_INTERVAL = 2 * 24 * 60 * 60 * 1000L // 3 days
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Set up the heart button
        val heartButton: AppCompatImageButton = findViewById(R.id.heart_button)
        val heartButtonAnimation = AnimationUtils.loadAnimation(this, R.anim.location_button_animation)
        heartButton.setOnClickListener { view ->
            view.startAnimation(heartButtonAnimation)
            // Trigger the location save dialog when the heart button is clicked
            onHeartDrawableClick()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
            startActivity(intent)
        }

        // Initialize Map and Services
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyCFpPr4-VEcfSMMT5CeH8DlQbjErz6yORk")
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val sharedPreferences: SharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val uniqueId = sharedPreferences.getString("unique_id", null)

        if (uniqueId != null) {
            // Use uniqueId for fetching or displaying user data
            Log.e("Current Unique id", "Unique ID: $uniqueId")
            //            Toast.makeText(this, "Unique ID: $uniqueId", Toast.LENGTH_SHORT).show()
            fetchUserData(uniqueId) // Implement this method to load user-specific data
        } else {
            Toast.makeText(this, "Unique ID not found. Please log in again.", Toast.LENGTH_SHORT)
                .show()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }

        // Initialize UI
        initializeViews()
        setupNavigationDrawer()
        initializeMap()

        // **Ensure UI is fully initialized before showing alert**
        Handler(Looper.getMainLooper()).postDelayed({
            if (uniqueId == null) {
                SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("User ID Not Found")
                    .setContentText("Your account has been deactivated. Please contact us!")
                    .setConfirmText("OK")
                    .setConfirmClickListener { dialog ->
                        dialog.dismissWithAnimation()
                        navigateToLogin() // Navigate to login screen
                    }
                    .show()
            } else {
                Log.e("Current Unique ID", "Unique ID: $uniqueId")
                fetchUserData(uniqueId) // Load user data
            }
        }, 1000) // Delay by 1 second to allow UI to initialize

        // Book Ambulance Button
        val bookAmbulanceButton = findViewById<Button>(R.id.book_ambulance_button)
        val buttonClickAnimation = AnimationUtils.loadAnimation(this, R.anim.click_animation)
        bookAmbulanceButton.setOnClickListener { view ->
            view.startAnimation(buttonClickAnimation)
            if (checkBookingCooldown()) showBottomSheet()
        }

        val label = intent.getStringExtra("LABEL")
        val address = intent.getStringExtra("ADDRESS")

        if (label != null && address != null) {
            Toast.makeText(this, "Selected Location: $label, $address", Toast.LENGTH_SHORT).show()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("NotificationDebug", "POST_NOTIFICATIONS permission not granted");
                return
            }
        }

        // Search Bar Setup
        setupSearchBar()

        val currentLocationButton: AppCompatImageButton = findViewById(R.id.get_current_location)
        val locationButtonAnimation = AnimationUtils.loadAnimation(this, R.anim.location_button_animation)
        currentLocationButton.setOnClickListener { view ->
            view.startAnimation(locationButtonAnimation)
            // Check if location permission is granted
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
                Toast.makeText(this, "Please enable location permission", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if internet is available
            if (!isInternetAvailable()) {
                Toast.makeText(
                    this,
                    "No internet connection. Please enable it and try again.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Show a loader
            val loaderDialog = showLoaderDialog("Fetching your current location...")

            // Fetch location
            fetchCurrentLocation { lat, lng, address, city, pincode ->
                // Always dismiss the loader when the function finishes
                loaderDialog.dismiss()

                if (lat != 0.0 && lng != 0.0 && address.isNotEmpty()) {
                    // Update map and marker
                    val currentLatLng = LatLng(lat, lng)
                    updatedLatitude = lat
                    updatedLongitude = lng
                    updatedAddress = address
                    updatedCity = city
                    updatedPincode = pincode

                    googleMap.clear()
                    googleMap.addMarker(
                        MarkerOptions().position(currentLatLng).title("Current Location")
                            .draggable(true)
                    )
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

                    // Update UI fields
                    updateBottomSheetFields(lat, lng, address, city, pincode)
                    findViewById<AutoCompleteTextView>(R.id.search_bar).setText(address)
                } else {
                    Toast.makeText(
                        this,
                        "Failed to fetch location. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        val searchBar = findViewById<AutoCompleteTextView>(R.id.search_bar)

        if (searchBar == null) {
            Log.e("Error", "searchBar view is null")
        }

    }


    // Function to Redirect User to Login Activity
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear back stack
        startActivity(intent)
        finish() // Close current activity
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }

    private fun showLoaderDialog(message: String): AlertDialog {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.custom_progress_dialog, null)
        val progressText = dialogView.findViewById<TextView>(R.id.progress_message)
        progressText.text = message
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.show()
        return dialog
    }

    private fun onHeartDrawableClick() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_heart_add_location, null)
        //Save Location Ui Component

        val locationAddressCurrent = dialogView.findViewById<TextView>(R.id.location_address_current)
        val latitudeTextView = dialogView.findViewById<TextView>(R.id.latitudeTextView)
        val longitudeTextView = dialogView.findViewById<TextView>(R.id.longitudeTextView)
        val cityTextView = dialogView.findViewById<TextView>(R.id.cityTextView)
        val pinCodeTextView = dialogView.findViewById<TextView>(R.id.pinCodeTextView)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radio_group_labels)
        val saveButton = dialogView.findViewById<TextView>(R.id.save_button)
        val cancelButton = dialogView.findViewById<TextView>(R.id.cancel_button)

        // ✅ Check if Any View is Null
        if (locationAddressCurrent == null || latitudeTextView == null || longitudeTextView == null ||
            cityTextView == null || pinCodeTextView == null || saveButton == null || cancelButton == null
        ) {
            Log.e("DialogError", "One or more views are NULL! Check dialog_heart_add_location1.xml")
            return
        }

        // ✅ Pre-fill fields if data is available
        if (updatedLatitude != 0.0 && updatedLongitude != 0.0 && updatedAddress.isNotEmpty()) {
            locationAddressCurrent.text = updatedAddress
            latitudeTextView.text = updatedLatitude.toString()
            longitudeTextView.text = updatedLongitude.toString()
            cityTextView.text = updatedCity
            pinCodeTextView.text = updatedPincode
        } else {
            fetchCurrentLocation { lat, lng, address, city, pincode ->
                locationAddressCurrent.text = address
                latitudeTextView.text = lat.toString()
                longitudeTextView.text = lng.toString()
                cityTextView.text = city
                pinCodeTextView.text = pincode
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        //✅ Use UI Buttons Instead of AlertDialog Default Buttons
        cancelButton.setOnClickListener { dialog.dismiss() }

        saveButton.setOnClickListener {
            val selectedAddressField = when (radioGroup.checkedRadioButtonId) {
                R.id.address_1 -> "address1"
                R.id.address_2 -> "address2"
                R.id.address_3 -> "address3"
                else -> null
            }

            if (selectedAddressField != null) {
                val address = locationAddressCurrent.text.toString().trim()
                val latitude = latitudeTextView.text.toString().toDoubleOrNull() ?: 0.0
                val longitude = longitudeTextView.text.toString().toDoubleOrNull() ?: 0.0
                val city = cityTextView.text.toString().trim()
                val pinCode = pinCodeTextView.text.toString().trim()

                if (address.isNotEmpty()) {
//                    saveLocation(selectedAddressField, address, latitude, longitude, city, pinCode)
                    updateProfileAddress(
                        selectedAddressField,
                        address,
                        latitude,
                        longitude,
                        city,
                        pinCode
                    )
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please select a location type", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun updateProfileAddress(
        addressField: String,
        address: String,
        latitude: Double,
        longitude: Double,
        city: String,
        pinCode: String
    ) {
        val userId = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("user_id", null)
        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "User ID not found. Please login again.", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val progressDialog = createProgressDialog("Updating address...")
        progressDialog.show()

        val updateRequest = mutableMapOf<String, String>().apply {
            put(addressField, address)  // Updates address1, address2, or address3 dynamically
            put(
                when (addressField) {
                    "address1" -> "latitude1"
                    "address2" -> "latitude2"
                    "address3" -> "latitude3"
                    else -> "latitude"
                }, latitude.toString()
            )
            put(
                when (addressField) {
                    "address1" -> "longitude1"
                    "address2" -> "longitude2"
                    "address3" -> "longitude3"
                    else -> "longitude"
                }, longitude.toString()
            )
            put("city", city)
            put("pinCode", pinCode)
        }

        RetrofitClient.instance.updateProfile(userId, updateRequest)
            .enqueue(object : Callback<ProfileResponse> {
                override fun onResponse(
                    call: Call<ProfileResponse>,
                    response: Response<ProfileResponse>
                ) {
                    progressDialog.dismiss()
                    if (response.isSuccessful) {
                        saveLocationNotification()
                        SweetAlertDialog(this@MainActivity, SweetAlertDialog.SUCCESS_TYPE)
                            .setTitleText("Success!")
                            .setContentText("Location saved successfully.")
                            .setConfirmText("OK")
                            .show()
                    } else {
                        val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                        Toast.makeText(this@MainActivity, "Error: $errorMessage", Toast.LENGTH_LONG)
                            .show()
                        Log.e("API Error", errorMessage)
                    }
                }

                override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                    progressDialog.dismiss()
                    SweetAlertDialog(this@MainActivity, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("No Internet Connection")
                        .setContentText("Network error. Try again!")
                        .setConfirmText("Retry")
                        .show()
                }
            })
    }

    private fun createProgressDialog(message: String): ProgressDialog {
        return ProgressDialog(this).apply {
            setMessage(message)
            setCancelable(false)
        }
    }

    private fun fetchUserData(uniqueId: String) {
        Log.d("FetchUserData", "Fetching user data for ID: $uniqueId")
        try {
            RetrofitClient.instance.getProfile(uniqueId)
                .enqueue(object : Callback<ProfileResponse> {
                    override fun onResponse(
                        call: Call<ProfileResponse>,
                        response: Response<ProfileResponse>
                    ) {
                        Log.d("FetchUserData", "API Response Code: ${response.code()}")

                        if (response.isSuccessful) {
                            val user = response.body()?.user
                            if (user != null) {
                                Log.d(
                                    "FetchUserData",
                                    "User profile fetched successfully: ${user.full_name}"
                                )
                            } else {
                                Log.e("FetchUserData", "User data is null")
                                Toast.makeText(
                                    this@MainActivity,
                                    "No user data found.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            val errorBody = response.errorBody()?.string()
                            Log.e("FetchUserData", "API Error: $errorBody")

                            SweetAlertDialog(this@MainActivity, SweetAlertDialog.WARNING_TYPE)
                                .setTitleText("User ID Not Found")
                                .setContentText("Your account has been deactivated. Please contact us!")
                                .setConfirmText("OK")
                                .setConfirmClickListener { dialog ->
                                    dialog.dismissWithAnimation()
                                    navigateToLogin() // Navigate to login screen
                                }
                                .show()
                        }
                    }

                    override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                        Log.e("FetchUserData", "Network failure: ${t.message}")
                        Toast.makeText(
                            this@MainActivity,
                            "No Internet Connection",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("FetchUserData", "Exception: ${e.message}")
            Toast.makeText(
                this@MainActivity,
                "Error fetching user data: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun setupSearchBar() {
        val searchBar = findViewById<AutoCompleteTextView>(R.id.search_bar)
        val searchBarAnimation = AnimationUtils.loadAnimation(this, R.anim.location_button_animation)
        
        searchBar.setOnClickListener { view ->
            view.startAnimation(searchBarAnimation)
            // Check if location services are enabled before opening the search bar
            if (!isLocationEnabled()) {
                // If location services are not enabled, show the dialog
                showEnableLocationDialog()
            } else {
                try {
                    val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
                    val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                        .setCountries(listOf("IN")) // Restrict to India for better accuracy
                        .build(this)

                    startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Google Search Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Show Bottom Sheet
    private fun showBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_layout, null)

        // Fields
        currentLocationField = bottomSheetView.findViewById(R.id.current_location)
        if (currentLocationField == null) {
            Log.e("Error", "Current location field not found in bottom sheet layout")
            return
        }
        latitudeField = bottomSheetView.findViewById(R.id.latitude)
        longitudeField = bottomSheetView.findViewById(R.id.longitude)
        val cityField = bottomSheetView.findViewById<EditText>(R.id.city_field)
        val pinCodeField = bottomSheetView.findViewById<EditText>(R.id.pin_code_field)
        val mobileNumberField = bottomSheetView.findViewById<EditText>(R.id.mobileNumberEditText)

        // Make all fields except mobile number non-editable
        currentLocationField.isFocusable = false
        currentLocationField.isFocusableInTouchMode = false
        currentLocationField.isCursorVisible = false

        latitudeField.isFocusable = false
        latitudeField.isFocusableInTouchMode = false
        latitudeField.isCursorVisible = false

        longitudeField.isFocusable = false
        longitudeField.isFocusableInTouchMode = false
        longitudeField.isCursorVisible = false

        cityField.isFocusable = false
        cityField.isFocusableInTouchMode = false
        cityField.isCursorVisible = false

        pinCodeField.isFocusable = false
        pinCodeField.isFocusableInTouchMode = false
        pinCodeField.isCursorVisible = false

        // Fetch Mobile Number
//          val headerView = navView.getHeaderView(0)
//          Access navigation header
        val headerView = navView.getHeaderView(0) ?: run {
            Log.e("Error", "Navigation header is null")
            return
        }

        val userPhoneTextView =
            headerView.findViewById<TextView>(R.id.user_phone) // Get user phone TextView
        val fetchedMobileNumber = userPhoneTextView.text.toString().trim() // Get text from TextView
        mobileNumberField.setText(fetchedMobileNumber) // Set it to the mobile number field
        // Fetch Initial Location
        if (updatedLatitude == 0.0 || updatedLongitude == 0.0 || updatedAddress.isEmpty()) {
            fetchCurrentLocation { lat, lng, address, city, pincode ->
                updateLocationFields(lat, lng, address, city, pincode, cityField, pinCodeField)
            }
        } else {
            updateLocationFields(
                updatedLatitude,
                updatedLongitude,
                updatedAddress,
                updatedCity,
                updatedPincode,
                cityField,
                pinCodeField
            )
        }

        // Confirm Booking
        bottomSheetView.findViewById<Button>(R.id.confirm_button).setOnClickListener {
            val phone = mobileNumberField.text.toString().trim()
            if (isValidPhoneNumber(phone)) {
                bookAmbulance(
                    phone,
                    updatedLatitude,
                    updatedLongitude,
                    updatedAddress,
                    updatedCity,
                    updatedPincode,
                    bottomSheetDialog
                )
            } else {
                showErrorDialog(
                    "Invalid Mobile Number",
                    "Please enter a valid 10-digit mobile number starting with 6-9."
                )
            }
        }
        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()
    }

    // Update Location Fields in Bottom Sheet
    private fun updateLocationFields(
        lat: Double,
        lng: Double,
        address: String,
        city: String,
        pincode: String,
        cityField: EditText,
        pinCodeField: EditText
    ) {
        currentLocationField.setText(address)
        latitudeField.setText(lat.toString())
        longitudeField.setText(lng.toString())
        cityField.setText(city)
        pinCodeField.setText(pincode)
    }

    // Validate Phone Number
    private fun isValidPhoneNumber(phone: String): Boolean {
        return phone.matches(Regex("\\d{10}$"))
    }

    // Show Error Dialog
    private fun showErrorDialog(title: String, message: String) {
        SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
            .setTitleText(title)
            .setContentText(message)
            .setConfirmText("OK")
            .show()
    }

    private fun updateMapLocation(latLng: LatLng, title: String) {
        googleMap.clear() // Clear existing markers
        val marker = googleMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(title)
                .draggable(true) // Make marker draggable
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)) // Keep marker red
        )
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))

        // Update location details (for booking)
        updateLocationDetails(latLng.latitude, latLng.longitude)

        // Add drag listener to marker
        googleMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragStart(marker: Marker) {
                // Optional: Perform actions on drag start
            }

            override fun onMarkerDrag(marker: Marker) {
                // Optional: Perform actions during dragging
            }

            override fun onMarkerDragEnd(marker: Marker) {
                val newPosition = marker.position
                updateLocationDetails(newPosition.latitude, newPosition.longitude)
            }
        })
    }

    private fun updateLocationDetails(latitude: Double, longitude: Double) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addressList = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addressList.isNullOrEmpty()) {
                val address = addressList[0].getAddressLine(0) ?: "Address not found"
                val city = addressList[0].locality ?: "City not found"
                val pincode = addressList[0].postalCode ?: "Pincode not found"
                updatedLatitude = latitude
                updatedLongitude = longitude
                updatedAddress = address
                updatedCity = city
                updatedPincode = pincode

                // Update search bar and UI fields
                findViewById<AutoCompleteTextView>(R.id.search_bar)?.setText(address)
                updateBottomSheetFields(latitude, longitude, address, city, pincode)
            } else {
                Toast.makeText(this, "Unable to fetch address", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Unable to fetch address", Toast.LENGTH_SHORT).show()
            Log.e("Error fetching address: ${e.message}", "Error")
        }
    }

    private fun initializeMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Check and request location permissions
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true

            // Fetch the current location and place the initial marker
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        LOCATION_PERMISSION_REQUEST_CODE
                    )
                }

                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    updateMapLocation(currentLatLng, "Current Location")
                } else {
                    Toast.makeText(this, "Unable to fetch current location", Toast.LENGTH_SHORT)
                        .show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to fetch location", Toast.LENGTH_SHORT).show()
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun updateBottomSheetFields(
        latitude: Double,
        longitude: Double,
        address: String,
        city: String,
        pinCode: String
    ) {
        if (::currentLocationField.isInitialized) {
            currentLocationField.setText(address)
            latitudeField.setText(latitude.toString())
            longitudeField.setText(longitude.toString())
            findViewById<EditText>(R.id.city_field)?.setText(city)
            findViewById<EditText>(R.id.pin_code_field)?.setText(pinCode)
        } else {
            Toast.makeText(this, "Getting your location", Toast.LENGTH_SHORT).show()
        }
    }

    private val profileUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Show loader while updating
            updateNavigationHeader()

            // Simulate a delay or wait for the update process to complete
            android.os.Handler().postDelayed({
                updateNavigationHeader() // Hide loader after update
            }, 2000) // Adjust delay as needed
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            when (resultCode) {
                RESULT_OK -> {
                    val place = Autocomplete.getPlaceFromIntent(data!!)
                    if (data == null) {
                        Log.e("Error", "Autocomplete data is null")
                        return
                    }
                    val latLng = place.latLng
                    if (latLng != null) {
                        updateMapLocation(latLng, place.name ?: "Selected Location")
                    }
                }

                AutocompleteActivity.RESULT_ERROR -> {
                    val status = Autocomplete.getStatusFromIntent(data!!)
                    Toast.makeText(this, "Internet Connection not Proper", Toast.LENGTH_SHORT)
                        .show()
                    Log.e("Error: ${status.statusMessage}", "Error")
                }

                RESULT_CANCELED -> {
                    // User canceled the operation
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(profileUpdateReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e("Error", "Receiver not registered: ${e.message}")
        }
    }

    // Book Ambulance
    private fun bookAmbulance(
        phone: String,
        latitude: Double,
        longitude: Double,
        address: String,
        city: String,
        pinCode: String,
        dialog: BottomSheetDialog
    ) {
        try {
            val apiLoadingDialog = showGifProgressDialog()
            // **Delay booking request by 5 seconds**
            Handler(Looper.getMainLooper()).postDelayed({
                val bookingDetails = BookingDetails(
                    phone = phone,
                    latitude = latitude,
                    longitude = longitude,
                    address = address,
                    city = city,
                    pin_code = pinCode
                )

                val call = RetrofitClient.instance.bookAmbulance(bookingDetails)
                call.enqueue(object : retrofit2.Callback<BookingResponse> {
                    override fun onResponse(
                        call: Call<BookingResponse>,
                        response: Response<BookingResponse>
                    ) {
                        apiLoadingDialog.dismiss()
                        try {
                            if (response.isSuccessful && response.body()?.status == "success") {
                                showBookingNotification()
                                SweetAlertDialog(this@MainActivity, SweetAlertDialog.SUCCESS_TYPE)
                                    .setTitleText("Success!")
                                    .setContentText("Your ride is successfully booked!")
                                    .setConfirmText("OK")
                                    .setConfirmClickListener { sweetAlertDialog ->
                                        sweetAlertDialog.dismissWithAnimation()
                                        dialog.dismiss()
                                    }
                                    .show()
                            } else {
                                SweetAlertDialog(this@MainActivity, SweetAlertDialog.ERROR_TYPE)
                                    .setTitleText("Failed")
                                    .setContentText(
                                        response.body()?.message ?: "Something went wrong!"
                                    )
                                    .setConfirmText("OK")
                                    .show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            apiLoadingDialog.dismiss()
                            SweetAlertDialog(this@MainActivity, SweetAlertDialog.ERROR_TYPE)
                                .setTitleText("Error")
                                .setContentText("An unexpected error occurred. Please try again later.")
                                .setConfirmText("OK")
                                .show()
                        }
                    }

                    override fun onFailure(call: Call<BookingResponse>, t: Throwable) {
                        apiLoadingDialog.dismiss()
                        SweetAlertDialog(this@MainActivity, SweetAlertDialog.WARNING_TYPE)
                            .setTitleText("Error")
                            .setContentText("Unable to fetch address")
                            .setConfirmText("OK")
                            .show()
                    }
                })
            }, 3000) // **Waits 5 seconds before making the API call**

        } catch (e: Exception) {
            e.printStackTrace()
            SweetAlertDialog(this@MainActivity, SweetAlertDialog.ERROR_TYPE)
                .setTitleText("Error")
                .setContentText("An error occurred while processing your booking.")
                .setConfirmText("OK")
                .show()
        }
    }

    private fun showBookingNotification() {
        // Intent to open the app when the notification is clicked
        val intent = Intent(this, MyRidesActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_IMMUTABLE)
        // Set the notification sound URI (default notification sound)
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        // Build the notification
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.raftaar_seva_logo) // Replace with your app logo
            .setContentTitle("🚑 Ambulance Booked Successfully!")
            .setContentText("Your ambulance is on its way to your location.")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority
            .setSound(soundUri) // Add the sound
            .setAutoCancel(true) // Dismiss notification when clicked
            .setContentIntent(pendingIntent) // Intent to open the app

        // Show the notification
        with(NotificationManagerCompat.from(this)) {
            notify(1, builder.build()) // Use a unique ID for this notification
        }
    }

    private fun saveLocationNotification() {
        // Intent to open the app when the notification is clicked
        val intent = Intent(this, SaveLocationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_IMMUTABLE)
        // Set the notification sound URI (default notification sound)
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        // Build the notification
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.raftaar_seva_logo) // Replace with your app logo
            .setContentTitle("📍Location Saved Successfully!")
            .setContentText("Your location has been saved successfully.")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority
            .setSound(soundUri) // Add the sound
            .setAutoCancel(true) // Dismiss notification when clicked
            .setContentIntent(pendingIntent) // Intent to open the app

        // Show the notification
        with(NotificationManagerCompat.from(this)) {
            notify(1, builder.build()) // Use a unique ID for this notification
        }
    }

    private fun checkBookingCooldown(): Boolean {
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val lastBookingTime = sharedPreferences.getLong("lastBookingTime", 0L)
        val currentTime = System.currentTimeMillis()

        // Calculate the time difference in milliseconds
        val timeDifference = currentTime - lastBookingTime

        return if (timeDifference < 5000) {
            // Calculate remaining time in seconds
            val remainingTime = (5000 - timeDifference) / 1000
            Toast.makeText(this, "Please book after $remainingTime seconds", Toast.LENGTH_SHORT)
                .show()
            false
        } else {
            true
        }
    }

    private fun fetchCurrentLocation(onLocationFetched: (Double, Double, String, String, String) -> Unit) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("LocationError", "Location permission not granted!")
            onLocationFetched(0.0, 0.0, "N/A", "N/A", "N/A") // Handle permission denial
            return
        }

        if (!isLocationEnabled()) {
            Log.e("LocationError", "Location services are disabled!")
            showEnableLocationDialog()
            onLocationFetched(0.0, 0.0, "N/A", "N/A", "N/A") // Handle disabled location services
            return
        }

        // Show SweetAlertDialog as progress
        val progressDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
        progressDialog.titleText = "Fetching Location..."
        progressDialog.setCancelable(false)
        progressDialog.show()

        val handler = Handler(Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            progressDialog.dismiss() // Dismiss the dialog after timeout
            Log.e("LocationError", "Timeout: Location fetch took too long!")
            Toast.makeText(
                this,
                "Internet connection is not stable. Please check your connection and try again.",
                Toast.LENGTH_SHORT
            ).show()
            onLocationFetched(0.0, 0.0, "N/A", "N/A", "N/A") // Pass failed location
        }

        handler.postDelayed(timeoutRunnable, 12000) // 12 seconds timeout

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                Log.d(
                    "LocationSuccess",
                    "Latitude: ${location.latitude}, Longitude: ${location.longitude}"
                )

                val latitude = location.latitude
                val longitude = location.longitude

                try {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addressList = geocoder.getFromLocation(latitude, longitude, 1)

                    if (!addressList.isNullOrEmpty()) {
                        val address = addressList[0].getAddressLine(0) ?: "Address not found"
                        val city = addressList[0].locality ?: "City not found"
                        val pincode = addressList[0].postalCode ?: "Pincode not found"

                        // Update map with marker first before dismissing loader
                        val currentLatLng = LatLng(latitude, longitude)
                        googleMap.clear()
                        googleMap.addMarker(
                            MarkerOptions().position(currentLatLng).title("Current Location")
                                .draggable(true)
                        )
                        googleMap.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                currentLatLng,
                                15f
                            )
                        )

                        // Stop timeout and dismiss loader only after the marker is updated
                        handler.removeCallbacks(timeoutRunnable)
                        // Dismiss SweetAlertDialog when marker is updated
                        progressDialog.dismiss()
                        onLocationFetched(latitude, longitude, address, city, pincode)
                    } else {
                        Log.e("LocationError", "Geocoder returned empty list!")
                        progressDialog.dismiss()
                        onLocationFetched(
                            latitude,
                            longitude,
                            "Unknown Address",
                            "Unknown City",
                            "Unknown Pincode"
                        )
                    }
                } catch (e: Exception) {
                    Log.e("GeocoderError", "Geocoder failed: ${e.message}")
                    progressDialog.dismiss()
                    onLocationFetched(
                        latitude,
                        longitude,
                        "Unknown Address",
                        "Unknown City",
                        "Unknown Pincode"
                    )
                }
            } else {
                Log.e("LocationError", "FusedLocationClient returned NULL!")
                progressDialog.dismiss()
                onLocationFetched(0.0, 0.0, "N/A", "N/A", "N/A")
            }
        }.addOnFailureListener { exception ->
            handler.removeCallbacks(timeoutRunnable)
            Log.e("LocationError", "Failed to fetch location: ${exception.message}")
            progressDialog.dismiss()
            Toast.makeText(
                this,
                "Failed to fetch location. Please try again later.",
                Toast.LENGTH_SHORT
            ).show()
            onLocationFetched(0.0, 0.0, "N/A", "N/A", "N/A")
        }
    }

    private fun showEnableLocationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable Location Services")
            .setMessage("Location services are required to fetch your current location. Please enable them.")
            .setPositiveButton("Settings") { _, _ ->
                val intent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager =
            getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    }

    private fun showGifProgressDialog(): AlertDialog {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.custom_progress_gif, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.loading_lottie)

        // Define the URL to load
//      val gifUrl = "https://emergencyseva.in/emergencyseva.in/images/KAdwRRUAsQ-ezgif.com-crop.gif"
        val gifUrl = "https://i.giphy.com/media/v1.Y2lkPTc5MGI3NjExdWJmNzJ0MmNpMGw0djhzZXZidG9mcXJ3cW5mdmRtdzd6ajhxbjBqaCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/wSnOQErP2oRnk4RB36/giphy.gif"
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // Prevent dismissal by user
            .create()

        dialog.show()

        // Load the GIF using Glide and get a GifDrawable to control playback
        Glide.with(this)
            .asGif()
            .load(gifUrl)
            .into(imageView)
            .clearOnDetach()

        // **Ensure the dialog remains open for 5 seconds**
        imageView.post {
            Handler(Looper.getMainLooper()).postDelayed({
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            }, 5000) // 5 seconds delay
        }
        return dialog
    }

    // Helper function to enable/disable Confirm button
    private fun initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        progressDialog = ProgressDialog(this).apply {
            setMessage("Fetching current location...")
            setCancelable(false)
        }
        findViewById<ImageView>(R.id.menu_button).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        val headerView = navView.getHeaderView(0) // Fetch header view
        val userName = headerView.findViewById<TextView>(R.id.user_name)
        val userPhone = headerView.findViewById<TextView>(R.id.user_phone)
        userName.text = "Fetching..."
        userPhone.text = "Fetching.."
    }

    private fun setupNavigationDrawer() {
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_profile -> navigateTo(ProfileActivity::class.java)
                R.id.nav_my_rides -> navigateTo(MyRidesActivity::class.java)
                R.id.nav_save_locations -> navigateTo(SaveLocationActivity::class.java)
                R.id.termAndCondition -> navigateTo(TermConditionActivity::class.java)
                R.id.nav_contact_us -> navigateTo(ContactUsActivity::class.java)
                R.id.nav_privacy_policy -> navigateTo(PrivacyPolicyActivity::class.java)
                R.id.share -> shareAppLink()
                R.id.nav_logout -> logoutUser()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        twitter = findViewById(R.id.twitter_link)
        youtube = findViewById(R.id.youtube_link)
        instagram = findViewById(R.id.instagram_link)
        facebook = findViewById(R.id.facebook_link)
        facebook.setOnClickListener {
            val urlFb = "https://www.facebook.com/profile.php?id=61562053965215"
            val uri = Uri.parse(urlFb)
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.facebook.katana") // Package name for Facebook
            }
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlFb))
                intent.setPackage("com.facebook.katana") // Package name for Facebook
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlFb))) // Open in browser
            }
        }
        twitter.setOnClickListener {
            val urlTwitter = "https://x.com/emergencyseva"
            val uri = Uri.parse(urlTwitter)
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.twitter.android") // Package name for Twitter
            }
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlTwitter)))
            }
        }
        youtube.setOnClickListener {
            val urlYoutube = "https://www.youtube.com/@RaftaarHelpEmergencySeva"
            val uri = Uri.parse(urlYoutube)
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.youtube") // Package name for YouTube
            }
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlYoutube)))
            }
        }
        instagram.setOnClickListener {
            val urlInsta = "https://www.instagram.com/emergency.seva/"
            val uri = Uri.parse(urlInsta)
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.instagram.android") // Package name for Instagram
            }
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlInsta)))
            }
        }
    }

    private fun shareAppLink() {
        // App link or message to share
        val shareMessage = """
               Check out this amazing app! Download it now from the Play Store:
               https://play.google.com/store/apps/details?id=${applicationContext.packageName}
           """.trimIndent()

        // Create share intent
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
        // Start share activity
        startActivity(Intent.createChooser(shareIntent, "Share App via"))
    }

    private fun logoutUser() {
        val sweetAlertDialog = SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
            .setTitleText("Are you sure?")
            .setContentText("You will be logged out of your account")
            .setConfirmText("Logout")
            .setCancelText("Cancel")
            .setConfirmClickListener { sweetAlertDialog ->
                // Clear all shared preferences
                clearAllSharedPreferences()
                // Clear cache
                clearCache()
                // Clear navigation header
                clearNavigationHeader()
                // **Step 3: Reset Navigation Header**
                resetNavigationHeader()
                // Navigate to LoginActivity
                val intent = Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)

                Toast.makeText(
                    this,
                    "Logged out successfully and all data cleared.",
                    Toast.LENGTH_SHORT
                ).show()
                sweetAlertDialog.dismissWithAnimation()
            }
            .setCancelClickListener { sweetAlertDialog ->
                sweetAlertDialog.dismissWithAnimation() // Dismiss dialog without logging out
            }

        // Accessing the buttons and customizing their background color
        val confirmButton = sweetAlertDialog.findViewById<Button>(R.id.confirm_button)
        val cancelButton = sweetAlertDialog.findViewById<Button>(R.id.cancel_button)

        // Set the custom background colors
        confirmButton?.setBackgroundColor(Color.GREEN)  // Green color for the "Logout" button
        cancelButton?.setBackgroundColor(Color.RED)    // Red color for the "Cancel" button

        // Show the dialog
        sweetAlertDialog.show()
    }

    private fun resetNavigationHeader() {
        val headerView = navView.getHeaderView(0)
        val profileInitials = headerView.findViewById<TextView>(R.id.profile_initials)
        val userName = headerView.findViewById<TextView>(R.id.user_name)
        val userPhone = headerView.findViewById<TextView>(R.id.user_phone)

        profileInitials.text = "?"
        userName.text = "Guest User"
        userPhone.text = "Not Logged In"

        Log.d("Logout", "Navigation header reset successfully.")
    }

    private fun clearAllSharedPreferences() {
        try {
            val preferences = listOf(
                "AppPrefs",       // Main preferences
                "ProfilePrefs",   // Profile data
                "SavedLocations", // Saved locations
                "RideDetails"     // Ride details
            )

            for (pref in preferences) {
                getSharedPreferences(pref, Context.MODE_PRIVATE).edit().clear().apply()
            }

            Log.d("Logout", "All shared preferences cleared successfully.")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Not Clear Successfully", Toast.LENGTH_SHORT).show()
            Log.d("Error clearing preferences: ${e.message}", "Cache not Clear")
        }
    }

    private fun clearCache() {
        try {
            val cacheDir = cacheDir
            if (cacheDir.isDirectory) {
                cacheDir.listFiles()?.forEach { file ->
                    file.deleteRecursively() // Deletes files and subdirectories
                }
            }
            Log.d("Logout", "Cache cleared successfully.")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Cache cleared not successfully", Toast.LENGTH_SHORT).show()
            Log.d("Error clearing cache: ${e.message}", "Error in clear Cache")
        }
    }

    private fun clearNavigationHeader() {
        val headerView = navView.getHeaderView(0)
        val profileInitials = headerView.findViewById<TextView>(R.id.profile_initials)
        val userName = headerView.findViewById<TextView>(R.id.user_name)
        val userPhone = headerView.findViewById<TextView>(R.id.user_phone)

        // Reset header values to default
        profileInitials.text = "?"
        userName.text = "Default Name"
        userPhone.text = "Default Phone"

        Log.d("Logout", "Navigation header reset to default.")
    }

    private fun navigateTo(activity: Class<*>) {
        startActivity(Intent(this, activity))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    googleMap.isMyLocationEnabled = true
                    // Restart fetching the current location
                    fetchCurrentLocation { lat, lng, address, city, pincode ->
                        // Dismiss loader if it's still showing
                        progressDialog.dismiss()

                        // Update the UI with fetched location
                        val currentLatLng = LatLng(lat, lng)
                        googleMap.clear()
                        googleMap.addMarker(
                            MarkerOptions().position(currentLatLng).title("Current Location")
                                .draggable(true)
                        )
                        googleMap.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                currentLatLng,
                                15f
                            )
                        )
                        updateBottomSheetFields(lat, lng, address, city, pincode)
                    }
                }
            } else {
                // Permission denied
                progressDialog.dismiss()
                Toast.makeText(this, "Permission denied! Location is required.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            profileUpdateReceiver,
            IntentFilter("PROFILE_UPDATED")
        )
        updateNavigationHeader(showLoader = true) // Force header to reload
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(profileUpdateReceiver)
    }

    // Update Navigation Header
    private fun updateNavigationHeader(showLoader: Boolean = false) {
        try {
            val headerView = navView.getHeaderView(0)
            val profileInitials = headerView.findViewById<TextView>(R.id.profile_initials)
            val userName = headerView.findViewById<TextView>(R.id.user_name)
            val userPhone = headerView.findViewById<TextView>(R.id.user_phone)
            val navHeaderLoader = headerView.findViewById<ProgressBar>(R.id.nav_header_loader)

            val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
            val cachedName = sharedPreferences.getString("cached_name", null)
            val cachedPhone = sharedPreferences.getString("cached_phone", null)

            if (cachedName != null && cachedPhone != null) {
                userName.text = cachedName
                userPhone.text = cachedPhone
                profileInitials.text = getInitials(cachedName)
                navHeaderLoader.visibility = View.GONE
                return
            }

            if (showLoader) {
                navHeaderLoader.visibility = View.VISIBLE
            }

            val uniqueId = sharedPreferences.getString("unique_id", null)
            if (uniqueId != null) {
                RetrofitClient.instance.getProfile(uniqueId)
                    .enqueue(object : Callback<ProfileResponse> {
                        override fun onResponse(
                            call: Call<ProfileResponse>,
                            response: Response<ProfileResponse>
                        ) {
                            try {
                                navHeaderLoader.visibility = View.GONE
                                if (response.isSuccessful) {
                                    val user = response.body()?.user
                                    if (user != null) {
                                        with(sharedPreferences.edit()) {
                                            putString("cached_name", user.full_name)
                                            putString("cached_phone", user.phone1)
                                            apply()
                                        }

                                        userName.text = user.full_name
                                        userPhone.text = user.phone1
                                        profileInitials.text = getInitials(user.full_name)
                                    }
                                } else {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Failed to fetch profile.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: Exception) {
                                // Handle any issues in processing the response
                                Log.e(
                                    "Profile Error",
                                    "Error in processing profile response: ${e.message}"
                                )
                                Toast.makeText(
                                    this@MainActivity,
                                    "Error processing profile response: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                            navHeaderLoader.visibility = View.GONE
                            // Handling network or other failures
                            Toast.makeText(
                                this@MainActivity,
                                "Network error. Please try again.",
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.e("Profile Error", "Network error: ${t.message}")
                        }
                    })
            } else {
                userName.text = "Default Name"
                userPhone.text = "Default Phone"
                profileInitials.text = "?"
                navHeaderLoader.visibility = View.GONE
            }
        } catch (e: Exception) {
            // Handling any unexpected errors in updating the header
            e.printStackTrace()
            Toast.makeText(
                this@MainActivity,
                "Error updating navigation header: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun getInitials(name: String): String {
        return if (name.isNotBlank()) {
            name.split(" ").mapNotNull { it.firstOrNull()?.toString()?.uppercase() }
                .joinToString("").take(2)
        } else {
            "?"
        }
    }
}


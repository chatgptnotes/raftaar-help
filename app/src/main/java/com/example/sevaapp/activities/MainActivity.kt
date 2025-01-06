package com.example.sevaapp.activities

import android.Manifest
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.ArrayAdapter
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import cn.pedant.SweetAlert.SweetAlertDialog
import com.bumptech.glide.Glide
import com.example.sevaapp.R
import com.example.sevaapp.apiinterface.RetrofitClient
import com.example.sevaapp.models.BookingDetails
import com.example.sevaapp.models.BookingResponse
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
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.navigation.NavigationView
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    // UI Components
    private lateinit var currentLocationField: EditText
    private lateinit var latitudeField: EditText
    private lateinit var longitudeField: EditText
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var progressDialog: ProgressDialog
    private lateinit var drawerLayout: DrawerLayout
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize Map and Services
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyCFpPr4-VEcfSMMT5CeH8DlQbjErz6yORk")
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize UI
        initializeViews()
        setupNavigationDrawer()
        initializeMap()

        // Book Ambulance Button
        findViewById<Button>(R.id.book_ambulance_button).setOnClickListener {
            if (checkBookingCooldown()) showBottomSheet()
        }

        val label = intent.getStringExtra("LABEL")
        val address = intent.getStringExtra("ADDRESS")

        if (label != null && address != null) {
            Toast.makeText(this, "Selected Location: $label, $address", Toast.LENGTH_SHORT).show()
        }

        // Search Bar Setup
        setupSearchBar()

        val currentLocationButton: de.hdodenhof.circleimageview.CircleImageView = findViewById(R.id.get_current_location)
        currentLocationButton.setOnClickListener {

            // Check if location permission is granted
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                Toast.makeText(this, "No internet connection. Please enable it and try again.", Toast.LENGTH_SHORT).show()
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
                        MarkerOptions().position(currentLatLng).title("Current Location").draggable(true)
                    )
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

                    // Update UI fields
                    updateBottomSheetFields(lat, lng, address, city, pincode)
                    findViewById<AutoCompleteTextView>(R.id.search_bar).setText(address)
                } else {
                    Toast.makeText(this, "Failed to fetch location. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val searchBar = findViewById<AutoCompleteTextView>(R.id.search_bar)
        searchBar.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                // Check if the touch is within the bounds of the drawableRight
                if (event.rawX >= (searchBar.right - searchBar.compoundDrawables[2].bounds.width())) {
                    // Handle the click for the drawableRight
                    onHeartDrawableClick()
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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
        // Inflate the dialog layout
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_heart_add_location, null)
        // Initialize dialog components
        val locationAddressCurrent = dialogView.findViewById<TextView>(R.id.location_address_current)
        val latitudeTextView = dialogView.findViewById<TextView>(R.id.latitudeTextView)
        val longitudeTextView = dialogView.findViewById<TextView>(R.id.longitudeTextView)
        val cityTextView = dialogView.findViewById<TextView>(R.id.cityTextView)
        val pinCodeTextView = dialogView.findViewById<TextView>(R.id.pinCodeTextView)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radio_group_labels)

        // Populate the fields with current location data
        if (updatedLatitude != 0.0 && updatedLongitude != 0.0 && updatedAddress.isNotEmpty()) {
            locationAddressCurrent.text = updatedAddress
            latitudeTextView.text = updatedLatitude.toString()
            longitudeTextView.text = updatedLongitude.toString()
            cityTextView.text = updatedCity
            pinCodeTextView.text = updatedPincode
        } else {
            // Fetch the current location if not already updated
            fetchCurrentLocation { lat, lng, address, city, pincode ->
                locationAddressCurrent.text = address
                latitudeTextView.text = lat.toString()
                longitudeTextView.text = lng.toString()
                cityTextView.text = city
                pinCodeTextView.text = pincode
            }
        }

        // Show the dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Favorite Location")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                // Get the selected label
                val selectedLabel = when (radioGroup.checkedRadioButtonId) {
                    R.id.radio_office -> "Office"
                    R.id.radio_home -> "Home"
                    R.id.radio_other -> "Other"
                    else -> "Unknown"
                }

                // Collect all location details
                val label = selectedLabel
                val address = locationAddressCurrent.text.toString().trim()
                val latitude = latitudeTextView.text.toString().toDoubleOrNull() ?: 0.0
                val longitude = longitudeTextView.text.toString().toDoubleOrNull() ?: 0.0
                val city = cityTextView.text.toString().trim()
                val pinCode = pinCodeTextView.text.toString().trim()

                // Save the location
                if (label.isNotEmpty() && address.isNotEmpty()) {
                    saveLocation(label, address, latitude, longitude, city, pinCode)
                } else {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun saveLocation(
        label: String,
        address: String,
        latitude: Double,
        longitude: Double,
        city: String,
        pinCode: String
    ) {
        try {
            val sharedPreferences = getSharedPreferences("SavedLocations", Context.MODE_PRIVATE)

            // Check and reset the key if corrupted
            if (sharedPreferences.contains("locations")) {
                try {
                    sharedPreferences.getString("locations", "[]") // Try reading as a String
                } catch (e: ClassCastException) {
                    sharedPreferences.edit().remove("locations").apply() // Reset the key
                    Log.d("SaveLocationFix", "Resetting corrupted 'locations' key.")
                }
            }

            // Initialize the JSON array
            val locationsJson = sharedPreferences.getString("locations", "[]") ?: "[]"
            val locationsArray = try {
                JSONArray(locationsJson)
            } catch (e: Exception) {
                Log.e("SaveLocationError", "Failed to parse locations JSON: ${e.localizedMessage}", e)
                JSONArray() // Start with a new array if parsing fails
            }

            // Add the new location
            val newLocation = JSONObject().apply {
                put("label", label)
                put("address", address)
                put("latitude", latitude)
                put("longitude", longitude)
                put("city", city)
                put("pincode", pinCode)
            }

            locationsArray.put(newLocation)

            // Save back to SharedPreferences
            sharedPreferences.edit().putString("locations", locationsArray.toString()).apply()

            // Show SweetAlert success dialog
            SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText("Success!")
                .setContentText("Location saved successfully.")
                .setConfirmText("OK")
                .show()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("SaveLocationError", "Error saving location: ${e.localizedMessage}", e)
            // Show SweetAlert error dialog
            SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                .setTitleText("Error!")
                .setContentText("Failed to save location. Please try again.")
                .setConfirmText("OK")
                .show()
        }
    }

    // Initialize Search Bar with Autocomplete
    private fun setupSearchBar() {
        val searchBar = findViewById<AutoCompleteTextView>(R.id.search_bar)
        val autocompleteAdapter = ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line)
        searchBar.setAdapter(autocompleteAdapter)

        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()) fetchPlacePredictions(s.toString(), autocompleteAdapter)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        searchBar.setOnItemClickListener { _, _, position, _ ->
            val selectedPlace = autocompleteAdapter.getItem(position)
            if (!selectedPlace.isNullOrEmpty()) {
                val geocoder = Geocoder(this, Locale.getDefault())
                val addressList = geocoder.getFromLocationName(selectedPlace, 1)
                if (!addressList.isNullOrEmpty()) {
                    val location = LatLng(addressList[0].latitude, addressList[0].longitude)
                    updateMapLocation(location, selectedPlace)
                } else {
                    Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Fetch Place Predictions for Search Bar
    private fun fetchPlacePredictions(query: String, adapter: ArrayAdapter<String>) {
        val placesClient: PlacesClient = Places.createClient(this)
        val request = FindAutocompletePredictionsRequest.builder().setQuery(query).build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                adapter.clear()
                adapter.addAll(response.autocompletePredictions.map { it.getFullText(null).toString() })
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error fetching predictions: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Show Bottom Sheet
    private fun showBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_layout, null)

        // Fields
        currentLocationField = bottomSheetView.findViewById(R.id.current_location)
        latitudeField = bottomSheetView.findViewById(R.id.latitude)
        longitudeField = bottomSheetView.findViewById(R.id.longitude)
        val cityField = bottomSheetView.findViewById<EditText>(R.id.city_field)
        val pinCodeField = bottomSheetView.findViewById<EditText>(R.id.pin_code_field)
        val mobileNumberField = bottomSheetView.findViewById<EditText>(R.id.mobileNumberEditText)

        // Fetch Mobile Number
        val savedMobileNumber = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            .getString("userPhone", "")
        mobileNumberField.setText(savedMobileNumber)

        // Fetch Initial Location
        if (updatedLatitude == 0.0 || updatedLongitude == 0.0 || updatedAddress.isEmpty()) {
            fetchCurrentLocation { lat, lng, address, city, pincode ->
                updateLocationFields(lat, lng, address, city, pincode, cityField, pinCodeField)
            }
        } else {
            updateLocationFields(updatedLatitude, updatedLongitude, updatedAddress, updatedCity, updatedPincode, cityField, pinCodeField)
        }

        // Confirm Booking
        bottomSheetView.findViewById<Button>(R.id.confirm_button).setOnClickListener {
            val phone = mobileNumberField.text.toString().trim()
            if (isValidPhoneNumber(phone)) {
                bookAmbulance(phone, updatedLatitude, updatedLongitude, updatedAddress, updatedCity, updatedPincode, bottomSheetDialog)
            } else {
                showErrorDialog("Invalid Mobile Number", "Please enter a valid 10-digit mobile number starting with 6-9.")
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
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

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
            Toast.makeText(this, "Error fetching address: ${e.message}", Toast.LENGTH_SHORT).show()
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
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    updateMapLocation(currentLatLng, "Current Location")
                } else {
                    Toast.makeText(this, "Unable to fetch current location", Toast.LENGTH_SHORT).show()
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
            updateNavigationHeader(showLoader = true)

            // Simulate a delay or wait for the update process to complete
            android.os.Handler().postDelayed({
                updateNavigationHeader(showLoader = false) // Hide loader after update
            }, 2000) // Adjust delay as needed
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            when (resultCode) {
                RESULT_OK -> {
                    val place = Autocomplete.getPlaceFromIntent(data!!)
                    val latLng = place.latLng
                    if (latLng != null) {
                        updateMapLocation(latLng, place.name ?: "Selected Location")
                    }
                }
                AutocompleteActivity.RESULT_ERROR -> {
                    val status = Autocomplete.getStatusFromIntent(data!!)
                    Toast.makeText(this, "Error: ${status.statusMessage}", Toast.LENGTH_SHORT).show()
                }
                RESULT_CANCELED -> {
                    // User canceled the operation
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(profileUpdateReceiver)
    }

    private fun bookAmbulance(
        phone: String,
        latitude: Double,
        longitude: Double,
        address: String,
        city: String,
        pinCode: String,
        dialog: BottomSheetDialog
    ) {
        val apiLoadingDialog = showGifProgressDialog()

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
            override fun onResponse(call: Call<BookingResponse>, response: Response<BookingResponse>) {
                apiLoadingDialog.dismiss()
                if (response.isSuccessful && response.body()?.status == "success") {
                    saveRideDetails(phone, latitude, longitude, address, city, pinCode)
                    saveBookingTimestamp()
                    SweetAlertDialog(this@MainActivity, SweetAlertDialog.SUCCESS_TYPE)
                        .setTitleText("Success!")
                        .setContentText("Your ride is successfully booked!")
                        .setConfirmText("OK")
                        .setConfirmClickListener { sweetAlertDialog ->
                            sweetAlertDialog.dismissWithAnimation() // Explicitly dismiss the dialog
                            dialog.dismiss() // Dismiss the BottomSheetDialog
                        }
                        .show()
                } else {
                    SweetAlertDialog(this@MainActivity, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Failed")
                        .setContentText(response.body()?.message ?: "Something went wrong!")
                        .setConfirmText("OK")
                        .show()
                }
            }

            override fun onFailure(call: Call<BookingResponse>, t: Throwable) {
                apiLoadingDialog.dismiss()
                SweetAlertDialog(this@MainActivity, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("Error")
                    .setContentText("Check your Internet Connection!")
                    .setConfirmText("OK")
                    .show()
            }
        })
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
            Toast.makeText(this, "Please book after $remainingTime seconds", Toast.LENGTH_SHORT).show()
            false
        } else {
            true
        }
    }

    private fun saveBookingTimestamp() {
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putLong("lastBookingTime", System.currentTimeMillis()).apply()
    }

    private fun fetchCurrentLocation(onLocationFetched: (Double, Double, String, String, String) -> Unit) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            onLocationFetched(0.0, 0.0, "N/A", "N/A", "N/A") // Handle permission denial
            return
        }

        if (!isLocationEnabled()) {
            // Prompt user to enable location services
            showEnableLocationDialog()
            onLocationFetched(0.0, 0.0, "N/A", "N/A", "N/A") // Handle disabled location services
            return
        }

        // Fetch the current location
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude

                // Get address details
                val geocoder = Geocoder(this, Locale.getDefault())
                val addressList: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
                val address = addressList?.get(0)?.getAddressLine(0) ?: "Address not found"
                val city = addressList?.get(0)?.locality ?: "City not found"
                val pincode = addressList?.get(0)?.postalCode ?: "Pincode not found"

                onLocationFetched(latitude, longitude, address, city, pincode)
            } else {
                // Fallback for null location
                onLocationFetched(0.0, 0.0, "N/A", "N/A", "N/A")
            }
        }.addOnFailureListener {
            // Handle failure to fetch location
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
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    }

    private fun showGifProgressDialog(): AlertDialog {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.custom_progress_gif, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.loading_lottie)

        // Use Glide to load the GIF
        try {
            Glide.with(this)
                .asGif()
//                .load("https://drive.google.com/file/d/1mqmi_GqQ-mizlsdfbmGwBDTnmF4sNDIi/view?usp=sharing")
                .load("https://i.giphy.com/media/v1.Y2lkPTc5MGI3NjExdWJmNzJ0MmNpMGw0djhzZXZidG9mcXJ3cW5mdmRtdzd6ajhxbjBqaCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/wSnOQErP2oRnk4RB36/giphy.gif")
                .into(imageView)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.show()
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
    }

    private fun setupNavigationDrawer() {
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_profile -> navigateTo(ProfileActivity::class.java)
                R.id.nav_my_rides -> navigateTo(MyRidesActivity::class.java)
                R.id.nav_save_locations -> navigateTo(SaveLocationActivity::class.java)
                R.id.termAndCondition -> navigateTo(TermConditionActivity::class.java)
                R.id.nav_settings -> navigateTo(SettingActivity::class.java)
                R.id.nav_logout -> logoutUser()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        val headerView = navView.getHeaderView(0)
        val profileImage = headerView.findViewById<ImageView>(R.id.profile_image)
        val userName = headerView.findViewById<TextView>(R.id.user_name)
        val userPhone = headerView.findViewById<TextView>(R.id.user_phone)

        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val name = sharedPreferences.getString("userName", "User Name")
        val phone = sharedPreferences.getString("userPhone", "+91 00000 00000")
        val base64Image = sharedPreferences.getString("userImage", null)

        userName.text = name
        userPhone.text = phone

        if (!base64Image.isNullOrEmpty()) {
            val decodedBitmap = decodeBase64ToBitmap(base64Image)
            profileImage.setImageBitmap(decodedBitmap)
        } else {
            profileImage.setImageResource(R.drawable.profile_user_icon) // Default placeholder
        }
    }

    private fun logoutUser() {
        SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
            .setTitleText("Are you sure?")
            .setContentText("You will be logged out of your account.")
            .setConfirmText("Logout")
            .setCancelText("Cancel")
            .setConfirmClickListener { sweetAlertDialog ->
                // Clear Profile Data
                clearProfileData()
                // Clear Cache
                clearCache()
                // Perform logout actions
                getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().clear().apply()
                val intent = Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }

                startActivity(intent)
                Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
                sweetAlertDialog.dismissWithAnimation() // Close the dialog
            }
            .setCancelClickListener { sweetAlertDialog ->
                sweetAlertDialog.dismissWithAnimation() // Close the dialog without logging out
            }
            .show()
    }
    private fun clearProfileData() {
        try {
            // Clear any saved data in SharedPreferences related to profile
            getSharedPreferences("ProfilePrefs", MODE_PRIVATE).edit().clear().apply()

            // If profile data is saved in a database, clear it as well
            // Example: MyDatabase.getInstance(this).profileDao().clearProfile()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun clearCache() {
        try {
            val cacheDir = cacheDir
            if (cacheDir.isDirectory) {
                cacheDir.list()?.forEach { file ->
                    File(cacheDir, file).deleteRecursively()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun navigateTo(activity: Class<*>) {
        startActivity(Intent(this, activity))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
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
                            MarkerOptions().position(currentLatLng).title("Current Location").draggable(true)
                        )
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                        updateBottomSheetFields(lat, lng, address, city, pincode)
                    }
                }
            } else {
                // Permission denied
                progressDialog.dismiss()
                Toast.makeText(this, "Permission denied! Location is required.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun saveRideDetails(
        phone: String,
        latitude: Double,
        longitude: Double,
        address: String,
        city: String,
        pinCode: String
    ) {
        val sharedPreferences = getSharedPreferences("RideDetails", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Fetch the current date and time
        val (currentDate, currentTime) = getCurrentDateTime()

        // Fetch existing rides from SharedPreferences
        val existingRidesJson = sharedPreferences.getString("rides", "[]") ?: "[]"
        val existingRidesArray = try {
            JSONArray(existingRidesJson)
        } catch (e: Exception) {
            e.printStackTrace()
            JSONArray() // Fallback to a new array if parsing fails
        }

// Add new ride details
        val newRide = JSONObject().apply {
            put("phone", phone)
            put("latitude", latitude)
            put("longitude", longitude)
            put("address", address)
            put("city", city)
            put("pinCode", pinCode)
            put("date", currentDate)
            put("time", currentTime)
            put("status", "Confirmed")
            put("vehicle", "Ambulance")
        }
        existingRidesArray.put(newRide)

// Save back to SharedPreferences
        editor.putString("rides", existingRidesArray.toString())
        editor.apply()

    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            profileUpdateReceiver,
            IntentFilter("PROFILE_UPDATED")
        )
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(profileUpdateReceiver)
    }

    private fun updateNavigationHeader(showLoader: Boolean = false) {
        val headerView = navView.getHeaderView(0)
        val profileImage = headerView.findViewById<ImageView>(R.id.profile_image)
        val userName = headerView.findViewById<TextView>(R.id.user_name)
        val userPhone = headerView.findViewById<TextView>(R.id.user_phone)
        val navHeaderLoader = headerView.findViewById<ProgressBar>(R.id.nav_header_loader)

        if (showLoader) {
            navHeaderLoader.visibility = View.VISIBLE
        } else {
            navHeaderLoader.visibility = View.GONE

            // Fetch updated data from SharedPreferences
            val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
            val name = sharedPreferences.getString("userName", "Default Name")
            val phone = sharedPreferences.getString("userPhone", "Default Phone")
            val base64Image = sharedPreferences.getString("userImage", null)

            // Update UI elements
            userName.text = name
            userPhone.text = phone

            if (!base64Image.isNullOrEmpty()) {
                try {
                    val decodedBitmap = decodeBase64ToBitmap(base64Image)
                    profileImage.setImageBitmap(decodedBitmap)
                } catch (e: Exception) {
                    profileImage.setImageResource(R.drawable.profile_user_icon) // Default placeholder
                }
            } else {
                profileImage.setImageResource(R.drawable.profile_user_icon) // Default placeholder
            }
        }
    }

    private fun decodeBase64ToBitmap(base64Image: String): Bitmap {
        val decodedString = Base64.decode(base64Image, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
    }

    fun getCurrentDateTime(): Pair<String, String> {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()) // Format: Day-Month-Year
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())  // Format: Hour:Minute:Second
        val now = Date()

        return Pair(dateFormat.format(now), timeFormat.format(now))
    }
}


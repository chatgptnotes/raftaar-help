package com.example.sevaapp.activities

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.pedant.SweetAlert.SweetAlertDialog
import com.bumptech.glide.Glide
import com.example.sevaapp.R
import com.example.sevaapp.adapter.LocationAdapter
import com.example.sevaapp.apiinterface.RetrofitClient
import com.example.sevaapp.models.BookingDetails
import com.example.sevaapp.models.BookingResponse
import com.example.sevaapp.models.LocationModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SaveLocationActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val locationList = mutableListOf<LocationModel>()
    private lateinit var adapter: LocationAdapter
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_save_location)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("SavedLocations", MODE_PRIVATE)

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recycler_view_locations)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = LocationAdapter(
            locationList,
            onEditClick = { position -> showEditDialog(position) },
            onDeleteClick = { position -> deleteLocation(position) },
            onItemClick = { location -> showBottomSheet(location) }
        )

        recyclerView.adapter = adapter

        // Load existing locations
        loadLocations()

        // Add functionality for "Add More" button
        val addMoreCard = findViewById<CardView>(R.id.add_more_card)
        addMoreCard.setOnClickListener {
//            showAddLocationDialog()
        }
    }

    private fun showBottomSheet(location: LocationModel) {
        // Inflate the bottom sheet layout
        val bottomSheetView = LayoutInflater.from(this).inflate(R.layout.book_from_save_location_bottom_sheet, null)
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(bottomSheetView)
        // Initialize fields
        val currentLocationField = bottomSheetView.findViewById<EditText>(R.id.current_location)
        val latitudeField = bottomSheetView.findViewById<EditText>(R.id.latitude)
        val longitudeField = bottomSheetView.findViewById<EditText>(R.id.longitude)
        val cityField = bottomSheetView.findViewById<EditText>(R.id.city_field)
        val pinCodeField = bottomSheetView.findViewById<EditText>(R.id.pin_code_field)
        val mobileNumberField = bottomSheetView.findViewById<EditText>(R.id.mobileNumberEditText)

        // Fetch Mobile Number
        val savedMobileNumber = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            .getString("userPhone", "")
        mobileNumberField.setText(savedMobileNumber)
        // Pre-fill fields with location details
        currentLocationField.setText(location.address)
        latitudeField.setText(location.latitude.toString())
        longitudeField.setText(location.longitude.toString())
        cityField.setText(location.city)
        pinCodeField.setText(location.pincode)
        // Handle confirm button click
        bottomSheetView.findViewById<Button>(R.id.confirm_button).setOnClickListener {
            val phone = mobileNumberField.text.toString().trim()
            val address = currentLocationField.text.toString().trim()
            val latitude = latitudeField.text.toString().toDoubleOrNull() ?: 0.0
            val longitude = longitudeField.text.toString().toDoubleOrNull() ?: 0.0
            val city = cityField.text.toString().trim()
            val pincode = pinCodeField.text.toString().trim()

            if (isValidPhoneNumber(phone)) {
                bookAmbulance(phone, latitude, longitude, address, city, pincode, bottomSheetDialog)
            } else {
                showErrorDialog("Invalid Mobile Number", "Please enter a valid 10-digit mobile number starting with 6-9.")
            }
        }

        // Show the bottom sheet
        bottomSheetDialog.show()
    }

    // Show Error Dialog
    private fun showErrorDialog(title: String, message: String) {
        SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
            .setTitleText(title)
            .setContentText(message)
            .setConfirmText("OK")
            .show()
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        return phone.matches(Regex("\\d{10}$"))
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
                    SweetAlertDialog(this@SaveLocationActivity, SweetAlertDialog.SUCCESS_TYPE)
                        .setTitleText("Success!")
                        .setContentText("Your ride is successfully booked!")
                        .setConfirmText("OK")
                        .setConfirmClickListener { sweetAlertDialog ->
                            sweetAlertDialog.dismissWithAnimation() // Explicitly dismiss the dialog
                            dialog.dismiss() // Dismiss the BottomSheetDialog
                        }
                        .show()
                } else {
                    SweetAlertDialog(this@SaveLocationActivity, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Failed")
                        .setContentText(response.body()?.message ?: "Something went wrong!")
                        .setConfirmText("OK")
                        .show()
                }
            }

            override fun onFailure(call: Call<BookingResponse>, t: Throwable) {
                apiLoadingDialog.dismiss()
                SweetAlertDialog(this@SaveLocationActivity, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("Error")
                    .setContentText("Check your Internet Connection!")
                    .setConfirmText("OK")
                    .show()
            }
        })
    }

    private fun saveBookingTimestamp() {
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putLong("lastBookingTime", System.currentTimeMillis()).apply()
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


    fun getCurrentDateTime(): Pair<String, String> {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()) // Format: Day-Month-Year
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())  // Format: Hour:Minute:Second
        val now = Date()

        return Pair(dateFormat.format(now), timeFormat.format(now))
    }


    private fun loadLocations() {
        try {
            val savedLocationsJson = sharedPreferences.getString("locations", "[]") ?: "[]"
            val savedLocationsArray = JSONArray(savedLocationsJson)

            locationList.clear()

            for (i in 0 until savedLocationsArray.length()) {
                val locationObject = savedLocationsArray.getJSONObject(i)
                val label = locationObject.getString("label")
                val address = locationObject.getString("address")
                val latitude = locationObject.optDouble("latitude", 0.0)
                val longitude = locationObject.optDouble("longitude", 0.0)
                val city = locationObject.optString("city", "Unknown")
                val pincode = locationObject.optString("pincode", "Unknown")
                locationList.add(LocationModel(label, address, latitude, longitude, city, pincode))
            }

            adapter.notifyDataSetChanged()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveLocation(label: String, address: String, latitude: Double, longitude: Double, city: String, pincode: String) {
        try {
            val savedLocationsJson = sharedPreferences.getString("locations", "[]") ?: "[]"
            val savedLocationsArray = JSONArray(savedLocationsJson)

            val newLocation = JSONObject().apply {
                put("label", label)
                put("address", address)
                put("latitude", latitude)
                put("longitude", longitude)
                put("city", city)
                put("pincode", pincode)
            }

            savedLocationsArray.put(newLocation)
            sharedPreferences.edit().putString("locations", savedLocationsArray.toString()).apply()

            locationList.add(LocationModel(label, address, latitude, longitude, city, pincode))
            adapter.notifyItemInserted(locationList.size - 1)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun deleteLocation(position: Int) {
        try {
            // Show confirmation dialog
            SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Are you sure?")
                .setContentText("Do you want to delete this location?")
                .setConfirmText("Yes")
                .setCancelText("No")
                .setConfirmClickListener { sweetAlertDialog ->
                    // Proceed with deletion
                    val savedLocationsJson = sharedPreferences.getString("locations", "[]") ?: "[]"
                    val savedLocationsArray = JSONArray(savedLocationsJson)

                    val updatedLocationsArray = JSONArray()
                    for (i in 0 until savedLocationsArray.length()) {
                        if (i != position) {
                            updatedLocationsArray.put(savedLocationsArray.getJSONObject(i))
                        }
                    }

                    sharedPreferences.edit().putString("locations", updatedLocationsArray.toString()).apply()

                    locationList.removeAt(position)
                    adapter.notifyItemRemoved(position)

                    sweetAlertDialog.dismissWithAnimation()
                    SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                        .setTitleText("Deleted!")
                        .setContentText("Location has been deleted.")
                        .setConfirmText("OK")
                        .show()
                }
                .setCancelClickListener { sweetAlertDialog ->
                    // Dismiss the dialog
                    sweetAlertDialog.dismissWithAnimation()
                }
                .show()
        } catch (e: Exception) {
            e.printStackTrace()
            SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                .setTitleText("Error!")
                .setContentText("Failed to delete location. Please try again.")
                .setConfirmText("OK")
                .show()
        }
    }

    private fun showEditDialog(position: Int) {
        val locationToEdit = locationList[position]
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_location, null)
        val locationInput = dialogView.findViewById<AutoCompleteTextView>(R.id.location_address_input)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radio_group_labels)

        // Populate fields with existing data
        locationInput.setText(locationToEdit.address)
        when (locationToEdit.label) {
            "Office" -> radioGroup.check(R.id.radio_office)
            "Home" -> radioGroup.check(R.id.radio_home)
            "Other" -> radioGroup.check(R.id.radio_other)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit Location")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val selectedLabel = when (radioGroup.checkedRadioButtonId) {
                    R.id.radio_office -> "Office"
                    R.id.radio_home -> "Home"
                    R.id.radio_other -> "Other"
                    else -> "Unknown"
                }
                val address = locationInput.text.toString().trim()

                if (selectedLabel.isNotEmpty() && address.isNotEmpty()) {
                    updateLocation(position, selectedLabel, address)
                } else {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun updateLocation(position: Int, label: String, address: String) {
        try {
            val savedLocationsJson = sharedPreferences.getString("locations", "[]") ?: "[]"
            val savedLocationsArray = JSONArray(savedLocationsJson)

            val updatedLocationsArray = JSONArray()
            for (i in 0 until savedLocationsArray.length()) {
                val locationObject = savedLocationsArray.getJSONObject(i)
                if (i == position) {
                    updatedLocationsArray.put(
                        JSONObject().apply {
                            put("label", label)
                            put("address", address)
                        }
                    )
                } else {
                    updatedLocationsArray.put(locationObject)
                }
            }

            sharedPreferences.edit().putString("locations", updatedLocationsArray.toString()).apply()

//            locationList[position] = LocationModel(label, address)
            adapter.notifyItemChanged(position)

            Toast.makeText(this, "Location updated", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

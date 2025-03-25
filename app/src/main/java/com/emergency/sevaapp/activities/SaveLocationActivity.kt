package com.emergency.sevaapp.activities

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.pedant.SweetAlert.SweetAlertDialog
import com.bumptech.glide.Glide
import com.emergency.sevaapp.R
import com.emergency.sevaapp.adapter.LocationAdapter
import com.emergency.sevaapp.apiclient.RetrofitClient
import com.emergency.sevaapp.models.BookingDetails
import com.emergency.sevaapp.models.BookingResponse
import com.emergency.sevaapp.models.LocationModel
import com.emergency.sevaapp.models.ProfileResponse
import com.emergency.sevaapp.models.User
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SaveLocationActivity : AppCompatActivity() {
    private val CHANNEL_ID = "raftaar_seva_notifications"
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
        // Fetch saved locations from API
        fetchUserProfile()
        // Load existing locations
//        loadLocations()

        val backButton = findViewById<ImageView>(R.id.back_button)
        backButton.setOnClickListener {
            // Finish the current activity and go back to the previous one
            onBackPressed()
        }
    }

    private fun fetchUserProfile() {
        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val uniqueId = sharedPreferences.getString("unique_id", null)

        if (uniqueId.isNullOrEmpty()) {
            Toast.makeText(this, "User not found. Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }

        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Fetching saved locations...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        RetrofitClient.instance.getProfile(uniqueId).enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                try {
                    progressDialog.dismiss()
                    if (response.isSuccessful) {
                        val user = response.body()?.user
                        if (user != null) {
                            loadProfileLocations(user)
                        } else {
                            SweetAlertDialog(this@SaveLocationActivity, SweetAlertDialog.WARNING_TYPE)
                                .setTitleText("Saved Location")
                                .setContentText("No saved locations found.")
                                .setConfirmText("OK")
                                .show()
                        }
                    } else {
                        SweetAlertDialog(this@SaveLocationActivity, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Network Error")
                            .setContentText("Failed to fetch saved locations.")
                            .setConfirmText("OK")
                            .show()
                        Log.e("SaveLocationActivity", "Error: ${response.message()}")
                    }
                } catch (e: Exception) {
                    progressDialog.dismiss()
                    Log.e("SaveLocationActivity", "Error in onResponse: ${e.message}", e)
                    SweetAlertDialog(this@SaveLocationActivity, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Error")
                        .setContentText("An unexpected error occurred while processing the response.")
                        .setConfirmText("OK")
                        .show()
                }
            }

            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                progressDialog.dismiss()
                Log.e("SaveLocationActivity", "Network Error: ${t.message}", t)
                SweetAlertDialog(this@SaveLocationActivity, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("No Internet Connection")
                    .setContentText("Network error. Try again!")
                    .setConfirmText("OK")
                    .show()
                Toast.makeText(this@SaveLocationActivity, "Network error. Try again!", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadProfileLocations(user: User) {
        locationList.clear() // Clear previous data

        if (!user.address1.isNullOrEmpty()  && user.latitude1 != null && user.longitude1 != null) {
            locationList.add(LocationModel("🏡 Home", user.address1, user.latitude1, user.longitude1, user.address1, user.pinCode))
        }
        if (!user.address2.isNullOrEmpty() && user.latitude2!= null && user.longitude2!= null) {
            locationList.add(LocationModel("🏢 Office", user.address2, user.latitude2,user.longitude2, user.address2, user.pinCode))
        }
        if (!user.address3.isNullOrEmpty() && user.latitude3!= null && user.longitude3!= null) {
            locationList.add(LocationModel("📍Other", user.address3, user.latitude3,user.longitude3, user.address3, user.pinCode))
        }
        adapter.notifyDataSetChanged()
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

        pinCodeField.isFocusable = true
        pinCodeField.isFocusableInTouchMode = false
        pinCodeField.isCursorVisible = false

        // Fetch Mobile Number
        val savedMobileNumber = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            .getString("cached_phone", null)

        if (!savedMobileNumber.isNullOrEmpty()) {
            mobileNumberField.setText(savedMobileNumber)
        } else {
            showErrorDialog("Missing Mobile Number", "Please update your profile to include your mobile number.")
        }

        // Pre-fill fields with location details
        currentLocationField.setText(location.address)
        mobileNumberField.setText(savedMobileNumber)
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

        // **Wait for 5 seconds before proceeding with the booking request**
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
                override fun onResponse(call: Call<BookingResponse>, response: Response<BookingResponse>) {
                    try {
                        apiLoadingDialog.dismiss()
                        if (response.isSuccessful && response.body()?.status == "success") {
                            showBookingNotification()

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
                            Log.e("BookAmbulance", "Error: ${response.body()?.message}")
                        }
                    } catch (e: Exception) {
                        apiLoadingDialog.dismiss()
                        Log.e("BookAmbulance", "Error processing the response: ${e.message}", e)
                        SweetAlertDialog(this@SaveLocationActivity, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Error")
                            .setContentText("An unexpected error occurred while processing your request.")
                            .setConfirmText("OK")
                            .show()
                    }
                }

                override fun onFailure(call: Call<BookingResponse>, t: Throwable) {
                    apiLoadingDialog.dismiss()
                    Log.e("BookAmbulance", "Network Error: ${t.message}", t)
                    SweetAlertDialog(this@SaveLocationActivity, SweetAlertDialog.WARNING_TYPE)
                        .setTitleText("Error")
                        .setContentText("Check your Internet Connection!")
                        .setConfirmText("OK")
                        .show()
                }
            })
        }, 3000) // **5 seconds delay before sending request**
    }


    private fun showBookingNotification() {
        try {
            // Intent to open the app when the notification is clicked
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

            // Set the notification sound URI (default notification sound)
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            // Create notification channel (Required for Android 8.0 and above)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channelId = "ambulance_notification_channel"
                val channelName = "Ambulance Booking Notifications"
                val channelDescription = "Notifications related to ambulance bookings"
                val importance = NotificationManager.IMPORTANCE_HIGH

                val notificationChannel = NotificationChannel(channelId, channelName, importance).apply {
                    description = channelDescription
                    enableLights(true)
                    lightColor = Color.RED
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 1000)
                }

                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(notificationChannel)
            }

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
        } catch (e: Exception) {
            Log.e("BookingNotification", "Error displaying notification: ${e.message}", e)
        }
    }

    private fun showGifProgressDialog1(): AlertDialog {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.custom_progress_gif, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.loading_lottie) // Use GifImageView

//        val gifUrl = "https://emergencyseva.in/emergencyseva.in/images/KAdwRRUAsQ-ezgif.com-crop.gif"
        val gifUrl = "https://i.giphy.com/media/v1.Y2lkPTc5MGI3NjExdWJmNzJ0MmNpMGw0djhzZXZidG9mcXJ3cW5mdmRtdzd6ajhxbjBqaCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/wSnOQErP2oRnk4RB36/giphy.gif"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // Prevent manual dismiss
            .create()

        dialog.show()

        // Load the GIF and ensure it plays completely before starting timer
        Glide.with(this)
            .asGif()
            .load(gifUrl)
            .into(imageView)

        // **Start a strict 5-second countdown timer**
        object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Do nothing, just wait
            }

            override fun onFinish() {
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            }
        }.start()
        return dialog
    }

    private fun showGifProgressDialog(): AlertDialog {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.custom_progress_gif, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.loading_lottie)
//        val gifUrl = "https://emergencyseva.in/emergencyseva.in/images/KAdwRRUAsQ-ezgif.com-crop.gif"
        val gifUrl = "https://i.giphy.com/media/v1.Y2lkPTc5MGI3NjExdWJmNzJ0MmNpMGw0djhzZXZidG9mcXJ3cW5mdmRtdzd6ajhxbjBqaCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/wSnOQErP2oRnk4RB36/giphy.gif"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // Prevent user from closing manually
            .create()

        dialog.show()

        // Keep the dialog open for exactly 1 minute (60,000 ms)0.
        val timer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Do nothing, just wait
            }

            override fun onFinish() {
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            }
        }
        timer.start() // Start the countdown timer immediately

        // Load GIF using Glide
        Glide.with(this)
            .asGif()
            .load(gifUrl)
            .into(imageView)

        return dialog
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

                    val updatedLocationsArray = (JSONArray())
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
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_location, null)
        val editText = dialogView.findViewById<EditText>(R.id.location_address_input)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radio_group_labels)

        editText.setText(locationToEdit.address)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // Prevent accidental dismiss
            .create()

        dialogView.findViewById<TextView>(R.id.cancel_button).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<TextView>(R.id.save_button).setOnClickListener {
            val updatedAddress = editText.text.toString()
            if (updatedAddress.isEmpty()) {
                Toast.makeText(this, "Please enter an address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedLabel = when (radioGroup.checkedRadioButtonId) {
                R.id.radio_office -> "Office"
                R.id.radio_home -> "Home"
                R.id.radio_other -> "Other"
                else -> locationToEdit.label
            }

            updateLocation(position, selectedLabel, updatedAddress)
            dialog.dismiss()
        }

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

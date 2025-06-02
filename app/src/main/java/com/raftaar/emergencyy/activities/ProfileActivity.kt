package com.raftaar.emergencyy.activities

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import cn.pedant.SweetAlert.SweetAlertDialog
import com.raftaar.emergencyy.R
import com.raftaar.emergencyy.apiclient.RetrofitClient
import com.raftaar.emergencyy.models.ProfileResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.Calendar

class ProfileActivity : AppCompatActivity() {

    private lateinit var nameTextView: TextView
    private lateinit var userNameDetail: TextView
    private lateinit var dobTextView: TextView
    private lateinit var mobileNumberTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var cityTextView: TextView
    private lateinit var pinCodeTextView: TextView
    private lateinit var profileInitials: TextView
    private lateinit var editProfileButton: LinearLayout
    private lateinit var deleteProfileButton: LinearLayout
    private var userId: String? = null
    private var uniqueId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile1)
        requestNotificationPermission()
        initializeViews()
        logCachedProfile()

        val sharedPreferences: SharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        userId = sharedPreferences.getString("user_id", null)
        uniqueId = sharedPreferences.getString("unique_id", null)

        Log.e("User id is","$userId")
        Log.e(" Unique id is","$uniqueId")

        if (uniqueId == null) {
            Toast.makeText(this, "Unique ID not found. Please register or log in again.", Toast.LENGTH_SHORT).show()
            return
        }

        fetchUserProfile(uniqueId!!)
        editProfileButton.setOnClickListener { showEditProfileDialog() }
        deleteProfileButton.setOnClickListener { deleteAccount() }
        findViewById<ImageView>(R.id.backButton).setOnClickListener { onBackPressed() }
    }

    private fun showEditProfileDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val editDob = dialogView.findViewById<EditText>(R.id.editDob)
        val editAddress = dialogView.findViewById<AutoCompleteTextView>(R.id.editCity)
        val editPinCode = dialogView.findViewById<EditText>(R.id.editPinCode)
        val saveButton = dialogView.findViewById<Button>(R.id.saveButton)

        // City dropdown data (list of cities)
        val cities = listOf(
            "Nagpur", "Mumbai", "Delhi", "Bangalore", "Chennai", "Kolkata", "Hyderabad",
            "Pune", "Ahmedabad", "Surat", "Jaipur", "Lucknow", "Kanpur", "Nagaland",
            "Indore", "Patna", "Vadodara", "Coimbatore", "Kochi", "Visakhapatnam",
            "Chandigarh", "Bhopal", "Mysuru", "Agra", "Nashik", "Raipur", "Ranchi",
            "Thane", "Faridabad", "Meerut", "Vijayawada", "Mangalore", "Kochi", "Bhubaneswar",
            "Noida", "Ghaziabad", "Jaipur", "Aurangabad", "Bhopal", "Trivandrum", "Dehradun",
            "Amritsar", "Tirupati", "Jodhpur", "Navi Mumbai", "Gwalior", "Jammu", "Shimla",
            "Udaipur", "Surat", "Aligarh", "Chandrapur", "Patiala", "Jalandhar", "Hoshiarpur",
            "Srinagar", "Vadodara", "Ranchi", "Visakhapatnam", "Pondicherry", "Gurugram",
            "Kolkata", "Kochi", "Madurai", "Chennai", "Nagapattinam", "Tirunelveli", "Moradabad",
            "Varanasi", "Kozhikode", "Bhilai", "Gwalior", "Bikaner", "Agartala", "Gurgaon",
            "Mysore", "Bhopal", "Durgapur", "Nellore", "Kolkata", "Shivamogga", "Vellore", "Rourkela"
        )
        val cityAdapter = ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, cities)
        editAddress.setAdapter(cityAdapter)

        // Pre-fill fields with current values
        editDob.setText(dobTextView.text.toString())
        editAddress.setText(cityTextView.text.toString())
        editPinCode.setText(pinCodeTextView.text.toString())

        // Show DatePickerDialog when clicking on the DOB field
        editDob.setOnClickListener {
            showDatePicker(editDob)
        }

        saveButton.setOnClickListener {
            val dob = editDob.text.toString()
            val address = editAddress.text.toString()
            val pincode = editPinCode.text.toString()

            // Validate inputs
            if (validateProfileInputs(dob, address, pincode)) {
                saveProfileData(dob, address, pincode)
                dialog.dismiss()
            }
        }

        dialog.show()
    }




    private fun fetchUserProfile(uniqueId: String) {
        val progressDialog = createProgressDialog("Fetching profile...")
        progressDialog.show()

        RetrofitClient.instance.getProfile(uniqueId).enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                try {
                    progressDialog.dismiss()

                    if (response.isSuccessful) {
                        val user = response.body()?.user
                        if (user != null) {
                            saveProfileToCache(
                                user.full_name.orEmpty(),
                                user.dob.orEmpty(),
                                user.phone1.orEmpty(),
                                user.user_name.orEmpty(),
                                user.address1.orEmpty(),
                                user.pinCode.orEmpty()
                            )
                            updateUI(
                                user.full_name.orEmpty(),
                                user.dob.orEmpty(),
                                user.phone1.orEmpty(),
                                user.user_name.orEmpty(),
                                user.address1.orEmpty(),
                                user.pinCode.orEmpty()
                            )
                        } else {
                            Toast.makeText(this@ProfileActivity, "No profile data found.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@ProfileActivity, "Failed to fetch profile.", Toast.LENGTH_SHORT).show()
                        Log.e("ProfileActivity", "Error: ${response.message()}")
                    }
                } catch (e: Exception) {
                    progressDialog.dismiss()
                    Log.e("ProfileActivity", "Error processing response: ${e.message}", e)
                    Toast.makeText(this@ProfileActivity, "Error processing profile data: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                progressDialog.dismiss()
                Log.e("ProfileActivity", "Network Error: ${t.message}", t)
                Toast.makeText(this@ProfileActivity, "No internet Connection", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun deleteAccount() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_account, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.deleteButton).setOnClickListener {
            if (uniqueId.isNullOrEmpty()) {
                Toast.makeText(this, "User ID not found!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val progressDialog = createProgressDialog("Deleting account...")
            progressDialog.show()

            RetrofitClient.instance.softDeleteUser(uniqueId!!).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    try {
                        progressDialog.dismiss()
                        if (response.isSuccessful) {
                            Toast.makeText(this@ProfileActivity, "Account deactivated successfully.", Toast.LENGTH_SHORT).show()
                            clearProfileData()
                            clearCache()
                            getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().clear().apply()
                            val intent = Intent(this@ProfileActivity, LoginActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            dialog.dismiss()
                        } else {
                            Toast.makeText(this@ProfileActivity, "Failed to deactivate account. Please try again.", Toast.LENGTH_SHORT).show()
                            clearProfileData()
                            clearCache()
                            getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().clear().apply()
                            val intent = Intent(this@ProfileActivity, WelcomeActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            dialog.dismiss()
                            Log.e("ProfileActivity", "Error in response: ${response.message()}")
                        }
                    } catch (e: Exception) {
                        progressDialog.dismiss()
                        Log.e("ProfileActivity", "Error in onResponse: ${e.message}", e)
                        Toast.makeText(this@ProfileActivity, "Error processing deactivation request: ${e.message}", Toast.LENGTH_LONG).show()
                        dialog.dismiss()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    progressDialog.dismiss()
                    Log.e("ProfileActivity", "Network Error: ${t.message}", t)
                    Toast.makeText(this@ProfileActivity, "Network error. Try again!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            })
        }

        dialog.show()
    }
    private fun deleteAccount1() {
        SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
            .setTitleText("Are you sure?")
            .setContentText("Your account will be deactivated.")
            .setConfirmText("Delete Account")
            .setCancelText("Cancel")
            .setConfirmClickListener { sweetAlertDialog ->

                if (uniqueId.isNullOrEmpty()) {
                    Toast.makeText(this, "User ID not found!", Toast.LENGTH_SHORT).show()
                    return@setConfirmClickListener
                }

                val progressDialog = createProgressDialog("Deleting account...")
                progressDialog.show()

                // Retrofit API Call
                RetrofitClient.instance.softDeleteUser(uniqueId!!).enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        try {
                            progressDialog.dismiss()

                            if (response.isSuccessful) {
                                Toast.makeText(this@ProfileActivity, "Account deactivated successfully.", Toast.LENGTH_SHORT).show()

                                // Clear profile data and cache
                                clearProfileData()
                                clearCache()

                                // Logout user and redirect to Login screen
                                getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().clear().apply()
                                val intent = Intent(this@ProfileActivity, LoginActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                startActivity(intent)
                                sweetAlertDialog.dismissWithAnimation()
                            } else {
                                Toast.makeText(this@ProfileActivity, "Failed to deactivate account. Please try again.", Toast.LENGTH_SHORT).show()

                                // Clear profile data and cache
                                clearProfileData()
                                clearCache()

                                // Logout user and redirect to Welcome screen
                                getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().clear().apply()
                                val intent = Intent(this@ProfileActivity, WelcomeActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                startActivity(intent)
                                sweetAlertDialog.dismissWithAnimation()
                                Log.e("ProfileActivity", "Error in response: ${response.message()}")
                            }
                        } catch (e: Exception) {
                            progressDialog.dismiss()
                            Log.e("ProfileActivity", "Error in onResponse: ${e.message}", e)
                            Toast.makeText(this@ProfileActivity, "Error processing deactivation request: ${e.message}", Toast.LENGTH_LONG).show()
                            sweetAlertDialog.dismissWithAnimation()
                        }
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        progressDialog.dismiss()
                        Log.e("ProfileActivity", "Network Error: ${t.message}", t)
                        Toast.makeText(this@ProfileActivity, "Network error. Try again!", Toast.LENGTH_SHORT).show()
                        sweetAlertDialog.dismissWithAnimation()
                    }
                })
            }
            .setCancelClickListener { sweetAlertDialog ->
                sweetAlertDialog.dismissWithAnimation() // Close the dialog without deleting the account
            }
            .show()
    }

    private fun clearProfileData() {
        try {
            // Clear any saved data in SharedPreferences related to the profile
            getSharedPreferences("ProfileCache", MODE_PRIVATE).edit().clear().apply()
            // If profile data is saved in a database, clear it as well (optional)
            // Example: MyDatabase.getInstance(this).profileDao().clearProfile()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun logCachedProfile() {
        val cache = getCachedProfile()
        if (cache != null) {
            cache.forEach { (key, value) ->
                println("Cached $key: $value")
            }
        } else {
            println("No data in cache.")
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



    private fun showDatePicker(targetEditText: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                targetEditText.setText(formattedDate)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun saveProfileData(dob: String, address: String, pincode: String) {
        if (dob.isEmpty() || address.isEmpty() || pincode.isEmpty()) {
            Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show()
            return
        }

        if (uniqueId.isNullOrEmpty()) {
            Toast.makeText(this, "Unique ID is missing. Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }

        val profileData = mapOf(
            "unique_id" to uniqueId!!,
            "dob" to dob,
            "address1" to address,
            "pinCode" to pincode
        )

        println("Saving profile data: $profileData")

        val progressDialog = createProgressDialog("Updating profile...")
        progressDialog.show()

        RetrofitClient.instance.updateProfile(uniqueId!!, profileData).enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                try {
                    progressDialog.dismiss()

                    if (response.isSuccessful && response.body()?.user != null) {
                        val updatedProfile = response.body()?.user
                        updatedProfile?.let {
                            saveProfileToCache(
                                it.full_name.orEmpty(),
                                it.dob.orEmpty(),
                                it.phone1.orEmpty(),
                                it.user_name.orEmpty(),
                                it.address1.orEmpty(),
                                it.pinCode.orEmpty()
                            )
                            updateUI(
                                it.full_name.orEmpty(),
                                it.dob.orEmpty(),
                                it.phone1.orEmpty(),
                                it.user_name.orEmpty(),
                                it.address1.orEmpty(),
                                it.pinCode.orEmpty()
                            )
                            Toast.makeText(this@ProfileActivity, "Profile updated successfully.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("ProfileActivity", "Error updating profile: $errorBody")
                        Toast.makeText(this@ProfileActivity, "Failed to update profile. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    progressDialog.dismiss()
                    Log.e("ProfileActivity", "Error processing response: ${e.message}", e)
                    Toast.makeText(this@ProfileActivity, "Error processing profile update: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                progressDialog.dismiss()
                Log.e("ProfileActivity", "Network error: ${t.message}", t)
                Toast.makeText(this@ProfileActivity, "Network error. Please try again.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveProfileToCache(
        name: String, dob: String, phone: String, email: String, city: String, pincode: String
    ) {
        val sharedPreferences = getSharedPreferences("ProfileCache", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("name", name)
            putString("dob", dob)
            putString("phone", phone)
            putString("email", email)
            putString("city", city)
            putString("pincode", pincode)
            putLong("last_updated", System.currentTimeMillis()) // Save the last updated timestamp
            apply()
        }
    }
    private fun getCachedProfile(): Map<String, String>? {
        val sharedPreferences = getSharedPreferences("ProfileCache", MODE_PRIVATE)

        // Fetch values from SharedPreferences
        val profile = mapOf(
            "name" to sharedPreferences.getString("name", null),
            "dob" to sharedPreferences.getString("dob", null),
            "phone" to sharedPreferences.getString("phone", null),
            "email" to sharedPreferences.getString("email", null),
            "city" to sharedPreferences.getString("city", null),
            "pincode" to sharedPreferences.getString("pincode", null)
        )

        // Check if all required fields are present
        return if (profile.values.none { it.isNullOrEmpty() }) {
            profile.filterValues { !it.isNullOrEmpty() } as Map<String, String>
        } else {
            null // Return null if any value is missing
        }
    }

    private val CACHE_VALIDITY_DURATION = 24 * 60 * 60 * 1000L // 24 hours in milliseconds

    override fun onResume() {
        super.onResume()

        // Check if cache exists and is up-to-date
        val cachedProfile = getCachedProfile()
        val lastUpdated = getLastUpdatedTime()
        val isCacheValid = System.currentTimeMillis() - lastUpdated < CACHE_VALIDITY_DURATION

        if (cachedProfile != null && isCacheValid) {
            // Use cached data if valid
            updateUI(
                cachedProfile["name"] ?: "",
                cachedProfile["dob"] ?: "",
                cachedProfile["phone"] ?: "",
                cachedProfile["email"] ?: "",
                cachedProfile["city"] ?: "",
                cachedProfile["pincode"] ?: ""
            )
        } else {
            // Fetch from server if cache is missing or outdated
            uniqueId?.let {
                fetchUserProfile(it)
            } ?: Toast.makeText(this, "Unique ID not found. Please log in again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getLastUpdatedTime(): Long {
        val sharedPreferences = getSharedPreferences("ProfileCache", MODE_PRIVATE)
        return sharedPreferences.getLong("last_updated", 0L)
    }

    private fun validateProfileInputs(dob: String, address: String, pinCode: String): Boolean {
        return when {
            dob.isEmpty() -> {
                Toast.makeText(this, "Date of birth is required.", Toast.LENGTH_SHORT).show()
                false
            }
            address.isEmpty() -> {
                Toast.makeText(this, "Address is required.", Toast.LENGTH_SHORT).show()
                false
            }
            pinCode.length != 6 -> {
                Toast.makeText(this, "Pin code must be 6 digits.", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun updateUI(
        name: String, birthday: String,
        phone: String, email: String, city: String, pincode: String
    ) {
        nameTextView.text = name
        userNameDetail.text = name
        dobTextView.text = birthday
        mobileNumberTextView.text = phone
        emailTextView.text = email
        cityTextView.text = city
        pinCodeTextView.text = pincode
        profileInitials.text = getInitials(name)

        // Animate each value
        floatInView(nameTextView)
        floatInView(dobTextView)
        floatInView(mobileNumberTextView)
        floatInView(emailTextView)
        floatInView(cityTextView)
        floatInView(pinCodeTextView)
        floatInView(profileInitials)

//        // Animate each value
//        zoomInView(nameTextView)
//        zoomInView(dobTextView)
//        zoomInView(mobileNumberTextView)
//        zoomInView(emailTextView)
//        zoomInView(cityTextView)
//        zoomInView(pinCodeTextView)
//        zoomInView(profileInitials)
    }
    
    private fun floatInView(view: View) {
        view.translationY = 60f   // Start 60 pixels below
        view.alpha = 0f           // Start invisible
        view.animate()
            .translationY(0f)     // Move to original position
            .alpha(1f)            // Fade in
            .setDuration(500)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }
    private fun zoomInView(view: View) {
        view.scaleX = 0.5f
        view.scaleY = 0.5f
        view.alpha = 0f
        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(500)
            .setInterpolator(android.view.animation.OvershootInterpolator(2f))
            .start()
    }

    private fun getInitials(name: String): String {
        return if (name.isNotBlank()) {
            name.split(" ").mapNotNull { it.firstOrNull()?.toString()?.uppercase() }.joinToString("").take(2)
        } else {
            "?"
        }
    }

    private fun createProgressDialog(message: String): ProgressDialog {
        return ProgressDialog(this).apply {
            setMessage(message)
            setCancelable(false)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    private fun initializeViews() {
        nameTextView = findViewById(R.id.nameTextView)
        dobTextView = findViewById(R.id.dobTextView)
        mobileNumberTextView = findViewById(R.id.mobileNumberTextView)
        emailTextView = findViewById(R.id.emailTextView)
        cityTextView = findViewById(R.id.cityTextView)
        pinCodeTextView = findViewById(R.id.pinCodeTextView)
        userNameDetail = findViewById(R.id.userNameDetail)
        profileInitials = findViewById(R.id.profileInitials)
        editProfileButton = findViewById(R.id.editProfileButton)
        deleteProfileButton = findViewById(R.id.deleteProfileButton)
    }
}

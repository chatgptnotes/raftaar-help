package com.example.sevaapp.activities

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import cn.pedant.SweetAlert.SweetAlertDialog
import com.bumptech.glide.Glide
import com.example.sevaapp.R
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*

class ProfileActivity : AppCompatActivity() {
    private lateinit var userName: TextView
    private lateinit var userNameDetail: TextView
    private lateinit var userBirthday: TextView
    private lateinit var userPhone: TextView
    private lateinit var userEmail: TextView
    private lateinit var userCity: TextView
    private lateinit var pin_code_field: TextView
    private lateinit var profileImage: ImageView
    private lateinit var editProfileButton: Button
    private lateinit var deleteProfileButton: Button
    private val REQUEST_IMAGE_PICK = 1
    private val REQUEST_IMAGE_CAPTURE = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        initializeViews()
        loadProfileData()

        profileImage.setOnClickListener {
            showImagePickerDialog()
        }

        editProfileButton.setOnClickListener {
            showEditProfileDialog()
        }

        deleteProfileButton.setOnClickListener {
            deleteAccount()
        }

        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            onBackPressed()
        }
    }

    private fun initializeViews() {
        userName = findViewById(R.id.userName)
        userNameDetail = findViewById(R.id.userNameDetail)
        userBirthday = findViewById(R.id.userBirthday)
        userPhone = findViewById(R.id.userPhone)
        userEmail = findViewById(R.id.userEmail)
        userCity = findViewById(R.id.city)
        pin_code_field = findViewById(R.id.pin_code_field)
        profileImage = findViewById(R.id.profileImage)
        editProfileButton = findViewById(R.id.editProfileButton)
        deleteProfileButton = findViewById(R.id.deleteProfileButton)
    }

    private fun deleteAccount() {
        SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
            .setTitleText("Are you sure?")
            .setContentText("Your account will be permanently delete")
            .setConfirmText("Delete Account")
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
                Toast.makeText(this, "Account Deleted successfully", Toast.LENGTH_SHORT).show()
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

    private fun loadProfileData() {
        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        val name = sharedPreferences.getString("userName", "Name Not Updated")
        val dob = sharedPreferences.getString("userBirthday", "DOB Not Updated")
        val phone = sharedPreferences.getString("userPhone", "Mobile Number Not Updated")
        val email = sharedPreferences.getString("userEmail", "Email Not Updated")
        val city = sharedPreferences.getString("userCity", "Address Not Updated")
        val pincode = sharedPreferences.getString("userPincode", "Pincode")
        val base64Image = sharedPreferences.getString("userImage", null)

        updateUI(name.orEmpty(), dob.orEmpty(), phone.orEmpty(), email.orEmpty(), city.orEmpty(),  pincode.orEmpty(), base64Image)
    }

    private fun updateUI(name: String, birthday: String, phone: String, email: String, city: String, pincode: String, base64Image: String?) {
        userName.text = name
        userNameDetail.text = name
        userBirthday.text = birthday // Ensure this field is updated
        userPhone.text = phone
        userEmail.text = email
        userCity.text = city // Ensure this field is updated
        pin_code_field.text = pincode // Ensure this field is updated

        if (!base64Image.isNullOrEmpty()) {
            try {
                val decodedBitmap = decodeBase64ToBitmap(base64Image)
                val circularBitmap = getCircularBitmap(decodedBitmap) // Make the image circular
                profileImage.setImageBitmap(circularBitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
            }
        } else {
            Glide.with(this)
                .load(R.drawable.profile_user_icon)
                .circleCrop() // Make placeholder circular
                .into(profileImage)
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Choose from Gallery", "Take a Photo")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Update Profile Picture")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> chooseFromGallery()
                1 -> capturePhoto()
            }
        }
        builder.show()
    }

    private fun chooseFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    private fun capturePhoto() {
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
            val currentName = sharedPreferences.getString("userName", "Name Not Updated") ?: "Name Not Updated"

            when (requestCode) {
                REQUEST_IMAGE_PICK -> {
                    val imageUri = data?.data
                    if (imageUri != null) {
                        val inputStream = contentResolver.openInputStream(imageUri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        val base64Image = encodeImageToBase64(bitmap)
                        saveImageToPreferences(base64Image, currentName)
                        val circularBitmap = getCircularBitmap(bitmap) // Make circular
                        profileImage.setImageBitmap(circularBitmap)
                    }
                }
                REQUEST_IMAGE_CAPTURE -> {
                    val bitmap = data?.extras?.get("data") as? Bitmap
                    if (bitmap != null) {
                        val resizedBitmap = resizeBitmap(bitmap, 800, 800)
                        val base64Image = encodeImageToBase64(resizedBitmap)
                        saveImageToPreferences(base64Image, currentName)
                        val circularBitmap = getCircularBitmap(resizedBitmap) // Make circular
                        profileImage.setImageBitmap(circularBitmap)
                    }
                }
            }
        }
    }

    private fun saveImageToPreferences(base64Image: String, updatedName: String) {
        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("userImage", base64Image)
        editor.putString("userName", updatedName)
        editor.apply()

        // Debug logs
        println("DEBUG: Updated Name in SharedPreferences: $updatedName")
        println("DEBUG: Updated Image in SharedPreferences: $base64Image")

        // Notify MainActivity
        val intent = Intent("PROFILE_UPDATED")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun encodeImageToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun decodeBase64ToBitmap(base64Image: String): Bitmap {
        val decodedString = Base64.decode(base64Image, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
    }

    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(output)
        val paint = Paint()
        paint.isAntiAlias = true

        val rect = Rect(0, 0, size, size)
        val rectF = RectF(rect)

        canvas.drawARGB(0, 0, 0, 0)
        paint.color = Color.BLACK
        canvas.drawOval(rectF, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        return output
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val ratio = width.toFloat() / height.toFloat()

        val finalWidth: Int
        val finalHeight: Int
        if (ratio > 1) {
            finalWidth = maxWidth
            finalHeight = (maxWidth / ratio).toInt()
        } else {
            finalWidth = (maxHeight * ratio).toInt()
            finalHeight = maxHeight
        }
        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
    }

    private fun showEditProfileDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val editName = dialogView.findViewById<EditText>(R.id.editName)
        val editPhone = dialogView.findViewById<EditText>(R.id.editPhone)
        val editStreet1 = dialogView.findViewById<EditText>(R.id.editStreet1)
        val editDob = dialogView.findViewById<EditText>(R.id.editDob)
        val editEmail = dialogView.findViewById<EditText>(R.id.editEmail)
        var editPinCode = dialogView.findViewById<EditText>(R.id.editPinCode)
        val saveButton = dialogView.findViewById<Button>(R.id.saveButton)

        editDob.setOnClickListener { showDatePicker(editDob) }

        editName.setText(userName.text.toString())
        editDob.setText(userBirthday.text.toString()) // Bind Date of Birth
        editPhone.setText(userPhone.text.toString())
        editEmail.setText(userEmail.text.toString())
        editStreet1.setText(userCity.text.toString()) // Bind City
        editPinCode.setText(pin_code_field.text.toString()) // Bind City

        saveButton.setOnClickListener {
            val name = editName.text.toString()
            val dob = editDob.text.toString()
            val phone = editPhone.text.toString()
            val email = editEmail.text.toString()
            val address = editStreet1.text.toString()
            val pincode = editPinCode.text.toString() // Example Pincode; replace this with actual data from your UI
            val base64Image = encodeImageToBase64((profileImage.drawable as BitmapDrawable).bitmap)

            // Show loader
            val progressDialog = createProgressDialog("Saving your profile...")
            progressDialog.show()

            // Simulate saving process
            saveProfileData(name, dob, phone, email, address, pincode, base64Image,dialog)

            // Dismiss loader after saving (You can dismiss after actual server response if needed)
            progressDialog.dismiss()
            loadProfileData()

        }
        dialog.show()
    }

    private fun saveProfileData(
        name: String,
        dob: String,
        phone: String,
        email: String,
        address: String,
        pincode: String,
        base64Image: String,
        editDialog: AlertDialog
    ) {
        var pincodeField = editDialog.findViewById<EditText>(R.id.editPinCode)
        when {
            name.isEmpty() -> {
                showInvalidAlert("Invalid Name", "Please update your name.")
                return
            }
            dob.isEmpty() -> {
                showInvalidAlert("Invalid Date of Birth", "Please update your date of birth.")
                return
            }
            phone.isEmpty() || !phone.matches(Regex("^[6-9]\\d{9}$")) -> {
                showInvalidAlert("Invalid Mobile Number", "Please enter a valid 10-digit mobile number.")
                return
            }
            email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showInvalidAlert("Invalid Email", "Please enter a valid email address.")
                return
            }
            address.isEmpty() -> {
                showInvalidAlert("Invalid Address", "Please update your address.")
                return
            }
            pincode.isEmpty() -> {
                pincodeField?.error = "Pincode cannot be empty."
                return
            }
            !pincode.matches(Regex("^\\d{6}$")) -> {
                pincodeField?.error = "Pincode must be exactly 6 digits."
                return
            }
        }


        // Create and show a loader
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Updating profile...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("userName", name)
            putString("userBirthday", dob)
            putString("userPhone", phone)
            putString("userEmail", email)
            putString("userCity", address)
            putString("userPincode", pincode)
            putString("userImage", base64Image)
            apply()
        }

        // Simulate a delay for demonstration (replace with API call if needed)
        android.os.Handler().postDelayed({
            progressDialog.dismiss() // Dismiss loader
            editDialog.dismiss()     // Dismiss edit profile dialog

            val intent = Intent("PROFILE_UPDATED")
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

            SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText("Profile Updated")
                .setContentText("Your profile has been updated successfully.")
                .setConfirmText("OK")
                .show()
        }, 2000)
    }


    private fun showInvalidAlert(title: String, message: String) {
        SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
            .setTitleText(title)
            .setContentText(message)
            .setConfirmText("OK")
            .show() // User needs to manually dismiss the dialog
    }

    private fun showDatePicker(editDob: EditText) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                editDob.setText(String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun createProgressDialog(message: String): AlertDialog {
        val builder = AlertDialog.Builder(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.custom_progress_dialog, null)
        val messageTextView = dialogView.findViewById<TextView>(R.id.progress_message)
        messageTextView.text = message
        builder.setView(dialogView)
        builder.setCancelable(false)
        return builder.create()
    }
}

package com.raftaar.emergencyy.activities

import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import cn.pedant.SweetAlert.SweetAlertDialog
import com.raftaar.emergencyy.R
import com.raftaar.emergencyy.apiclient.RetrofitClient
import com.raftaar.emergencyy.models.RegisterRequest
import com.raftaar.emergencyy.models.RegisterResponse
import com.google.android.material.textfield.TextInputLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {

    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val nameField: EditText = findViewById(R.id.nameFieldTextInputEditText)
        val phoneField: EditText = findViewById(R.id.mobileFieldTextInputEditText)
        val emailField: EditText = findViewById(R.id.emailFieldTextInputEditText)
        val pinCodeField: EditText = findViewById(R.id.pinCodeField)
        val passwordField: EditText = findViewById(R.id.passwordField)
        val confirmPasswordField: EditText = findViewById(R.id.confirmPasswordField)
        val passwordLayout: TextInputLayout = findViewById(R.id.passwordLayout)
        val confirmPasswordLayout: TextInputLayout = findViewById(R.id.confirmPasswordLayout)
        val registerButton: Button = findViewById(R.id.registerButton)

        initializeProgressDialog()
        setupAnimations()

        passwordLayout.setEndIconOnClickListener {
            togglePasswordVisibility(passwordField, passwordLayout)
        }

        confirmPasswordLayout.setEndIconOnClickListener {
            togglePasswordVisibility(confirmPasswordField, confirmPasswordLayout)
        }

        registerButton.setOnClickListener {
            val name = nameField.text.toString().trim()
            val phone = phoneField.text.toString().trim()
            val email = emailField.text.toString().trim()
            val pinCode = pinCodeField.text.toString().trim()
            val password = passwordField.text.toString().trim()
            val confirmPassword = confirmPasswordField.text.toString().trim()

            if (validateInputs(name, phone, pinCode, email, password, confirmPassword)) {
                registerUser(name, phone, pinCode, email, password, confirmPassword)
            }
        }
    }

    private fun setupAnimations() {
        val floatUpAnimation = AnimationUtils.loadAnimation(this, R.anim.float_up)
        
        // Apply animations to all views
        // Input fields and their containers
        (findViewById<View>(R.id.nameFieldTextInputEditText).parent as? View)?.startAnimation(floatUpAnimation)
        (findViewById<View>(R.id.mobileFieldTextInputEditText).parent as? View)?.startAnimation(floatUpAnimation)
        (findViewById<View>(R.id.emailFieldTextInputEditText).parent as? View)?.startAnimation(floatUpAnimation)
        (findViewById<View>(R.id.pinCodeField).parent as? View)?.startAnimation(floatUpAnimation)
        (findViewById<View>(R.id.passwordField).parent as? View)?.startAnimation(floatUpAnimation)
        (findViewById<View>(R.id.confirmPasswordField).parent as? View)?.startAnimation(floatUpAnimation)
        
        // Register button
        findViewById<View>(R.id.registerButton)?.startAnimation(floatUpAnimation)
    }

    private fun togglePasswordVisibility(field: EditText, layout: TextInputLayout) {
        if (field.inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            field.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            layout.endIconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_eye_open)
        } else {
            field.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            layout.endIconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_eye_closed)
        }
        field.setSelection(field.text?.length ?: 0)
    }

    private fun initializeProgressDialog() {
        progressDialog = ProgressDialog(this).apply {
            setMessage("Registering...")
            setCancelable(false)
        }
    }

    private fun validateInputs(
        name: String,
        phone: String,
        pinCode: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        return when {
            name.isEmpty() -> {
                Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show()
                false
            }
            phone.length != 10 -> {
                Toast.makeText(this, "Invalid Mobile Number", Toast.LENGTH_SHORT).show()
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                Toast.makeText(this, "Invalid Email Address", Toast.LENGTH_SHORT).show()
                false
            }
            password.length < 8 || !password.any { it.isDigit() } || !password.any { it.isLetter() } -> {
                Toast.makeText(this, "Password must be at least 8 characters with letters and numbers", Toast.LENGTH_SHORT).show()
                false
            }
            password != confirmPassword -> {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun registerUser(
        name: String,
        phone: String,
        pinCode: String,
        email: String,
        password: String,
        confirmPassword: String
    ) {
        progressDialog.show()

        val request = RegisterRequest(
            full_name = name,
            phone1 = phone,
            pinCode = pinCode,
            user_name = email,
            password = password,
            password_confirmation = confirmPassword
        )

        RetrofitClient.instance.register(request).enqueue(object : Callback<RegisterResponse> {
            override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                try {
                    progressDialog.dismiss()
                    if (response.isSuccessful) {
                        val registerResponse = response.body()
                        registerResponse?.let {
                            saveLoginState(it.user.unique_id)
                            navigateToMainActivity()
                            Toast.makeText(this@RegisterActivity, "Registration Successful", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        SweetAlertDialog(this@RegisterActivity, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Email Already Exists")
                            .setContentText("The email address you entered is already registered. Please try logging in or use a different email.")
                            .setConfirmText("OK")
                            .show()
                    }
                } catch (e: Exception) {
                    progressDialog.dismiss()
                    Toast.makeText(this@RegisterActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                progressDialog.dismiss()
                Toast.makeText(this@RegisterActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun saveLoginState(uniqueId: String) {
        val sharedPreferences: SharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean("is_logged_in", true) // Indicate user is logged in
            putString("unique_id", uniqueId) // Save unique ID for future reference
            putString("user_id", uniqueId) // Save user ID as well (if separate)
            apply()
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear back stack
        startActivity(intent)
        finish()
    }
}

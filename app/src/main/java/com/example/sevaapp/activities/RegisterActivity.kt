package com.example.sevaapp.activities

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.sevaapp.R
import com.example.sevaapp.apiinterface.RetrofitClient
import com.example.sevaapp.models.CheckEmailResponse
import com.example.sevaapp.models.RegisterRequest
import com.example.sevaapp.models.RegisterResponse
import com.google.android.material.textfield.TextInputLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {

    private lateinit var progressDialog: ProgressDialog
    private lateinit var passwordField: EditText
    private lateinit var confirmPasswordField: EditText
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var confirmPasswordLayout: TextInputLayout
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val nameField: EditText = findViewById(R.id.nameFieldTextInputEditText)
        val mobileField: EditText = findViewById(R.id.mobileFieldTextInputEditText)
        val emailField: EditText = findViewById(R.id.emailFieldTextInputEditText)
        val registerButton: Button = findViewById(R.id.registerButton)

        passwordField = findViewById(R.id.passwordField)
        confirmPasswordField = findViewById(R.id.confirmPasswordField)
        passwordLayout = findViewById(R.id.passwordLayout)
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout)

        initializeProgressDialog()

        registerButton.setOnClickListener {
            val name = nameField.text.toString().trim()
            val mobile = mobileField.text.toString().trim()
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()
            val confirmPassword = confirmPasswordField.text.toString().trim()

            if (name.isEmpty()) {
                nameField.error = "Name is required"
                return@setOnClickListener
            }
            if (!isValidEmail(email)) {
                emailField.error = "Invalid Email"
                return@setOnClickListener
            }

            emailField.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) { // Trigger API call when focus is lost
                    val email = emailField.text.toString().trim()
                    if (email.isNotEmpty() && isValidEmail(email)) {
                        checkEmailExists(email, emailField)
                    }
                }
            }

            if (mobile.isEmpty() || !isValidMobileNumber(mobile)) {
                mobileField.error = "Invalid Mobile Number"
                return@setOnClickListener
            }

            if (!isValidPassword(password)) {
                passwordField.error = "Password must be at least 8 characters long, with a mix of letters and numbers"
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                confirmPasswordField.error = "Passwords do not match"
                return@setOnClickListener
            }

            registerUser(name, mobile, email, password, confirmPassword)
        }
        // Toggle password visibility
        passwordLayout.setEndIconOnClickListener {
            togglePasswordVisibility(passwordField, passwordLayout)
        }

        // Toggle confirm password visibility
        confirmPasswordLayout.setEndIconOnClickListener {
            togglePasswordVisibility(confirmPasswordField, confirmPasswordLayout)
        }
    }

    private fun togglePasswordVisibility(field: EditText, layout: TextInputLayout) {
        if (field.inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            // Show password
            field.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            layout.endIconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_eye_open)
        } else {
            // Hide password
            field.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            layout.endIconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_eye_closed)
        }
        // Move cursor to the end
        field.setSelection(field.text?.length ?: 0)
    }

    private fun checkEmailExists(email: String, emailField: EditText) {
        RetrofitClient.instance.checkEmailExists(email).enqueue(object : Callback<CheckEmailResponse> {
            override fun onResponse(call: Call<CheckEmailResponse>, response: Response<CheckEmailResponse>) {
                if (response.isSuccessful && response.body()?.exists == true) {
                    emailField.error = "Email already exists"
                }
            }

            override fun onFailure(call: Call<CheckEmailResponse>, t: Throwable) {
                Toast.makeText(this@RegisterActivity, "Unable to verify email: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun initializeProgressDialog() {
        progressDialog = ProgressDialog(this).apply {
            setMessage("Registering...")
            setCancelable(false)
        }
    }

    private fun registerUser(name: String, mobile: String, email: String, password: String, confirmPassword: String) {
        progressDialog.show()

        val registerRequest = RegisterRequest(name, mobile, email, password, confirmPassword)
        RetrofitClient.instance.register(registerRequest).enqueue(object : Callback<RegisterResponse> {
            override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                progressDialog.dismiss()
                if (response.isSuccessful) {
                    val registerResponse = response.body()
                    Toast.makeText(this@RegisterActivity, "Registration Successfull", Toast.LENGTH_SHORT).show()
                    checkLocationPermission()
                }else {
                    SweetAlertDialog(this@RegisterActivity, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Registration Failed")
                        .setContentText("Unable to register. Email is already registered")
                        .setConfirmText("OK")
                        .show()
                }
            }

            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                progressDialog.dismiss()
                SweetAlertDialog(this@RegisterActivity, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Error")
                    .setContentText(t.message ?: "An unknown error occurred.")
                    .setConfirmText("OK")
                    .show()
            }
        })
    }

    private fun isValidMobileNumber(mobile: String): Boolean {
        return mobile.length == 10 && mobile.all { it.isDigit() }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // If location is on, navigate to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            // If location is off, navigate to LocationPermissionActivity
            startActivity(Intent(this, LocationPermissionActivity::class.java))
            finish()
        }
    }
    private fun isValidPassword(password: String): Boolean {
        return password.length >= 8 && password.any { it.isDigit() } && password.any { it.isLetter() }
    }
}

package com.example.sevaapp.activities

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sevaapp.R
import com.example.sevaapp.models.LoginRequest
import com.example.sevaapp.models.LoginResponse
import com.example.sevaapp.apiinterface.RetrofitClient
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.widget.Button
import androidx.core.content.ContextCompat
import cn.pedant.SweetAlert.SweetAlertDialog
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var progressDialog: ProgressDialog
    private lateinit var passwordLayout: TextInputLayout
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        initializeViews()
        initializeProgressDialog()
        loginButton.setOnClickListener { handleLogin() }
        registerButton.setOnClickListener { navigateToRegister() }

        passwordEditText = findViewById(R.id.passwordEditText)
        passwordLayout = findViewById(R.id.passwordLayout)

        passwordLayout.setEndIconOnClickListener {
            togglePasswordVisibility()
        }
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        if (isPasswordVisible) {
            // Show password
            passwordEditText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            passwordLayout.endIconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_eye_open)
        } else {
            // Hide password
            passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            passwordLayout.endIconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_eye_closed)
        }

        // Move cursor to the end of the text
        passwordEditText.setSelection(passwordEditText.text?.length ?: 0)
    }

    private fun initializeViews() {
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        registerButton = findViewById(R.id.register)
    }

    private fun initializeProgressDialog() {
        progressDialog = ProgressDialog(this).apply {
            setMessage(getString(R.string.logging_in))
            setCancelable(false) // Prevent dismissal while loading
        }
    }

    private fun handleLogin() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (!validateInputs(email, password)) return

        progressDialog.show()
        if (isNetworkAvailable()) {
            performOnlineLogin(email, password)
        } else {
            handleOfflineLogin(email, password)
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        return when {
            email.isEmpty() -> {
                emailEditText.error = getString(R.string.email_cannot_be_empty)
                emailEditText.requestFocus()
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                emailEditText.error = getString(R.string.invalid_email_format)
                emailEditText.requestFocus()
                false
            }
            password.isEmpty() -> {
                passwordEditText.error = getString(R.string.password_cannot_be_empty)
                passwordEditText.requestFocus()
                false
            }
            password.length < 8 || !password.any { it.isDigit() } || !password.any { it.isLetter() } -> {
                passwordEditText.error =
                    getString(R.string.password_must_be_at_least_8_characters_with_letters_and_numbers)
                passwordEditText.requestFocus()
                false
            }
            else -> true
        }
    }

    private fun performOnlineLogin(phone: String, password: String) {
        val loginRequest = LoginRequest(phone, password)
        RetrofitClient.instance.login(loginRequest).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                progressDialog.dismiss()
                if (response.isSuccessful) {
                    val loginResponse = response.body()

                    saveLoginDetails(
                        token = loginResponse?.access_token.orEmpty(),
                        userId = loginResponse?.userId.orEmpty()
                    )
                    Toast.makeText(this@LoginActivity, "Login Successful", Toast.LENGTH_SHORT).show()
                    checkLocationAndNavigate()
                } else {
                    SweetAlertDialog(this@LoginActivity, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Login Failed")
                        .setContentText("Invalid credentials or account issue.")
                        .setConfirmText("Retry")
                        .show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                progressDialog.dismiss()

                // Show error dialog for network or server failure
                SweetAlertDialog(this@LoginActivity, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Network Error")
                    .setContentText("Please check your internet connection")
                    .setConfirmText("Retry")
                    .show()
            }
        })
    }

    private fun saveLoginDetails(token: String, userId: String) {
        val sharedPreferences = getSharedPreferences(getString(R.string.appprefs), MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean("isLoggedIn", true)
            putString("auth_token", token)
            putString("userId", userId) // Save the user ID here
            apply()
        }
    }

    private fun handleOfflineLogin(phone: String, password: String) {
        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val savedPhone = sharedPreferences.getString(getString(R.string.saved_phone), null)
        val savedPassword = sharedPreferences.getString("saved_password", null)

        progressDialog.dismiss()

        if (phone == savedPhone && password == savedPassword) {
            Toast.makeText(this, getString(R.string.login_successful), Toast.LENGTH_SHORT).show()
            navigateToHome()
        } else {
            Toast.makeText(this, getString(R.string.invalid_credentials), Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkLocationAndNavigate() {
        if (isLocationEnabled()) {
            navigateToHome()
        } else {
            navigateToLocationPermission()
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork?.isConnected == true
    }

    private fun navigateToHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun navigateToLocationPermission() {
        startActivity(Intent(this, LocationPermissionActivity::class.java))
        finish()
    }

    private fun navigateToRegister() {
        startActivity(Intent(this, RegisterActivity::class.java))
    }
}

package com.raftaar.emergencyy.activities

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.raftaar.emergencyy.R
import com.raftaar.emergencyy.models.LoginRequest
import com.raftaar.emergencyy.models.LoginResponse
import com.raftaar.emergencyy.apiclient.RetrofitClient
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import cn.pedant.SweetAlert.SweetAlertDialog
import com.raftaar.emergencyy.models.ProfileResponse
import com.google.android.material.textfield.TextInputLayout
import android.view.animation.AnimationUtils
import android.view.View

class LoginActivity : AppCompatActivity() {

    private lateinit var forgetPasswordTextView: TextView
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
        setupAnimations()
        loginButton.setOnClickListener { handleLogin() }
        registerButton.setOnClickListener { navigateToRegister() }
        forgetPasswordTextView.setOnClickListener { navigateToForgetPassword() }
        passwordEditText = findViewById(R.id.passwordEditText)
        passwordLayout = findViewById(R.id.passwordLayout)

        passwordLayout.setEndIconOnClickListener {
            togglePasswordVisibility()
        }
    }

    private fun setupAnimations() {
        val zoomOutAnimation = AnimationUtils.loadAnimation(this, R.anim.zoom_out)
        
        // Apply animations to all views
        // Title and subtitle
        findViewById<View>(R.id.textView)?.startAnimation(zoomOutAnimation)
        
        // Input fields and their containers
        (findViewById<View>(R.id.emailEditText).parent as? View)?.startAnimation(zoomOutAnimation)
        (findViewById<View>(R.id.passwordEditText).parent as? View)?.startAnimation(zoomOutAnimation)
        // Buttons and text
        findViewById<View>(R.id.forgetPasswordTextView)?.startAnimation(zoomOutAnimation)
        findViewById<View>(R.id.loginButton)?.startAnimation(zoomOutAnimation)
        findViewById<View>(R.id.register)?.startAnimation(zoomOutAnimation)
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
        forgetPasswordTextView = findViewById(R.id.forgetPasswordTextView)
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

        if (isNetworkAvailable()) {
            progressDialog.show()
            performOnlineLogin(email, password)
        } else {
            showErrorDialog("No Internet Connection","Please Connect to Internet")
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
//            password.length < 8 || !password.any { it.isDigit() } || !password.any { it.isLetter() } -> {
//                passwordEditText.error =
//                    getString(R.string.password_must_be_at_least_8_characters_with_letters_and_numbers)
//                passwordEditText.requestFocus()
//                false
//            }
            else -> true
        }
    }

    private fun performOnlineLogin(username: String, password: String) {
        val loginRequest = LoginRequest(user_name = username, password = password)

        RetrofitClient.instance.login(loginRequest).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                try {
                    progressDialog.dismiss()
                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        loginResponse?.let {
                            saveLoginDetails(it.unique_id) // Save unique ID
                            Toast.makeText(this@LoginActivity, it.message, Toast.LENGTH_SHORT).show()
                            fetchUserProfile(it.unique_id) // Fetch profile
                        }
                    } else {
                        showErrorDialog("Login Failed", "Invalid credentials or server issue.")
                    }
                } catch (e: Exception) {
                    progressDialog.dismiss()
                    showErrorDialog("Error", "An unexpected error occurred: ${e.message}")
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                progressDialog.dismiss()
                showErrorDialog("Network Error", "Unable to reach the server. Please try again.")
            }
        })
    }

    private fun fetchUserProfile(uniqueId: String) {
        RetrofitClient.instance.getProfile(uniqueId).enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                try {
                    if (response.isSuccessful) {
                        val profile = response.body()
                        profile?.let {
                            navigateToHome() // Navigate to home screen
                        }
                        saveLoginState(uniqueId) // Save the unique ID after successful profile fetch
                    } else {
                        Toast.makeText(this@LoginActivity, "Failed to fetch profile", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "Network Error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveLoginState(uniqueId: String) {
        val sharedPreferences: SharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean("is_logged_in", true)
            putString("unique_id", uniqueId)
            apply()
        }
    }

    private fun showErrorDialog(title: String, message: String) {
        SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
            .setTitleText(title)
            .setContentText(message)
            .setConfirmText("Retry")
            .show()
    }

    private fun saveLoginDetails(userId: String) {
        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean("is_logged_in", true) // Mark user as logged in
            putString("user_id", userId) // Save the user ID
            apply()
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
        startActivity(Intent(this, MainActivity ::class.java))
        finish()
    }

    private fun navigateToRegister() {
        startActivity(Intent(this, RegisterActivity::class.java))
    }

    private fun navigateToForgetPassword() {
        startActivity(Intent(this, ForgetPasswordActivity::class.java))
    }
}

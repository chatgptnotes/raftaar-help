package com.raftaar.emergencyy.activities

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputLayout
import com.raftaar.emergencyy.R
import com.raftaar.emergencyy.apiclient.RetrofitClient
import com.raftaar.emergencyy.models.ResetPasswordRequest
import com.raftaar.emergencyy.models.ResetPasswordResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var newPasswordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var btnResetPassword: Button
    private lateinit var newPasswordLayout: TextInputLayout
    private lateinit var confirmPasswordLayout: TextInputLayout
    private lateinit var progressDialog: ProgressDialog

    private var isNewPasswordVisible = false
    private var isConfirmPasswordVisible = false
    private var mobileNumber: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.newpass_screen)

        // Initialize UI elements
        newPasswordEditText = findViewById(R.id.etNewPassword)
        confirmPasswordEditText = findViewById(R.id.etConfirmPassword)
        btnResetPassword = findViewById(R.id.btnSetPassword)
        newPasswordLayout = findViewById(R.id.newPasswordLayout)
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout)

        // Initialize ProgressDialog
        initializeProgressDialog()

        // Get mobile number from intent
        mobileNumber = intent.getStringExtra("MOBILE_NUMBER")

        // Toggle Password Visibility Listeners
        newPasswordLayout.setEndIconOnClickListener { togglePasswordVisibility(newPasswordEditText, newPasswordLayout, true) }
        confirmPasswordLayout.setEndIconOnClickListener { togglePasswordVisibility(confirmPasswordEditText, confirmPasswordLayout, false) }

        // Reset Password Button Click
        btnResetPassword.setOnClickListener {
            val newPassword = newPasswordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Password fields cannot be empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters long.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "New Password and Confirm Password do not match.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            resetPassword(mobileNumber ?: "", newPassword, confirmPassword)
        }
    }

    private fun initializeProgressDialog() {
        progressDialog = ProgressDialog(this).apply {
            setMessage("Resetting Password, please wait...")
            setCancelable(false) // Prevent dismissal while loading
        }
    }

    private fun togglePasswordVisibility(editText: EditText, layout: TextInputLayout, isNewPassword: Boolean) {
        val isPasswordVisible = if (isNewPassword) {
            isNewPasswordVisible = !isNewPasswordVisible
            isNewPasswordVisible
        } else {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            isConfirmPasswordVisible
        }

        if (isPasswordVisible) {
            // Show password
            editText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            layout.endIconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_eye_open)
        } else {
            // Hide password
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            layout.endIconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_eye_closed)
        }

        // Move cursor to the end of the text
        editText.setSelection(editText.text?.length ?: 0)
    }

    private fun resetPassword(phone: String, password: String, confirmPassword: String) {
        val request = ResetPasswordRequest(phone, password, confirmPassword)

        // Show ProgressDialog
        progressDialog.show()
        btnResetPassword.isEnabled = false

        RetrofitClient.instance.resetPassword(request).enqueue(object : Callback<ResetPasswordResponse> {
            override fun onResponse(call: Call<ResetPasswordResponse>, response: Response<ResetPasswordResponse>) {
                try {
                    progressDialog.dismiss()
                    btnResetPassword.isEnabled = true

                    if (response.isSuccessful) {
                        Toast.makeText(this@ResetPasswordActivity, "Password reset successfully!", Toast.LENGTH_LONG).show()

                        // Redirect to Login Screen
                        val intent = Intent(this@ResetPasswordActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                        Toast.makeText(this@ResetPasswordActivity, "Failed to reset password. Try again.", Toast.LENGTH_SHORT).show()
                        Log.e("ResetPasswordActivity", "Error: $errorMessage")
                    }
                } catch (e: Exception) {
                    progressDialog.dismiss()
                    btnResetPassword.isEnabled = true
                    Log.e("ResetPasswordActivity", "Error processing response: ${e.message}", e)
                    Toast.makeText(this@ResetPasswordActivity, "Error resetting password: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ResetPasswordResponse>, t: Throwable) {
                progressDialog.dismiss()
                btnResetPassword.isEnabled = true
                Log.e("ResetPasswordActivity", "Network Error: ${t.message}", t)
                Toast.makeText(this@ResetPasswordActivity, "Network Error: Please check your internet connection.", Toast.LENGTH_SHORT).show()
            }
        })
    }

}

package com.raftaar.emergencyy.activities

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.raftaar.emergencyy.apiclient.RetrofitClient
import com.raftaar.emergencyy.models.ForgetPasswordRequest
import com.raftaar.emergencyy.models.ForgetPasswordResponse
import com.raftaar.emergencyy.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ForgetPasswordActivity : AppCompatActivity() {

    private lateinit var mobileEditText: EditText
    private lateinit var btnSendOtp: Button
    private lateinit var tvBackToLogin: TextView
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.forgetp)

        // Initialize UI elements
        mobileEditText = findViewById(R.id.etMobileNumber)
        btnSendOtp = findViewById(R.id.btnResetPassword)
        tvBackToLogin = findViewById(R.id.tvBackToLogin)

        // Initialize ProgressDialog
        initializeProgressDialog()

        tvBackToLogin.setOnClickListener {
            onBackPressed()
        }

        // Send OTP Button Click
        btnSendOtp.setOnClickListener {
            val mobileNumber = mobileEditText.text.toString().trim()

            if (mobileNumber.isEmpty() || mobileNumber.length != 10) {
                Toast.makeText(this, "Please enter a valid 10-digit mobile number.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendOtpRequest(mobileNumber)
        }
    }

    private fun initializeProgressDialog() {
        progressDialog = ProgressDialog(this).apply {
            setMessage("Sending OTP, please wait...")
            setCancelable(false) // Prevent dismissal while loading
        }
    }

    private fun sendOtpRequest(mobileNumber: String) {
        val request = ForgetPasswordRequest(mobileNumber)

        // Show ProgressDialog
        progressDialog.show()
        btnSendOtp.isEnabled = false

        // Calling the API
        RetrofitClient.instance.forgetPassword(request).enqueue(object : Callback<ForgetPasswordResponse> {
            override fun onResponse(call: Call<ForgetPasswordResponse>, response: Response<ForgetPasswordResponse>) {
                try {
                    progressDialog.dismiss()
                    btnSendOtp.isEnabled = true

                    if (response.isSuccessful) {
                        val result = response.body()
                        if (result != null) {
                            Toast.makeText(this@ForgetPasswordActivity, "OTP sent successfully to your WhatsApp number", Toast.LENGTH_LONG).show()
                            // Redirect to OTP Verification Screen
                            val intent = Intent(this@ForgetPasswordActivity, OtpVerificationActivity::class.java)
                            intent.putExtra("MOBILE_NUMBER", mobileNumber)
                            startActivity(intent)
                        }
                    } else {
                        Toast.makeText(this@ForgetPasswordActivity, "Failed to send OTP. Try again.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    progressDialog.dismiss()
                    btnSendOtp.isEnabled = true
                    Toast.makeText(this@ForgetPasswordActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ForgetPasswordResponse>, t: Throwable) {
                progressDialog.dismiss()
                btnSendOtp.isEnabled = true
                Toast.makeText(this@ForgetPasswordActivity, "Network Error: Please check your internet connection.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
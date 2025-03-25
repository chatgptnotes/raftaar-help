package com.emergency.sevaapp.activities

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.emergency.sevaapp.R
import com.emergency.sevaapp.apiclient.RetrofitClient
import com.emergency.sevaapp.models.ForgetPasswordRequest
import com.emergency.sevaapp.models.ForgetPasswordResponse
import com.emergency.sevaapp.models.OtpVerificationRequest
import com.emergency.sevaapp.models.OtpVerificationResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OtpVerificationActivity : AppCompatActivity() {

    private lateinit var otpBox1: EditText
    private lateinit var otpBox2: EditText
    private lateinit var otpBox3: EditText
    private lateinit var otpBox4: EditText
    private lateinit var btnVerifyOtp: Button
    private lateinit var tvResendOtp: TextView
    private lateinit var progressDialog: ProgressDialog
    // Declare this variable at the class level
    private lateinit var countDownTimer: CountDownTimer
    private val timerDuration: Long = 30000 // 30 seconds
    private var mobileNumber: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp_verification)

        // Initialize UI elements
        otpBox1 = findViewById(R.id.otpBox1)
        otpBox2 = findViewById(R.id.otpBox2)
        otpBox3 = findViewById(R.id.otpBox3)
        otpBox4 = findViewById(R.id.otpBox4)
        btnVerifyOtp = findViewById(R.id.btnContinue)
        tvResendOtp = findViewById(R.id.tvResendOtp)

        // Initialize ProgressDialog
        initializeProgressDialog()

        // Start countdown timer when activity is created
        startResendOtpTimer()
        // Get mobile number from intent
        mobileNumber = intent.getStringExtra("MOBILE_NUMBER")
        if (mobileNumber.isNullOrEmpty()) {
            Toast.makeText(this, "Mobile number is missing", Toast.LENGTH_SHORT).show()
        } else {
            Log.d("OtpVerification", "Received mobile number: $mobileNumber")
        }

        // Set up TextWatchers for OTP inputs
        setupOtpInputs()

        // Verify OTP Button Click
        btnVerifyOtp.setOnClickListener {
            val enteredOtp = otpBox1.text.toString() +
                    otpBox2.text.toString() +
                    otpBox3.text.toString() +
                    otpBox4.text.toString()

            if (enteredOtp.length != 4) {
                Toast.makeText(this, "Please enter a valid 4-digit OTP.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            verifyOtp(mobileNumber ?: "", enteredOtp)
        }

        // Resend OTP Button Click
        tvResendOtp.setOnClickListener {
            resendOtp()
        }
    }

    private fun resendOtp(){
        startResendOtpTimer() // Restart the timer when user clicks Resend OTP

        val formattedMobile = "$mobileNumber"
        val request = ForgetPasswordRequest(formattedMobile)

        // Show ProgressDialog
        progressDialog.show()
        tvResendOtp.isEnabled = false

        // Calling the API
        RetrofitClient.instance.forgetPassword(request).enqueue(object : Callback<ForgetPasswordResponse> {
            override fun onResponse(call: Call<ForgetPasswordResponse>, response: Response<ForgetPasswordResponse>) {
                try {
                    progressDialog.dismiss()

                    Log.d("OtpVerification", "API Response: ${response.body()}")

                    if (response.isSuccessful) {
                        Toast.makeText(this@OtpVerificationActivity, "OTP sent successfully to your WhatsApp number", Toast.LENGTH_LONG).show()
                    } else {
                        val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                        Toast.makeText(this@OtpVerificationActivity, "Failed to send OTP. Try again.", Toast.LENGTH_SHORT).show()
                        Log.e("API Error", errorMessage)
                        tvResendOtp.isEnabled = true // Enable button if failed
                    }
                } catch (e: Exception) {
                    progressDialog.dismiss()
                    Log.e("OtpVerification", "Error in onResponse: ${e.message}", e)
                    Toast.makeText(this@OtpVerificationActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ForgetPasswordResponse>, t: Throwable) {
                progressDialog.dismiss()
                tvResendOtp.isEnabled = true
                Log.e("OtpVerification", "Network Error: ${t.message}", t)
                Toast.makeText(this@OtpVerificationActivity, "Network Error: Please check your internet connection.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun startResendOtpTimer() {
        tvResendOtp.isEnabled = false // Disable the button initially

        countDownTimer = object : CountDownTimer(timerDuration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                tvResendOtp.text = "Resend OTP in $secondsRemaining sec" // Show countdown text
            }

            override fun onFinish() {
                tvResendOtp.isEnabled = true // Enable the button after 30 sec
                tvResendOtp.text = "Resend OTP" // Reset text
            }
        }
        countDownTimer.start()
    }

    private fun initializeProgressDialog() {
        progressDialog = ProgressDialog(this).apply {
            setMessage("Processing, please wait...")
            setCancelable(false)
        }
    }

    private fun verifyOtp(phone: String, otp: String) {
        val request = OtpVerificationRequest(phone, otp)

        // Show ProgressDialog
        progressDialog.show()
        btnVerifyOtp.isEnabled = false

        RetrofitClient.instance.verifyOtp(request)
            .enqueue(object : Callback<OtpVerificationResponse> {
                override fun onResponse(
                    call: Call<OtpVerificationResponse>,
                    response: Response<OtpVerificationResponse>
                ) {
                    try {
                        progressDialog.dismiss()
                        btnVerifyOtp.isEnabled = true

                        if (response.isSuccessful) {
                            val result = response.body()
                            if (result != null) {
                                Toast.makeText(
                                    this@OtpVerificationActivity,
                                    "OTP verified successfully!",
                                    Toast.LENGTH_LONG
                                ).show()

                                // Redirect to Reset Password Screen
                                val intent = Intent(
                                    this@OtpVerificationActivity,
                                    ResetPasswordActivity::class.java
                                )
                                intent.putExtra("MOBILE_NUMBER", phone)
                                startActivity(intent)
                                finish()
                            }
                        } else {
                            Toast.makeText(
                                this@OtpVerificationActivity,
                                "Incorrect OTP. Please try again.",
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.e("OTP Verification", "Error: Incorrect OTP")
                        }
                    } catch (e: Exception) {
                        progressDialog.dismiss()
                        btnVerifyOtp.isEnabled = true
                        Log.e("OtpVerification", "Error in onResponse: ${e.message}", e)
                        Toast.makeText(
                            this@OtpVerificationActivity,
                            "Error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<OtpVerificationResponse>, t: Throwable) {
                    progressDialog.dismiss()
                    btnVerifyOtp.isEnabled = true
                    Log.e("OtpVerification", "Network Error: ${t.message}", t)
                    Toast.makeText(
                        this@OtpVerificationActivity,
                        "Network Error: Please check your internet connection.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun setupOtpInputs() {
        val otpBoxes = listOf(otpBox1, otpBox2, otpBox3, otpBox4)
        for (i in otpBoxes.indices) {
            otpBoxes[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1) {
                        if (i < otpBoxes.size - 1) otpBoxes[i + 1].requestFocus() // Move to next OTP box
                    } else if (s?.isEmpty() == true) {
                        if (i > 0) otpBoxes[i - 1].requestFocus() // Move to previous OTP box on delete
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }
}

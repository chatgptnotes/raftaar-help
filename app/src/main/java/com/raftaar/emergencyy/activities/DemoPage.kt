package com.raftaar.emergencyy.activities

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputEditText
import com.raftaar.emergencyy.R

class DemoPage : AppCompatActivity() {
    private lateinit var nameField: TextInputEditText
    private lateinit var mobileField: TextInputEditText
    private lateinit var emailField: TextInputEditText
    private lateinit var passwordField: TextInputEditText
    private lateinit var confirmPasswordField: TextInputEditText
    private lateinit var registerButton: View
    private lateinit var progressBar: View
    private lateinit var loginText: View
    private lateinit var logoImage: View
    private lateinit var titleText: View
    private lateinit var subtitleText: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo)

        initializeViews()
        setupAnimations()
        setupInputValidation()
        setupRegisterButton()
        setupLoginText()
    }

    private fun initializeViews() {
        nameField = findViewById(R.id.nameFieldTextInputEditText)
        mobileField = findViewById(R.id.mobileFieldTextInputEditText)
        emailField = findViewById(R.id.emailFieldTextInputEditText)
        passwordField = findViewById(R.id.passwordField)
        confirmPasswordField = findViewById(R.id.confirmPasswordField)
        registerButton = findViewById(R.id.registerButton)
        progressBar = findViewById(R.id.progressBar)
        loginText = findViewById(R.id.loginText)

        titleText = findViewById(R.id.titleText)
        subtitleText = findViewById(R.id.subtitleText)
    }

    private fun setupAnimations() {
        // Logo animation
        val logoScaleX = ObjectAnimator.ofFloat(logoImage, "scaleX", 0f, 1f)
        val logoScaleY = ObjectAnimator.ofFloat(logoImage, "scaleY", 0f, 1f)
        val logoAlpha = ObjectAnimator.ofFloat(logoImage, "alpha", 0f, 1f)
        
        val logoAnimator = AnimatorSet().apply {
            playTogether(logoScaleX, logoScaleY, logoAlpha)
            duration = 1000
            interpolator = AccelerateDecelerateInterpolator()
        }

        // Title and subtitle animation
        val titleAlpha = ObjectAnimator.ofFloat(titleText, "alpha", 0f, 1f)
        val titleTranslation = ObjectAnimator.ofFloat(titleText, "translationY", 50f, 0f)
        
        val subtitleAlpha = ObjectAnimator.ofFloat(subtitleText, "alpha", 0f, 1f)
        val subtitleTranslation = ObjectAnimator.ofFloat(subtitleText, "translationY", 50f, 0f)

        val titleAnimator = AnimatorSet().apply {
            playTogether(titleAlpha, titleTranslation)
            duration = 500
            startDelay = 500
            interpolator = AccelerateDecelerateInterpolator()
        }

        val subtitleAnimator = AnimatorSet().apply {
            playTogether(subtitleAlpha, subtitleTranslation)
            duration = 500
            startDelay = 700
            interpolator = AccelerateDecelerateInterpolator()
        }

        // Input fields animation
        val inputFields = listOf(
            nameField,
            mobileField,
            emailField,
            passwordField,
            confirmPasswordField
        )

        inputFields.forEachIndexed { index, field ->
            field.alpha = 0f
            field.translationY = 50f
            field.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setStartDelay(1000L + (index * 100L))
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }

        // Button animation
        registerButton.alpha = 0f
        registerButton.animate()
            .alpha(1f)
            .setDuration(300)
            .setStartDelay(1500L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Start animations
        logoAnimator.start()
        titleAnimator.start()
        subtitleAnimator.start()
    }

    private fun setupInputValidation() {
        nameField.addTextChangedListener {
            validateName(it.toString())
        }

        mobileField.addTextChangedListener {
            validateMobile(it.toString())
        }

        emailField.addTextChangedListener {
            validateEmail(it.toString())
        }
        
        passwordField.addTextChangedListener {
            validatePassword(it.toString())
        }

        confirmPasswordField.addTextChangedListener {
            validateConfirmPassword(it.toString())
        }
    }

    private fun validateName(name: String): Boolean {
        return if (name.length < 3) {
            nameField.error = "Name must be at least 3 characters"
            false
        } else {
            nameField.error = null
            true
        }
    }

    private fun validateMobile(mobile: String): Boolean {
        return if (mobile.length != 10 || !mobile.all { it.isDigit() }) {
            mobileField.error = "Enter a valid 10-digit mobile number"
            false
        } else {
            mobileField.error = null
            true
        }
    }

    private fun validateEmail(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return if (!email.matches(emailPattern.toRegex())) {
            emailField.error = "Enter a valid email address"
            false
        } else {
            emailField.error = null
            true
        }
    }

    private fun validatePassword(password: String): Boolean {
        return if (password.length < 6) {
            passwordField.error = "Password must be at least 6 characters"
            false
        } else {
            passwordField.error = null
            true
        }
    }

    private fun validateConfirmPassword(confirmPassword: String): Boolean {
        val password = passwordField.text.toString()
        return if (confirmPassword != password) {
            confirmPasswordField.error = "Passwords do not match"
            false
        } else {
            confirmPasswordField.error = null
            true
        }
    }

    private fun setupRegisterButton() {
        registerButton.setOnClickListener {
            if (validateAllFields()) {
                showLoading(true)
                // Simulate registration process
                registerButton.postDelayed({
                    showLoading(false)
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                    // Add your registration logic here
                }, 2000)
            }
        }
    }

    private fun setupLoginText() {
        loginText.setOnClickListener {
            // Add navigation to login screen
            Toast.makeText(this, "Navigate to login screen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateAllFields(): Boolean {
        val name = nameField.text.toString()
        val mobile = mobileField.text.toString()
        val email = emailField.text.toString()
        val password = passwordField.text.toString()
        val confirmPassword = confirmPasswordField.text.toString()

        return validateName(name) &&
                validateMobile(mobile) &&
                validateEmail(email) &&
                validatePassword(password) &&
                validateConfirmPassword(confirmPassword)
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        registerButton.isEnabled = !show
    }
}
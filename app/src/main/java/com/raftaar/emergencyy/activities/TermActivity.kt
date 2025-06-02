package com.raftaar.emergencyy.activities

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.raftaar.emergencyy.R

class TermActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var noInternetLayout: View // Declare no internet layout here
    private lateinit var backButton: ImageView // Declare back button
    private lateinit var titleTextView: TextView // Declare title text view

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.web_page_activity)

        // Initialize Views
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        noInternetLayout = findViewById(R.id.no_internet_layout) // Initialize No Internet layout
        backButton = findViewById(R.id.back_button) // Initialize back button
        titleTextView = findViewById(R.id.title_text) // Initialize title text view

        // Apply zoom in animation to WebView
        val zoomInAnimation = AnimationUtils.loadAnimation(this, R.anim.zoom_in_animation)
        webView.startAnimation(zoomInAnimation)

        backButton.setOnClickListener {
            // Log to check if the button is clicked
            Log.d("TermActivity", "Back button clicked")

            // Start LoginActivity and clear previous activities in the stack
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            // Finish the current activity to prevent going back
            finish()
        }

        // Set up the back button functionality
        backButton.setOnClickListener {
            // Log to check if the button is clicked
            Log.d("TermActivity", "Back button clicked")
            // Start LoginActivity and clear previous activities in the stack
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            // Finish the current activity to prevent going back
            finish()
        }

        // Retrieve the title passed through the Intent
        val title = intent.getStringExtra("PAGE_TITLE")
        // Set the title dynamically
        if (title != null) {
            titleTextView.text = title
        } else {
            titleTextView.text = "Terms & Conditions"  // Fallback to a default title if no title is passed
        }

        // Check Internet Connectivity
        if (!isInternetAvailable()) {
            showNoInternetScreen()
        } else {
            setupWebView()
        }
    }

    private fun setupWebView() {
        // Show loader initially
        progressBar.visibility = View.VISIBLE

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                progressBar.visibility = View.VISIBLE // Show ProgressBar when loading starts
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE // Hide ProgressBar when loading finishes
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress < 100) {
                    progressBar.visibility = View.VISIBLE
                } else {
                    progressBar.visibility = View.GONE
                }
            }
        }

        // Enable JavaScript and Zoom Support
        webView.settings.javaScriptEnabled = true
        webView.settings.setSupportZoom(true)

        // Load the page based on the title (example: Terms & Conditions or Privacy Policy)
        if (title == "Privacy Policy") {
            webView.loadUrl("https://admin.emergencyseva.in/privacy-policy")
        } else {
            webView.loadUrl("https://admin.emergencyseva.in/term-and-condition")
        }
    }

    private fun showNoInternetScreen() {
        progressBar.visibility = View.GONE
        noInternetLayout.visibility = View.VISIBLE  // Show the No Internet Layout
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}


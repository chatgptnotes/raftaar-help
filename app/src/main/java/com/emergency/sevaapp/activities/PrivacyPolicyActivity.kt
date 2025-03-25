package com.emergency.sevaapp.activities

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import com.emergency.sevaapp.R

@Suppress("DEPRECATION")
//class PrivacyPolicyActivity : AppCompatActivity() {
//
//    private lateinit var webView: WebView
//    private lateinit var progressBar: ProgressBar
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.web_page_activity)
//
//        // Initialize Views
//        webView = findViewById(R.id.webView)
//        webView.clearCache(true)
//        webView.clearHistory()
//        progressBar = findViewById(R.id.progressBar)
//
//        // Check Internet Connectivity
//        if (!isInternetAvailable()) {
//            showNoInternetDialog()
//        } else {
//            setupWebView()
//        }
//    }
//
//    private fun setupWebView() {
//        // Show loader initially
//        progressBar.visibility = View.VISIBLE
//
//        webView.webViewClient = object : WebViewClient() {
//            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
//                progressBar.visibility = View.VISIBLE
//            }
//
//            override fun onPageFinished(view: WebView?, url: String?) {
//                progressBar.visibility = View.GONE
//            }
//        }
//
//        webView.webChromeClient = object : WebChromeClient() {
//            override fun onProgressChanged(view: WebView?, newProgress: Int) {
//                if (newProgress < 100) {
//                    progressBar.visibility = View.VISIBLE
//                } else {
//                    progressBar.visibility = View.GONE
//                }
//            }
//        }
//
//        webView.settings.javaScriptEnabled = true
//        webView.settings.setSupportZoom(true)
//
//        webView.loadUrl("https://demo.bachao.co/privacy-policy")
//    }
//
//    private fun showNoInternetDialog() {
//        SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
//            .setTitleText("No Internet Connection")
//            .setContentText("Please check your internet connection and try again.")
//            .setConfirmText("OK")
//            .setConfirmClickListener { dialog ->
//                dialog.dismissWithAnimation()
//                navigateToWelcome()
//            }
//            .show()
//    }
//
//    private fun navigateToWelcome() {
//        val intent = Intent(this, MainActivity::class.java)
//        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
//        startActivity(intent)
//        finish()  // Close TermConditionActivity
//    }
//
//    private fun isInternetAvailable(): Boolean {
//        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//        val network = connectivityManager.activeNetwork ?: return false
//        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
//        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
//    }
//
//    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
//    override fun onBackPressed() {
//        super.onBackPressed()
//        if (webView.canGoBack()) {
//            webView.goBack()
//        } else {
//            navigateToWelcome()
//        }
//    }
//
//    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
//        progressBar.visibility = View.VISIBLE
//        Log.d("WebView", "Page Started: $url")
//    }
//
//    override fun onPageFinished(view: WebView?, url: String?) {
//        progressBar.visibility = View.GONE
//        Log.d("WebView", "Page Finished: $url")
//    }
//}

class PrivacyPolicyActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.web_page_activity)

        // Initialize Views
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)

        // Check Internet Connectivity
        if (!isInternetAvailable()) {
            showNoInternetDialog()
        } else {
            setupWebView()
        }
    }

    private fun setupWebView() {
        // Show loader initially
        progressBar.visibility = View.VISIBLE

        // Setup WebView settings
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                // Show the progress bar when the page starts loading
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                // Hide the progress bar once the page finishes loading
                progressBar.visibility = View.GONE
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                // Show the progress bar until the page is fully loaded
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

        // Load the Privacy Policy page
        webView.loadUrl("https://demo.bachao.co/privacy-policy")
    }

    private fun showNoInternetDialog() {
        SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
            .setTitleText("No Internet Connection")
            .setContentText("Please check your internet connection and try again.")
            .setConfirmText("OK")
            .setConfirmClickListener { dialog ->
                dialog.dismissWithAnimation()
                navigateToWelcome()
            }
            .show()
    }

    private fun navigateToWelcome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()  // Close PrivacyPolicyActivity
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        super.onBackPressed()
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            navigateToWelcome()
        }
    }
}
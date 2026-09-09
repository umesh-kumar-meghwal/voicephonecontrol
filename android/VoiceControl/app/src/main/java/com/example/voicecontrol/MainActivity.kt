package com.example.voicecontrol

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast

class MainActivity : Activity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val MIC_PERMISSION_REQUEST = 500
    }

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "========== APP STARTED ==========")

        // -------------------------------------------------
        // WEBVIEW
        // -------------------------------------------------

        webView = WebView(this)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        // Android -> JavaScript bridge
        webView.addJavascriptInterface(
            DeviceBridge(this),
            "Android"
        )

        setContentView(webView)

        // Local HTML
        webView.loadUrl("file:///android_asset/index.html")

        // -------------------------------------------------
        // REGISTER DEVICE
        // -------------------------------------------------

        Thread {

            val deviceId =
                ApiClient.registerDevice(this)

            Log.d(
                TAG,
                "MY DEVICE ID = $deviceId"
            )

            runOnUiThread {

                if (!deviceId.isNullOrBlank()) {

                    webView.evaluateJavascript(
                        "window.setDeviceId(${JSONObjectHelper.quote(deviceId)});",
                        null
                    )

                } else {

                    webView.evaluateJavascript(
                        "window.setDeviceId('Registration failed');",
                        null
                    )

                    Toast.makeText(
                        this,
                        "Device registration failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        }.start()

        // -------------------------------------------------
        // COMMAND SERVICE
        // -------------------------------------------------

        startCommandService()

        // -------------------------------------------------
        // MICROPHONE
        // -------------------------------------------------

        checkMicrophonePermission()
    }

    // =====================================================
    // COMMAND SERVICE
    // =====================================================

    private fun startCommandService() {

        try {

            val intent =
                Intent(
                    this,
                    CommandService::class.java
                )

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {

                startForegroundService(intent)

            } else {

                startService(intent)
            }

            Log.d(
                TAG,
                "COMMAND SERVICE START REQUESTED"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "FAILED TO START COMMAND SERVICE",
                e
            )
        }
    }

    // =====================================================
    // MICROPHONE PERMISSION
    // =====================================================

    private fun checkMicrophonePermission() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.M
        ) {

            if (
                checkSelfPermission(
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                Log.d(
                    TAG,
                    "MICROPHONE PERMISSION NOT GRANTED"
                )

                requestPermissions(
                    arrayOf(
                        Manifest.permission.RECORD_AUDIO
                    ),
                    MIC_PERMISSION_REQUEST
                )

            } else {

                Log.d(
                    TAG,
                    "MICROPHONE PERMISSION ALREADY GRANTED"
                )
            }
        }
    }

    // =====================================================
    // PERMISSION RESULT
    // =====================================================

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (
            requestCode ==
            MIC_PERMISSION_REQUEST
        ) {

            if (
                grantResults.isNotEmpty() &&
                grantResults[0] ==
                PackageManager.PERMISSION_GRANTED
            ) {

                Log.d(
                    TAG,
                    "MICROPHONE PERMISSION GRANTED"
                )

            } else {

                Log.d(
                    TAG,
                    "MICROPHONE PERMISSION DENIED"
                )
            }
        }
    }

    // =====================================================
    // BACK BUTTON
    // =====================================================

    override fun onBackPressed() {

        if (webView.canGoBack()) {

            webView.goBack()

        } else {

            super.onBackPressed()
        }
    }
}
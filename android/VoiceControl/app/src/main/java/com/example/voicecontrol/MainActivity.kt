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

        Log.d(TAG, "=================================")
        Log.d(TAG, "APP STARTED")
        Log.d(TAG, "=================================")

        // -------------------------
        // WEBVIEW
        // -------------------------
        webView = WebView(this)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        webView.addJavascriptInterface(
            DeviceBridge(this),
            "Android"
        )

        setContentView(webView)

        webView.loadUrl("file:///android_asset/index.html")

        // -------------------------
        // DEVICE REGISTRATION
        // -------------------------
        //
        // IMPORTANT:
        // Yaha clearDeviceCredentials() MAT lagana.
        // Warna har app start par naya device banega.
        //
        Thread {

            try {

                Log.d(TAG, "STARTING DEVICE REGISTRATION")

                val deviceId = ApiClient.registerDevice(this)

                Log.d(TAG, "MY DEVICE ID = $deviceId")

                runOnUiThread {

                    if (!deviceId.isNullOrBlank()) {

                        Log.d(
                            TAG,
                            "DEVICE REGISTRATION SUCCESS"
                        )

                        // WebView me device ID show karo
                        val safeDeviceId =
                            JSONObjectHelper.quote(deviceId)

                        webView.evaluateJavascript(
                            "window.setDeviceId($safeDeviceId);",
                            null
                        )

                        Toast.makeText(
                            this,
                            "Device: $deviceId",
                            Toast.LENGTH_LONG
                        ).show()

                        // -------------------------
                        // START COMMAND SERVICE
                        // -------------------------
                        startCommandService()

                    } else {

                        Log.e(
                            TAG,
                            "DEVICE REGISTRATION FAILED"
                        )

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

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "DEVICE REGISTRATION ERROR",
                    e
                )

                runOnUiThread {

                    Toast.makeText(
                        this,
                        "Registration error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        }.start()

        // -------------------------
        // MICROPHONE PERMISSION
        // -------------------------
        checkMicrophonePermission()
    }


    // =====================================================
    // START COMMAND SERVICE
    // =====================================================

    private fun startCommandService() {

        try {

            Log.d(
                TAG,
                "STARTING COMMAND SERVICE..."
            )

            val intent = Intent(
                this,
                CommandService::class.java
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                startForegroundService(intent)

                Log.d(
                    TAG,
                    "startForegroundService() CALLED"
                )

            } else {

                startService(intent)

                Log.d(
                    TAG,
                    "startService() CALLED"
                )
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "FAILED TO START COMMAND SERVICE",
                e
            )

            Toast.makeText(
                this,
                "Service start failed: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // =====================================================
    // MICROPHONE PERMISSION
    // =====================================================

    private fun checkMicrophonePermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

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

        if (requestCode == MIC_PERMISSION_REQUEST) {

            if (
                grantResults.isNotEmpty() &&
                grantResults[0] ==
                PackageManager.PERMISSION_GRANTED
            ) {

                Log.d(
                    TAG,
                    "MICROPHONE PERMISSION GRANTED"
                )

                Toast.makeText(
                    this,
                    "Microphone permission granted",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                Log.w(
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

        if (::webView.isInitialized &&
            webView.canGoBack()
        ) {

            webView.goBack()

        } else {

            super.onBackPressed()
        }
    }
}
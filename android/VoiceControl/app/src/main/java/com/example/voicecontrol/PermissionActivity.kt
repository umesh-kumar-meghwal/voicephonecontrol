package com.example.voicecontrol

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView

class PermissionActivity : Activity() {

    companion object {
        private const val LOCATION_REQUEST = 1001
        private const val NOTIFICATION_REQUEST = 1002
    }

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_permission)

        statusText = findViewById(R.id.statusText)

        findViewById<Button>(R.id.btnNotification).setOnClickListener {
            requestNotificationPermission()
        }

        findViewById<Button>(R.id.btnLocation).setOnClickListener {
            requestLocationPermission()
        }

        findViewById<Button>(R.id.btnAccessibility).setOnClickListener {
            openAccessibilitySettings()
        }

        findViewById<Button>(R.id.btnBattery).setOnClickListener {
            openBatterySettings()
        }

        findViewById<Button>(R.id.btnContinue).setOnClickListener {
            openMainApp()
        }

        updateStatus()
    }

    private fun requestNotificationPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (
                checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                requestPermissions(
                    arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS
                    ),
                    NOTIFICATION_REQUEST
                )

                return
            }
        }

        updateStatus()
    }

    private fun requestLocationPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            val permissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

            requestPermissions(
                permissions,
                LOCATION_REQUEST
            )
        }
    }

    private fun openAccessibilitySettings() {

        try {

            val intent = Intent(
                Settings.ACTION_ACCESSIBILITY_SETTINGS
            )

            startActivity(intent)

        } catch (e: Exception) {

            statusText.text =
                "Accessibility settings open nahi ho payi."
        }
    }

    private fun openBatterySettings() {

        try {

            val intent = Intent(
                Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
            )

            startActivity(intent)

        } catch (e: Exception) {

            statusText.text =
                "Battery settings open nahi ho payi."
        }
    }

    private fun updateStatus() {

        val notification =
            if (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                "✅ Notification permission"
            } else {
                "❌ Notification permission"
            }

        val location =
            if (
                checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                "✅ Location permission"
            } else {
                "❌ Location permission"
            }

        statusText.text = """
            $notification
            $location
            
            Accessibility aur Battery
            settings manually enable karni hongi.
        """.trimIndent()
    }

    override fun onResume() {

        super.onResume()

        updateStatus()
    }

    private fun openMainApp() {

        val intent = Intent(
            this,
            MainActivity::class.java
        )

        startActivity(intent)

        finish()
    }
}
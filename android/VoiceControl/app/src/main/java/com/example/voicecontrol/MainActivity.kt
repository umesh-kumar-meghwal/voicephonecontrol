package com.example.voicecontrol

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log

class MainActivity : Activity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val MIC_PERMISSION_REQUEST = 500
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "========== APP STARTED ==========")

        // Existing command service
        startCommandService()

        // Microphone permission check
        checkMicrophonePermission()

        // Accessibility permission manually Settings se deni hai.
        finish()
    }

    // =========================================================
    // COMMAND SERVICE
    // =========================================================

    private fun startCommandService() {
        try {

            val intent = Intent(
                this,
                CommandService::class.java
            )

            if (
                android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.O
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

    // =========================================================
    // MICROPHONE PERMISSION
    // =========================================================

    private fun checkMicrophonePermission() {

        if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.M
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

    // =========================================================
    // PERMISSION RESULT
    // =========================================================

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
}
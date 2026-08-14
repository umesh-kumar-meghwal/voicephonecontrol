package com.example.voicecontrol

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log

class MainActivity : Activity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "========== APP STARTED ==========")

        startCommandService()

        // Koi permission UI nahi
        // Accessibility permission manually Settings se deni hai.
        finish()
    }

    private fun startCommandService() {
        try {
            val intent = Intent(
                this,
                CommandService::class.java
            )

            if (android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.O
            ) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }

            Log.d(TAG, "COMMAND SERVICE START REQUESTED")

        } catch (e: Exception) {
            Log.e(
                TAG,
                "FAILED TO START COMMAND SERVICE",
                e
            )
        }
    }
}
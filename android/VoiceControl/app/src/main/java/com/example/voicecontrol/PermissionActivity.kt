package com.example.voicecontrol

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager

class PermissionActivity : Activity() {

    companion object {
        private const val TAG = "PermissionActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "PermissionActivity started")

        if (isAccessibilityEnabled()) {
            Log.d(TAG, "Accessibility already enabled")
            startCommandService()
            finishAndRemoveTask()
            return
        }

        Log.d(TAG, "Opening Accessibility Settings")

        startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        )
    }

    override fun onResume() {
        super.onResume()

        // Settings se wapas aane ke baad check
        if (isAccessibilityEnabled()) {
            Log.d(TAG, "Accessibility enabled successfully")

            startCommandService()

            finishAndRemoveTask()
        }
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

            Log.d(TAG, "CommandService start requested")

        } catch (e: Exception) {
            Log.e(
                TAG,
                "Failed to start CommandService",
                e
            )
        }
    }

    private fun isAccessibilityEnabled(): Boolean {

        val manager =
            getSystemService(ACCESSIBILITY_SERVICE)
                    as? AccessibilityManager
                ?: return false

        val services =
            manager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            )

        return services.any { service ->

            val info =
                service.resolveInfo?.serviceInfo
                    ?: return@any false

            info.packageName == packageName &&
                    info.name == ScreenshotService::class.java.name
        }
    }
}
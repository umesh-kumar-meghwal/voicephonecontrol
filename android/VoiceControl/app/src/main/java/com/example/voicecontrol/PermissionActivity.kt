package com.example.voicecontrol

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

class PermissionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Accessibility already ON → app ko bilkul open mat hone do
        if (isAccessibilityEnabled()) {
            finishAndRemoveTask()
            return
        }

        // First launch → Accessibility settings
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

        // Permission screen open hone ke baad
        // hamari activity recent me nahi rahegi
        finish()
    }

    private fun isAccessibilityEnabled(): Boolean {

        val manager =
            getSystemService(ACCESSIBILITY_SERVICE) as? AccessibilityManager
                ?: return false

        val services =
            manager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            )

        return services.any { service ->

            val info = service.resolveInfo?.serviceInfo
                ?: return@any false

            info.packageName == packageName &&
                    info.name == ScreenshotService::class.java.name
        }
    }
}
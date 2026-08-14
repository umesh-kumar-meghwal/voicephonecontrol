package com.example.voicecontrol

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings

class PermissionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isAccessibilityEnabled()) {
            finishAndRemoveTask()
            return
        }

        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    override fun onResume() {
        super.onResume()

        if (isAccessibilityEnabled()) {
            finishAndRemoveTask()
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabled = try {
            Settings.Secure.getInt(
                contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (_: Settings.SettingNotFoundException) {
            0
        }

        if (enabled != 1) return false

        val services = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return services
            .split(":")
            .any { service ->
                service.equals(
                    "$packageName/.ScreenshotService",
                    ignoreCase = true
                )
            }
    }
}
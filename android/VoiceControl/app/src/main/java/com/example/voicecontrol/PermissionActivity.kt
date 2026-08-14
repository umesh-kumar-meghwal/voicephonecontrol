package com.example.voicecontrol

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity

class PermissionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Koi UI nahi dikhani
        checkAccessibility()
    }

    private fun checkAccessibility() {
        if (!isAccessibilityEnabled()) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } else {
            finishAndRemoveTask()
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val accessibilityEnabled = try {
            Settings.Secure.getInt(
                contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: Settings.SettingNotFoundException) {
            0
        }

        if (accessibilityEnabled != 1) return false

        val services = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return services.contains(packageName)
    }

    override fun onResume() {
        super.onResume()

        if (isAccessibilityEnabled()) {
            finishAndRemoveTask()
        }
    }
}
package com.example.voicecontrol

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity

class PermissionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAccessibility()
    }

    override fun onResume() {
        super.onResume()

        // Settings se BACK aane ke baad permission check
        checkAccessibility()
    }

    private fun checkAccessibility() {

        if (isAccessibilityEnabled()) {

            // Permission already ON
            openMainActivity()

        } else {

            // Permission OFF → Accessibility Settings
            try {
                startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

        if (accessibilityEnabled != 1) {
            return false
        }

        val services = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return services.split(":").any {
            it.contains(
                "$packageName/.ScreenshotService",
                ignoreCase = true
            )
        }
    }

    private fun openMainActivity() {

        val intent = Intent(
            this,
            MainActivity::class.java
        )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP

        startActivity(intent)

        finish()
    }
}
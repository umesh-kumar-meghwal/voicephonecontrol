package com.example.voicecontrol

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log

object CommandHandler {

    private const val TAG = "CommandHandler"

    fun handle(
        context: Context,
        command: String,
        payload: Map<String, String> = emptyMap()
    ) {

        val cmd = command.trim().uppercase()

        Log.d(TAG, "=================================")
        Log.d(TAG, "COMMAND = $cmd")
        Log.d(TAG, "PAYLOAD = $payload")
        Log.d(TAG, "=================================")

        when (cmd) {

            // ==============================
            // HOME
            // ==============================
            "HOME" -> {
                Log.d(TAG, "HOME requested")

                if (ScreenshotService.performHome()) {
                    Log.d(TAG, "HOME executed successfully")
                } else {
                    Log.e(
                        TAG,
                        "HOME failed: AccessibilityService not connected"
                    )
                }
            }


            // ==============================
            // BACK
            // ==============================
            "BACK" -> {
                Log.d(TAG, "BACK requested")

                if (ScreenshotService.performBack()) {
                    Log.d(TAG, "BACK executed successfully")
                } else {
                    Log.e(
                        TAG,
                        "BACK failed: AccessibilityService not connected"
                    )
                }
            }

            // ==============================
            // VOLUME UP
            // ==============================
            "VOLUME_UP" -> {

                try {

                    val audioManager =
                        context.getSystemService(
                            Context.AUDIO_SERVICE
                        ) as AudioManager

                    audioManager.adjustVolume(
                        AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_SHOW_UI
                    )

                    Log.d(TAG, "VOLUME UP SUCCESS")

                } catch (e: Exception) {

                    Log.e(TAG, "VOLUME UP FAILED", e)
                }
            }


            // ==============================
            // VOLUME DOWN
            // ==============================
            "VOLUME_DOWN" -> {

                try {

                    val audioManager =
                        context.getSystemService(
                            Context.AUDIO_SERVICE
                        ) as AudioManager

                    audioManager.adjustVolume(
                        AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_SHOW_UI
                    )

                    Log.d(TAG, "VOLUME DOWN SUCCESS")

                } catch (e: Exception) {

                    Log.e(TAG, "VOLUME DOWN FAILED", e)
                }
            }


            // ==============================
            // SCREENSHOT
            // ==============================
            "SCREENSHOT",
            "TAKE_SCREENSHOT" -> {
                Log.d(TAG, "SCREENSHOT requested")

                try {
                    ScreenshotService.takeScreenshot()

                    Log.d(
                        TAG,
                        "Screenshot request sent"
                    )
                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        "SCREENSHOT failed",
                        e
                    )
                }
            }


            // ==============================
            // PHONE STATUS
            // ==============================
            "PHONE_STATUS" -> {

                Log.d(TAG, "PHONE STATUS REQUESTED")

                try {

                    PhoneStatus.send(context)

                    Log.d(
                        TAG,
                        "PHONE STATUS REQUEST SENT"
                    )

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "PHONE STATUS FAILED",
                        e
                    )
                }
            }


            // ==============================
            // OPEN APP
            // ==============================
            "OPEN_APP" -> {

                val appName =
                    payload["app"]
                        ?.trim()
                        ?.lowercase()

                if (appName.isNullOrEmpty()) {

                    Log.e(
                        TAG,
                        "APP NAME MISSING"
                    )

                } else {

                    openApp(
                        context,
                        appName
                    )
                }
            }
            "NOTIFICATION_STATUS" -> {

                Log.d(
                    TAG,
                    "NOTIFICATION_STATUS requested"
                )

                try {

                    val status =
                        NotificationService.getStatus()

                    Log.d(
                        TAG,
                        "Notification status = $status"
                    )


                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "Notification status failed",
                        e
                    )
                }
            }


            // ==============================
            // LIVE SCREEN
            // ==============================
            "LIVE_SCREEN" -> {

                try {

                    val intent =
                        Intent(
                            context,
                            MainActivity::class.java
                        ).apply {

                            putExtra(
                                "START_LIVE_SCREEN",
                                true
                            )

                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                            )
                        }

                    context.startActivity(intent)

                    Log.d(
                        TAG,
                        "LIVE SCREEN REQUEST SENT"
                    )

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "LIVE SCREEN FAILED",
                        e
                    )
                }
            }


            else -> {

                Log.w(
                    TAG,
                    "UNKNOWN COMMAND = $cmd"
                )
            }
        }
    }



    // ==============================
    // OPEN APP
    // ==============================

    private fun openApp(
        context: Context,
        appName: String
    ) {

        val packageName =
            when (appName) {

                "whatsapp" ->
                    "com.whatsapp"


                "youtube" ->
                    "com.google.android.youtube"

                "chrome",
                "google chrome" ->
                    "com.android.chrome"

                "settings" ->
                    "com.android.settings"

                "camera" ->
                    "com.android.camera"

                else -> {

                    Log.e(
                        TAG,
                        "UNSUPPORTED APP = $appName"
                    )

                    return
                }
            }

        try {

            val launchIntent =
                context.packageManager
                    .getLaunchIntentForPackage(
                        packageName
                    )

            if (launchIntent == null) {

                Log.e(
                    TAG,
                    "LAUNCH INTENT NOT FOUND = $packageName"
                )

                return
            }

            launchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )

            context.startActivity(
                launchIntent
            )

            Log.d(
                TAG,
                "APP OPEN SUCCESS = $appName"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "APP OPEN FAILED = $appName",
                e
            )
        }
    }
}
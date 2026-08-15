package com.example.voicecontrol

import android.content.Context
import android.media.AudioManager
import android.util.Log

object CommandHandler {

    private const val TAG = "CommandHandler"

    fun handle(
        context: Context,
        command: String,
        payload: Map<String, String>
    ) {

        val cmd = command
            .trim()
            .uppercase()
            .replace("_", " ")

        Log.d(TAG, "=================================")
        Log.d(TAG, "COMMAND RECEIVED = $cmd")
        Log.d(TAG, "=================================")

        when {

            // =================================================
            // HOME
            // =================================================

            cmd == "HOME" ||
                    cmd == "GO HOME" ||
                    cmd == "OPEN HOME" -> {

                Log.d(TAG, "Executing HOME")

                val success =
                    ScreenshotService.performHome()

                if (success) {
                    Log.d(TAG, "HOME SUCCESS")
                } else {
                    Log.e(TAG, "HOME FAILED")
                }
            }

            // =================================================
// PHONE STATUS
// =================================================

            cmd == "PHONE STATUS" ||
                    cmd == "STATUS" ||
                    cmd == "PHONE BATTERY" -> {

                Log.d(
                    TAG,
                    "Executing PHONE STATUS"
                )

                try {

                    val batteryManager =
                        context.getSystemService(
                            Context.BATTERY_SERVICE
                        ) as android.os.BatteryManager

                    val battery =
                        batteryManager.getIntProperty(
                            android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY
                        )



                    val chargingIntent =
                        context.registerReceiver(
                            null,
                            android.content.IntentFilter(
                                android.content.Intent.ACTION_BATTERY_CHANGED
                            )
                        )

                    val status =
                        chargingIntent?.getIntExtra(
                            android.os.BatteryManager.EXTRA_STATUS,
                            -1
                        ) ?: -1

                    val charging =
                        status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                                status == android.os.BatteryManager.BATTERY_STATUS_FULL

                    Log.d(TAG, "==============================")
                    Log.d(TAG, "PHONE STATUS")
                    Log.d(TAG, "Battery  = $battery%")
                    Log.d(TAG, "Charging = $charging")
                    Log.d(TAG, "Android  = ${android.os.Build.VERSION.RELEASE}")
                    Log.d(TAG, "API      = ${android.os.Build.VERSION.SDK_INT}")
                    Log.d(TAG, "==============================")
                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "PHONE STATUS ERROR",
                        e
                    )
                }
            }

            // =================================================
// OPEN APP
// =================================================

            cmd == "OPEN APP" -> {

                Log.d(
                    TAG,
                    "Executing OPEN APP"
                )

                val appName =
                    payload["app"]
                        ?.trim()
                        ?.lowercase()

                if (appName.isNullOrEmpty()) {

                    Log.e(
                        TAG,
                        "OPEN APP FAILED: APP NAME MISSING"
                    )

                    return
                }

                Log.d(
                    TAG,
                    "Requested App = $appName"
                )

                val packageName =
                    when (appName) {

                        "whatsapp" ->
                            "com.whatsapp"

                        "youtube" ->
                            "com.google.android.youtube"

                        "chrome" ->
                            "com.android.chrome"

                        "settings" ->
                            "com.android.settings"

                        "camera" ->
                            "com.android.camera2"

                        else -> null
                    }

                if (packageName == null) {

                    Log.e(
                        TAG,
                        "OPEN APP FAILED: UNKNOWN APP = $appName"
                    )

                    return
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
                            "OPEN APP FAILED: APP NOT INSTALLED = $packageName"
                        )

                        return
                    }

                    launchIntent.addFlags(
                        android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    )

                    context.startActivity(
                        launchIntent
                    )

                    Log.d(
                        TAG,
                        "OPEN APP SUCCESS = $appName"
                    )

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "OPEN APP ERROR",
                        e
                    )
                }
            }

            // =================================================
            // BACK
            // =================================================

            cmd == "BACK" ||
                    cmd == "GO BACK" ||
                    cmd == "PRESS BACK" -> {

                Log.d(TAG, "Executing BACK")

                val success =
                    ScreenshotService.performBack()

                if (success) {
                    Log.d(TAG, "BACK SUCCESS")
                } else {
                    Log.e(TAG, "BACK FAILED")
                }
            }

            // =================================================
            // RECENTS
            // =================================================

            cmd == "RECENTS" ||
                    cmd == "RECENT" ||
                    cmd == "RECENT APPS" ||
                    cmd == "OPEN RECENTS" ||
                    cmd == "SHOW RECENTS" -> {

                Log.d(TAG, "Executing RECENTS")

                val success =
                    ScreenshotService.performRecentApps()

                if (success) {
                    Log.d(TAG, "RECENTS SUCCESS")
                } else {
                    Log.e(TAG, "RECENTS FAILED")
                }
            }

            // =================================================
// SCREENSHOT
// =================================================

            cmd == "SCREENSHOT" ||
                    cmd == "TAKE SCREENSHOT" ||
                    cmd == "CAPTURE SCREEN" ||
                    cmd == "TAKE A SCREENSHOT" -> {

                Log.d(
                    TAG,
                    "Executing SCREENSHOT"
                )

                val success =
                    ScreenshotService.performScreenshot()

                if (success) {

                    Log.d(
                        TAG,
                        "SCREENSHOT REQUEST SENT"
                    )

                } else {

                    Log.e(
                        TAG,
                        "SCREENSHOT FAILED"
                    )
                }
            }
            // =================================================
// UP
// =================================================

            cmd == "UP" ||
                    cmd == "ARROW UP" ||
                    cmd == "MOVE UP" ||
                    cmd == "GO UP" -> {

                Log.d(
                    TAG,
                    "Executing UP"
                )

                val success =
                    ScreenshotService.performUp()

                if (success) {

                    Log.d(
                        TAG,
                        "UP SUCCESS"
                    )

                } else {

                    Log.e(
                        TAG,
                        "UP FAILED"
                    )
                }
            }


// =================================================
// DOWN
// =================================================

            cmd == "DOWN" ||
                    cmd == "ARROW DOWN" ||
                    cmd == "MOVE DOWN" ||
                    cmd == "GO DOWN" -> {

                Log.d(
                    TAG,
                    "Executing DOWN"
                )

                val success =
                    ScreenshotService.performDown()

                if (success) {

                    Log.d(
                        TAG,
                        "DOWN SUCCESS"
                    )

                } else {

                    Log.e(
                        TAG,
                        "DOWN FAILED"
                    )
                }
            }


// =================================================
// TAB
// =================================================

            cmd == "TAB" ||
                    cmd == "PRESS TAB" ||
                    cmd == "NEXT" ||
                    cmd == "NEXT FIELD" -> {

                Log.d(
                    TAG,
                    "Executing TAB"
                )

                val success =
                    ScreenshotService.performTab()

                if (success) {

                    Log.d(
                        TAG,
                        "TAB SUCCESS"
                    )

                } else {

                    Log.e(
                        TAG,
                        "TAB FAILED"
                    )
                }
            }
            cmd == "LEFT"
             -> {
                val success = ScreenshotService.performLeft()
                Log.d("CommandHandler", "LEFT = $success")
            }

            cmd == "RIGHT"
           -> {
                val success = ScreenshotService.performRight()
                Log.d("CommandHandler", "RIGHT = $success")
            }
            // =================================================
            // ENTER
            // =================================================

            cmd == "ENTER" ||
                    cmd == "PRESS ENTER" ||
                    cmd == "OK" ||
                    cmd == "PRESS OK" -> {

                Log.d(TAG, "Executing ENTER")

                val success =
                    ScreenshotService.performEnter()

                if (success) {
                    Log.d(TAG, "ENTER SUCCESS")
                } else {
                    Log.e(TAG, "ENTER FAILED")
                }
            }

            // =================================================
            // VOLUME UP
            // =================================================

            cmd == "VOLUME UP" ||
                    cmd == "INCREASE VOLUME" ||
                    cmd == "VOLUME INCREASE" ||
                    cmd == "TURN VOLUME UP" -> {

                Log.d(TAG, "Executing VOLUME UP")

                val audioManager =
                    context.getSystemService(
                        Context.AUDIO_SERVICE
                    ) as AudioManager

                audioManager.adjustVolume(
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI
                )

                Log.d(TAG, "VOLUME UP SUCCESS")
            }

            // =================================================
            // VOLUME DOWN
            // =================================================

            cmd == "VOLUME DOWN" ||
                    cmd == "DECREASE VOLUME" ||
                    cmd == "VOLUME DECREASE" ||
                    cmd == "TURN VOLUME DOWN" -> {

                Log.d(TAG, "Executing VOLUME DOWN")

                val audioManager =
                    context.getSystemService(
                        Context.AUDIO_SERVICE
                    ) as AudioManager

                audioManager.adjustVolume(
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI
                )

                Log.d(TAG, "VOLUME DOWN SUCCESS")
            }

            // =================================================
            // MUTE
            // =================================================

            cmd == "MUTE" ||
                    cmd == "MUTE VOLUME" -> {

                Log.d(TAG, "Executing MUTE")

                val audioManager =
                    context.getSystemService(
                        Context.AUDIO_SERVICE
                    ) as AudioManager

                audioManager.adjustVolume(
                    AudioManager.ADJUST_MUTE,
                    AudioManager.FLAG_SHOW_UI
                )

                Log.d(TAG, "MUTE SUCCESS")
            }

            // =================================================
            // UNKNOWN
            // =================================================

            else -> {

                Log.w(
                    TAG,
                    "UNKNOWN COMMAND = $cmd"
                )
            }
        }
    }
}
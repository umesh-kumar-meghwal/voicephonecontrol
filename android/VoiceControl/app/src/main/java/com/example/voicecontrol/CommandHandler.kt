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

                Log.d(TAG, "Executing PHONE STATUS")

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

                    val connectivityManager =
                        context.getSystemService(
                            Context.CONNECTIVITY_SERVICE
                        ) as android.net.ConnectivityManager

                    val network =
                        if (
                            android.os.Build.VERSION.SDK_INT >=
                            android.os.Build.VERSION_CODES.M
                        ) {

                            val activeNetwork =
                                connectivityManager.activeNetwork

                            val capabilities =
                                connectivityManager.getNetworkCapabilities(
                                    activeNetwork
                                )

                            capabilities != null &&
                                    (
                                            capabilities.hasTransport(
                                                android.net.NetworkCapabilities.TRANSPORT_WIFI
                                            ) ||
                                                    capabilities.hasTransport(
                                                        android.net.NetworkCapabilities.TRANSPORT_CELLULAR
                                                    ) ||
                                                    capabilities.hasTransport(
                                                        android.net.NetworkCapabilities.TRANSPORT_ETHERNET
                                                    )
                                            )

                        } else {

                            @Suppress("DEPRECATION")
                            connectivityManager.activeNetworkInfo?.isConnected == true
                        }

                    val androidVersion =
                        android.os.Build.VERSION.RELEASE

                    val apiVersion =
                        android.os.Build.VERSION.SDK_INT

                    Log.d(TAG, "==============================")
                    Log.d(TAG, "PHONE STATUS")
                    Log.d(TAG, "Battery  = $battery%")
                    Log.d(TAG, "Charging = $charging")
                    Log.d(TAG, "Network  = $network")
                    Log.d(TAG, "Android  = $androidVersion")
                    Log.d(TAG, "API      = $apiVersion")
                    Log.d(TAG, "==============================")

                    // -----------------------------------------
                    // UPLOAD STATUS TO SERVER
                    // -----------------------------------------

                    Thread {

                        var connection:
                                java.net.HttpURLConnection? = null

                        try {

                            val url =
                                java.net.URL(
                                    "https://phonecontrol-black.vercel.app/api/status"
                                )

                            connection =
                                url.openConnection()
                                        as java.net.HttpURLConnection

                            connection.requestMethod =
                                "POST"

                            connection.connectTimeout =
                                10000

                            connection.readTimeout =
                                15000

                            connection.doOutput =
                                true

                            connection.useCaches =
                                false

                            connection.setRequestProperty(
                                "Authorization",
                                "Bearer VPC-a8F3xK91-pQ7L2mZ6-4NwR8tY5U"
                            )

                            connection.setRequestProperty(
                                "Content-Type",
                                "application/json"
                            )

                            connection.setRequestProperty(
                                "Accept",
                                "application/json"
                            )

                            /*
                             * IMPORTANT:
                             *
                             * Python get_phone_status() expects:
                             *
                             * {
                             *   "ok": true,
                             *   "status": {
                             *      ...
                             *   }
                             * }
                             */

                            val json =
                                """
                                {
                                    "ok": true,
                                    "status": {
                                        "battery": $battery,
                                        "charging": $charging,
                                        "network": $network,
                                        "android": "$androidVersion",
                                        "api": $apiVersion
                                    }
                                }
                                """.trimIndent()

                            Log.d(
                                TAG,
                                "STATUS JSON = $json"
                            )

                            connection.outputStream.use { output ->

                                output.write(
                                    json.toByteArray(
                                        Charsets.UTF_8
                                    )
                                )

                                output.flush()
                            }

                            val responseCode =
                                connection.responseCode

                            Log.d(
                                TAG,
                                "STATUS UPLOAD RESPONSE = $responseCode"
                            )

                            val response =
                                try {

                                    val stream =
                                        if (
                                            responseCode in 200..299
                                        ) {
                                            connection.inputStream
                                        } else {
                                            connection.errorStream
                                        }

                                    stream
                                        ?.bufferedReader()
                                        ?.use {
                                            it.readText()
                                        }

                                } catch (e: Exception) {

                                    "Unable to read response: ${e.message}"
                                }

                            Log.d(
                                TAG,
                                "STATUS SERVER RESPONSE = $response"
                            )

                        } catch (e: Exception) {

                            Log.e(
                                TAG,
                                "PHONE STATUS UPLOAD ERROR",
                                e
                            )

                        } finally {

                            connection?.disconnect()
                        }

                    }.start()

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

            // =================================================
            // LEFT
            // =================================================

            cmd == "LEFT" -> {

                val success =
                    ScreenshotService.performLeft()

                Log.d(
                    TAG,
                    "LEFT = $success"
                )
            }

            // =================================================
            // RIGHT
            // =================================================

            cmd == "RIGHT" -> {

                val success =
                    ScreenshotService.performRight()

                Log.d(
                    TAG,
                    "RIGHT = $success"
                )
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

                Log.d(
                    TAG,
                    "VOLUME UP SUCCESS"
                )
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

                Log.d(
                    TAG,
                    "VOLUME DOWN SUCCESS"
                )
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

                Log.d(
                    TAG,
                    "MUTE SUCCESS"
                )
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
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
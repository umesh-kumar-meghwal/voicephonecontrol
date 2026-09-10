package com.example.voicecontrol

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log

object CommandHandler {

    private const val TAG =
        "CommandHandler"


    // =========================================================
    // MAIN COMMAND FUNCTION
    // =========================================================

    fun handle(
        context: Context,
        command: String,
        payload: Map<String, String>
    ) {
        Log.e(
            "TEST_COMMAND",
            "CommandHandler.handle() CALLED | command=[$command] | payload=$payload"
        )

        Log.d(
            TAG,
            "========== HANDLE() ENTERED =========="
        )

        Log.d(
            TAG,
            "RAW COMMAND = [$command]"
        )

        Log.d(
            TAG,
            "PAYLOAD = $payload"
        )

        val cmd =
            command
                .trim()
                .uppercase()
                .replace("_", " ")

        Log.d(
            TAG,
            "NORMALIZED COMMAND = [$cmd]"
        )




        when {


            // =================================================
            // HOME
            // =================================================

            cmd == "HOME" ||
                    cmd == "GO HOME" ||
                    cmd == "OPEN HOME" -> {

                Log.d(
                    TAG,
                    "Executing HOME"
                )


                val success =
                    ScreenshotService.performHome()


                if (success) {

                    Log.d(
                        TAG,
                        "HOME SUCCESS"
                    )

                } else {

                    Log.e(
                        TAG,
                        "HOME FAILED"
                    )
                }
            }


            // =================================================
            // BACK
            // =================================================

            cmd == "BACK" ||
                    cmd == "GO BACK" ||
                    cmd == "PRESS BACK" -> {

                Log.d(
                    TAG,
                    "Executing BACK"
                )


                val success =
                    ScreenshotService.performBack()


                if (success) {

                    Log.d(
                        TAG,
                        "BACK SUCCESS"
                    )

                } else {

                    Log.e(
                        TAG,
                        "BACK FAILED"
                    )
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

                Log.d(
                    TAG,
                    "Executing RECENTS"
                )


                val success =
                    ScreenshotService.performRecentApps()


                if (success) {

                    Log.d(
                        TAG,
                        "RECENTS SUCCESS"
                    )

                } else {

                    Log.e(
                        TAG,
                        "RECENTS FAILED"
                    )
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
            // LEFT
            // =================================================

            cmd == "LEFT" ||
                    cmd == "ARROW LEFT" -> {

                Log.d(
                    TAG,
                    "Executing LEFT"
                )


                val success =
                    ScreenshotService.performLeft()


                Log.d(
                    TAG,
                    "LEFT RESULT = $success"
                )
            }


            // =================================================
            // RIGHT
            // =================================================

            cmd == "RIGHT" ||
                    cmd == "ARROW RIGHT" -> {

                Log.d(
                    TAG,
                    "Executing RIGHT"
                )


                val success =
                    ScreenshotService.performRight()


                Log.d(
                    TAG,
                    "RIGHT RESULT = $success"
                )
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
            // ENTER
            // =================================================

            cmd == "ENTER" ||
                    cmd == "PRESS ENTER" ||
                    cmd == "OK" ||
                    cmd == "PRESS OK" -> {

                Log.d(
                    TAG,
                    "Executing ENTER"
                )


                val success =
                    ScreenshotService.performEnter()


                if (success) {

                    Log.d(
                        TAG,
                        "ENTER SUCCESS"
                    )

                } else {

                    Log.e(
                        TAG,
                        "ENTER FAILED"
                    )
                }
            }


            // =================================================
            // VOLUME UP
            // =================================================

            cmd == "VOLUME UP" ||
                    cmd == "INCREASE VOLUME" ||
                    cmd == "VOLUME INCREASE" ||
                    cmd == "TURN VOLUME UP" -> {

                Log.d(
                    TAG,
                    "Executing VOLUME UP"
                )


                try {

                    val audioManager =
                        context.getSystemService(
                            Context.AUDIO_SERVICE
                        ) as AudioManager


                    val before =
                        audioManager.getStreamVolume(
                            AudioManager.STREAM_MUSIC
                        )


                    val max =
                        audioManager.getStreamMaxVolume(
                            AudioManager.STREAM_MUSIC
                        )


                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_SHOW_UI
                    )


                    val after =
                        audioManager.getStreamVolume(
                            AudioManager.STREAM_MUSIC
                        )


                    Log.d(
                        TAG,
                        "VOLUME UP: before=$before after=$after max=$max"
                    )

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "VOLUME UP ERROR",
                        e
                    )
                }
            }


            // =================================================
            // VOLUME DOWN
            // =================================================

            cmd == "VOLUME DOWN" ||
                    cmd == "DECREASE VOLUME" ||
                    cmd == "VOLUME DECREASE" ||
                    cmd == "TURN VOLUME DOWN" -> {

                Log.d(
                    TAG,
                    "Executing VOLUME DOWN"
                )


                try {

                    val audioManager =
                        context.getSystemService(
                            Context.AUDIO_SERVICE
                        ) as AudioManager


                    val before =
                        audioManager.getStreamVolume(
                            AudioManager.STREAM_MUSIC
                        )


                    val max =
                        audioManager.getStreamMaxVolume(
                            AudioManager.STREAM_MUSIC
                        )


                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_SHOW_UI
                    )


                    val after =
                        audioManager.getStreamVolume(
                            AudioManager.STREAM_MUSIC
                        )


                    Log.d(
                        TAG,
                        "VOLUME DOWN: before=$before after=$after max=$max"
                    )

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "VOLUME DOWN ERROR",
                        e
                    )
                }
            }


            // =================================================
            // MUTE
            // =================================================

            cmd == "MUTE" ||
                    cmd == "MUTE VOLUME" -> {

                Log.d(
                    TAG,
                    "Executing MUTE"
                )


                try {

                    val audioManager =
                        context.getSystemService(
                            Context.AUDIO_SERVICE
                        ) as AudioManager


                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_MUTE,
                        AudioManager.FLAG_SHOW_UI
                    )


                    Log.d(
                        TAG,
                        "MUTE SUCCESS"
                    )

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "MUTE ERROR",
                        e
                    )
                }
            }


            // =================================================
            // UNMUTE
            // =================================================

            cmd == "UNMUTE" ||
                    cmd == "UNMUTE VOLUME" -> {

                Log.d(
                    TAG,
                    "Executing UNMUTE"
                )


                try {

                    val audioManager =
                        context.getSystemService(
                            Context.AUDIO_SERVICE
                        ) as AudioManager


                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_UNMUTE,
                        AudioManager.FLAG_SHOW_UI
                    )


                    Log.d(
                        TAG,
                        "UNMUTE SUCCESS"
                    )

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "UNMUTE ERROR",
                        e
                    )
                }
            }


            // =================================================
            // START MIC
            // =================================================

            cmd == "START MIC" -> {

                Log.d(
                    TAG,
                    "START_MIC received"
                )


                if (
                    android.os.Build.VERSION.SDK_INT >=
                    android.os.Build.VERSION_CODES.M &&
                    context.checkSelfPermission(
                        android.Manifest.permission.RECORD_AUDIO
                    ) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {

                    Log.e(
                        TAG,
                        "RECORD_AUDIO permission not granted"
                    )

                    return
                }


                try {

                    val intent =
                        Intent(
                            context,
                            MicrophoneService::class.java
                        )


                    intent.action =
                        "START_RECORDING"


                    if (
                        android.os.Build.VERSION.SDK_INT >=
                        android.os.Build.VERSION_CODES.O
                    ) {

                        context.startForegroundService(
                            intent
                        )

                    } else {

                        context.startService(
                            intent
                        )
                    }


                    Log.d(
                        TAG,
                        "MICROPHONE SERVICE STARTED"
                    )

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "FAILED TO START MICROPHONE SERVICE",
                        e
                    )
                }
            }


            // =================================================
            // STOP MIC
            // =================================================

            cmd == "STOP MIC" -> {

                Log.d(
                    TAG,
                    "STOP_MIC received"
                )


                try {

                    val intent =
                        Intent(
                            context,
                            MicrophoneService::class.java
                        )


                    intent.action =
                        "STOP_RECORDING"


                    context.startService(
                        intent
                    )


                    Log.d(
                        TAG,
                        "MICROPHONE SERVICE STOP REQUESTED"
                    )

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "FAILED TO STOP MICROPHONE SERVICE",
                        e
                    )
                }
            }
            // =================================================
// PHONE STATUS
// =================================================

            cmd == "PHONE STATUS" -> {

                Log.d(
                    TAG,
                    "Executing PHONE STATUS"
                )

                try {

                    PhoneStatus.send(context)

                    Log.d(
                        TAG,
                        "PHONE STATUS SEND STARTED"
                    )

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


                if (
                    appName.isNullOrEmpty()
                ) {

                    Log.e(
                        TAG,
                        "OPEN APP FAILED: APP NAME MISSING"
                    )

                    return
                }


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

                        else ->
                            null
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
                            "APP NOT INSTALLED = $packageName"
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
            // UNKNOWN COMMAND
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
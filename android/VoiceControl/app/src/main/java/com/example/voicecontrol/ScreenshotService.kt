package com.example.voicecontrol

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.util.Base64
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityNodeInfo
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import org.json.JSONObject

private const val SERVER_URL =
    "https://phonecontrol-black.vercel.app"

// IMPORTANT:
// Production app me token ko source code me hard-code mat rakhna.
private const val API_TOKEN =
    "VPC-a8F3xK91-pQ7L2mZ6-4NwR8tY5U"


class ScreenshotService : AccessibilityService() {

    companion object {

        private var instance: ScreenshotService? = null

        // =========================================
        // SCREENSHOT
        // =========================================

        fun takeScreenshot() {

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {

                Log.e(
                    "ScreenshotService",
                    "Android 11+ required"
                )

                return
            }

            val service = instance

            if (service == null) {

                Log.e(
                    "ScreenshotService",
                    "SCREENSHOT FAILED: SERVICE NOT CONNECTED"
                )

                return
            }

            Log.d(
                "ScreenshotService",
                "Starting screenshot..."
            )

            service.captureScreen()
        }


        // =========================================
        // BACK
        // =========================================

        fun performBack(): Boolean {

            val service = instance

            if (service == null) {

                Log.e(
                    "ScreenshotService",
                    "BACK FAILED: SERVICE NOT CONNECTED"
                )

                return false
            }

            return try {

                val result =
                    service.performGlobalAction(
                        GLOBAL_ACTION_BACK
                    )

                Log.d(
                    "ScreenshotService",
                    "BACK result = $result"
                )

                result

            } catch (e: Exception) {

                Log.e(
                    "ScreenshotService",
                    "BACK ERROR",
                    e
                )

                false
            }
        }

        fun pressEnter(): Boolean {

            val service = instance

            if (service == null) {
                Log.e(
                    "ScreenshotService",
                    "ENTER FAILED: SERVICE NOT CONNECTED"
                )
                return false
            }

            return try {

                val root = service.rootInActiveWindow

                if (root == null) {
                    Log.e(
                        "ScreenshotService",
                        "ENTER FAILED: ROOT WINDOW IS NULL"
                    )
                    return false
                }

                val focused = root.findFocus(
                    AccessibilityNodeInfo.FOCUS_INPUT
                )

                if (focused != null && focused.isClickable) {

                    val result =
                        focused.performAction(
                            AccessibilityNodeInfo.ACTION_CLICK
                        )

                    Log.d(
                        "ScreenshotService",
                        "ENTER/CLICK result = $result"
                    )

                    focused.recycle()

                    result

                } else {

                    Log.d(
                        "ScreenshotService",
                        "No clickable focused node found"
                    )

                    false
                }

            } catch (e: Exception) {

                Log.e(
                    "ScreenshotService",
                    "ENTER ERROR",
                    e
                )

                false
            }
        }


        // =========================================
        // HOME
        // =========================================

        fun performHome(): Boolean {

            val service = instance

            if (service == null) {

                Log.e(
                    "ScreenshotService",
                    "HOME FAILED: SERVICE NOT CONNECTED"
                )

                return false
            }

            return try {

                val result =
                    service.performGlobalAction(
                        GLOBAL_ACTION_HOME
                    )

                Log.d(
                    "ScreenshotService",
                    "HOME result = $result"
                )

                result

            } catch (e: Exception) {

                Log.e(
                    "ScreenshotService",
                    "HOME ERROR",
                    e
                )

                false
            }
        }
    }


    // =========================================
    // SERVICE CONNECTED
    // =========================================

    override fun onServiceConnected() {

        super.onServiceConnected()

        instance = this

        Log.d(
            "ScreenshotService",
            "================================="
        )

        Log.d(
            "ScreenshotService",
            "SERVICE CONNECTED SUCCESSFULLY"
        )

        Log.d(
            "ScreenshotService",
            "BACK / HOME / SCREENSHOT READY"
        )

        Log.d(
            "ScreenshotService",
            "================================="
        )
    }


    // =========================================
    // CAPTURE SCREEN
    // =========================================

    private fun captureScreen() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return
        }

        try {

            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : TakeScreenshotCallback {

                    override fun onSuccess(
                        screenshot: ScreenshotResult
                    ) {

                        Log.d(
                            "ScreenshotService",
                            "SCREENSHOT CAPTURE SUCCESS"
                        )

                        val buffer =
                            screenshot.hardwareBuffer

                        try {

                            val bitmap =
                                Bitmap.wrapHardwareBuffer(
                                    buffer,
                                    screenshot.colorSpace
                                )

                            if (bitmap == null) {

                                Log.e(
                                    "ScreenshotService",
                                    "BITMAP CREATION FAILED"
                                )

                                return
                            }

                            try {

                                // =================================
                                // SAVE LOCAL COPY
                                // =================================

                                saveScreenshot(bitmap)


                                // =================================
                                // COPY FOR UPLOAD
                                // =================================

                                val uploadBitmap =
                                    bitmap.copy(
                                        Bitmap.Config.ARGB_8888,
                                        false
                                    )

                                if (uploadBitmap == null) {

                                    Log.e(
                                        "ScreenshotService",
                                        "UPLOAD BITMAP COPY FAILED"
                                    )

                                    return
                                }


                                // =================================
                                // UPLOAD
                                // =================================

                                uploadScreenshot(
                                    uploadBitmap
                                )

                            } finally {

                                if (!bitmap.isRecycled) {
                                    bitmap.recycle()
                                }
                            }

                        } catch (e: Exception) {

                            Log.e(
                                "ScreenshotService",
                                "SCREENSHOT PROCESSING ERROR",
                                e
                            )

                        } finally {

                            buffer.close()
                        }
                    }


                    override fun onFailure(
                        errorCode: Int
                    ) {

                        Log.e(
                            "ScreenshotService",
                            "SCREENSHOT FAILED: $errorCode"
                        )
                    }
                }
            )

        } catch (e: Exception) {

            Log.e(
                "ScreenshotService",
                "SCREENSHOT START ERROR",
                e
            )
        }
    }


    // =========================================
    // SAVE SCREENSHOT
    // =========================================

    private fun saveScreenshot(
        bitmap: Bitmap
    ) {

        try {

            val picturesDirectory =
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
                )

            val folder =
                File(
                    picturesDirectory,
                    "VoicePhoneControl"
                )

            if (!folder.exists()) {
                folder.mkdirs()
            }

            val file =
                File(
                    folder,
                    "Screenshot_${System.currentTimeMillis()}.png"
                )

            FileOutputStream(file).use { output ->

                bitmap.compress(
                    Bitmap.CompressFormat.PNG,
                    100,
                    output
                )
            }

            Log.d(
                "ScreenshotService",
                "SCREENSHOT SAVED = ${file.absolutePath}"
            )

        } catch (e: Exception) {

            Log.e(
                "ScreenshotService",
                "SAVE ERROR",
                e
            )
        }
    }


    // =========================================
    // UPLOAD SCREENSHOT
    // =========================================

    private fun uploadScreenshot(
        bitmap: Bitmap
    ) {

        thread {

            try {

                Log.d(
                    "ScreenshotService",
                    "Preparing screenshot upload..."
                )

                val outputStream =
                    ByteArrayOutputStream()

                bitmap.compress(
                    Bitmap.CompressFormat.PNG,
                    100,
                    outputStream
                )

                val imageBytes =
                    outputStream.toByteArray()

                outputStream.close()


                val imageBase64 =
                    Base64.encodeToString(
                        imageBytes,
                        Base64.NO_WRAP
                    )


                val filename =
                    "Screenshot_${System.currentTimeMillis()}.png"


                val json =
                    JSONObject().apply {

                        put(
                            "filename",
                            filename
                        )

                        put(
                            "image",
                            imageBase64
                        )
                    }


                Log.d(
                    "ScreenshotService",
                    "Uploading screenshot..."
                )


                val url =
                    URL(
                        "$SERVER_URL/api/screenshot"
                    )


                val connection =
                    url.openConnection()
                            as HttpURLConnection


                try {

                    connection.requestMethod =
                        "POST"

                    connection.setRequestProperty(
                        "Authorization",
                        "Bearer $API_TOKEN"
                    )

                    connection.setRequestProperty(
                        "Content-Type",
                        "application/json"
                    )

                    connection.doOutput =
                        true

                    connection.connectTimeout =
                        15000

                    connection.readTimeout =
                        30000


                    connection.outputStream.use { output ->

                        output.write(
                            json.toString()
                                .toByteArray(
                                    Charsets.UTF_8
                                )
                        )
                    }


                    val responseCode =
                        connection.responseCode


                    Log.d(
                        "ScreenshotService",
                        "UPLOAD RESPONSE: $responseCode"
                    )


                    if (responseCode in 200..299) {

                        Log.d(
                            "ScreenshotService",
                            "SCREENSHOT UPLOAD SUCCESS"
                        )

                    } else {

                        Log.e(
                            "ScreenshotService",
                            "SCREENSHOT UPLOAD FAILED: HTTP $responseCode"
                        )
                    }

                } finally {

                    connection.disconnect()
                }

            } catch (e: Exception) {

                Log.e(
                    "ScreenshotService",
                    "UPLOAD ERROR",
                    e
                )

            } finally {

                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        }
    }


    // =========================================
    // ACCESSIBILITY EVENT
    // =========================================

    override fun onAccessibilityEvent(
        event: android.view.accessibility.AccessibilityEvent?
    ) {
        // No event processing required
    }


    // =========================================
    // INTERRUPTED
    // =========================================

    override fun onInterrupt() {

        Log.d(
            "ScreenshotService",
            "SERVICE INTERRUPTED"
        )
    }


    // =========================================
    // DESTROYED
    // =========================================

    override fun onDestroy() {

        instance = null

        Log.d(
            "ScreenshotService",
            "SERVICE DESTROYED"
        )

        super.onDestroy()
    }
}
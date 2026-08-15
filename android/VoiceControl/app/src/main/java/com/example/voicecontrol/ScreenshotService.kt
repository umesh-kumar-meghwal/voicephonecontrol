package com.example.voicecontrol

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.HardwareBuffer
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.io.OutputStream

class ScreenshotService : AccessibilityService() {

    companion object {

        private const val TAG = "ScreenshotService"

        @Volatile
        private var serviceInstance: ScreenshotService? = null

        // =====================================================
        // SERVICE STATUS
        // =====================================================

        fun isRunning(): Boolean {
            return serviceInstance != null
        }

        // =====================================================
        // HOME
        // =====================================================

        fun performHome(): Boolean {

            val service = serviceInstance ?: run {
                Log.e(TAG, "HOME FAILED: SERVICE NOT CONNECTED")
                return false
            }

            return try {

                val result =
                    service.performGlobalAction(
                        GLOBAL_ACTION_HOME
                    )

                Log.d(TAG, "HOME RESULT = $result")

                result

            } catch (e: Exception) {

                Log.e(TAG, "HOME ERROR", e)

                false
            }
        }

        // =====================================================
        // BACK
        // =====================================================

        fun performBack(): Boolean {

            val service = serviceInstance ?: run {
                Log.e(TAG, "BACK FAILED: SERVICE NOT CONNECTED")
                return false
            }

            return try {

                val result =
                    service.performGlobalAction(
                        GLOBAL_ACTION_BACK
                    )

                Log.d(TAG, "BACK RESULT = $result")

                result

            } catch (e: Exception) {

                Log.e(TAG, "BACK ERROR", e)

                false
            }
        }

        // =====================================================
        // RECENTS
        // =====================================================

        fun performRecentApps(): Boolean {

            val service = serviceInstance ?: run {
                Log.e(TAG, "RECENTS FAILED: SERVICE NOT CONNECTED")
                return false
            }

            return try {

                val result =
                    service.performGlobalAction(
                        GLOBAL_ACTION_RECENTS
                    )

                Log.d(TAG, "RECENTS RESULT = $result")

                result

            } catch (e: Exception) {

                Log.e(TAG, "RECENTS ERROR", e)

                false
            }
        }

        // =====================================================
        // SCREENSHOT
        // =====================================================

        fun performScreenshot(): Boolean {

            Log.d(TAG, "========== SCREENSHOT REQUEST ==========")

            val service = serviceInstance

            if (service == null) {
                Log.e(TAG, "SCREENSHOT FAILED: SERVICE NOT CONNECTED")
                return false
            }

            Log.d(TAG, "SERVICE INSTANCE = $service")

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                Log.e(
                    TAG,
                    "SCREENSHOT FAILED: API ${Build.VERSION.SDK_INT}, API 30+ REQUIRED"
                )
                return false
            }

            Log.d(TAG, "ANDROID API = ${Build.VERSION.SDK_INT}")
            Log.d(TAG, "CALLING takeScreenshotInternal()")

            return service.takeScreenshotInternal()
        }

        // =====================================================
        // ENTER
        // =====================================================

        fun performEnter(): Boolean {

            val service = serviceInstance ?: run {
                Log.e(TAG, "ENTER FAILED: SERVICE NOT CONNECTED")
                return false
            }

            return service.performEnterInternal()
        }
    }

    // =========================================================
    // SERVICE CONNECTED
    // =========================================================

    override fun onServiceConnected() {

        super.onServiceConnected()

        serviceInstance = this

        val info =
            serviceInfo ?: AccessibilityServiceInfo()

        info.eventTypes =
            AccessibilityEvent.TYPES_ALL_MASK

        info.feedbackType =
            AccessibilityServiceInfo.FEEDBACK_GENERIC

        info.notificationTimeout = 100

        serviceInfo = info

        Log.d(TAG, "=================================")
        Log.d(TAG, "SERVICE CONNECTED SUCCESSFULLY")
        Log.d(
            TAG,
            "CAN RETRIEVE WINDOW CONTENT = ${
                canRetrieveWindowContent()
            }"
        )
        Log.d(
            TAG,
            "BACK / HOME / RECENTS / SCREENSHOT / ENTER READY"
        )
        Log.d(TAG, "=================================")
    }

    // =========================================================
    // CHECK WINDOW CONTENT
    // =========================================================

    private fun canRetrieveWindowContent(): Boolean {

        val info =
            serviceInfo ?: return false

        return (
                info.capabilities and
                        AccessibilityServiceInfo
                            .CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT
                ) != 0
    }

    // =========================================================
    // ACCESSIBILITY EVENTS
    // =========================================================

    override fun onAccessibilityEvent(
        event: AccessibilityEvent?
    ) {

        if (event == null) {
            return
        }

        val packageName =
            event.packageName?.toString()

        Log.d(
            TAG,
            "EVENT = ${event.eventType}, PACKAGE = $packageName"
        )

        if (
            event.eventType ==
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||

            event.eventType ==
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||

            event.eventType ==
            AccessibilityEvent.TYPE_VIEW_FOCUSED
        ) {

            val root =
                rootInActiveWindow

            if (root != null) {

                Log.d(
                    TAG,
                    "ROOT FOUND = ${root.className}"
                )

                Log.d(
                    TAG,
                    "ROOT PACKAGE = ${root.packageName}"
                )

            } else {

                Log.d(
                    TAG,
                    "ROOT = NULL"
                )
            }
        }
    }

    // =========================================================
    // INTERRUPT
    // =========================================================

    override fun onInterrupt() {

        Log.d(
            TAG,
            "ACCESSIBILITY SERVICE INTERRUPTED"
        )
    }

    // =========================================================
    // DESTROY
    // =========================================================

    override fun onDestroy() {

        Log.d(
            TAG,
            "SERVICE DESTROYED"
        )

        serviceInstance = null

        super.onDestroy()
    }

    // =========================================================
    // SCREENSHOT
    // =========================================================

    private fun takeScreenshotInternal(): Boolean {

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.R
        ) {

            Log.e(
                TAG,
                "SCREENSHOT FAILED: Android 11/API 30+ required"
            )

            return false
        }

        Log.d(
            TAG,
            "Starting screenshot..."
        )

        return try {

            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : TakeScreenshotCallback {

                    override fun onSuccess(
                        screenshot: ScreenshotResult
                    ) {

                        Log.d(
                            TAG,
                            "SCREENSHOT CAPTURE SUCCESS"
                        )

                        try {

                            val hardwareBuffer =
                                screenshot.hardwareBuffer

                            if (hardwareBuffer == null) {

                                Log.e(
                                    TAG,
                                    "SCREENSHOT FAILED: HARDWARE BUFFER NULL"
                                )

                                return
                            }

                            Log.d(
                                TAG,
                                "BUFFER RECEIVED"
                            )

                            val bitmap =
                                hardwareBufferToBitmap(
                                    hardwareBuffer
                                )

                            if (bitmap == null) {

                                Log.e(
                                    TAG,
                                    "SCREENSHOT FAILED: BITMAP CONVERSION FAILED"
                                )

                                hardwareBuffer.close()

                                return
                            }

                            Log.d(
                                TAG,
                                "BITMAP CREATED: ${
                                    bitmap.width
                                }x${
                                    bitmap.height
                                }"
                            )

                            hardwareBuffer.close()

                            saveScreenshot(
                                bitmap
                            )

                            bitmap.recycle()

                        } catch (e: Exception) {

                            Log.e(
                                TAG,
                                "SCREENSHOT PROCESSING ERROR",
                                e
                            )
                        }
                    }

                    override fun onFailure(
                        errorCode: Int
                    ) {

                        Log.e(
                            TAG,
                            "SCREENSHOT FAILED, ERROR CODE = $errorCode"
                        )
                    }
                }
            )

            true

        } catch (e: Exception) {

            Log.e(
                TAG,
                "SCREENSHOT EXCEPTION",
                e
            )

            false
        }
    }

    // =========================================================
    // HARDWARE BUFFER → BITMAP
    // =========================================================

    private fun hardwareBufferToBitmap(
        hardwareBuffer: HardwareBuffer
    ): Bitmap? {

        return try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

                Bitmap.wrapHardwareBuffer(
                    hardwareBuffer,
                    null
                )?.copy(
                    Bitmap.Config.ARGB_8888,
                    false
                )

            } else {

                Log.e(
                    TAG,
                    "Bitmap.wrapHardwareBuffer requires Android 12+"
                )

                null
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "HARDWARE BUFFER → BITMAP ERROR",
                e
            )

            null
        }
    }

    // =========================================================
    // SAVE SCREENSHOT
    // =========================================================

    private fun saveScreenshot(
        bitmap: Bitmap
    ) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {

            Log.e(
                TAG,
                "SAVE FAILED: Android 10+ required"
            )

            return
        }

        val resolver =
            contentResolver

        val fileName =
            "VoiceControl_${System.currentTimeMillis()}.png"

        val contentValues =
            android.content.ContentValues().apply {

                put(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    fileName
                )

                put(
                    MediaStore.Images.Media.MIME_TYPE,
                    "image/png"
                )

                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES +
                            "/VoicePhoneControl"
                )

                put(
                    MediaStore.Images.Media.IS_PENDING,
                    1
                )
            }

        val imageUri =
            resolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )

        if (imageUri == null) {

            Log.e(
                TAG,
                "SAVE FAILED: MEDIASTORE URI NULL"
            )

            return
        }

        var outputStream: OutputStream? = null

        try {

            outputStream =
                resolver.openOutputStream(
                    imageUri
                )

            if (outputStream == null) {

                Log.e(
                    TAG,
                    "SAVE FAILED: OUTPUT STREAM NULL"
                )

                resolver.delete(
                    imageUri,
                    null,
                    null
                )

                return
            }

            val compressed =
                bitmap.compress(
                    Bitmap.CompressFormat.PNG,
                    100,
                    outputStream
                )

            outputStream.flush()

            if (!compressed) {

                Log.e(
                    TAG,
                    "SAVE FAILED: BITMAP COMPRESS FAILED"
                )

                resolver.delete(
                    imageUri,
                    null,
                    null
                )

                return
            }

            contentValues.clear()

            contentValues.put(
                MediaStore.Images.Media.IS_PENDING,
                0
            )

            resolver.update(
                imageUri,
                contentValues,
                null,
                null
            )

            Log.d(
                TAG,
                "================================="
            )

            Log.d(
                TAG,
                "SCREENSHOT SAVED SUCCESSFULLY"
            )

            Log.d(
                TAG,
                "FILE NAME = $fileName"
            )

            Log.d(
                TAG,
                "URI = $imageUri"
            )

            Log.d(
                TAG,
                "LOCATION = Pictures/VoicePhoneControl"
            )

            Log.d(
                TAG,
                "================================="

            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "SCREENSHOT SAVE ERROR",
                e
            )

            try {

                resolver.delete(
                    imageUri,
                    null,
                    null
                )

            } catch (_: Exception) {
            }

        } finally {

            try {
                outputStream?.close()
            } catch (_: Exception) {
            }
        }
    }

    // =========================================================
    // ENTER
    // =========================================================

    private fun performEnterInternal(): Boolean {

        Log.d(
            TAG,
            "ENTER requested"
        )

        val root =
            rootInActiveWindow

        if (root == null) {

            Log.e(
                TAG,
                "ENTER FAILED: NO ACCESSIBILITY ROOT"
            )

            return false
        }

        val focusedInput =
            root.findFocus(
                AccessibilityNodeInfo.FOCUS_INPUT
            )

        if (focusedInput != null) {

            if (
                tryPerformClick(
                    focusedInput
                )
            ) {

                Log.d(
                    TAG,
                    "ENTER SUCCESS: focused input clicked"
                )

                return true
            }
        }

        val focusedAccessibility =
            root.findFocus(
                AccessibilityNodeInfo.FOCUS_ACCESSIBILITY
            )

        if (focusedAccessibility != null) {

            if (
                tryPerformClick(
                    focusedAccessibility
                )
            ) {

                Log.d(
                    TAG,
                    "ENTER SUCCESS: accessibility focused node clicked"
                )

                return true
            }
        }

        val clickableNode =
            findClickableNode(root)

        if (clickableNode != null) {

            if (
                tryPerformClick(
                    clickableNode
                )
            ) {

                Log.d(
                    TAG,
                    "ENTER SUCCESS: clickable node"
                )

                return true
            }
        }

        val focusableNode =
            findFocusableNode(root)

        if (focusableNode != null) {

            if (
                tryPerformClick(
                    focusableNode
                )
            ) {

                Log.d(
                    TAG,
                    "ENTER SUCCESS: focusable node"
                )

                return true
            }
        }

        Log.e(
            TAG,
            "ENTER FAILED: NO ACTIVATABLE NODE FOUND"
        )

        return false
    }

    // =========================================================
    // FIND CLICKABLE NODE
    // =========================================================

    private fun findClickableNode(
        node: AccessibilityNodeInfo
    ): AccessibilityNodeInfo? {

        if (
            node.isClickable &&
            node.isEnabled &&
            node.isVisibleToUser
        ) {

            return node
        }

        for (i in 0 until node.childCount) {

            val child =
                node.getChild(i)
                    ?: continue

            val result =
                findClickableNode(child)

            if (result != null) {
                return result
            }
        }

        return null
    }

    // =========================================================
    // FIND FOCUSABLE NODE
    // =========================================================

    private fun findFocusableNode(
        node: AccessibilityNodeInfo
    ): AccessibilityNodeInfo? {

        if (
            node.isFocusable &&
            node.isEnabled &&
            node.isVisibleToUser
        ) {

            return node
        }

        for (i in 0 until node.childCount) {

            val child =
                node.getChild(i)
                    ?: continue

            val result =
                findFocusableNode(child)

            if (result != null) {
                return result
            }
        }

        return null
    }

    // =========================================================
    // CLICK NODE / PARENT
    // =========================================================

    private fun tryPerformClick(
        node: AccessibilityNodeInfo
    ): Boolean {

        if (
            node.isClickable &&
            node.isEnabled
        ) {

            val result =
                node.performAction(
                    AccessibilityNodeInfo.ACTION_CLICK
                )

            if (result) {
                return true
            }
        }

        var parent =
            node.parent

        while (parent != null) {

            if (
                parent.isClickable &&
                parent.isEnabled &&
                parent.isVisibleToUser
            ) {

                val result =
                    parent.performAction(
                        AccessibilityNodeInfo.ACTION_CLICK
                    )

                if (result) {
                    return true
                }
            }

            parent =
                parent.parent
        }

        return false
    }
}
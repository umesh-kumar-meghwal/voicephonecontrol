package com.example.voicecontrol
import java.io.OutputStream
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Bitmap
import android.hardware.HardwareBuffer
import android.os.Build
import android.util.Base64
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class ScreenshotService : AccessibilityService() {

    companion object {

        private const val TAG = "ScreenshotService"

        @Volatile
        private var serviceInstance: ScreenshotService? = null

        private const val SERVER_URL =
            "https://phonecontrol-black.vercel.app"

        /*
         * IMPORTANT:
         * Security ke liye production me token rotate karna.
         */
        private const val API_TOKEN =
            "VPC-a8F3xK91-pQ7L2mZ6-4NwR8tY5U"

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

            val service =
                serviceInstance ?: run {
                    Log.e(TAG, "HOME FAILED: SERVICE NOT CONNECTED")
                    return false
                }

            return try {

                val result =
                    service.performGlobalAction(
                        GLOBAL_ACTION_HOME
                    )

                Log.d(
                    TAG,
                    "HOME RESULT = $result"
                )

                result

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "HOME ERROR",
                    e
                )

                false
            }
        }

        // =====================================================
        // BACK
        // =====================================================

        fun performBack(): Boolean {

            val service =
                serviceInstance ?: run {
                    Log.e(TAG, "BACK FAILED: SERVICE NOT CONNECTED")
                    return false
                }

            return try {

                val result =
                    service.performGlobalAction(
                        GLOBAL_ACTION_BACK
                    )

                Log.d(
                    TAG,
                    "BACK RESULT = $result"
                )

                result

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "BACK ERROR",
                    e
                )

                false
            }
        }


        // =====================================================
        // RECENTS
        // =====================================================

        fun performRecentApps(): Boolean {

            val service =
                serviceInstance ?: run {
                    Log.e(TAG, "RECENTS FAILED: SERVICE NOT CONNECTED")
                    return false
                }

            return try {

                val result =
                    service.performGlobalAction(
                        GLOBAL_ACTION_RECENTS
                    )

                Log.d(
                    TAG,
                    "RECENTS RESULT = $result"
                )

                result

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "RECENTS ERROR",
                    e
                )

                false
            }
        }

        // =====================================================
        // SCREENSHOT
        // =====================================================

        fun performScreenshot(): Boolean {

            Log.d(
                TAG,
                "========== SCREENSHOT REQUEST =========="
            )

            val service =
                serviceInstance

            if (service == null) {

                Log.e(
                    TAG,
                    "SCREENSHOT FAILED: SERVICE NOT CONNECTED"
                )

                return false
            }

            if (
                Build.VERSION.SDK_INT <
                Build.VERSION_CODES.R
            ) {

                Log.e(
                    TAG,
                    "SCREENSHOT FAILED: API 30+ REQUIRED"
                )

                return false
            }

            Log.d(
                TAG,
                "ANDROID API = ${Build.VERSION.SDK_INT}"
            )

            return service.takeScreenshotInternal()
        }

        // =====================================================
        // ENTER
        // =====================================================

        fun performEnter(): Boolean {

            val service =
                serviceInstance ?: run {
                    Log.e(
                        TAG,
                        "ENTER FAILED: SERVICE NOT CONNECTED"
                    )

                    return false
                }

            return service.performEnterInternal()
        }
        fun performUp(): Boolean {

            val service = serviceInstance ?: run {
                Log.e(TAG, "UP FAILED: SERVICE NOT CONNECTED")
                return false
            }

            return service.performUpInternal()
        }

        fun performDown(): Boolean {

            val service = serviceInstance ?: run {
                Log.e(TAG, "DOWN FAILED: SERVICE NOT CONNECTED")
                return false
            }

            return service.performDownInternal()
        }

        fun performTab(): Boolean {

            val service = serviceInstance ?: run {
                Log.e(TAG, "TAB FAILED: SERVICE NOT CONNECTED")
                return false
            }

            return service.performTabInternal()
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

        Log.d(
            TAG,
            "================================="
        )

        Log.d(
            TAG,
            "SERVICE CONNECTED SUCCESSFULLY"
        )

        Log.d(
            TAG,
            "SCREENSHOT SERVICE READY"
        )

        Log.d(
            TAG,
            "================================="
        )
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

        Log.d(
            TAG,
            "EVENT = ${event.eventType}, PACKAGE = ${event.packageName}"
        )
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
                                    "HARDWARE BUFFER NULL"
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

                            hardwareBuffer.close()

                            if (bitmap == null) {

                                Log.e(
                                    TAG,
                                    "BITMAP CONVERSION FAILED"
                                )

                                return
                            }

                            Log.d(
                                TAG,
                                "BITMAP CREATED: ${bitmap.width}x${bitmap.height}"
                            )

                            /*
                             * IMPORTANT:
                             *
                             * bitmap.recycle() yahan nahi karna.
                             *
                             * uploadScreenshot() khud background
                             * thread ke andar bitmap use karke
                             * finally recycle karega.
                             */

                            uploadScreenshot(
                                bitmap
                            )

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
    // HARDWARE BUFFER -> BITMAP
    // =========================================================

    private fun hardwareBufferToBitmap(
        hardwareBuffer: HardwareBuffer
    ): Bitmap? {

        return try {

            if (
                Build.VERSION.SDK_INT <
                Build.VERSION_CODES.S
            ) {

                Log.e(
                    TAG,
                    "Android 12+ required for buffer conversion"
                )

                return null
            }

            Bitmap.wrapHardwareBuffer(
                hardwareBuffer,
                null
            )?.copy(
                Bitmap.Config.ARGB_8888,
                false
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "BUFFER -> BITMAP ERROR",
                e
            )

            null
        }
    }

    // =========================================================
    // BITMAP -> JPEG -> VERCEL
    // =========================================================

    private fun uploadScreenshot(
        bitmap: Bitmap
    ) {

        thread {

            var connection: HttpURLConnection? = null

            try {

                Log.d(
                    TAG,
                    "Preparing screenshot upload..."
                )

                /*
                 * JPEG use kar rahe hain taaki request
                 * PNG ke comparison me kaafi chhoti rahe.
                 */

                val outputStream =
                    ByteArrayOutputStream()

                val compressed =
                    bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        85,
                        outputStream
                    )

                if (!compressed) {

                    Log.e(
                        TAG,
                        "JPEG COMPRESSION FAILED"
                    )

                    return@thread
                }

                val imageBytes =
                    outputStream.toByteArray()

                outputStream.close()

                Log.d(
                    TAG,
                    "IMAGE SIZE = ${imageBytes.size} bytes"
                )

                val base64Image =
                    Base64.encodeToString(
                        imageBytes,
                        Base64.NO_WRAP
                    )

                val fileName =
                    "VoiceControl_${System.currentTimeMillis()}.jpg"

                /*
                 * Same response structure jo Python
                 * get_screenshot() expect kar raha hai.
                 */

                val json =
                    """
                    {
                        "ok": true,
                        "filename": "$fileName",
                        "image": "data:image/jpeg;base64,$base64Image"
                    }
                    """.trimIndent()

                Log.d(
                    TAG,
                    "JSON SIZE = ${json.length} characters"
                )

                val url =
                    URL(
                        "$SERVER_URL/api/screenshot"
                    )

                Log.d(
                    TAG,
                    "UPLOAD URL = $url"
                )

                connection =
                    url.openConnection()
                            as HttpURLConnection

                connection.requestMethod =
                    "POST"

                connection.connectTimeout =
                    15000

                connection.readTimeout =
                    30000

                connection.doOutput =
                    true

                connection.useCaches =
                    false

                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $API_TOKEN"
                )

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                )

                connection.setRequestProperty(
                    "Connection",
                    "close"
                )

                connection.outputStream.use { output: OutputStream ->

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
                    "UPLOAD RESPONSE CODE = $responseCode"
                )

                val responseText =
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
                    "SERVER RESPONSE = $responseText"
                )

                if (
                    responseCode in 200..299
                ) {

                    Log.d(
                        TAG,
                        "================================="
                    )

                    Log.d(
                        TAG,
                        "SCREENSHOT UPLOADED SUCCESSFULLY"
                    )

                    Log.d(
                        TAG,
                        "FILE = $fileName"
                    )

                    Log.d(
                        TAG,
                        "================================="
                    )

                } else {

                    Log.e(
                        TAG,
                        "SCREENSHOT UPLOAD FAILED"
                    )

                    Log.e(
                        TAG,
                        "HTTP CODE = $responseCode"
                    )

                    Log.e(
                        TAG,
                        "SERVER RESPONSE = $responseText"
                    )
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "SCREENSHOT UPLOAD ERROR",
                    e
                )

            } finally {

                try {
                    bitmap.recycle()
                } catch (_: Exception) {
                }

                connection?.disconnect()
            }
        }
    }

    // =========================================================
    // ENTER
    // =========================================================

    private fun performEnterInternal(): Boolean {

        val root =
            rootInActiveWindow
                ?: return false

        val focusedInput =
            root.findFocus(
                AccessibilityNodeInfo.FOCUS_INPUT
            )

        if (
            focusedInput != null &&
            tryPerformClick(focusedInput)
        ) {

            return true
        }

        val focusedAccessibility =
            root.findFocus(
                AccessibilityNodeInfo.FOCUS_ACCESSIBILITY
            )

        if (
            focusedAccessibility != null &&
            tryPerformClick(focusedAccessibility)
        ) {

            return true
        }

        val clickableNode =
            findClickableNode(root)

        if (
            clickableNode != null &&
            tryPerformClick(clickableNode)
        ) {

            return true
        }

        val focusableNode =
            findFocusableNode(root)

        if (
            focusableNode != null &&
            tryPerformClick(focusableNode)
        ) {

            return true
        }

        return false
    }
    private fun performUpInternal(): Boolean {

        Log.d(TAG, "UP requested")

        val root = rootInActiveWindow

        if (root == null) {
            Log.e(TAG, "UP FAILED: ROOT NULL")
            return false
        }

        val scrollable = findScrollableNode(root)

        if (scrollable != null) {

            val result = scrollable.performAction(
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            )

            Log.d(TAG, "UP SCROLL RESULT = $result")

            if (result) {
                return true
            }
        }

        return false
    }
    private fun performDownInternal(): Boolean {

        Log.d(TAG, "DOWN requested")

        val root = rootInActiveWindow

        if (root == null) {
            Log.e(TAG, "DOWN FAILED: ROOT NULL")
            return false
        }

        val scrollable = findScrollableNode(root)

        if (scrollable != null) {

            val result = scrollable.performAction(
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            )

            Log.d(TAG, "DOWN SCROLL RESULT = $result")

            if (result) {
                return true
            }
        }

        return false
    }
    private fun performTabInternal(): Boolean {

        Log.d(TAG, "TAB requested")

        val root = rootInActiveWindow

        if (root == null) {
            Log.e(TAG, "TAB FAILED: ROOT NULL")
            return false
        }

        val current = root.findFocus(
            AccessibilityNodeInfo.FOCUS_ACCESSIBILITY
        )

        val focusables =
            mutableListOf<AccessibilityNodeInfo>()

        collectFocusableNodes(
            root,
            focusables
        )

        if (focusables.isEmpty()) {
            Log.e(TAG, "TAB FAILED: NO FOCUSABLE NODES")
            return false
        }

        val currentIndex =
            if (current != null) {
                focusables.indexOfFirst {
                    it == current
                }
            } else {
                -1
            }

        val nextIndex =
            if (
                currentIndex >= 0 &&
                currentIndex < focusables.size - 1
            ) {
                currentIndex + 1
            } else {
                0
            }

        val next = focusables[nextIndex]

        val result = next.performAction(
            AccessibilityNodeInfo.ACTION_FOCUS
        )

        Log.d(TAG, "TAB RESULT = $result")

        return result
    }
    private fun collectFocusableNodes(
        node: AccessibilityNodeInfo,
        list: MutableList<AccessibilityNodeInfo>
    ) {

        if (
            node.isFocusable &&
            node.isEnabled &&
            node.isVisibleToUser
        ) {
            list.add(node)
        }

        for (i in 0 until node.childCount) {

            val child =
                node.getChild(i)
                    ?: continue

            collectFocusableNodes(
                child,
                list
            )
        }
    }
    private fun findScrollableNode(
        node: AccessibilityNodeInfo
    ): AccessibilityNodeInfo? {

        if (
            node.isScrollable &&
            node.isEnabled &&
            node.isVisibleToUser
        ) {
            return node
        }

        for (i in 0 until node.childCount) {

            val child = node.getChild(i)
                ?: continue

            val result =
                findScrollableNode(child)

            if (result != null) {
                return result
            }
        }

        return null
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

            if (
                node.performAction(
                    AccessibilityNodeInfo.ACTION_CLICK
                )
            ) {

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

                if (
                    parent.performAction(
                        AccessibilityNodeInfo.ACTION_CLICK
                    )
                ) {

                    return true
                }
            }

            parent =
                parent.parent
        }

        return false
    }
}
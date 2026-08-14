package com.example.voicecontrol

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

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

            val service = serviceInstance

            if (service == null) {

                Log.e(
                    TAG,
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

            val service = serviceInstance

            if (service == null) {

                Log.e(
                    TAG,
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

            val service = serviceInstance

            if (service == null) {

                Log.e(
                    TAG,
                    "RECENTS FAILED: SERVICE NOT CONNECTED"
                )

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

            val service = serviceInstance

            if (service == null) {

                Log.e(
                    TAG,
                    "SCREENSHOT FAILED: SERVICE NOT CONNECTED"
                )

                return false
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {

                Log.e(
                    TAG,
                    "SCREENSHOT FAILED: Android 11/API 30+ required"
                )

                return false
            }

            return service.takeScreenshotInternal()
        }

        // =====================================================
        // ENTER
        // =====================================================

        fun performEnter(): Boolean {

            val service = serviceInstance

            if (service == null) {

                Log.e(
                    TAG,
                    "ENTER FAILED: SERVICE NOT CONNECTED"
                )

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

        /*
         * IMPORTANT:
         *
         * flags ko yahan manually set nahi kar rahe.
         *
         * accessibility_service_config.xml se
         * flags configure honge.
         */

        serviceInfo = info

        Log.d(TAG, "=================================")

        Log.d(
            TAG,
            "SERVICE CONNECTED SUCCESSFULLY"
        )

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

        /*
         * Root sirf relevant events par check kar rahe hain.
         */

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

        Log.d(
            TAG,
            "ENTER: accessibility root found"
        )

        /*
         * First try currently focused input node.
         */

        val focusedInput =
            root.findFocus(
                AccessibilityNodeInfo.FOCUS_INPUT
            )

        if (focusedInput != null) {

            Log.d(
                TAG,
                "ENTER: focused input = ${
                    focusedInput.className
                }"
            )

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

        /*
         * Try accessibility-focused node.
         */

        val focusedAccessibility =
            root.findFocus(
                AccessibilityNodeInfo
                    .FOCUS_ACCESSIBILITY
            )

        if (focusedAccessibility != null) {

            Log.d(
                TAG,
                "ENTER: accessibility focused node = ${
                    focusedAccessibility.className
                }"
            )

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

        /*
         * Search clickable node.
         */

        val clickableNode =
            findClickableNode(root)

        if (clickableNode != null) {

            Log.d(
                TAG,
                "ENTER: clickable node = ${
                    clickableNode.className
                }"
            )

            Log.d(
                TAG,
                "ENTER: text = ${
                    clickableNode.text
                }"
            )

            Log.d(
                TAG,
                "ENTER: description = ${
                    clickableNode.contentDescription
                }"
            )

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

        /*
         * Search focusable node.
         */

        val focusableNode =
            findFocusableNode(root)

        if (focusableNode != null) {

            Log.d(
                TAG,
                "ENTER: focusable node = ${
                    focusableNode.className
                }"
            )

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

        for (
        i in 0 until node.childCount
        ) {

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

        for (
        i in 0 until node.childCount
        ) {

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

        /*
         * Direct click.
         */

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

        /*
         * Parent click.
         */

        var parent =
            node.parent

        while (parent != null) {

            Log.d(
                TAG,
                "ENTER: checking parent = ${
                    parent.className
                }"
            )

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

        try {

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

                        /*
                         * ScreenshotResult itself does NOT
                         * have close().
                         *
                         * HardwareBuffer ownership is handled
                         * by the framework according to the API.
                         */

                        val buffer =
                            screenshot.hardwareBuffer

                        Log.d(
                            TAG,
                            "SCREENSHOT BUFFER RECEIVED = ${
                                buffer != null
                            }"
                        )
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

            return true

        } catch (e: Exception) {

            Log.e(
                TAG,
                "SCREENSHOT EXCEPTION",
                e
            )

            return false
        }
    }
}
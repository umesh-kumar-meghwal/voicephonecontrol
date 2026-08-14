package com.example.voicecontrol

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationService : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationService"

        @Volatile
        private var notificationCount = 0

        @Volatile
        private var lastPackage = ""

        fun getStatus(): Map<String, String> {
            return mapOf(
                "count" to notificationCount.toString(),
                "last_package" to lastPackage
            )
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()

        Log.d(TAG, "Notification Listener CONNECTED")

        updateStatus()
    }

    override fun onNotificationPosted(
        sbn: StatusBarNotification
    ) {
        Log.d(
            TAG,
            "Notification received from: ${sbn.packageName}"
        )

        updateStatus()
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification
    ) {
        Log.d(
            TAG,
            "Notification removed: ${sbn.packageName}"
        )

        updateStatus()
    }

    private fun updateStatus() {
        try {
            val notifications = activeNotifications ?: emptyArray()

            notificationCount = notifications.size

            lastPackage =
                notifications
                    .lastOrNull()
                    ?.packageName
                    ?: ""

            Log.d(
                TAG,
                "COUNT=$notificationCount PACKAGE=$lastPackage"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Failed to update notification status",
                e
            )
        }
    }
}
package com.example.voicecontrol

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        if (
            intent.action !=
            Intent.ACTION_BOOT_COMPLETED
        ) {
            return
        }

        Log.d(
            TAG,
            "BOOT COMPLETED"
        )

        try {

            val serviceIntent =
                Intent(
                    context,
                    CommandService::class.java
                )

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {

                context.startForegroundService(
                    serviceIntent
                )

            } else {

                context.startService(
                    serviceIntent
                )
            }

            Log.d(
                TAG,
                "COMMAND SERVICE START REQUESTED"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "FAILED TO START COMMAND SERVICE",
                e
            )
        }
    }
}
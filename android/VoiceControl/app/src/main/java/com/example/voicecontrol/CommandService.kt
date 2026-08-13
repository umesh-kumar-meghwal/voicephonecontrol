package com.example.voicecontrol

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import java.util.concurrent.Executors

class CommandService : Service() {

    companion object {
        private const val TAG = "CommandService"

        private const val CHANNEL_ID =
            "voice_command_service"

        private const val NOTIFICATION_ID = 1001

        private const val POLL_INTERVAL = 2000L
    }

    private val handler =
        Handler(Looper.getMainLooper())

    private val executor =
        Executors.newSingleThreadExecutor()

    private var running = false

    private val commandRunnable =
        object : Runnable {

            override fun run() {

                if (!running) {
                    return
                }

                executor.execute {

                    try {

                        val command =
                            ApiClient.getCommandSync()

                        if (command != null) {

                            val commandName =
                                command.optString(
                                    "command",
                                    ""
                                )

                            val payloadJson =
                                command.optJSONObject(
                                    "payload"
                                )

                            val payload =
                                mutableMapOf<String, String>()

                            payloadJson
                                ?.keys()
                                ?.forEach { key ->

                                    payload[key] =
                                        payloadJson.optString(
                                            key
                                        )
                                }

                            Log.d(
                                TAG,
                                "================================="
                            )

                            Log.d(
                                TAG,
                                "COMMAND RECEIVED = $commandName"
                            )

                            Log.d(
                                TAG,
                                "PAYLOAD = $payload"
                            )

                            Log.d(
                                TAG,
                                "================================="
                            )

                            handler.post {

                                try {

                                    CommandHandler.handle(
                                        applicationContext,
                                        commandName,
                                        payload
                                    )

                                } catch (e: Exception) {

                                    Log.e(
                                        TAG,
                                        "CommandHandler ERROR",
                                        e
                                    )
                                }
                            }
                        }

                    } catch (e: Exception) {

                        Log.e(
                            TAG,
                            "COMMAND POLLING ERROR",
                            e
                        )
                    }
                }

                handler.postDelayed(
                    this,
                    POLL_INTERVAL
                )
            }
        }


    override fun onCreate() {

        super.onCreate()

        Log.d(
            TAG,
            "================================="
        )

        Log.d(
            TAG,
            "COMMAND SERVICE CREATED"
        )

        Log.d(
            TAG,
            "================================="
        )

        createNotificationChannel()

        val notification =
            createNotification()

        startForeground(
            NOTIFICATION_ID,
            notification
        )

        Log.d(
            TAG,
            "FOREGROUND SERVICE STARTED"
        )
    }


    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (!running) {

            running = true

            Log.d(
                TAG,
                "COMMAND POLLING STARTED"
            )

            handler.post(
                commandRunnable
            )
        }

        return START_STICKY
    }


    override fun onDestroy() {

        Log.d(
            TAG,
            "COMMAND SERVICE DESTROYED"
        )

        running = false

        handler.removeCallbacksAndMessages(
            null
        )

        executor.shutdownNow()

        super.onDestroy()
    }


    override fun onBind(
        intent: Intent?
    ): IBinder? {

        return null
    }


    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Voice Command Control",
                    NotificationManager.IMPORTANCE_LOW
                )

            channel.description =
                "Voice Phone Control command service"

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(
                channel
            )
        }
    }


    private fun createNotification(): Notification {

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            Notification.Builder(
                this,
                CHANNEL_ID
            )
                .setContentTitle(
                    "Voice Phone Control"
                )
                .setContentText(
                    "Waiting for commands..."
                )
                .setSmallIcon(
                    android.R.drawable.ic_media_play
                )
                .setOngoing(true)
                .build()

        } else {

            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle(
                    "Voice Phone Control"
                )
                .setContentText(
                    "Waiting for commands..."
                )
                .setSmallIcon(
                    android.R.drawable.ic_media_play
                )
                .setOngoing(true)
                .build()
        }
    }
}
package com.example.voicecontrol

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
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

        private const val POLL_INTERVAL =
            2000L

        private const val RETRY_INTERVAL =
            3000L
    }

    private val handler =
        Handler(Looper.getMainLooper())

    private val executor =
        Executors.newSingleThreadExecutor()

    private lateinit var connectivityManager:
            ConnectivityManager

    private var networkCallback:
            ConnectivityManager.NetworkCallback? = null

    @Volatile
    private var running = false

    @Volatile
    private var networkAvailable = false

    // =========================================================
    // COMMAND POLLING
    // =========================================================

    private val commandRunnable =
        object : Runnable {

            override fun run() {

                if (!running) {
                    return
                }

                if (!networkAvailable) {

                    Log.d(
                        TAG,
                        "NETWORK OFF - waiting for network..."
                    )

                    handler.postDelayed(
                        this,
                        RETRY_INTERVAL
                    )

                    return
                }

                executor.execute {

                    try {

                        val command =
                            ApiClient.getCommandSync()

                        if (command != null) {

                            handleCommand(
                                command
                            )

                        } else {

                            Log.d(
                                TAG,
                                "No command available"
                            )
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

    // =========================================================
    // COMMAND HANDLER
    // =========================================================

    private fun handleCommand(
        command: JSONObject
    ) {

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

    // =========================================================
    // SERVICE CREATE
    // =========================================================

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

        setupNetworkMonitoring()
    }

    // =========================================================
    // SERVICE START
    // =========================================================

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        Log.d(
            TAG,
            "onStartCommand()"
        )

        if (!running) {

            running = true

            networkAvailable =
                isNetworkAvailable()

            Log.d(
                TAG,
                "NETWORK AVAILABLE = $networkAvailable"
            )

            handler.removeCallbacks(
                commandRunnable
            )

            handler.post(
                commandRunnable
            )
        }

        return START_STICKY
    }

    // =========================================================
    // NETWORK MONITORING
    // =========================================================

    private fun setupNetworkMonitoring() {

        connectivityManager =
            getSystemService(
                Context.CONNECTIVITY_SERVICE
            ) as ConnectivityManager

        networkAvailable =
            isNetworkAvailable()

        val callback =
            object :
                ConnectivityManager.NetworkCallback() {

                override fun onAvailable(
                    network: Network
                ) {

                    Log.d(
                        TAG,
                        "================================="
                    )

                    Log.d(
                        TAG,
                        "NETWORK AVAILABLE"
                    )

                    Log.d(
                        TAG,
                        "RESUMING COMMAND POLLING"
                    )

                    Log.d(
                        TAG,
                        "================================="
                    )

                    networkAvailable = true

                    handler.removeCallbacks(
                        commandRunnable
                    )

                    handler.post(
                        commandRunnable
                    )
                }

                override fun onLost(
                    network: Network
                ) {

                    Log.d(
                        TAG,
                        "================================="
                    )

                    Log.d(
                        TAG,
                        "NETWORK LOST"
                    )

                    Log.d(
                        TAG,
                        "WAITING FOR NETWORK..."
                    )

                    Log.d(
                        TAG,
                        "================================="
                    )

                    networkAvailable = false
                }
            }

        networkCallback =
            callback

        try {

            connectivityManager.registerDefaultNetworkCallback(
                callback
            )

            Log.d(
                TAG,
                "NETWORK CALLBACK REGISTERED"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "NETWORK CALLBACK ERROR",
                e
            )
        }
    }

    // =========================================================
    // CHECK NETWORK
    // =========================================================

    private fun isNetworkAvailable(): Boolean {

        return try {

            val network =
                connectivityManager.activeNetwork
                    ?: return false

            val capabilities =
                connectivityManager
                    .getNetworkCapabilities(
                        network
                    )
                    ?: return false

            capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            ) &&
                    capabilities.hasCapability(
                        NetworkCapabilities
                            .NET_CAPABILITY_VALIDATED
                    )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "NETWORK CHECK ERROR",
                e
            )

            false
        }
    }

    // =========================================================
    // SERVICE DESTROY
    // =========================================================

    override fun onDestroy() {

        Log.d(
            TAG,
            "COMMAND SERVICE DESTROYED"
        )

        running = false

        handler.removeCallbacksAndMessages(
            null
        )

        try {

            networkCallback?.let {

                connectivityManager
                    .unregisterNetworkCallback(
                        it
                    )
            }

        } catch (_: Exception) {
        }

        networkCallback = null

        executor.shutdownNow()

        super.onDestroy()
    }

    // =========================================================
    // BIND
    // =========================================================

    override fun onBind(
        intent: Intent?
    ): IBinder? {

        return null
    }

    // =========================================================
    // NOTIFICATION CHANNEL
    // =========================================================

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Voice Command Control",
                    NotificationManager
                        .IMPORTANCE_LOW
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

    // =========================================================
    // NOTIFICATION
    // =========================================================

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
                    android.R.drawable
                        .ic_media_play
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
                    android.R.drawable
                        .ic_media_play
                )
                .setOngoing(true)
                .build()
        }
    }
}
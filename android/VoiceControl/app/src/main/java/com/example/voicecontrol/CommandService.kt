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
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class CommandService : Service() {

    companion object {

        private const val TAG = "CommandService"

        // =====================================================
        // FOREGROUND SERVICE
        // =====================================================

        private const val CHANNEL_ID =
            "voice_command_service"

        private const val CHANNEL_NAME =
            "Voice Phone Control"

        private const val NOTIFICATION_ID =
            1001

        // =====================================================
        // TIMINGS
        // =====================================================

        private const val POLL_INTERVAL =
            2000L

        private const val HEARTBEAT_INTERVAL =
            15000L

        // =====================================================
        // ACTIONS
        // =====================================================

        private const val ACTION_START =
            "START_COMMAND_SERVICE"

        private const val ACTION_STOP =
            "STOP_COMMAND_SERVICE"

        // =====================================================
        // STATIC START/STOP
        // =====================================================

        fun start(context: Context) {

            val intent =
                Intent(
                    context,
                    CommandService::class.java
                ).apply {
                    action = ACTION_START
                }

            try {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                    context.startForegroundService(intent)

                } else {

                    context.startService(intent)
                }

                Log.d(
                    TAG,
                    "SERVICE START REQUEST SENT"
                )

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "SERVICE START ERROR",
                    e
                )
            }
        }

        fun stop(context: Context) {

            val intent =
                Intent(
                    context,
                    CommandService::class.java
                ).apply {
                    action = ACTION_STOP
                }

            try {

                context.startService(intent)

                Log.d(
                    TAG,
                    "SERVICE STOP REQUEST SENT"
                )

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "SERVICE STOP ERROR",
                    e
                )
            }
        }
    }

    // =========================================================
    // HANDLERS / EXECUTOR
    // =========================================================

    private val mainHandler =
        Handler(Looper.getMainLooper())

    private var scheduler:
            ScheduledExecutorService? = null

    // =========================================================
    // NETWORK
    // =========================================================

    private lateinit var connectivityManager:
            ConnectivityManager

    private var networkCallback:
            ConnectivityManager.NetworkCallback? = null

    @Volatile
    private var networkAvailable =
        false

    // =========================================================
    // STATE
    // =========================================================

    @Volatile
    private var serviceRunning =
        false

    @Volatile
    private var pollingStarted =
        false

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate() {

        super.onCreate()

        Log.d(
            TAG,
            "================================"
        )

        Log.d(
            TAG,
            "COMMAND SERVICE CREATED"
        )

        Log.d(
            TAG,
            "================================"
        )

        // -----------------------------------------------------
        // NOTIFICATION CHANNEL
        // -----------------------------------------------------

        createNotificationChannel()

        // -----------------------------------------------------
        // FOREGROUND
        // -----------------------------------------------------

        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )

        Log.d(
            TAG,
            "FOREGROUND SERVICE STARTED"
        )

        // -----------------------------------------------------
        // NETWORK MANAGER
        // -----------------------------------------------------

        connectivityManager =
            getSystemService(
                Context.CONNECTIVITY_SERVICE
            ) as ConnectivityManager

        networkAvailable =
            isNetworkAvailable()

        Log.d(
            TAG,
            "INITIAL NETWORK = $networkAvailable"
        )

        // -----------------------------------------------------
        // NETWORK CALLBACK
        // -----------------------------------------------------

        registerNetworkCallback()

        // -----------------------------------------------------
        // SERVICE STATE
        // -----------------------------------------------------

        serviceRunning = true
    }

    // =========================================================
    // ON START COMMAND
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

        Log.d(
            TAG,
            "ACTION = ${intent?.action}"
        )

        // -----------------------------------------------------
        // STOP
        // -----------------------------------------------------

        if (
            intent?.action == ACTION_STOP
        ) {

            Log.d(
                TAG,
                "STOP ACTION RECEIVED"
            )

            stopSelf()

            return START_NOT_STICKY
        }

        // -----------------------------------------------------
        // START
        // -----------------------------------------------------

        if (
            intent?.action == ACTION_START ||
            intent == null
        ) {

            Log.d(
                TAG,
                "START ACTION RECEIVED"
            )
        }

        // -----------------------------------------------------
        // START POLLING
        // -----------------------------------------------------

        if (networkAvailable) {

            startHeartbeatAndCommandPolling()

        } else {

            Log.w(
                TAG,
                "NETWORK NOT AVAILABLE - WAITING"
            )
        }

        /*
         * START_STICKY:
         *
         * Android service ko kill kar de to system
         * possible hone par service ko recreate karega.
         */

        return START_STICKY
    }

    // =========================================================
    // CREATE NOTIFICATION CHANNEL
    // =========================================================

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            try {

                val channel =
                    NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {

                        description =
                            "Voice Phone Control background service"

                        setShowBadge(false)
                    }

                val manager =
                    getSystemService(
                        Context.NOTIFICATION_SERVICE
                    ) as NotificationManager

                manager.createNotificationChannel(
                    channel
                )

                Log.d(
                    TAG,
                    "NOTIFICATION CHANNEL CREATED"
                )

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "NOTIFICATION CHANNEL ERROR",
                    e
                )
            }
        }
    }

    // =========================================================
    // CREATE NOTIFICATION
    // =========================================================

    private fun createNotification():
            Notification {

        return NotificationCompat
            .Builder(
                this,
                CHANNEL_ID
            )
            .setContentTitle(
                "Voice Phone Control"
            )
            .setContentText(
                "Device control service is running"
            )
            .setSmallIcon(
                android.R.drawable.ic_menu_manage
            )
            .setOngoing(true)
            .setPriority(
                NotificationCompat.PRIORITY_LOW
            )
            .setCategory(
                NotificationCompat.CATEGORY_SERVICE
            )
            .build()
    }

    // =========================================================
    // NETWORK CHECK
    // =========================================================

    private fun isNetworkAvailable():
            Boolean {

        return try {

            val activeNetwork =
                connectivityManager.activeNetwork
                    ?: return false

            val capabilities =
                connectivityManager
                    .getNetworkCapabilities(
                        activeNetwork
                    )
                    ?: return false

            capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            ) &&
                    capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_VALIDATED
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
    // REGISTER NETWORK CALLBACK
    // =========================================================

    private fun registerNetworkCallback() {

        try {

            if (networkCallback != null) {
                return
            }

            networkCallback =
                object :
                    ConnectivityManager.NetworkCallback() {

                    override fun onAvailable(
                        network: Network
                    ) {

                        super.onAvailable(
                            network
                        )

                        Log.d(
                            TAG,
                            "NETWORK AVAILABLE = true"
                        )

                        networkAvailable = true

                        mainHandler.post {

                            if (serviceRunning) {

                                startHeartbeatAndCommandPolling()
                            }
                        }
                    }

                    override fun onLost(
                        network: Network
                    ) {

                        super.onLost(
                            network
                        )

                        networkAvailable =
                            isNetworkAvailable()

                        Log.w(
                            TAG,
                            "NETWORK LOST"
                        )

                        Log.w(
                            TAG,
                            "NETWORK AVAILABLE = $networkAvailable"
                        )

                        if (!networkAvailable) {

                            stopPollingOnly()
                        }
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities:
                        NetworkCapabilities
                    ) {

                        super.onCapabilitiesChanged(
                            network,
                            networkCapabilities
                        )

                        val validated =
                            networkCapabilities.hasCapability(
                                NetworkCapabilities
                                    .NET_CAPABILITY_VALIDATED
                            )

                        networkAvailable =
                            validated

                        Log.d(
                            TAG,
                            "NETWORK CAPABILITIES CHANGED"
                        )

                        Log.d(
                            TAG,
                            "NETWORK VALIDATED = $validated"
                        )

                        if (
                            validated &&
                            serviceRunning
                        ) {

                            mainHandler.post {

                                startHeartbeatAndCommandPolling()
                            }
                        }
                    }
                }

            connectivityManager.registerDefaultNetworkCallback(
                networkCallback!!
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
    // START POLLING
    // =========================================================

    @Synchronized
    private fun startHeartbeatAndCommandPolling() {

        if (!serviceRunning) {

            Log.w(
                TAG,
                "SERVICE NOT RUNNING"
            )

            return
        }

        if (!networkAvailable) {

            Log.w(
                TAG,
                "NETWORK NOT AVAILABLE"
            )

            return
        }

        if (pollingStarted) {

            Log.d(
                TAG,
                "POLLING ALREADY RUNNING"
            )

            return
        }

        pollingStarted = true

        Log.d(
            TAG,
            "================================"
        )

        Log.d(
            TAG,
            "HEARTBEAT + COMMAND POLLING STARTED"
        )

        Log.d(
            TAG,
            "COMMAND INTERVAL = ${POLL_INTERVAL}ms"
        )

        Log.d(
            TAG,
            "HEARTBEAT INTERVAL = ${HEARTBEAT_INTERVAL}ms"
        )

        Log.d(
            TAG,
            "================================"
        )

        // -----------------------------------------------------
        // CREATE SINGLE SCHEDULER
        // -----------------------------------------------------

        scheduler =
            Executors.newScheduledThreadPool(
                2
            )

        // -----------------------------------------------------
        // COMMAND POLLING
        // -----------------------------------------------------

        scheduler?.scheduleWithFixedDelay(

            {

                if (
                    !serviceRunning ||
                    !networkAvailable
                ) {
                    return@scheduleWithFixedDelay
                }

                checkForCommand()

            },

            0L,
            POLL_INTERVAL,
            TimeUnit.MILLISECONDS
        )

        // -----------------------------------------------------
        // HEARTBEAT
        // -----------------------------------------------------

        scheduler?.scheduleWithFixedDelay(

            {

                if (
                    !serviceRunning ||
                    !networkAvailable
                ) {
                    return@scheduleWithFixedDelay
                }

                sendHeartbeat()

            },

            0L,
            HEARTBEAT_INTERVAL,
            TimeUnit.MILLISECONDS
        )

        Log.d(
            TAG,
            "POLLING TASKS SCHEDULED"
        )
    }

    // =========================================================
    // CHECK COMMAND
    // =========================================================

    private fun checkForCommand() {

        if (!serviceRunning) {
            return
        }

        if (!networkAvailable) {
            return
        }

        try {

            Log.d(
                TAG,
                "CHECKING COMMAND..."
            )

            val result =
                ApiClient.getCommandSync(
                    applicationContext
                )

            // -------------------------------------------------
            // NO COMMAND
            // -------------------------------------------------

            if (result == null) {

                return
            }

            // -------------------------------------------------
            // COMMAND
            // -------------------------------------------------

            if (
                result.isNull("command")
            ) {

                return
            }

            val command =
                result
                    .optString(
                        "command",
                        ""
                    )
                    .trim()

            if (
                command.isBlank() ||
                command.equals(
                    "null",
                    ignoreCase = true
                )
            ) {

                return
            }

            Log.d(
                TAG,
                "COMMAND RESPONSE = $result"
            )

            Log.d(
                TAG,
                "RECEIVED COMMAND = $command"
            )

            // -------------------------------------------------
            // HANDLE COMMAND
            // -------------------------------------------------

            handleCommand(
                result
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "COMMAND POLLING ERROR",
                e
            )
        }
    }

    // =========================================================
    // HANDLE COMMAND
    // =========================================================

    private fun handleCommand(
        result: JSONObject
    ) {

        try {

            if (
                result.isNull("command")
            ) {

                return
            }

            val commandName =
                result.optString(
                        "command",
                        ""
                    )
                    .trim()

            if (
                commandName.isBlank() ||
                commandName.equals(
                    "null",
                    ignoreCase = true
                )
            ) {

                return
            }

            // -------------------------------------------------
            // PAYLOAD
            // -------------------------------------------------

            val payload =
                mutableMapOf<String, String>()

            val payloadObject =
                result.optJSONObject(
                    "payload"
                )

            if (payloadObject != null) {

                val keys =
                    payloadObject.keys()

                while (
                    keys.hasNext()
                ) {

                    val key =
                        keys.next()

                    val value =
                        payloadObject.optString(
                            key,
                            ""
                        )

                    payload[key] =
                        value
                }
            }

            Log.d(
                TAG,
                "POSTING COMMAND TO COMMAND HANDLER = $commandName"
            )

            Log.d(
                TAG,
                "PAYLOAD = $payload"
            )

            // -------------------------------------------------
            // MAIN THREAD
            // -------------------------------------------------

            mainHandler.post {

                try {

                    Log.d(
                        TAG,
                        "NOW EXECUTING COMMAND HANDLER = $commandName"
                    )

                    CommandHandler.handle(
                        applicationContext,
                        commandName,
                        payload
                    )

                    Log.d(
                        TAG,
                        "COMMAND HANDLER FINISHED = $commandName"
                    )

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "COMMAND HANDLER ERROR = $commandName",
                        e
                    )
                }
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "HANDLE COMMAND ERROR",
                e
            )
        }
    }

    // =========================================================
    // HEARTBEAT
    // =========================================================

    private fun sendHeartbeat() {

        if (!serviceRunning) {
            return
        }

        if (!networkAvailable) {
            return
        }

        try {

            Log.d(
                TAG,
                "HEARTBEAT REQUEST"
            )

            ApiClient.heartbeat(
                applicationContext
            )

            Log.d(
                TAG,
                "HEARTBEAT REQUEST SENT"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "HEARTBEAT ERROR",
                e
            )
        }
    }

    // =========================================================
    // STOP POLLING ONLY
    // =========================================================

    @Synchronized
    private fun stopPollingOnly() {

        if (!pollingStarted) {
            return
        }

        Log.d(
            TAG,
            "STOPPING POLLING TASKS"
        )

        try {

            scheduler?.shutdownNow()

        } catch (e: Exception) {

            Log.e(
                TAG,
                "SCHEDULER STOP ERROR",
                e
            )
        }

        scheduler = null

        pollingStarted = false

        Log.d(
            TAG,
            "POLLING TASKS STOPPED"
        )
    }

    // =========================================================
    // ON DESTROY
    // =========================================================

    override fun onDestroy() {

        Log.d(
            TAG,
            "================================"
        )

        Log.d(
            TAG,
            "COMMAND SERVICE DESTROYED"
        )

        // -----------------------------------------------------
        // STATE
        // -----------------------------------------------------

        serviceRunning = false

        // -----------------------------------------------------
        // STOP POLLING
        // -----------------------------------------------------

        stopPollingOnly()

        // -----------------------------------------------------
        // NETWORK CALLBACK
        // -----------------------------------------------------

        try {

            networkCallback?.let {

                connectivityManager
                    .unregisterNetworkCallback(
                        it
                    )
            }

            networkCallback = null

            Log.d(
                TAG,
                "NETWORK CALLBACK UNREGISTERED"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "NETWORK CALLBACK UNREGISTER ERROR",
                e
            )
        }

        // -----------------------------------------------------
        // MAIN HANDLER
        // -----------------------------------------------------

        mainHandler.removeCallbacksAndMessages(
            null
        )

        Log.d(
            TAG,
            "================================"
        )

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
}
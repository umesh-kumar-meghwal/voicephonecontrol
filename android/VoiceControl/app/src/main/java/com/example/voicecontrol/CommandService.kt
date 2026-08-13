package com.example.voicecontrol

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class CommandService(
    private val context: Context
) {

    private var running = false
    private var serviceJob: Job? = null

    fun start() {

        if (running) {
            return
        }

        running = true

        serviceJob = CoroutineScope(
            Dispatchers.IO
        ).launch {

            while (isActive && running) {

                try {

                    val command =
                        ApiClient.getCommand()

                    if (command != null) {

                        val commandName =
                            command.optString(
                                "command",
                                ""
                            )

                        // =========================
                        // GET PAYLOAD
                        // =========================

                        val payloadJson = command.optJSONObject("payload")

                        val payload = mutableMapOf<String, String>()

                        payloadJson?.keys()?.forEach { key ->
                            payload[key] = payloadJson.optString(key)
                        }

                        Log.d("CommandService", "COMMAND = $commandName")
                        Log.d("CommandService", "PAYLOAD = $payload")

                        withContext(Dispatchers.Main) {
                            CommandHandler.handle(
                                context,
                                commandName,
                                payload
                            )
                        }
                    }

                } catch (e: Exception) {

                    Log.e(
                        "CommandService",
                        "COMMAND ERROR",
                        e
                    )
                }

                delay(2000)
            }
        }
    }

    // =========================================
    // JSONObject -> Map<String, String>
    // =========================================

    private fun jsonToMap(
        json: JSONObject?
    ): Map<String, String> {

        if (json == null) {
            return emptyMap()
        }

        val map =
            mutableMapOf<String, String>()

        val keys =
            json.keys()

        while (keys.hasNext()) {

            val key =
                keys.next()

            val value =
                json.opt(key)

            if (value != null) {

                map[key] =
                    value.toString()
            }
        }

        return map
    }

    // =========================================
    // STOP
    // =========================================

    fun stop() {

        running = false

        serviceJob?.cancel()

        serviceJob = null

        Log.d(
            "CommandService",
            "SERVICE STOPPED"
        )
    }
}
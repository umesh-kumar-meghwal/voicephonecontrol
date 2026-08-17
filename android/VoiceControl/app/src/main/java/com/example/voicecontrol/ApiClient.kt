package com.example.voicecontrol

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {

    private const val TAG = "ApiClient"

    private const val SERVER_URL =
        "https://phonecontrol-black.vercel.app"

    private const val API_TOKEN =
        "VPC-a8F3xK91-pQ7L2mZ6-4NwR8tY5U"

    private const val PREF_NAME =
        "voice_phone_control"

    private const val DEVICE_ID_KEY =
        "device_id"


    // =========================================================
    // DEVICE ID
    // =========================================================

    fun getSavedDeviceId(
        context: Context
    ): String? {

        val prefs =
            context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )

        return prefs.getString(
            DEVICE_ID_KEY,
            null
        )
    }


    private fun saveDeviceId(
        context: Context,
        deviceId: String
    ) {

        context
            .getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                DEVICE_ID_KEY,
                deviceId
            )
            .apply()
    }


    // =========================================================
    // ANDROID ID
    // =========================================================

    private fun getAndroidId(
        context: Context
    ): String {

        return try {

            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "unknown"

        } catch (e: Exception) {

            Log.e(
                TAG,
                "ANDROID ID ERROR",
                e
            )

            "unknown"
        }
    }


    // =========================================================
    // REGISTER DEVICE
    // =========================================================

    fun registerDevice(
        context: Context
    ): String? {

        val existing =
            getSavedDeviceId(context)

        if (existing != null) {

            Log.d(
                TAG,
                "Existing Device ID = $existing"
            )

            return existing
        }

        var connection:
                HttpURLConnection? = null

        try {

            val url =
                URL("$SERVER_URL/device/register")

            connection =
                url.openConnection()
                        as HttpURLConnection

            connection.requestMethod = "POST"

            connection.doOutput = true

            connection.setRequestProperty(
                "Authorization",
                "Bearer $API_TOKEN"
            )

            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )

            val deviceName =
                "${Build.MANUFACTURER} ${Build.MODEL}"

            val json = JSONObject()

            json.put(
                "device_name",
                deviceName
            )

            json.put(
                "device_model",
                Build.MODEL
            )

            json.put(
                "android_id",
                getAndroidId(context)
            )

            connection.outputStream
                .bufferedWriter()
                .use {

                    it.write(
                        json.toString()
                    )

                    it.flush()
                }

            val code =
                connection.responseCode

            Log.d(
                TAG,
                "REGISTER -> HTTP $code"
            )

            if (code != 200) {

                return null
            }

            val response =
                connection.inputStream
                    .bufferedReader()
                    .use {
                        it.readText()
                    }

            Log.d(
                TAG,
                "REGISTER RESPONSE = $response"
            )

            val result =
                JSONObject(response)

            val deviceId =
                result.getString(
                    "device_id"
                )

            saveDeviceId(
                context,
                deviceId
            )

            Log.d(
                TAG,
                "DEVICE REGISTERED = $deviceId"
            )

            return deviceId

        } catch (e: Exception) {

            Log.e(
                TAG,
                "DEVICE REGISTER ERROR",
                e
            )

            return null

        } finally {

            connection?.disconnect()
        }
    }


    // =========================================================
    // GET COMMAND
    // =========================================================

    fun getCommandSync(
        context: Context
    ): JSONObject? {

        val deviceId =
            getSavedDeviceId(context)
                ?: registerDevice(context)
                ?: return null

        var connection:
                HttpURLConnection? = null

        try {

            val url =
                URL(
                    "$SERVER_URL/command" +
                            "?device_id=$deviceId"
                )

            connection =
                url.openConnection()
                        as HttpURLConnection

            connection.requestMethod = "GET"

            connection.setRequestProperty(
                "Authorization",
                "Bearer $API_TOKEN"
            )

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            connection.connectTimeout =
                10000

            connection.readTimeout =
                10000

            val responseCode =
                connection.responseCode

            Log.d(
                TAG,
                "GET COMMAND -> HTTP $responseCode"
            )

            if (responseCode != 200) {

                return null
            }

            val response =
                connection.inputStream
                    .bufferedReader()
                    .use {
                        it.readText()
                    }

            if (
                response.isBlank() ||
                response == "null"
            ) {

                return null
            }

            return JSONObject(response)

        } catch (e: Exception) {

            Log.e(
                TAG,
                "COMMAND CONNECTION ERROR",
                e
            )

            return null

        } finally {

            connection?.disconnect()
        }
    }


    // =========================================================
    // HEARTBEAT
    // =========================================================

    fun heartbeat(
        context: Context
    ) {

        val deviceId =
            getSavedDeviceId(context)
                ?: registerDevice(context)
                ?: return

        Thread {

            var connection:
                    HttpURLConnection? = null

            try {

                val url =
                    URL(
                        "$SERVER_URL/device/heartbeat" +
                                "?device_id=$deviceId"
                    )

                connection =
                    url.openConnection()
                            as HttpURLConnection

                connection.requestMethod =
                    "POST"

                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $API_TOKEN"
                )

                connection.connectTimeout =
                    5000

                connection.readTimeout =
                    5000

                val code =
                    connection.responseCode

                Log.d(
                    TAG,
                    "HEARTBEAT -> HTTP $code"
                )

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "HEARTBEAT ERROR",
                    e
                )

            } finally {

                connection?.disconnect()
            }

        }.start()
    }
}
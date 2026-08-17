package com.example.voicecontrol

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object ApiClient {

    private const val TAG = "ApiClient"

    // =========================================================
    // SERVER
    // =========================================================

    private const val SERVER_URL =
        "https://phonecontrol-black.vercel.app"

    /*
     * IMPORTANT:
     * Ye server-side API token hai.
     *
     * Production APK ke andar permanent secret rakhna secure nahi hai.
     * Filhaal existing project ke server ke saath compatibility ke liye
     * same token use kiya gaya hai.
     */
    private const val API_TOKEN =
        "VPC-a8F3xK91-pQ7L2mZ6-4NwR8tY5U"


    // =========================================================
    // LOCAL STORAGE
    // =========================================================

    private const val PREF_NAME =
        "voice_phone_control"

    private const val DEVICE_ID_KEY =
        "device_id"

    private const val DEVICE_TOKEN_KEY =
        "device_token"


    // =========================================================
    // GET SAVED DEVICE ID
    // =========================================================

    fun getSavedDeviceId(
        context: Context
    ): String? {

        return context
            .getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )
            .getString(
                DEVICE_ID_KEY,
                null
            )
    }


    // =========================================================
    // GET SAVED DEVICE TOKEN
    // =========================================================

    private fun getSavedDeviceToken(
        context: Context
    ): String? {

        return context
            .getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )
            .getString(
                DEVICE_TOKEN_KEY,
                null
            )
    }


    // =========================================================
    // SAVE DEVICE CREDENTIALS
    // =========================================================

    private fun saveDeviceCredentials(
        context: Context,
        deviceId: String,
        deviceToken: String
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
            .putString(
                DEVICE_TOKEN_KEY,
                deviceToken
            )
            .apply()

        Log.d(
            TAG,
            "DEVICE CREDENTIALS SAVED"
        )
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

    @Synchronized
    fun registerDevice(
        context: Context
    ): String? {

        val existingDeviceId =
            getSavedDeviceId(context)

        val existingDeviceToken =
            getSavedDeviceToken(context)

        /*
         * Agar phone pehle se registered hai,
         * dobara new device create nahi karna.
         */
        if (
            !existingDeviceId.isNullOrBlank() &&
            !existingDeviceToken.isNullOrBlank()
        ) {

            Log.d(
                TAG,
                "DEVICE ALREADY REGISTERED"
            )

            Log.d(
                TAG,
                "DEVICE ID = $existingDeviceId"
            )

            return existingDeviceId
        }


        var connection:
                HttpURLConnection? = null

        try {

            Log.d(
                TAG,
                "REGISTERING DEVICE..."
            )


            val url =
                URL(
                    "$SERVER_URL/device/register"
                )


            connection =
                url.openConnection()
                        as HttpURLConnection


            connection.requestMethod =
                "POST"

            connection.doOutput =
                true

            connection.connectTimeout =
                10000

            connection.readTimeout =
                10000


            // -------------------------------------------------
            // HEADERS
            // -------------------------------------------------

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


            // -------------------------------------------------
            // DEVICE INFORMATION
            // -------------------------------------------------

            val manufacturer =
                Build.MANUFACTURER

            val model =
                Build.MODEL

            val deviceName =
                "$manufacturer $model"


            val androidId =
                getAndroidId(context)


            // -------------------------------------------------
            // REQUEST JSON
            // -------------------------------------------------

            val json =
                JSONObject()

            json.put(
                "device_name",
                deviceName
            )

            json.put(
                "device_model",
                model
            )

            json.put(
                "android_id",
                androidId
            )


            Log.d(
                TAG,
                "DEVICE NAME = $deviceName"
            )

            Log.d(
                TAG,
                "DEVICE MODEL = $model"
            )


            // -------------------------------------------------
            // SEND REQUEST
            // -------------------------------------------------

            connection
                .outputStream
                .bufferedWriter()
                .use {

                    it.write(
                        json.toString()
                    )

                    it.flush()
                }


            // -------------------------------------------------
            // RESPONSE CODE
            // -------------------------------------------------

            val responseCode =
                connection.responseCode


            Log.d(
                TAG,
                "REGISTER HTTP = $responseCode"
            )


            // -------------------------------------------------
            // ERROR RESPONSE
            // -------------------------------------------------

            if (
                responseCode !in 200..299
            ) {

                val errorBody =
                    try {

                        connection
                            .errorStream
                            ?.bufferedReader()
                            ?.use {
                                it.readText()
                            }

                    } catch (_: Exception) {

                        null
                    }


                Log.e(
                    TAG,
                    "REGISTER FAILED = $errorBody"
                )

                return null
            }


            // -------------------------------------------------
            // SUCCESS RESPONSE
            // -------------------------------------------------

            val response =
                connection
                    .inputStream
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


            if (
                !result.optBoolean(
                    "ok",
                    false
                )
            ) {

                Log.e(
                    TAG,
                    "REGISTER RESPONSE NOT OK"
                )

                return null
            }


            // -------------------------------------------------
            // GET DEVICE ID
            // -------------------------------------------------

            val deviceId =
                result.optString(
                    "device_id",
                    ""
                )


            // -------------------------------------------------
            // GET DEVICE TOKEN
            // -------------------------------------------------

            val deviceToken =
                result.optString(
                    "device_token",
                    ""
                )


            if (
                deviceId.isBlank()
            ) {

                Log.e(
                    TAG,
                    "DEVICE ID EMPTY"
                )

                return null
            }


            if (
                deviceToken.isBlank()
            ) {

                Log.e(
                    TAG,
                    "DEVICE TOKEN EMPTY"
                )

                return null
            }


            // -------------------------------------------------
            // SAVE
            // -------------------------------------------------

            saveDeviceCredentials(
                context,
                deviceId,
                deviceToken
            )


            Log.d(
                TAG,
                "================================"
            )

            Log.d(
                TAG,
                "DEVICE REGISTERED SUCCESSFULLY"
            )

            Log.d(
                TAG,
                "DEVICE ID = $deviceId"
            )

            Log.d(
                TAG,
                "================================"
            )


            return deviceId


        } catch (e: Exception) {

            Log.e(
                TAG,
                "REGISTER DEVICE ERROR",
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

        /*
         * Device ID nahi hai to automatically register.
         */
        val deviceId =
            getSavedDeviceId(context)
                ?: registerDevice(context)
                ?: return null


        /*
         * Device token required.
         */
        val deviceToken =
            getSavedDeviceToken(context)
                ?: return null


        var connection:
                HttpURLConnection? = null


        try {

            val encodedDeviceId =
                URLEncoder.encode(
                    deviceId,
                    "UTF-8"
                )


            val encodedToken =
                URLEncoder.encode(
                    deviceToken,
                    "UTF-8"
                )


            val url =
                URL(
                    "$SERVER_URL/device/command" +
                            "?device_id=$encodedDeviceId" +
                            "&device_token=$encodedToken"
                )


            connection =
                url.openConnection()
                        as HttpURLConnection


            connection.requestMethod =
                "GET"

            connection.connectTimeout =
                10000

            connection.readTimeout =
                10000


            connection.setRequestProperty(
                "Accept",
                "application/json"
            )


            val responseCode =
                connection.responseCode


            Log.d(
                TAG,
                "COMMAND HTTP = $responseCode"
            )


            if (responseCode != 200) {

                val errorBody =
                    try {
                        connection
                            .errorStream
                            ?.bufferedReader()
                            ?.use {
                                it.readText()
                            }
                    } catch (e: Exception) {
                        "Unable to read error body: ${e.message}"
                    }

                Log.e(
                    TAG,
                    "COMMAND HTTP = $responseCode"
                )

                Log.e(
                    TAG,
                    "COMMAND ERROR BODY = $errorBody"
                )

                Log.e(
                    TAG,
                    "DEVICE ID = $deviceId"
                )

                return null
            }


            val response =
                connection
                    .inputStream
                    .bufferedReader()
                    .use {
                        it.readText()
                    }


            if (
                response.isBlank()
            ) {

                return null
            }


            val result =
                JSONObject(response)


            val command =
                result.optString(
                    "command",
                    ""
                )


            if (
                command.isBlank()
            ) {

                return null
            }


            Log.d(
                TAG,
                "COMMAND RECEIVED = $command"
            )


            return result


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


        val deviceToken =
            getSavedDeviceToken(context)
                ?: return


        Thread {

            var connection:
                    HttpURLConnection? = null


            try {

                val encodedToken =
                    URLEncoder.encode(
                        deviceToken,
                        "UTF-8"
                    )


                /*
                 * Current FastAPI endpoint:
                 *
                 * POST /device/heartbeat
                 * Body:
                 * {
                 *   "device_id": "..."
                 * }
                 *
                 * Token query parameter mein.
                 */
                val url =
                    URL(
                        "$SERVER_URL/device/heartbeat" +
                                "?device_token=$encodedToken"
                    )


                connection =
                    url.openConnection()
                            as HttpURLConnection


                connection.requestMethod =
                    "POST"

                connection.doOutput =
                    true

                connection.connectTimeout =
                    5000

                connection.readTimeout =
                    5000


                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                )


                val json =
                    JSONObject()


                json.put(
                    "device_id",
                    deviceId
                )


                connection
                    .outputStream
                    .bufferedWriter()
                    .use {

                        it.write(
                            json.toString()
                        )

                        it.flush()
                    }


                val responseCode =
                    connection.responseCode


                Log.d(
                    TAG,
                    "HEARTBEAT HTTP = $responseCode"
                )


                if (
                    responseCode == 200
                ) {

                    Log.d(
                        TAG,
                        "HEARTBEAT SUCCESS"
                    )

                } else {

                    Log.e(
                        TAG,
                        "HEARTBEAT FAILED"
                    )
                }


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


    // =========================================================
    // CLEAR DEVICE CREDENTIALS
    // =========================================================

    /*
     * Development/testing ke liye.
     *
     * Agar phone ko fresh registration karwana ho:
     *
     * ApiClient.clearDeviceCredentials(context)
     *
     * Next request par new device registration hogi.
     */
    fun clearDeviceCredentials(
        context: Context
    ) {

        context
            .getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .remove(
                DEVICE_ID_KEY
            )
            .remove(
                DEVICE_TOKEN_KEY
            )
            .apply()


        Log.d(
            TAG,
            "DEVICE CREDENTIALS CLEARED"
        )
    }
}
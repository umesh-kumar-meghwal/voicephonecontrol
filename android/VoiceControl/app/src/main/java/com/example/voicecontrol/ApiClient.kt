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

    /*
     * Current FastAPI / Vercel server
     *
     * IMPORTANT:
     * Agar tumhara latest Vercel deployment kisi aur URL par hai,
     * sirf is URL ko change karna.
     */
    private const val SERVER_URL =
        "https://voicephonecontrol.vercel.app/"


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

        // -----------------------------------------------------
        // CHECK EXISTING CREDENTIALS
        // -----------------------------------------------------

        val existingDeviceId =
            getSavedDeviceId(context)

        val existingDeviceToken =
            getSavedDeviceToken(context)


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
                "================================"
            )

            Log.d(
                TAG,
                "REGISTERING DEVICE..."
            )

            Log.d(
                TAG,
                "SERVER = $SERVER_URL"
            )


            // -------------------------------------------------
            // URL
            // -------------------------------------------------

            val url =
                URL(
                    "${SERVER_URL}device/register"
                )


            connection =
                url.openConnection()
                        as HttpURLConnection


            // -------------------------------------------------
            // HTTP CONFIG
            // -------------------------------------------------

            connection.requestMethod =
                "POST"

            connection.doOutput =
                true

            connection.useCaches =
                false

            connection.connectTimeout =
                15000

            connection.readTimeout =
                15000


            // -------------------------------------------------
            // HEADERS
            // -------------------------------------------------

            /*
             * Current FastAPI /device/register endpoint
             * API_TOKEN nahi maangta.
             */

            connection.setRequestProperty(
                "Content-Type",
                "application/json; charset=UTF-8"
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
                    .orEmpty()
                    .trim()

            val model =
                Build.MODEL
                    .orEmpty()
                    .trim()

            val deviceName =
                if (manufacturer.isNotBlank()) {
                    "$manufacturer $model".trim()
                } else {
                    model
                }


            val androidId =
                getAndroidId(context)


            Log.d(
                TAG,
                "DEVICE NAME = $deviceName"
            )

            Log.d(
                TAG,
                "DEVICE MODEL = $model"
            )

            Log.d(
                TAG,
                "ANDROID ID = $androidId"
            )


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


            val requestBody =
                json.toString()


            Log.d(
                TAG,
                "REGISTER REQUEST = $requestBody"
            )


            // -------------------------------------------------
            // SEND REQUEST
            // -------------------------------------------------

            connection
                .outputStream
                .bufferedWriter(Charsets.UTF_8)
                .use { writer ->

                    writer.write(
                        requestBody
                    )

                    writer.flush()
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
            // READ RESPONSE
            // -------------------------------------------------

            val responseBody =
                if (responseCode in 200..299) {

                    connection
                        .inputStream
                        .bufferedReader(Charsets.UTF_8)
                        .use {
                            it.readText()
                        }

                } else {

                    connection
                        .errorStream
                        ?.bufferedReader(Charsets.UTF_8)
                        ?.use {
                            it.readText()
                        }
                        ?: ""
                }


            Log.d(
                TAG,
                "REGISTER RESPONSE = $responseBody"
            )


            // -------------------------------------------------
            // HTTP ERROR
            // -------------------------------------------------

            if (
                responseCode !in 200..299
            ) {

                Log.e(
                    TAG,
                    "REGISTER FAILED"
                )

                Log.e(
                    TAG,
                    "HTTP CODE = $responseCode"
                )

                Log.e(
                    TAG,
                    "ERROR BODY = $responseBody"
                )

                return null
            }


            // -------------------------------------------------
            // EMPTY RESPONSE
            // -------------------------------------------------

            if (
                responseBody.isBlank()
            ) {

                Log.e(
                    TAG,
                    "REGISTER RESPONSE EMPTY"
                )

                return null
            }


            // -------------------------------------------------
            // PARSE JSON
            // -------------------------------------------------

            val result =
                try {

                    JSONObject(
                        responseBody
                    )

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "REGISTER JSON PARSE ERROR",
                        e
                    )

                    return null
                }


            // -------------------------------------------------
            // CHECK OK
            // -------------------------------------------------

            val ok =
                result.optBoolean(
                    "ok",
                    false
                )


            if (!ok) {

                Log.e(
                    TAG,
                    "REGISTER RESPONSE NOT OK"

                )

                Log.e(
                    TAG,
                    "SERVER RESPONSE = $responseBody"
                )

                return null
            }


            // -------------------------------------------------
            // DEVICE ID
            // -------------------------------------------------

            val deviceId =
                result.optString(
                    "device_id",
                    ""
                ).trim()


            // -------------------------------------------------
            // DEVICE TOKEN
            // -------------------------------------------------

            val deviceToken =
                result.optString(
                    "device_token",
                    ""
                ).trim()


            // -------------------------------------------------
            // VALIDATE DEVICE ID
            // -------------------------------------------------

            if (
                deviceId.isBlank()
            ) {

                Log.e(
                    TAG,
                    "DEVICE ID EMPTY"
                )

                return null
            }


            // -------------------------------------------------
            // VALIDATE DEVICE TOKEN
            // -------------------------------------------------

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


            // -------------------------------------------------
            // SUCCESS
            // -------------------------------------------------

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

        // -----------------------------------------------------
        // GET DEVICE ID
        // -----------------------------------------------------

        val deviceId =
            getSavedDeviceId(context)
                ?: registerDevice(context)
                ?: return null


        // -----------------------------------------------------
        // GET DEVICE TOKEN
        // -----------------------------------------------------

        val deviceToken =
            getSavedDeviceToken(context)
                ?: return null


        var connection:
                HttpURLConnection? = null


        try {

            // -------------------------------------------------
            // URL ENCODING
            // -------------------------------------------------

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


            // -------------------------------------------------
            // URL
            // -------------------------------------------------

            val url =
                URL(
                    "${SERVER_URL}device/command" +
                            "?device_id=$encodedDeviceId" +
                            "&device_token=$encodedToken"
                )


            connection =
                url.openConnection()
                        as HttpURLConnection


            // -------------------------------------------------
            // HTTP CONFIG
            // -------------------------------------------------

            connection.requestMethod =
                "GET"

            connection.connectTimeout =
                10000

            connection.readTimeout =
                10000

            connection.useCaches =
                false


            // -------------------------------------------------
            // HEADERS
            // -------------------------------------------------

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )


            // -------------------------------------------------
            // RESPONSE
            // -------------------------------------------------

            val responseCode =
                connection.responseCode


            Log.d(
                TAG,
                "COMMAND HTTP = $responseCode"
            )


            // -------------------------------------------------
            // ERROR
            // -------------------------------------------------

            if (
                responseCode !in 200..299
            ) {

                val errorBody =
                    try {

                        connection
                            .errorStream
                            ?.bufferedReader(Charsets.UTF_8)
                            ?.use {
                                it.readText()
                            }

                    } catch (e: Exception) {

                        "Unable to read error body: ${e.message}"
                    }


                Log.e(
                    TAG,
                    "COMMAND FAILED"
                )

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


            // -------------------------------------------------
            // SUCCESS RESPONSE
            // -------------------------------------------------

            val response =
                connection
                    .inputStream
                    .bufferedReader(Charsets.UTF_8)
                    .use {
                        it.readText()
                    }


            if (
                response.isBlank()
            ) {

                return null
            }


            // -------------------------------------------------
            // PARSE JSON
            // -------------------------------------------------

            val result =
                try {

                    JSONObject(
                        response
                    )

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "COMMAND JSON PARSE ERROR",
                        e
                    )

                    return null
                }


            // -------------------------------------------------
            // COMMAND
            // -------------------------------------------------

            val command =
                result.optString(
                    "command",
                    ""
                ).trim()


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

        // -----------------------------------------------------
        // GET DEVICE ID
        // -----------------------------------------------------

        val deviceId =
            getSavedDeviceId(context)
                ?: registerDevice(context)
                ?: return


        // -----------------------------------------------------
        // GET DEVICE TOKEN
        // -----------------------------------------------------

        val deviceToken =
            getSavedDeviceToken(context)
                ?: return


        Thread {

            var connection:
                    HttpURLConnection? = null


            try {

                // -------------------------------------------------
                // ENCODE TOKEN
                // -------------------------------------------------

                val encodedToken =
                    URLEncoder.encode(
                        deviceToken,
                        "UTF-8"
                    )


                // -------------------------------------------------
                // URL
                // -------------------------------------------------

                val url =
                    URL(
                        "${SERVER_URL}device/heartbeat" +
                                "?device_token=$encodedToken"
                    )


                connection =
                    url.openConnection()
                            as HttpURLConnection


                // -------------------------------------------------
                // HTTP CONFIG
                // -------------------------------------------------

                connection.requestMethod =
                    "POST"

                connection.doOutput =
                    true

                connection.connectTimeout =
                    10000

                connection.readTimeout =
                    10000

                connection.useCaches =
                    false


                // -------------------------------------------------
                // HEADERS
                // -------------------------------------------------

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8"
                )

                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                )


                // -------------------------------------------------
                // REQUEST JSON
                // -------------------------------------------------

                val json =
                    JSONObject()


                json.put(
                    "device_id",
                    deviceId
                )


                val requestBody =
                    json.toString()


                // -------------------------------------------------
                // SEND
                // -------------------------------------------------

                connection
                    .outputStream
                    .bufferedWriter(Charsets.UTF_8)
                    .use { writer ->

                        writer.write(
                            requestBody
                        )

                        writer.flush()
                    }


                // -------------------------------------------------
                // RESPONSE
                // -------------------------------------------------

                val responseCode =
                    connection.responseCode


                Log.d(
                    TAG,
                    "HEARTBEAT HTTP = $responseCode"
                )


                // -------------------------------------------------
                // SUCCESS
                // -------------------------------------------------

                if (
                    responseCode in 200..299
                ) {

                    val responseBody =
                        connection
                            .inputStream
                            .bufferedReader(Charsets.UTF_8)
                            .use {
                                it.readText()
                            }


                    Log.d(
                        TAG,
                        "HEARTBEAT SUCCESS"
                    )

                    Log.d(
                        TAG,
                        "HEARTBEAT RESPONSE = $responseBody"
                    )

                } else {

                    val errorBody =
                        try {

                            connection
                                .errorStream
                                ?.bufferedReader(Charsets.UTF_8)
                                ?.use {
                                    it.readText()
                                }

                        } catch (e: Exception) {

                            null
                        }


                    Log.e(
                        TAG,
                        "HEARTBEAT FAILED"
                    )

                    Log.e(
                        TAG,
                        "HEARTBEAT HTTP = $responseCode"
                    )

                    Log.e(
                        TAG,
                        "HEARTBEAT ERROR = $errorBody"
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
     * DEVELOPMENT / TESTING ONLY
     *
     * Isse phone ke local SharedPreferences se
     * device_id aur device_token delete ho jayenge.
     *
     * Next registerDevice() call par server se
     * fresh registration request jayegi.
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
            "================================"
        )

        Log.d(
            TAG,
            "DEVICE CREDENTIALS CLEARED"
        )

        Log.d(
            TAG,
            "NEXT REGISTRATION WILL BE FRESH"
        )

        Log.d(
            TAG,
            "================================"
        )
    }
}

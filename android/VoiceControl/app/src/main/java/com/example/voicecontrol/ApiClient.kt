package com.example.voicecontrol

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {

    private const val SERVER_URL =
        "https://phonecontrol-gircra40r-ins13.vercel.app"

    private const val API_TOKEN =
        "VPC-a8F3xK91-pQ7L2mZ6-4NwR8tY5U"

    fun getCommandSync(): JSONObject? {

        var connection: HttpURLConnection? = null

        try {

            val url =
                URL("$SERVER_URL/api/command")

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
                "ApiClient",
                "GET /api/command -> HTTP $responseCode"
            )

            if (responseCode == 200) {

                val response =
                    connection.inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }

                Log.d(
                    "ApiClient",
                    "Response = $response"
                )

                if (
                    response.isBlank() ||
                    response == "null"
                ) {
                    return null
                }

                return JSONObject(response)

            } else {

                Log.e(
                    "ApiClient",
                    "HTTP ERROR = $responseCode"
                )

                return null
            }

        } catch (e: Exception) {

            Log.e(
                "ApiClient",
                "API CONNECTION ERROR",
                e
            )

            return null

        } finally {

            connection?.disconnect()
        }
    }
}
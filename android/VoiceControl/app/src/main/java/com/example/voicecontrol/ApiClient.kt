package com.example.voicecontrol

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {

    private const val SERVER_URL =
        "https://voice-phone-control-five.vercel.app"

    private const val API_TOKEN =
        "VPC-a8F3xK91-pQ7L2mZ6-4NwR8tY5U"

    suspend fun getCommand(): JSONObject? = withContext(Dispatchers.IO) {

        try {
            val url = URL("$SERVER_URL/api/command")

            val connection =
                url.openConnection() as HttpURLConnection

            connection.requestMethod = "GET"

            connection.setRequestProperty(
                "Authorization",
                "Bearer $API_TOKEN"
            )

            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode

            Log.d("ApiClient", "GET /api/command -> HTTP $responseCode")

            if (responseCode == 200) {

                val response =
                    connection.inputStream
                        .bufferedReader()
                        .use { it.readText() }

                Log.d("ApiClient", "Response: $response")

                JSONObject(response)

            } else {

                Log.e("ApiClient", "HTTP $responseCode")
                null
            }

        } catch (e: Exception) {

            Log.e(
                "ApiClient",
                "Connection error: ${e.message}",
                e
            )

            null
        }
    }
}
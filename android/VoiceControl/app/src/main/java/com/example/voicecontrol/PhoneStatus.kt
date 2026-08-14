package com.example.voicecontrol

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread


private const val SERVER_URL =
    "https://phonecontrol-black.vercel.app"

private const val API_TOKEN =
    "VPC-a8F3xK91-pQ7L2mZ6-4NwR8tY5U"


object PhoneStatus {

    fun send(context: Context) {

        thread {

            try {

                val batteryManager =
                    context.getSystemService(
                        Context.BATTERY_SERVICE
                    ) as BatteryManager

                val battery =
                    batteryManager.getIntProperty(
                        BatteryManager.BATTERY_PROPERTY_CAPACITY
                    )

                val charging =
                    batteryManager.isCharging

                val network =
                    isNetworkConnected(context)

                val androidVersion =
                    Build.VERSION.RELEASE


                Log.d(
                    "PhoneStatus",
                    "Battery: $battery%"
                )

                Log.d(
                    "PhoneStatus",
                    "Charging: $charging"
                )

                Log.d(
                    "PhoneStatus",
                    "Network: $network"
                )

                Log.d(
                    "PhoneStatus",
                    "Android: $androidVersion"
                )


                // =================================
                // JSON
                // =================================

                val json =
                    JSONObject().apply {

                        put(
                            "battery",
                            battery
                        )

                        put(
                            "charging",
                            charging
                        )

                        put(
                            "network",
                            network
                        )

                        put(
                            "android",
                            androidVersion
                        )
                    }


                // =================================
                // UPLOAD
                // =================================

                val url =
                    URL(
                        "$SERVER_URL/api/status"
                    )

                val connection =
                    url.openConnection()
                            as HttpURLConnection

                connection.requestMethod = "POST"

                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $API_TOKEN"
                )

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                connection.doOutput = true

                connection.connectTimeout = 15000
                connection.readTimeout = 30000


                connection.outputStream.use { output ->

                    output.write(
                        json.toString()
                            .toByteArray(
                                Charsets.UTF_8
                            )
                    )
                }


                val responseCode =
                    connection.responseCode

                Log.d(
                    "PhoneStatus",
                    "STATUS UPLOAD RESPONSE: $responseCode"
                )


                connection.disconnect()

            } catch (e: Exception) {

                Log.e(
                    "PhoneStatus",
                    "STATUS UPLOAD ERROR",
                    e
                )
            }
        }
    }


    // =========================================
    // NETWORK CHECK
    // =========================================

    private fun isNetworkConnected(
        context: Context
    ): Boolean {

        val manager =
            context.getSystemService(
                Context.CONNECTIVITY_SERVICE
            ) as ConnectivityManager


        val network =
            manager.activeNetwork
                ?: return false


        val capabilities =
            manager.getNetworkCapabilities(
                network
            ) ?: return false


        return capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        )
    }
}
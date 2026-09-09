package com.example.voicecontrol

import android.content.Context
import android.webkit.JavascriptInterface

class DeviceBridge(
    private val context: Context
) {

    @JavascriptInterface
    fun getDeviceId(): String {

        return ApiClient
            .getSavedDeviceId(context)
            ?: ""
    }

    @JavascriptInterface
    fun refreshDeviceId(): String {

        return ApiClient
            .getSavedDeviceId(context)
            ?: ""
    }
}
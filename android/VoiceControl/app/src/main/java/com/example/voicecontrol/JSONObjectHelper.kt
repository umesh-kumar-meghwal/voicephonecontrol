package com.example.voicecontrol

import org.json.JSONObject

object JSONObjectHelper {

    fun quote(value: String): String {
        return JSONObject.quote(value)
    }
}
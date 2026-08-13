package com.example.voicecontrol

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log

class MainActivity : Activity() {

    companion object {

        private const val TAG = "MainActivity"

        private const val SCREEN_CAPTURE_REQUEST = 5001

        private const val EXTRA_START_LIVE =
            "START_LIVE_SCREEN"
    }

    private lateinit var projectionManager:
            MediaProjectionManager

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        Log.d(TAG, "========== ON CREATE ==========")

        projectionManager =
            getSystemService(
                MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

        val startLive =
            intent.getBooleanExtra(
                EXTRA_START_LIVE,
                false
            )

        Log.d(
            TAG,
            "START_LIVE_SCREEN = $startLive"
        )

        if (startLive) {

            Log.d(
                TAG,
                "LIVE SCREEN REQUEST RECEIVED"
            )

            requestLiveScreenPermission()
        }
    }

    override fun onNewIntent(
        intent: Intent?
    ) {

        super.onNewIntent(intent)

        if (intent == null) {
            return
        }

        setIntent(intent)

        Log.d(
            TAG,
            "========== ON NEW INTENT =========="
        )

        val startLive =
            intent.getBooleanExtra(
                EXTRA_START_LIVE,
                false
            )

        Log.d(
            TAG,
            "START_LIVE_SCREEN = $startLive"
        )

        if (startLive) {

            Log.d(
                TAG,
                "LIVE SCREEN REQUEST RECEIVED"
            )

            requestLiveScreenPermission()
        }
    }

    private fun requestLiveScreenPermission() {

        Log.d(
            TAG,
            "Opening MediaProjection permission..."
        )

        try {

            val captureIntent =
                projectionManager
                    .createScreenCaptureIntent()

            startActivityForResult(
                captureIntent,
                SCREEN_CAPTURE_REQUEST
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Failed to open screen capture permission",
                e
            )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        Log.d(
            TAG,
            "onActivityResult: request=$requestCode result=$resultCode"
        )

        if (
            requestCode !=
            SCREEN_CAPTURE_REQUEST
        ) {
            return
        }

        if (
            resultCode == RESULT_OK &&
            data != null
        ) {

            Log.d(
                TAG,
                "SCREEN CAPTURE PERMISSION GRANTED"
            )

            LiveScreenService.start(
                this,
                resultCode,
                data
            )

        } else {

            Log.e(
                TAG,
                "SCREEN CAPTURE PERMISSION DENIED"
            )
        }
    }
}
package com.example.voicecontrol

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.File

class MicrophoneService : Service() {

    companion object {
        private const val CHANNEL_ID = "microphone_channel"
        private const val NOTIFICATION_ID = 2001

        const val ACTION_START = "START_MICROPHONE"
        const val ACTION_STOP = "STOP_MICROPHONE"
    }

    private var recorder: MediaRecorder? = null
    private var outputFile: String? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {

            ACTION_START -> {
                startMicrophone()
            }

            ACTION_STOP -> {
                stopMicrophone()
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startMicrophone() {

        if (recorder != null) {
            return
        }

        val directory = File(
            getExternalFilesDir(null),
            "Recordings"
        )

        if (!directory.exists()) {
            directory.mkdirs()
        }

        outputFile = File(
            directory,
            "recording_${System.currentTimeMillis()}.m4a"
        ).absolutePath

        val notification = createNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(
                NOTIFICATION_ID,
                notification
            )
        }

        try {

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder?.apply {

                setAudioSource(
                    MediaRecorder.AudioSource.MIC
                )

                setOutputFormat(
                    MediaRecorder.OutputFormat.MPEG_4
                )

                setAudioEncoder(
                    MediaRecorder.AudioEncoder.AAC
                )

                setAudioEncodingBitRate(128000)

                setAudioSamplingRate(44100)

                setOutputFile(outputFile)

                prepare()

                start()
            }

        } catch (e: Exception) {

            recorder?.release()
            recorder = null

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun stopMicrophone() {

        try {
            recorder?.stop()
        } catch (_: Exception) {
        }

        recorder?.release()
        recorder = null

        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Microphone",
                NotificationManager.IMPORTANCE_LOW
            )

            channel.description =
                "Shows when microphone recording is active"

            val manager =
                getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {

        return NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setSmallIcon(
                android.R.drawable.ic_btn_speak_now
            )
            .setContentTitle("🎤 Microphone active")
            .setContentText(
                "Microphone recording is currently active"
            )
            .setOngoing(true)
            .setCategory(
                NotificationCompat.CATEGORY_SERVICE
            )
            .build()
    }

    override fun onDestroy() {

        stopMicrophone()

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
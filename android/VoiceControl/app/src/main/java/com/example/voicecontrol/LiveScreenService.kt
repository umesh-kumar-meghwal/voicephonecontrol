package com.example.voicecontrol

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class LiveScreenService : Service() {

    companion object {

        private const val TAG = "LiveScreenService"

        private const val SERVER_URL =
            "https://phonecontrol-black.vercel.app"

        private const val API_TOKEN =
            "VPC-a8F3xK91-pQ7L2mZ6-4NwR8tY5U"

        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "projection_data"

        private var running = false

        fun start(
            context: Context,
            resultCode: Int,
            data: Intent
        ) {

            val intent =
                Intent(context, LiveScreenService::class.java).apply {

                    putExtra(
                        EXTRA_RESULT_CODE,
                        resultCode
                    )

                    putExtra(
                        EXTRA_DATA,
                        data
                    )
                }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                context.startForegroundService(intent)

            } else {

                context.startService(intent)
            }
        }

        fun stop(context: Context) {

            context.stopService(
                Intent(
                    context,
                    LiveScreenService::class.java
                )
            )
        }
    }

    private var mediaProjection: MediaProjection? = null

    private var virtualDisplay: VirtualDisplay? = null

    private var imageReader: ImageReader? = null

    private var lastUploadTime = 0L

    override fun onCreate() {

        super.onCreate()

        createNotificationChannel()

        startForeground(
            1001,
            createNotification()
        )

        Log.d(
            TAG,
            "LiveScreenService created"
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        val resultCode =
            intent?.getIntExtra(
                EXTRA_RESULT_CODE,
                -1
            ) ?: -1

        val data =
            if (Build.VERSION.SDK_INT >= 33) {

                intent?.getParcelableExtra(
                    EXTRA_DATA,
                    Intent::class.java
                )

            } else {

                @Suppress("DEPRECATION")
                intent?.getParcelableExtra(
                    EXTRA_DATA
                )
            }

        if (resultCode == -1 || data == null) {

            Log.e(
                TAG,
                "MediaProjection permission data missing"
            )

            stopSelf()

            return START_NOT_STICKY
        }

        if (!running) {

            startProjection(
                resultCode,
                data
            )
        }

        return START_STICKY
    }

    private fun startProjection(
        resultCode: Int,
        data: Intent
    ) {

        try {

            val manager =
                getSystemService(
                    MEDIA_PROJECTION_SERVICE
                ) as MediaProjectionManager

            mediaProjection =
                manager.getMediaProjection(
                    resultCode,
                    data
                )

            if (mediaProjection == null) {

                Log.e(
                    TAG,
                    "MediaProjection is null"
                )

                stopSelf()

                return
            }

            val metrics =
                DisplayMetrics()

            val display =
                getSystemService(
                    Context.WINDOW_SERVICE
                ) as android.view.WindowManager

            @Suppress("DEPRECATION")
            display.defaultDisplay.getMetrics(
                metrics
            )

            val width =
                metrics.widthPixels

            val height =
                metrics.heightPixels

            val density =
                metrics.densityDpi

            Log.d(
                TAG,
                "Screen: ${width}x${height}"
            )

            imageReader =
                ImageReader.newInstance(
                    width,
                    height,
                    PixelFormat.RGBA_8888,
                    2
                )

            imageReader?.setOnImageAvailableListener(
                { reader ->

                    processFrame(reader)

                },
                null
            )

            virtualDisplay =
                mediaProjection?.createVirtualDisplay(
                    "VoicePhoneLiveScreen",
                    width,
                    height,
                    density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader?.surface,
                    null,
                    null
                )

            running = true

            Log.d(
                TAG,
                "LIVE SCREEN STARTED"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Failed to start projection",
                e
            )

            stopSelf()
        }
    }

    private fun processFrame(
        reader: ImageReader
    ) {

        val now =
            System.currentTimeMillis()

        // Approximately 3 FPS.
        if (now - lastUploadTime < 330) {

            try {

                reader.acquireLatestImage()?.close()

            } catch (_: Exception) {
            }

            return
        }

        lastUploadTime = now

        val image: Image? =

            try {

                reader.acquireLatestImage()

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Image acquire failed",
                    e
                )

                null
            }

        image ?: return

        try {

            val width =
                image.width

            val height =
                image.height

            val plane =
                image.planes[0]

            val buffer =
                plane.buffer

            val pixelStride =
                plane.pixelStride

            val rowStride =
                plane.rowStride

            val rowPadding =
                rowStride -
                        pixelStride * width

            val bitmapWidth =
                width +
                        rowPadding / pixelStride

            val bitmap =
                Bitmap.createBitmap(
                    bitmapWidth,
                    height,
                    Bitmap.Config.ARGB_8888
                )

            bitmap.copyPixelsFromBuffer(
                buffer
            )

            val croppedBitmap =
                if (bitmapWidth != width) {

                    Bitmap.createBitmap(
                        bitmap,
                        0,
                        0,
                        width,
                        height
                    )

                } else {

                    bitmap
                }

            uploadFrame(
                croppedBitmap
            )

            if (croppedBitmap !== bitmap) {

                croppedBitmap.recycle()
            }

            bitmap.recycle()

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Frame processing error",
                e
            )

        } finally {

            image.close()
        }
    }

    private fun uploadFrame(
        bitmap: Bitmap
    ) {

        thread {

            try {

                val output =
                    ByteArrayOutputStream()

                bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    55,
                    output
                )

                val bytes =
                    output.toByteArray()

                val encoded =
                    Base64.encodeToString(
                        bytes,
                        Base64.NO_WRAP
                    )

                val json =
                    JSONObject().apply {

                        put(
                            "filename",
                            "live.jpg"
                        )

                        put(
                            "image",
                            encoded
                        )
                    }

                val connection =
                    URL(
                        "$SERVER_URL/api/screenshot"
                    ).openConnection()
                            as HttpURLConnection

                connection.requestMethod =
                    "POST"

                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $API_TOKEN"
                )

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                connection.doOutput = true

                connection.connectTimeout =
                    10000

                connection.readTimeout =
                    10000

                connection.outputStream.use { stream ->

                    stream.write(
                        json.toString()
                            .toByteArray(
                                Charsets.UTF_8
                            )
                    )
                }

                val response =
                    connection.responseCode

                Log.d(
                    TAG,
                    "FRAME UPLOAD: $response"
                )

                connection.disconnect()

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Frame upload failed",
                    e
                )
            }
        }
    }

    override fun onDestroy() {

        running = false

        virtualDisplay?.release()

        virtualDisplay = null

        imageReader?.close()

        imageReader = null

        mediaProjection?.stop()

        mediaProjection = null

        Log.d(
            TAG,
            "LIVE SCREEN STOPPED"
        )

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {

        return null
    }

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    "live_screen",
                    "Live Screen",
                    NotificationManager.IMPORTANCE_LOW
                )

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(
                channel
            )
        }
    }

    private fun createNotification(): Notification {

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            Notification.Builder(
                this,
                "live_screen"
            )
                .setContentTitle(
                    "Voice Phone Control"
                )
                .setContentText(
                    "Live screen is active"
                )
                .setSmallIcon(
                    android.R.drawable.ic_menu_view
                )
                .build()

        } else {

            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle(
                    "Voice Phone Control"
                )
                .setContentText(
                    "Live screen is active"
                )
                .setSmallIcon(
                    android.R.drawable.ic_menu_view
                )
                .build()
        }
    }
}
package com.example.voicecontrol

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.app.DownloadManager
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import androidx.webkit.WebViewAssetLoader
import java.io.File
import java.io.FileOutputStream
import org.json.JSONArray
import org.json.JSONObject

class PermissionActivity : Activity() {

    companion object {
        private const val TAG = "PermissionActivity"

        private const val LOCAL_PAGE =
            "https://appassets.androidplatform.net/assets/index.html"

        private const val DOWNLOAD_PAGE =
            "https://appassets.androidplatform.net/assets/carddownload.html"

        private const val FILE_CHOOSER_REQUEST = 1001
    }

    private lateinit var webView: WebView
    private lateinit var accessibilityButton: Button
    private lateinit var accessibilityCard: View

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_permission)
        Toast.makeText(
            this,
            "Developer US",
            Toast.LENGTH_SHORT
        ).show()

        webView = findViewById(R.id.webView)
        accessibilityButton = findViewById(R.id.btnAccessibility)
        accessibilityCard = findViewById(R.id.accessibilityCard)

        // =========================================================
        // WEBVIEW SETTINGS
        // =========================================================

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true
        webView.settings.javaScriptCanOpenWindowsAutomatically = true

        // =========================================================
        // ASSET LOADER
        // =========================================================

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler(
                "/assets/",
                WebViewAssetLoader.AssetsPathHandler(this)
            )
            .build()

        // =========================================================
        // WEBVIEW CLIENT
        // =========================================================

        webView.webViewClient = object : WebViewClient() {

            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {

                return assetLoader.shouldInterceptRequest(
                    request.url
                )
            }

            @Suppress("DEPRECATION")
            override fun shouldInterceptRequest(
                view: WebView,
                url: String
            ): WebResourceResponse? {

                return assetLoader.shouldInterceptRequest(
                    Uri.parse(url)
                )
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {

                val url = request.url.toString()

                Log.d(TAG, "Navigation: $url")

                /*
                 * IMPORTANT:
                 * Same appassets URL ko khud load mat karo.
                 * false return karne se WebView normally navigate karega.
                 */
                if (
                    url.startsWith(
                        "https://appassets.androidplatform.net/assets/"
                    )
                ) {
                    return false
                }

                return false
            }

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(
                view: WebView,
                url: String
            ): Boolean {

                Log.d(TAG, "Navigation: $url")

                if (
                    url.startsWith(
                        "https://appassets.androidplatform.net/assets/"
                    )
                ) {
                    return false
                }

                return false
            }
        }

        // =========================================================
        // JAVASCRIPT BRIDGE
        // =========================================================

        webView.addJavascriptInterface(
            DownloadBridge(),
            "AndroidDownload"
        )

        // =========================================================
        // FILE PICKER
        // =========================================================

        webView.webChromeClient =
            object : WebChromeClient() {

                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {

                    this@PermissionActivity
                        .filePathCallback
                        ?.onReceiveValue(null)

                    this@PermissionActivity.filePathCallback =
                        filePathCallback

                    return try {

                        val intent =
                            fileChooserParams
                                ?.createIntent()
                                ?: Intent(
                                    Intent.ACTION_GET_CONTENT
                                ).apply {

                                    type = "image/*"

                                    addCategory(
                                        Intent.CATEGORY_OPENABLE
                                    )
                                }

                        startActivityForResult(
                            intent,
                            FILE_CHOOSER_REQUEST
                        )

                        true

                    } catch (e: Exception) {

                        Log.e(
                            TAG,
                            "File chooser failed",
                            e
                        )

                        this@PermissionActivity
                            .filePathCallback = null

                        false
                    }
                }
            }

        // =========================================================
        // NORMAL WEB DOWNLOAD
        // =========================================================

        webView.setDownloadListener {
                url,
                userAgent,
                contentDisposition,
                mimeType,
                contentLength ->

            try {

                val request =
                    DownloadManager.Request(
                        Uri.parse(url)
                    )

                if (!mimeType.isNullOrEmpty()) {
                    request.setMimeType(mimeType)
                }

                if (!userAgent.isNullOrEmpty()) {
                    request.addRequestHeader(
                        "User-Agent",
                        userAgent
                    )
                }

                request.setDescription(
                    "Downloading file..."
                )

                val fileName =
                    when {
                        mimeType == "application/pdf" ->
                            "document.pdf"

                        mimeType == "image/jpeg" ||
                                mimeType == "image/jpg" ->
                            "image.jpg"

                        mimeType == "image/png" ->
                            "image.png"

                        else ->
                            "download"
                    }

                request.setTitle(fileName)

                request.setNotificationVisibility(
                    DownloadManager
                        .Request
                        .VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )

                request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    fileName
                )

                val downloadManager =
                    getSystemService(
                        DOWNLOAD_SERVICE
                    ) as DownloadManager

                downloadManager.enqueue(request)

                Toast.makeText(
                    this,
                    "Download started",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Normal download failed",
                    e
                )

                Toast.makeText(
                    this,
                    "Download failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // =========================================================
        // LOAD PAGE
        // =========================================================

        webView.loadUrl(LOCAL_PAGE)

        // =========================================================
        // ACCESSIBILITY
        // =========================================================

        accessibilityButton.setOnClickListener {
            openAccessibilitySettings()
        }

        updateScreen()
    }

    // =========================================================
    // DOWNLOAD BRIDGE
    // =========================================================

    private inner class DownloadBridge {

        // =====================================================
        // SAVE FILE
        // =====================================================

        @JavascriptInterface
        fun saveFile(
            base64Data: String,
            fileName: String,
            mimeType: String
        ) {

            try {

                val cleanBase64 =
                    base64Data
                        .substringAfter(",")
                        .replace(
                            "\\s".toRegex(),
                            ""
                        )

                if (cleanBase64.isEmpty()) {
                    throw Exception(
                        "Empty file data"
                    )
                }

                val bytes =
                    Base64.decode(
                        cleanBase64,
                        Base64.DEFAULT
                    )

                if (bytes.isEmpty()) {
                    throw Exception(
                        "File data empty hai"
                    )
                }

                // =================================================
                // ANDROID 10+
                // =================================================

                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.Q
                ) {

                    val values =
                        ContentValues().apply {

                            put(
                                MediaStore.Downloads.DISPLAY_NAME,
                                fileName
                            )

                            put(
                                MediaStore.Downloads.MIME_TYPE,
                                mimeType
                            )

                            put(
                                MediaStore.Downloads.RELATIVE_PATH,
                                Environment.DIRECTORY_DOWNLOADS
                            )

                            put(
                                MediaStore.Downloads.IS_PENDING,
                                1
                            )
                        }

                    val uri =
                        contentResolver.insert(
                            MediaStore.Downloads
                                .EXTERNAL_CONTENT_URI,
                            values
                        )

                    if (uri == null) {
                        throw Exception(
                            "Could not create download file"
                        )
                    }

                    contentResolver
                        .openOutputStream(uri)
                        ?.use { output ->

                            output.write(bytes)
                            output.flush()
                        }
                        ?: throw Exception(
                            "Could not open output stream"
                        )

                    val completedValues =
                        ContentValues().apply {

                            put(
                                MediaStore.Downloads.IS_PENDING,
                                0
                            )
                        }

                    contentResolver.update(
                        uri,
                        completedValues,
                        null,
                        null
                    )

                } else {

                    // =================================================
                    // ANDROID 9 AND BELOW
                    // =================================================

                    val directory =
                        Environment
                            .getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS
                            )

                    if (!directory.exists()) {
                        directory.mkdirs()
                    }

                    val file =
                        File(
                            directory,
                            fileName
                        )

                    FileOutputStream(file).use { output ->
                        output.write(bytes)
                        output.flush()
                    }
                }

                runOnUiThread {

                    Toast.makeText(
                        this@PermissionActivity,
                        "Saved in Downloads/$fileName",
                        Toast.LENGTH_LONG
                    ).show()
                }

                Log.d(
                    TAG,
                    "File saved: $fileName"
                )

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Bridge save failed",
                    e
                )

                runOnUiThread {

                    Toast.makeText(
                        this@PermissionActivity,
                        "Download failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // =====================================================
        // GET DOWNLOADED FILES
        // =====================================================

        @JavascriptInterface
        fun getDownloadedFiles(): String {

            val result = JSONArray()

            try {

                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.Q
                ) {

                    val collection =
                        MediaStore.Downloads
                            .EXTERNAL_CONTENT_URI

                    val projection =
                        arrayOf(
                            MediaStore.Downloads._ID,
                            MediaStore.Downloads.DISPLAY_NAME,
                            MediaStore.Downloads.MIME_TYPE,
                            MediaStore.Downloads.DATE_MODIFIED
                        )

                    /*
                     * MIME type ke saath filename extension
                     * bhi check kar rahe hain.
                     */
                    val selection =
                        "(" +
                                "${MediaStore.Downloads.MIME_TYPE} = ? OR " +
                                "${MediaStore.Downloads.MIME_TYPE} = ? OR " +
                                "${MediaStore.Downloads.MIME_TYPE} = ? OR " +
                                "${MediaStore.Downloads.DISPLAY_NAME} LIKE ? OR " +
                                "${MediaStore.Downloads.DISPLAY_NAME} LIKE ? OR " +
                                "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?" +
                                ")"

                    val selectionArgs =
                        arrayOf(
                            "application/pdf",
                            "image/jpeg",
                            "image/jpg",
                            "%.pdf",
                            "%.jpg",
                            "%.jpeg"
                        )

                    contentResolver.query(
                        collection,
                        projection,
                        selection,
                        selectionArgs,
                        "${MediaStore.Downloads.DATE_MODIFIED} DESC"
                    )?.use { cursor ->

                        val idColumn =
                            cursor.getColumnIndexOrThrow(
                                MediaStore.Downloads._ID
                            )

                        val nameColumn =
                            cursor.getColumnIndexOrThrow(
                                MediaStore.Downloads.DISPLAY_NAME
                            )

                        val mimeColumn =
                            cursor.getColumnIndexOrThrow(
                                MediaStore.Downloads.MIME_TYPE
                            )

                        while (cursor.moveToNext()) {

                            val id =
                                cursor.getLong(
                                    idColumn
                                )

                            val name =
                                cursor.getString(
                                    nameColumn
                                )

                            val mime =
                                cursor.getString(
                                    mimeColumn
                                )

                            val uri =
                                Uri.withAppendedPath(
                                    collection,
                                    id.toString()
                                )

                            val item =
                                JSONObject()

                            item.put(
                                "name",
                                name
                            )

                            item.put(
                                "mime",
                                mime ?: getMimeFromName(name)
                            )

                            item.put(
                                "uri",
                                uri.toString()
                            )

                            result.put(item)
                        }
                    }

                } else {

                    // =================================================
                    // ANDROID 9 AND BELOW
                    // =================================================

                    val directory =
                        Environment
                            .getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS
                            )

                    if (directory.exists()) {

                        directory
                            .listFiles()
                            ?.forEach { file ->

                                if (
                                    file.isFile &&
                                    (
                                            file.name.endsWith(
                                                ".jpg",
                                                true
                                            ) ||
                                                    file.name.endsWith(
                                                        ".jpeg",
                                                        true
                                                    ) ||
                                                    file.name.endsWith(
                                                        ".pdf",
                                                        true
                                                    )
                                            )
                                ) {

                                    val item =
                                        JSONObject()

                                    item.put(
                                        "name",
                                        file.name
                                    )

                                    item.put(
                                        "mime",
                                        getMimeFromName(
                                            file.name
                                        )
                                    )

                                    /*
                                     * Old Android ke liye path bhi
                                     * bhej rahe hain.
                                     */
                                    item.put(
                                        "path",
                                        file.absolutePath
                                    )

                                    result.put(item)
                                }
                            }
                    }
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Get downloaded files failed",
                    e
                )
            }

            Log.d(
                TAG,
                "Downloaded files: $result"
            )

            return result.toString()
        }

        // =====================================================
        // MIME TYPE
        // =====================================================

        private fun getMimeFromName(
            fileName: String
        ): String {

            return when {

                fileName.endsWith(
                    ".pdf",
                    true
                ) ->
                    "application/pdf"

                fileName.endsWith(
                    ".jpg",
                    true
                ) ||
                        fileName.endsWith(
                            ".jpeg",
                            true
                        ) ->
                    "image/jpeg"

                else ->
                    "application/octet-stream"
            }
        }

        // =====================================================
        // OPEN DOWNLOADED FILE
        // =====================================================

        @JavascriptInterface
        fun openDownloadedFile(
            uriString: String,
            mimeType: String
        ) {

            try {

                if (uriString.isBlank()) {
                    throw Exception(
                        "File URI empty hai"
                    )
                }

                Log.d(
                    TAG,
                    "Opening file: $uriString"
                )

                var uri =
                    Uri.parse(uriString)

                /*
                 * Android 10+:
                 * content:// URI directly open ho jayegi.
                 *
                 * Old Android:
                 * file:// URI ko bhi support karne ki
                 * koshish kar rahe hain.
                 */
                if (
                    !uri.scheme.equals(
                        "content",
                        true
                    ) &&
                    !uri.scheme.equals(
                        "file",
                        true
                    )
                ) {

                    uri =
                        Uri.fromFile(
                            File(uriString)
                        )
                }

                val finalMime =
                    if (
                        mimeType.isNotBlank()
                    ) {
                        mimeType
                    } else {
                        "application/octet-stream"
                    }

                val intent =
                    Intent(
                        Intent.ACTION_VIEW
                    ).apply {

                        setDataAndType(
                            uri,
                            finalMime
                        )

                        addFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )

                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                        )
                    }

                /*
                 * Pehle direct open try.
                 */
                try {

                    startActivity(intent)

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "Direct open failed",
                        e
                    )

                    /*
                     * Agar exact MIME app nahi milti,
                     * generic chooser try karo.
                     */
                    val chooser =
                        Intent.createChooser(
                            intent,
                            "Open file with"
                        )

                    chooser.addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    startActivity(chooser)
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Unable to open downloaded file",
                    e
                )

                runOnUiThread {

                    Toast.makeText(
                        this@PermissionActivity,
                        "File open nahi ho paayi: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // =========================================================
    // FILE PICKER RESULT
    // =========================================================

    @Suppress("DEPRECATION")
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

        if (
            requestCode !=
            FILE_CHOOSER_REQUEST
        ) {
            return
        }

        val callback =
            filePathCallback

        filePathCallback = null

        if (callback == null) {
            return
        }

        val result: Array<Uri>? =

            if (
                resultCode == RESULT_OK &&
                data != null
            ) {

                val clipData =
                    data.clipData

                if (clipData != null) {

                    Array(
                        clipData.itemCount
                    ) { index ->

                        clipData
                            .getItemAt(index)
                            .uri
                    }

                } else {

                    val uri =
                        data.data

                    if (uri != null) {
                        arrayOf(uri)
                    } else {
                        null
                    }
                }

            } else {
                null
            }

        callback.onReceiveValue(result)

        Log.d(
            TAG,
            "File picker result delivered"
        )
    }

    // =========================================================
    // RESUME
    // =========================================================

    override fun onResume() {

        super.onResume()

        updateScreen()
    }

    // =========================================================
    // SCREEN UPDATE
    // =========================================================

    private fun updateScreen() {

        if (isAccessibilityEnabled()) {

            Log.d(
                TAG,
                "Accessibility enabled"
            )

            webView.visibility =
                View.VISIBLE

            accessibilityCard.visibility =
                View.GONE

            startCommandService()

        } else {

            Log.d(
                TAG,
                "Accessibility not enabled"
            )

            webView.visibility =
                View.GONE

            accessibilityCard.visibility =
                View.VISIBLE

            accessibilityButton.isEnabled =
                true

            accessibilityButton.text =
                "Enable Accessibility"
        }
    }

    // =========================================================
    // ACCESSIBILITY SETTINGS
    // =========================================================

    private fun openAccessibilitySettings() {

        startActivity(
            Intent(
                Settings.ACTION_ACCESSIBILITY_SETTINGS
            )
        )
    }

    // =========================================================
    // COMMAND SERVICE
    // =========================================================

    private fun startCommandService() {

        try {

            val intent =
                Intent(
                    this,
                    CommandService::class.java
                )

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {

                startForegroundService(intent)

            } else {

                startService(intent)
            }

            Log.d(
                TAG,
                "CommandService start requested"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "CommandService start failed",
                e
            )
        }
    }

    // =========================================================
    // ACCESSIBILITY CHECK
    // =========================================================

    private fun isAccessibilityEnabled(): Boolean {

        val manager =
            getSystemService(
                ACCESSIBILITY_SERVICE
            ) as? AccessibilityManager
                ?: return false

        val services =
            manager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo
                    .FEEDBACK_ALL_MASK
            )

        return services.any { service ->

            val info =
                service.resolveInfo
                    ?.serviceInfo
                    ?: return@any false

            info.packageName ==
                    packageName &&
                    info.name ==
                    "com.example.voicecontrol.ScreenshotService"
        }
    }

    // =========================================================
    // BACK BUTTON
    // =========================================================

    @Suppress("DEPRECATION")
    override fun onBackPressed() {

        if (
            webView.visibility ==
            View.VISIBLE &&
            webView.canGoBack()
        ) {

            webView.goBack()

        } else {

            super.onBackPressed()
        }
    }

    // =========================================================
    // DESTROY
    // =========================================================

    override fun onDestroy() {

        filePathCallback
            ?.onReceiveValue(null)

        filePathCallback = null

        if (::webView.isInitialized) {

            webView.stopLoading()
            webView.destroy()
        }

        super.onDestroy()
    }
}
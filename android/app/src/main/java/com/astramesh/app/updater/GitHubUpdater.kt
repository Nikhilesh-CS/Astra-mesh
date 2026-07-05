package com.astramesh.app.updater

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.net.URL
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection

data class UpdateInfo(
    val version: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val isUpdateAvailable: Boolean,
    val sha256: String? = null
)

class GitHubUpdater(private val context: Context) {

    private val REPO_URL = "https://api.github.com/repos/Nikhilesh-CS/Astra-mesh/releases/latest"
    private val PREFS_NAME = "updater_prefs"
    private val LAST_CHECK_TIME = "last_check_time"
    private val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun checkForUpdates(manual: Boolean = false): UpdateInfo? = withContext(Dispatchers.IO) {
        if (!manual) {
            val lastCheck = prefs.getLong(LAST_CHECK_TIME, 0)
            if (System.currentTimeMillis() - lastCheck < CHECK_INTERVAL_MS) {
                return@withContext null // Too soon for an automatic check
            }
        }

        try {
            val url = URL(REPO_URL)
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                
                val tagName = json.getString("tag_name").removePrefix("v")
                val releaseNotes = json.getString("body")
                
                val assets = json.getJSONArray("assets")
                var downloadUrl = ""
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.getString("name").endsWith(".apk")) {
                        downloadUrl = asset.getString("browser_download_url")
                        break
                    }
                }

                // Simple version string comparison (assuming semantic versioning like 1.0.0)
                val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
                val isUpdateAvailable = isVersionGreater(tagName, currentVersion)

                if (!manual && isUpdateAvailable) {
                    prefs.edit().putLong(LAST_CHECK_TIME, System.currentTimeMillis()).apply()
                }

                return@withContext UpdateInfo(
                    version = tagName,
                    releaseNotes = releaseNotes,
                    downloadUrl = downloadUrl,
                    isUpdateAvailable = isUpdateAvailable
                )
            }
        } catch (e: Exception) {
            Log.e("GitHubUpdater", "Failed to check for updates: ${e.message}")
        }
        return@withContext null
    }

    fun downloadAndInstallUpdate(updateInfo: UpdateInfo, onProgress: (Float) -> Unit, onComplete: () -> Unit, onError: (String) -> Unit) {
        if (updateInfo.downloadUrl.isEmpty()) {
            onError("No APK found in the release.")
            return
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(updateInfo.downloadUrl)
        val request = DownloadManager.Request(uri)
            .setTitle("AstraMesh Update")
            .setDescription("Downloading version ${updateInfo.version}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "AstraMesh-v${updateInfo.version}.apk")

        val downloadId = downloadManager.enqueue(request)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    context?.unregisterReceiver(this)
                    onComplete()
                    installApk("AstraMesh-v${updateInfo.version}.apk", onError)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }

        // We could run a coroutine here to poll DownloadManager for progress if needed for UI
    }

    private fun installApk(fileName: String, onError: (String) -> Unit) {
        try {
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
            if (!file.exists()) {
                onError("Downloaded APK not found.")
                return
            }

            // Verify signature matches currently installed app
            if (!verifyApkSignature(file)) {
                onError("Security Error: The downloaded update's signature does not match the currently installed app. Update aborted to prevent hijacking.")
                file.delete()
                return
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("GitHubUpdater", "Failed to install APK", e)
            onError("Failed to launch installer: ${e.message}")
        }
    }

    private fun verifyApkSignature(apkFile: File): Boolean {
        return try {
            val pm = context.packageManager
            // Get info for the downloaded APK
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNATURES)
            } ?: return false

            // Get info for the currently installed app
            val currentInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val newSigs = packageInfo.signingInfo?.apkContentsSigners
                val oldSigs = currentInfo.signingInfo?.apkContentsSigners
                if (newSigs != null && oldSigs != null && newSigs.isNotEmpty() && oldSigs.isNotEmpty()) {
                    // Very simple check: the primary signing certificate must match
                    return newSigs[0].toByteArray().contentEquals(oldSigs[0].toByteArray())
                }
            } else {
                @Suppress("DEPRECATION")
                val newSigs = packageInfo.signatures
                @Suppress("DEPRECATION")
                val oldSigs = currentInfo.signatures
                if (newSigs != null && oldSigs != null && newSigs.isNotEmpty() && oldSigs.isNotEmpty()) {
                    return newSigs[0].toByteArray().contentEquals(oldSigs[0].toByteArray())
                }
            }
            false
        } catch (e: Exception) {
            Log.e("GitHubUpdater", "Signature verification failed", e)
            false
        }
    }

    private fun isVersionGreater(remote: String, local: String): Boolean {
        try {
            val rParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
            val lParts = local.split(".").map { it.toIntOrNull() ?: 0 }
            val length = maxOf(rParts.size, lParts.size)
            for (i in 0 until length) {
                val r = rParts.getOrElse(i) { 0 }
                val l = lParts.getOrElse(i) { 0 }
                if (r > l) return true
                if (r < l) return false
            }
        } catch (e: Exception) {
            // Ignore parse errors
        }
        return false
    }
}

package ae.tii.saca_store.data.repos

import ae.tii.saca_store.domain.AppInfo
import ae.tii.saca_store.domain.repos.IDownloadRepo
import ae.tii.saca_store.receivers.InstallResultReceiver
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class DownloadRepoImpl(
    context: Context
) : IDownloadRepo {

    companion object {
        private const val TAG = "DownloadRepoImpl"
    }

    private val downloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private val downloadsDir =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)


    override fun startDownload(app: AppInfo): Long {

        val apkFile = File(downloadsDir, "${app.packageName}.apk")

        // Delete existing file if it exists
        if (apkFile.exists()) {
            apkFile.delete()
        }

        val request = DownloadManager.Request(app.downloadUrl.toUri())
            .setTitle(app.name)
            .setDescription("Downloading ${app.name}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationUri(Uri.fromFile(apkFile))

        val downloadId = downloadManager.enqueue(request)
        Log.d(TAG, "Download Started: $downloadId")

        return downloadId
    }

    override suspend fun downloadApkFile(context: Context, appInfo: AppInfo): File? {
        val url = URL(appInfo.downloadUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = true // follow GitHub redirects
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "Mozilla/5.0") // GitHub requires User-Agent
        conn.connectTimeout = 15000
        conn.readTimeout = 15000

        if (conn.responseCode !in 200..299) {
            throw IOException("Failed to download file: HTTP ${conn.responseCode}")
        }
        val file = File(context.filesDir, "${appInfo.packageName}.apk")
//        val file = File(destPath)
        file.parentFile?.mkdirs()

        Log.i(TAG, "starting inputstream from conn to: ${file.absolutePath}")
        try {
            conn.inputStream.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            conn.disconnect()
            Log.i(TAG, "Error while inputstream: $e")
            return null
        }

        Log.i(TAG, "File downloaded: file-path ${file.absolutePath}")
        return file
    }

    @SuppressLint("RequestInstallPackagesPolicy")
    override fun installApkFile(
        context: Context,
        apkFile: File,
        singleApk: Boolean
    ) {
        val packageInstaller = context.packageManager.packageInstaller
        val sessionParams =
            PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)

        // Create installation session
        val sessionId = packageInstaller.createSession(sessionParams)
        val session = packageInstaller.openSession(sessionId)

        try {
            // Write APK to session
            apkFile.inputStream().use { inputStream ->
                session.openWrite(apkFile.name, 0, -1).use { outputStream ->
                    val buffer = ByteArray(1024 * 4)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    session.fsync(outputStream)
                }
            }

            // Create PendingIntent with correct flags
            val intent = Intent(context, InstallResultReceiver::class.java).apply {
                action = "INSTALL_COMPLETE"
                putExtra(InstallResultReceiver.SESSION_ID, sessionId)
                putExtra(InstallResultReceiver.FILE_URI, apkFile.toUri().toString())
                putExtra(InstallResultReceiver.SINGLE_APK_INSTALL, singleApk)
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context, sessionId, intent, flags
            )

            // Commit the session - THIS WILL TRIGGER THE INSTALL CONFIRMATION DIALOG
            session.commit(pendingIntent.intentSender)
            session.close()

        } catch (e: Exception) {
            session.abandon()
            Log.i(TAG, "Error Installing App $e")
        }
    }
}
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
import java.util.concurrent.ConcurrentLinkedQueue

class DownloadRepoImpl(
    context: Context
) : IDownloadRepo {

    private val apkUriQueue = ConcurrentLinkedQueue<Uri>()

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
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationUri(Uri.fromFile(apkFile))

        val downloadId = downloadManager.enqueue(request)
        Log.d(TAG, "Download Started: $downloadId")

        return downloadId
    }

    @SuppressLint("RequestInstallPackagesPolicy")
    private fun installApkFile(context: Context, apkFile: File) {
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
            Log.e(TAG, "Error Installing App $e")
        }
    }

    override fun getDownloadedFileUri(downloadId: Long): Uri? {
        return try {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)
            val uri = cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                    val fileUri = it.getString(columnIndex)
                    fileUri?.toUri()
                } else null
            }
            uri?.let { apkUriQueue.add(it) }
            uri
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Uri of downloaded file $downloadId: ${e.localizedMessage}")
            null
        }
    }

    override fun installNext(context: Context) {
        synchronized(this) {
            val apkUri = apkUriQueue.poll()
            apkUri?.let { uri ->
                val apkFile = File(uri.path ?: "")
                installApkFile(context, apkFile)
            }
        }
    }
}
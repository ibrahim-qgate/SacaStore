package ae.tii.saca_store.service

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import ae.tii.saca_store.domain.AppInfo
import ae.tii.saca_store.receivers.InstallResultReceiver
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class DownloadService : Service() {

    private var downloadId: Long = -1

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_DOWNLOAD" -> {
                val appInfo = getAppInfoObject()
                downloadApp(appInfo)
            }

            "INSTALL_APK" -> {
                val receivedDownloadId = intent.getLongExtra("downloadId", -1)
                if (receivedDownloadId != -1L) {
                    installAppWithPackageInstaller(this, receivedDownloadId)
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun getAppInfoObject(): AppInfo {
        return AppInfo(
            name = "Outlook",
            packageName = "com.microsoft.outlook",
            version = "1.0.0",
//            downloadUrl = "https://github.com/mik237/BonialBrochures/releases/download/main-1.0/lujo-release.apk"
//            downloadUrl = "https://github.com/ibrahim-qgate/host_apk/releases/download/outlook/Outlook_4.2534.2_apkcombo.com.apk",
            downloadUrl = "https://github.com/ibrahim-qgate/host_apk/releases/download/outlook/microsoft_outlook_minAPI28-universal-nodpi_.apk",
        )
    }


    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun downloadApp(appInfo: AppInfo) {
        try {

            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val apkFile = File(downloadsDir, "${appInfo.packageName}.apk")

            // Delete existing file if it exists
            if (apkFile.exists()) {
                apkFile.delete()
            }

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            val request = DownloadManager.Request(appInfo.downloadUrl.toUri())
                .setTitle("${appInfo.name}.apk")
                .setDescription("Downloading application")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
//                .setDestinationInExternalPublicDir(
//                    Environment.DIRECTORY_DOWNLOADS,
//                    "${appInfo.name}.apk"
//                )
                .setDestinationUri(Uri.fromFile(apkFile))
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            downloadId = downloadManager.enqueue(request)
        } catch (e: Exception) {
            Toast.makeText(this, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
            stopSelf()
        }
    }

    private fun installAppWithPackageInstaller(context: Context, receivedDownloadId: Long) {
        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(receivedDownloadId)
        val cursor = downloadManager.query(query)
        Log.d("DownloadReceiver", "Installing: $receivedDownloadId")

        if (cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
            val fileUri = cursor.getString(columnIndex)

            fileUri?.let {
                val apkUri = fileUri.toUri()
                val apkFile = File(apkUri.path ?: "")

                if (apkFile.exists()) {
                    try {
                        installUsingPackageInstaller(context, apkFile)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Install failed: ${e.message}", Toast.LENGTH_SHORT)
                            .show()
                        // Fallback to legacy method for older devices
                        //installUsingLegacyMethod(context, apkFile)
                    }
                } else {
                    Log.d("DownloadReceiver", "APK file not found: $receivedDownloadId")
                    Toast.makeText(context, "APK file not found", Toast.LENGTH_SHORT).show()
                }
            }
        }
        cursor.close()
    }

    @SuppressLint("RequestInstallPackagesPolicy")
    private fun installUsingPackageInstaller(context: Context, apkFile: File) {
        val packageInstaller = context.packageManager.packageInstaller
        val sessionParams =
            PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)

        // Create installation session
        val sessionId = packageInstaller.createSession(sessionParams)
        val session = packageInstaller.openSession(sessionId)

        try {
            // Write APK to session
            FileInputStream(apkFile).use { inputStream ->
                session.openWrite("package", 0, -1).use { outputStream ->
                    inputStream.copyTo(outputStream)
                    session.fsync(outputStream)
                }
            }

            // Create PendingIntent with correct flags
            val intent = Intent(context, InstallResultReceiver::class.java).apply {
                action = "INSTALL_COMPLETE"
                putExtra("sessionId", sessionId)
                putExtra("fileUri", apkFile.toUri().toString())
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                intent,
                flags
            )

            // Commit the session - THIS WILL TRIGGER THE INSTALL CONFIRMATION DIALOG
            session.commit(pendingIntent.intentSender)
            session.close()

        } catch (e: IOException) {
            session.abandon()
            throw e
        }
    }


    // Fallback method for older Android versions
    private fun installUsingLegacyMethod(context: Context, apkFile: File) {
        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot install app: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun startDownload(context: Context) {
            val intent = Intent(context, DownloadService::class.java)
                .apply {
                    action = "START_DOWNLOAD"
                }
            context.startService(intent)
        }
    }

}



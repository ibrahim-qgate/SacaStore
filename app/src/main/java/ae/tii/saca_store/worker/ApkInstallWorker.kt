package ae.tii.saca_store.worker

import ae.tii.saca_store.domain.room.AppDatabase
import ae.tii.saca_store.domain.room.DownloadDao
import ae.tii.saca_store.receivers.InstallResultReceiver
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File

class ApkInstallWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val DOWNLOAD_ID = "downloadId"
        const val TAG = "ApkInstallWorker"
    }

    private val downloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val dao: DownloadDao = AppDatabase.get(context).downloadDao()

    override suspend fun doWork(): Result {
        try {
            val itemId = inputData.getString(DOWNLOAD_ID) ?: return Result.failure()
            val item = dao.get(itemId) ?: return Result.failure()
            Log.d(TAG, "doWork: itemId ${item.id}, uri: ${item.destPath}")
            val apkFile = File(item.destPath)
            installApkFile(context, apkFile)
        } catch (e: Exception) {
            Log.d(TAG, "doWork: Error installing: ${e.localizedMessage}")
            return Result.failure()
        }
        return Result.success()
    }

    private fun getDownloadedFileUri(downloadId: Long): Uri? {
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
            Log.d(TAG, "getDownloadedFileUri: $uri")
            uri
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Uri of downloaded file $downloadId: ${e.localizedMessage}")
            null
        }
    }

    private fun installNext(uri: Uri) {
        val apkFile = File(uri.path ?: "")
        installApkFile(context, apkFile)
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
            Log.d(TAG, "installApkFile: ${apkFile.name}")
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
                action = "ae.tii.saca_store.action.INSTALL_COMPLETE"
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
            Log.d(TAG, "installApkFile: session commit & close")
            deleteFile(apkFile)
        } catch (e: Exception) {
            session.abandon()
            Log.e(TAG, "Error Installing App $e")
        }
    }

    private fun deleteFile(apkFile: File?) {
        apkFile?.let {
            if (it.exists()) {
                it.delete()
            }
        }
    }

}
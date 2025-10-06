package ae.tii.saca_store.data.repos

import ae.tii.saca_store.domain.AppInfo
import ae.tii.saca_store.domain.repos.IDownloadRepo
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.net.toUri
import java.io.File

class DownloadRepoImpl(
    context: Context
) : IDownloadRepo {


    private val downloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val downloads = mutableMapOf<String, Long>()

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
            // APK mime type
//            .setMimeType("application/vnd.android.package-archive")
            // Set destination (optional), else default public downloads folder
            //.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "${app.name}.apk")
            .setDestinationUri(Uri.fromFile(apkFile))

        val downloadId = downloadManager.enqueue(request)
        Log.d("DownloadReceiver", "Download Started: $downloadId")

        downloads[app.packageName] = downloadId

        return downloadId
    }

    private fun isDownloadInProgress(downloadUrl: String): Boolean {
        val query = DownloadManager.Query()
            .setFilterByStatus(
                DownloadManager.STATUS_RUNNING or
                        DownloadManager.STATUS_PENDING or
                        DownloadManager.STATUS_PAUSED
            )
        val cursor = downloadManager.query(query)

        cursor?.use {
            val columnUrl = cursor.getColumnIndex(DownloadManager.COLUMN_URI)

            while (cursor.moveToNext()) {
                val existingUrl = cursor.getString(columnUrl)
                if (existingUrl == downloadUrl) return true
            }
        }

        return false
    }
}
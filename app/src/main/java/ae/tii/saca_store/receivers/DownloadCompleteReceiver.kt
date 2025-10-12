package ae.tii.saca_store.receivers

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import ae.tii.saca_store.service.DownloadService

class DownloadCompleteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            Log.d("DownloadReceiver", "Download completed: $downloadId")
            // Start service to handle installation
            DownloadService.start(
                context,
                withAction = DownloadService.ACTION_INSTALL_APP,
                downloadId = downloadId
            )
        } else {
            Log.d("DownloadReceiver", "Download not completed: ${intent.action}")
            //process queue
            DownloadService.start(
                context,
                withAction = DownloadService.ACTION_START_APK_DOWNLOADS
            )
        }
    }
}
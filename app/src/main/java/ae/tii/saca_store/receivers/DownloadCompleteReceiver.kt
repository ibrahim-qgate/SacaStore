package ae.tii.saca_store.receivers

import ae.tii.saca_store.service.DownloadService
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log


class DownloadCompleteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            Log.d("DownloadReceiver", "Download Success: $downloadId")
            // Start service to handle installation
            DownloadService.start(
                context,
                withAction = DownloadService.ACTION_DOWNLOAD_COMPLETED,
                downloadId = downloadId
            )
        } else {
            Log.d(
                "DownloadReceiver",
                "Download Failed: action: ${intent.action}, downloadId: $downloadId"
            )
        }
    }
}
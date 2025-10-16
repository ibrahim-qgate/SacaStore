package ae.tii.saca_store.receivers

import ae.tii.saca_store.service.DownloadService
import ae.tii.saca_store.worker.ApkInstallWorker
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf


class DownloadCompleteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            Log.d("DownloadReceiver", "Download Success: $downloadId")
            // Start service to handle installation
            val work = OneTimeWorkRequestBuilder<ApkInstallWorker>()
                .setInputData(workDataOf(ApkInstallWorker.DOWNLOAD_ID to downloadId))
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "apk_install_queue",
                ExistingWorkPolicy.APPEND,
                work
            )

        } else {
            Log.d(
                "DownloadReceiver",
                "Download Failed: action: ${intent.action}, downloadId: $downloadId"
            )
        }
    }
}
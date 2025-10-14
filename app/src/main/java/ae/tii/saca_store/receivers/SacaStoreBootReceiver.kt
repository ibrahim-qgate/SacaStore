package ae.tii.saca_store.receivers

import ae.tii.saca_store.service.DownloadService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log


class SacaStoreBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("DownloadReceiver", "Broadcast received with action: ${intent?.action}")
        if (context == null) {
            Log.e("DownloadReceiver", "Received null context!");
        }

        if (intent == null) {
            Log.e("DownloadReceiver", "Received null intent!");
        }
        if (Intent.ACTION_BOOT_COMPLETED == intent?.action) {
            context?.let { startDownloadService(it) }
        }
    }

    private fun startDownloadService(context: Context) {
        DownloadService.start(context, withAction = DownloadService.ACTION_BIND_TO_SACA_SERVICE)
    }
}
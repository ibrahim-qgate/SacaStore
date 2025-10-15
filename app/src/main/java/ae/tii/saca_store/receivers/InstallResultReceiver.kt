package ae.tii.saca_store.receivers

import ae.tii.saca_store.service.DownloadService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import androidx.core.net.toUri
import java.io.File


class InstallResultReceiver : BroadcastReceiver() {

    companion object {
        const val SESSION_ID = "SESSION_ID"
        const val FILE_URI = "FILE_URI"
        private const val TAG = "InstallResultReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {

        val sessionId = intent.getIntExtra(SESSION_ID, -1)
        val fileUri = intent.getStringExtra(FILE_URI)
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)

        Log.d(TAG, "Session: $sessionId, Status: $status, fileUri: $fileUri")
        fileUri?.let {
            if (status != PackageInstaller.STATUS_PENDING_USER_ACTION) {
                deleteFile(it)
            }
        }


        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // shows the install confirmation dialog
                val confirmationIntent =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                    } else {
                        intent.getParcelableExtra(Intent.EXTRA_INTENT)
                    }
                confirmationIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(confirmationIntent)
            }

            PackageInstaller.STATUS_SUCCESS -> {
                Log.d(TAG, "onReceive: Installation successful!")
            }

            PackageInstaller.STATUS_FAILURE -> {
                Log.e(TAG, "onReceive: Installation failed")
            }

            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                Log.e(TAG, "onReceive: Installation aborted")
            }

            PackageInstaller.STATUS_FAILURE_BLOCKED -> {
                Log.e(TAG, "onReceive: Installation blocked")
            }

            PackageInstaller.STATUS_FAILURE_CONFLICT -> {
                Log.e(TAG, "onReceive: Installation conflict")
            }

            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> {
                Log.e(TAG, "onReceive: Incompatible package")
            }

            PackageInstaller.STATUS_FAILURE_INVALID -> {
                Log.e(TAG, "onReceive: Invalid package")
            }

            else -> {
                Log.e(TAG, "onReceive: Unknown installation status $status")
            }
        }

        DownloadService.start(context, withAction = DownloadService.ACTION_PROCESS_INSTALLATION)
    }

    private fun deleteFile(fileUri: String) {
        if (fileUri.startsWith("file://")) {
            val filePath = fileUri.toUri().path
            val apkFile = filePath?.let { File(it) }
            apkFile?.let {
                if (it.exists()) {
                    it.delete()
                }
            }
        }
    }
}
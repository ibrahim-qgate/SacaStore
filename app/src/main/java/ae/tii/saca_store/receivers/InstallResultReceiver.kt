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

@Suppress("DEPRECATION")
class InstallResultReceiver : BroadcastReceiver() {

    companion object {
        const val SESSION_ID = "SESSION_ID"
        const val FILE_URI = "FILE_URI"
        const val SINGLE_APK_INSTALL = "SINGLE_APK_INSTALL"

        private const val TAG = "InstallResultReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {

        val sessionId = intent.getIntExtra(SESSION_ID, -1)
        val fileUri = intent.getStringExtra(FILE_URI)
        val singleApkInstall = intent.getBooleanExtra(SINGLE_APK_INSTALL, false)
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)

        Log.d(TAG, "Session: $sessionId, Status: $status")
        Log.d(TAG, "Session: $sessionId, fileUri: $fileUri")

        if (fileUri != null && fileUri.startsWith("file://")) {
            val filePath = fileUri.toUri().path
            val apkFile = filePath?.let { File(filePath) }
            apkFile?.let {
                if (it.exists()) {
                    it.delete()
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
                    processInstallation(context, singleApkInstall)
                }

                PackageInstaller.STATUS_FAILURE -> {
                    Log.e(TAG, "onReceive: Installation failed")
                    processInstallation(context, singleApkInstall)
                }

                PackageInstaller.STATUS_FAILURE_ABORTED -> {
                    Log.e(TAG, "onReceive: Installation aborted")
                    processInstallation(context, singleApkInstall)
                }

                PackageInstaller.STATUS_FAILURE_BLOCKED -> {
                    Log.e(TAG, "onReceive: Installation blocked")
                    processInstallation(context, singleApkInstall)
                }

                PackageInstaller.STATUS_FAILURE_CONFLICT -> {
                    Log.e(TAG, "onReceive: Installation conflict")
                    processInstallation(context, singleApkInstall)
                }

                PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> {
                    Log.e(TAG, "onReceive: Incompatible package")
                    processInstallation(context, singleApkInstall)
                }

                PackageInstaller.STATUS_FAILURE_INVALID -> {
                    Log.e(TAG, "onReceive: Invalid package")
                    processInstallation(context, singleApkInstall)
                }

                else -> {
                    Log.e(TAG, "onReceive: Unknown installation status $status")
                    processInstallation(context, singleApkInstall)
                }
            }
        }
    }

    private fun processInstallation(context: Context, singleApkInstall: Boolean) {
        if (singleApkInstall.not()) {
            DownloadService.start(
                context,
                withAction = DownloadService.ACTION_PROCESS_INSTALLATION
            )
        }
    }

}
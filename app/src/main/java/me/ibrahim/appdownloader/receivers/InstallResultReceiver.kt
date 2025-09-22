package me.ibrahim.appdownloader.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import java.io.File

@Suppress("DEPRECATION")
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        val sessionId = intent.getIntExtra("sessionId", -1)
        val fileUri = intent.getStringExtra("fileUri")
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)

        Log.d("InstallReceiver", "Session: $sessionId, Status: $status")
        Log.d("InstallReceiver", "Session: $sessionId, fileUri: $fileUri")

        if (fileUri != null && fileUri.startsWith("file://")) {
            val filePath = fileUri.toUri().path
            val apkFile = filePath?.let { File(filePath) }
            apkFile?.let {
                if (it.exists()) {
                    val deleted: Boolean = it.delete() // deletes the file
                    if (deleted) {
                        Log.d("FileDelete", "APK File deleted successfully")
                    } else {
                        Log.d("FileDelete", "APK Failed to delete the file")
                    }
                } else {
                    Log.d("FileDelete", "APK File does not exist")
                }
            }
        }
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // This is what we want - shows the install confirmation dialog
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
                Toast.makeText(context, "Installation successful!", Toast.LENGTH_SHORT).show()
            }

            PackageInstaller.STATUS_FAILURE -> {
                Toast.makeText(context, "Installation failed", Toast.LENGTH_SHORT).show()
            }

            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                Toast.makeText(context, "Installation aborted", Toast.LENGTH_SHORT).show()
            }

            PackageInstaller.STATUS_FAILURE_BLOCKED -> {
                Toast.makeText(context, "Installation blocked", Toast.LENGTH_SHORT).show()
            }

            PackageInstaller.STATUS_FAILURE_CONFLICT -> {
                Toast.makeText(context, "Installation conflict", Toast.LENGTH_SHORT).show()
            }

            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> {
                Toast.makeText(context, "Incompatible package", Toast.LENGTH_SHORT).show()
            }

            PackageInstaller.STATUS_FAILURE_INVALID -> {
                Toast.makeText(context, "Invalid package", Toast.LENGTH_SHORT).show()
            }

            else -> {
                Toast.makeText(context, "Unknown installation status", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
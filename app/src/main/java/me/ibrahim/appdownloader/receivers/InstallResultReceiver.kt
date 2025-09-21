package me.ibrahim.appdownloader.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import android.widget.Toast

@Suppress("DEPRECATION")
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        val sessionId = intent.getIntExtra("sessionId", -1)
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)

        Log.d("InstallReceiver", "Session: $sessionId, Status: $status")
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // This is what we want - shows the install confirmation dialog
                val confirmationIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
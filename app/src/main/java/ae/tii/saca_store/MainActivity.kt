package ae.tii.saca_store

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ae.tii.saca_store.databinding.ActivityMainBinding
import ae.tii.saca_store.receivers.InstallResultReceiver
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AppDownloader"
        private const val APK_URL =
            "https://github.com/ibrahim-qgate/host_apk/releases/download/teams/microsoft_outlook_minAPI28-universal-nodpi_.apk"
        private const val APK_PATH = "/data/local/tmp/outlook.apk"
    }

    private lateinit var binding: ActivityMainBinding
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (packageManager.canRequestPackageInstalls()) {
                    Toast.makeText(this, "Install permission granted!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Install permission is required!", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnDownloadInstall.setOnClickListener {
//            DownloadService.startDownload(this)
            Log.i(TAG, "Download button clicked")

            lifecycleScope.launch {
                try {
                    binding.btnDownloadInstall.isEnabled = false
                    binding.progressBar.isVisible = true
                    binding.tvStatus.isVisible = true
                    binding.tvStatus.text = getString(R.string.downloading)

                    Log.i(TAG, "calling function : downloadApk")
                    val apkFile = withContext(Dispatchers.IO) { downloadApk(APK_URL, APK_PATH) }
                    if (apkFile != null && apkFile.exists()) {
                        Log.i(TAG, "Download complete, installing silently...")
                        binding.tvStatus.text = getString(R.string.installing)
                        val result =
                            withContext(Dispatchers.IO) { installWithPackageInstaller(apkFile) }
                        Log.i(TAG, "Install result code: $result")
                    } else {
                        Log.i(TAG, "apkFile is null")
                        Toast.makeText(
                            this@MainActivity,
                            "Failed: apkFile is null",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    binding.tvStatus.isVisible = false
                } catch (e: Exception) {
                    Log.e(TAG, "Error during download/install", e)
                    binding.tvStatus.text = "Error: ${e.localizedMessage}"
                }
                binding.progressBar.isVisible = false
                binding.btnDownloadInstall.isEnabled = true
            }
        }

        checkInstallPermission()
    }

    private fun checkInstallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (packageManager.canRequestPackageInstalls().not()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = "package:${BuildConfig.APPLICATION_ID}".toUri()
                }

                permissionLauncher.launch(intent)
            }
        }
    }


    private fun downloadApk(sourceUrl: String, destPath: String): File? {

        val url = URL(sourceUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = true // follow GitHub redirects
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "Mozilla/5.0") // GitHub requires User-Agent
        conn.connectTimeout = 15000
        conn.readTimeout = 15000

        if (conn.responseCode !in 200..299) {
            throw IOException("Failed to download file: HTTP ${conn.responseCode}")
        }
        val file = File(filesDir, "outlook.apk")
//        val file = File(destPath)
        file.parentFile?.mkdirs()
        if (file.exists().not()) {
            Log.i(TAG, "File Does not exists")
        }
        Log.i(TAG, "starting inputstream from conn to: ${file.absolutePath}")
        try {
            conn.inputStream.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            Log.i(TAG, "Error while inputstream: $e")
        }
        conn.disconnect()
        Log.i(TAG, "returning from downloadApk function: file-path ${file.absolutePath}")
        return file
    }


    private fun installSilently(apkFile: File): Int {
        return try {
            Log.i(TAG, "File absolute Path: ${apkFile.absolutePath}")

            val process =
                Runtime.getRuntime().exec(arrayOf("pm", "install", "-r", apkFile.absolutePath))
            process.waitFor()
            process.exitValue()
        } catch (e: Exception) {
            Log.i(TAG, "Exception while installSilently: ${e.localizedMessage}")
            Log.i(TAG, "Exception while installSilently: $e")
            -1
        }
    }

    @SuppressLint("RequestInstallPackagesPolicy")
    private fun installWithPackageInstaller(apkFile: File) {

        val packageInstaller = packageManager.packageInstaller
        val sessionParams =
            PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)

        // Create installation session
        val sessionId = packageInstaller.createSession(sessionParams)
        val session = packageInstaller.openSession(sessionId)

        try {
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
            val intent = Intent(this, InstallResultReceiver::class.java).apply {
                action = "INSTALL_COMPLETE"
                putExtra("sessionId", sessionId)
                putExtra("fileUri", apkFile.toUri().toString())
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getBroadcast(
                this,
                sessionId,
                intent,
                flags
            )

            // Commit the session - THIS WILL TRIGGER THE INSTALL CONFIRMATION DIALOG
            session.commit(pendingIntent.intentSender)
            session.close()

        } catch (e: IOException) {
            session.abandon()
            throw e
        }

    }

}
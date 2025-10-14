package ae.tii.saca_store.service

import ae.tii.saca_store.domain.AppInfo
import ae.tii.saca_store.domain.repos.IAppRepository
import ae.tii.saca_store.domain.repos.IDownloadRepo
import ae.tii.saca_store.receivers.InstallResultReceiver
import ae.tii.saca_store.util.NetworkResponse
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class DownloadService : Service() {


    @Inject
    lateinit var appRepository: IAppRepository

    @Inject
    lateinit var downloadRepo: IDownloadRepo

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private val downloadQueue = LinkedHashSet<AppInfo>()

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "DownloadService::onStartCommand - ${intent?.action}")

        when (intent?.action) {

            ACTION_BIND_TO_SACA_SERVICE -> {
                val bind = SacaExecuter.getInstance(this).bindToService()
                if (bind.not()) {
                    Log.e(TAG, "onStartCommand: Fail to bind to service")
                }
            }

            ACTION_FETCH_POLICIES -> {
                fetchAppsList()
            }

            ACTION_START_APK_DOWNLOADS -> {
                if (downloadQueue.isNotEmpty()) {
                    processQueue()
                } else {
                    stopSelf()
                }
            }

            ACTION_INSTALL_APP -> {
                val receivedDownloadId = intent.getLongExtra(DOWNLOAD_ID, -1)
                if (receivedDownloadId != -1L) {
                    installAppWithPackageInstaller(this, receivedDownloadId)
                }
            }

            ACTION_PROCESS_INSTALLATION -> {
                downloadQueue.firstOrNull()?.let { downloadQueue.remove(it) }
                if (downloadQueue.isNotEmpty()) {
                    processQueue()
                } else {
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun fetchAppsList() {
        job?.cancel()
        job = scope.launch {
            val appsListResponse = appRepository.getAppListResponse()
            Log.d(TAG, "FetchAppsList: Response: $appsListResponse")

            when (appsListResponse) {
                is NetworkResponse.Error -> {
                    //may be retry
                }

                is NetworkResponse.Success<*> -> {
                    val appsList = appsListResponse.data as? List<AppInfo> ?: emptyList()
                    downloadQueue.addAll(appsList)
                    processQueue()
                }
            }
        }
    }

    private fun processQueue() {
        scope.launch {
            val appInfo = downloadQueue.firstOrNull()
            val file = appInfo?.let { downloadRepo.downloadApkFile(this@DownloadService, it) }
            file?.let { downloadRepo.installApkFile(this@DownloadService, it, false) }
        }
    }

    private fun installAppWithPackageInstaller(context: Context, receivedDownloadId: Long) {
        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(receivedDownloadId)
        val cursor = downloadManager.query(query)
        Log.d(TAG, "Installing: $receivedDownloadId")

        if (cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
            val fileUri = cursor.getString(columnIndex)

            fileUri?.let {
                val apkUri = fileUri.toUri()
                val apkFile = File(apkUri.path ?: "")

                if (apkFile.exists()) {
                    try {
//                        installUsingPackageInstaller(context, apkFile)
                        downloadRepo.installApkFile(context, apkFile, false)
                    } catch (e: Exception) {
                        Log.d(TAG, "Install failed: ${e.message}")

                        // Fallback to legacy method for older devices
                        //installUsingLegacyMethod(context, apkFile)
                    }
                } else {
                    Log.d(TAG, "APK file not found: $receivedDownloadId")
                }
            }
        }
        cursor.close()
    }

    @SuppressLint("RequestInstallPackagesPolicy")
    private fun installUsingPackageInstaller(context: Context, apkFile: File) {
        val packageInstaller = context.packageManager.packageInstaller
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
            val intent = Intent(context, InstallResultReceiver::class.java).apply {
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
                context, sessionId, intent, flags
            )

            // Commit the session - THIS WILL TRIGGER THE INSTALL CONFIRMATION DIALOG
            session.commit(pendingIntent.intentSender)
            session.close()

        } catch (e: IOException) {
            session.abandon()
            throw e
        }
    }


    // Fallback method for older Android versions
    private fun installUsingLegacyMethod(context: Context, apkFile: File) {
        try {
            val apkUri = FileProvider.getUriForFile(
                context, "${context.packageName}.provider", apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot install app: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun start(context: Context, withAction: String, downloadId: Long = -1) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = withAction
                if (downloadId >= 0) {
                    putExtra(DOWNLOAD_ID, downloadId)
                }
            }

            Log.d(TAG, "DownloadService::start withAction: $withAction")
            if (withAction in actions) {
                context.startService(intent)
            }
        }

        private val actions = listOf<String>(
            ACTION_INSTALL_APP,
            ACTION_PROCESS_INSTALLATION,
            ACTION_FETCH_POLICIES,
            ACTION_START_APK_DOWNLOADS,
            ACTION_BIND_TO_SACA_SERVICE
        )

        const val ACTION_START_APK_DOWNLOADS =
            "ae.tii.saca_store.service.ACTION_START_APK_DOWNLOADS"
        const val ACTION_INSTALL_APP = "ae.tii.saca_store.service.ACTION_INSTALL_APP"
        const val ACTION_FETCH_POLICIES = "ae.tii.saca_store.service.ACTION_FETCH_POLICIES"
        const val ACTION_PROCESS_INSTALLATION =
            "ae.tii.saca_store.service.ACTION_PROCESS_INSTALLATION"

        const val ACTION_BIND_TO_SACA_SERVICE =
            "ae.tii.saca_store.service.ACTION_BIND_TO_SACA_SERVICE"
        const val DOWNLOAD_ID = "ae.tii.saca_store.service.DOWNLOAD_ID"

        private const val TAG = "DownloadService"
    }

}



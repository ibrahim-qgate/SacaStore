package ae.tii.saca_store.service

import ae.tii.saca_store.R
import ae.tii.saca_store.domain.AppInfo
import ae.tii.saca_store.domain.repos.IAppRepository
import ae.tii.saca_store.domain.repos.IDownloadRepo
import ae.tii.saca_store.presentation.ui.AppListActivity
import ae.tii.saca_store.util.NetworkResponse
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

@AndroidEntryPoint
class DownloadService : Service() {


    @Inject
    lateinit var appRepository: IAppRepository

    @Inject
    lateinit var downloadRepo: IDownloadRepo

    @Inject
    lateinit var sacaExecuter: SacaExecuter

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var tokenJob: Job? = null
    private val maxAttemptToFetchAppsList = 4
    private var currentAttempt = 0

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "DownloadService::onStartCommand - ${intent.action}")

        when (intent.action) {
            ACTION_SINGLE_APP_DOWNLOAD -> {
                val appInfoJson = intent.getStringExtra(SINGLE_APP_INFO)
                val appInfo = appInfoJson?.let { Json.decodeFromString<AppInfo>(it) }
                appInfo?.let {
                    Log.d(TAG, "start downloading: ${it.name}")
//                    downloadRepo.startDownload(it)
                }
            }

            ACTION_GET_CVD_ACCESS_TOKEN -> getCvdAccessToken()
            ACTION_DOWNLOAD_COMPLETED -> {
                val downloadId = intent.getLongExtra(DOWNLOAD_ID, -1)
                if (downloadId != -1L) {
                    handleDownloadCompleted(downloadId)
                }
            }

            ACTION_PROCESS_INSTALLATION -> checkForPendingDownloads()
        }
        return START_NOT_STICKY
    }

    private fun checkForPendingDownloads() {
//        if (downloadRepo.getPendingDownloadIds().isEmpty()) {
//            stopForeground(STOP_FOREGROUND_REMOVE)
//            stopSelf()
//        }
    }

    private fun getCvdAccessToken() {
        tokenJob?.cancel()
        tokenJob = scope.launch {
            val cvdToken = withContext(Dispatchers.IO) { sacaExecuter.getCvdAccessToken() }
            Log.d(TAG, "getCvdAccessToken: $cvdToken")
            fetchAppsList(cvdAccessToken = cvdToken)
        }
    }

    private suspend fun fetchAppsList(cvdAccessToken: String) {
        val appsListResponse = appRepository.getAppListResponse(cvdAccessToken = cvdAccessToken)
        Log.d(TAG, "FetchAppsList: Response: $appsListResponse")
        when (appsListResponse) {
            is NetworkResponse.Error -> {
                //retry
                ////get cvd access token first
                ////then fetch app list
                if (currentAttempt < maxAttemptToFetchAppsList) {
                    currentAttempt++
                    start(this, withAction = ACTION_GET_CVD_ACCESS_TOKEN)
                } else {
                    Log.d(TAG, "Stopping service after $currentAttempt attempts")
                    stopSelf()
                }
            }

            is NetworkResponse.Success<*> -> {
                val appsList = appsListResponse.data as? List<AppInfo> ?: emptyList()
                startDownloading(appsList)
            }
        }
    }

    private suspend fun startDownloading(appsList: List<AppInfo>) {
        appsList.filter { it.downloadUrl.isNotEmpty() }.forEach { app ->
            delay(300)
            val downloadId = downloadRepo.startDownload(app)
        }
    }

    private fun handleDownloadCompleted(downloadId: Long) {
        //create separate coroutine to handle downloaded file
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val uri = downloadRepo.getDownloadedFileUri(downloadId)
            if (uri == null) {
                Log.e(TAG, "handleDownloadCompleted: Uri not found for downloadId: $downloadId")
            }
            downloadRepo.installNext(applicationContext, downloadId)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Saca Store Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun getNotification(): Notification {
        val intent = Intent(this, AppListActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Download Service")
            .setContentText("Downloading in progress")
            .setSmallIcon(R.drawable.download)
            .setContentIntent(pendingIntent)
            .build()
        return notification
    }

    override fun onDestroy() {
        sacaExecuter.unbindToService()
        currentAttempt = 0
        tokenJob?.cancel()
        super.onDestroy()
    }

    companion object {
        fun start(
            context: Context,
            withAction: String,
            downloadId: Long = -1,
            singleAppDownload: AppInfo? = null
        ) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = withAction
                if (downloadId >= 0) {
                    putExtra(DOWNLOAD_ID, downloadId)
                }
                singleAppDownload?.let {
                    putExtra(SINGLE_APP_INFO, Json.encodeToString(singleAppDownload))
                }
            }

            Log.d(TAG, "DownloadService::start withAction: $withAction")
            //starting service for one of the pre-defined actions.
            if (withAction in actions) {
                context.startService(intent)
            }
        }

        private val actions = listOf<String>(
            ACTION_GET_CVD_ACCESS_TOKEN,
            ACTION_DOWNLOAD_COMPLETED,
            ACTION_PROCESS_INSTALLATION,
            ACTION_SINGLE_APP_DOWNLOAD
        )

        const val ACTION_DOWNLOAD_COMPLETED = "saca_store.ACTION_DOWNLOAD_COMPLETED"
        const val ACTION_SINGLE_APP_DOWNLOAD = "saca_store.ACTION_SINGLE_APP_DOWNLOAD"
        const val ACTION_PROCESS_INSTALLATION = "saca_store.ACTION_PROCESS_INSTALLATION"
        const val ACTION_GET_CVD_ACCESS_TOKEN = "saca_store.ACTION_GET_CVD_ACCESS_TOKEN"
        const val DOWNLOAD_ID = "saca_store.DOWNLOAD_ID"
        const val SINGLE_APP_INFO = "saca_store.SINGLE_APP_INFO"
        private const val TAG = "DownloadService"
        private const val CHANNEL_ID = "SacaStoreChannel"
    }

}



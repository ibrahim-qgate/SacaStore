package ae.tii.saca_store.worker

import ae.tii.saca_store.domain.room.AppDatabase
import ae.tii.saca_store.domain.room.DownloadDao
import ae.tii.saca_store.domain.room.DownloadItem
import ae.tii.saca_store.domain.room.DownloadStatus
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

class DownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val DOWNLOAD_ITEM_ID = "downloadItemId"
        private const val TAG = "DownloadWorker"
    }


    private val dao: DownloadDao = AppDatabase.Companion.get(appContext).downloadDao()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result {
        val id = inputData.getString(DOWNLOAD_ITEM_ID) ?: return Result.failure()
        val item = dao.get(id) ?: return Result.failure()

        dao.update(item.copy(status = DownloadStatus.RUNNING, lastError = null))

        return try {
            val result = downloadFile(item)
            if (result) {
                dao.update(item.copy(status = DownloadStatus.COMPLETED))
                enqueueInstall(item)
                Result.success()
            } else {
                dao.update(item.copy(status = DownloadStatus.FAILED, lastError = "Unknown"))
                Result.failure()
            }
        } catch (e: IOException) {
            // network error -> retry
            val newRetries = item.retries + 1
            dao.update(
                item.copy(
                    status = DownloadStatus.FAILED,
                    retries = newRetries,
                    lastError = e.message
                )
            )
            if (shouldRetry(e, item)) {
                Result.retry()
            } else {
                Result.failure()
            }
        } catch (e: Exception) {
            dao.update(item.copy(status = DownloadStatus.FAILED, lastError = e.message))
            Result.retry()
        }
    }

    private suspend fun downloadFile(item: DownloadItem): Boolean {
        val outputFile = File(item.destPath)

        val request = Request.Builder().url(item.url).build()
        val response = client.newCall(request).execute()

        if (response.isSuccessful.not()) {
            val code = response.code
            if (code in 400..499) {
                throw IOException("HTTP ${code}")
            } else {
                throw IOException("Server error ${code}")
            }
        }


        val contentLength = response.body?.contentLength() ?: -1L
        dao.update(item.copy(totalBytes = contentLength))

        val inputStream: InputStream =
            response.body?.byteStream() ?: throw Exception("No stream")
        val outputStream = FileOutputStream(outputFile)
        val buffer = ByteArray(8 * 1024)
        var bytesRead: Int
        var downloaded = 0L

        while (isStopped.not()) {
            val bytesRead = inputStream.read(buffer)
            if (bytesRead < 0) break
            outputStream.write(buffer, 0, bytesRead)
            downloaded += bytesRead
            dao.update(item.copy(downloadedBytes = downloaded))
        }
        outputStream.flush()
        outputStream.close()
        inputStream.close()

        if (outputFile.exists().not()) throw IOException("File Not Found: ${outputFile.path}")
        Log.d(
            TAG,
            "downloadFile: ${item.fileName} Total Bytes: $contentLength, downloaded: $downloaded"
        )
        return contentLength == downloaded
    }

    private fun shouldRetry(e: Exception, item: DownloadItem): Boolean {
        // unlimit retries
        return true //item.retries < 5
    }


    private fun enqueueInstall(item: DownloadItem) {
        val work = OneTimeWorkRequestBuilder<ApkInstallWorker>()
            .setInputData(workDataOf(ApkInstallWorker.DOWNLOAD_ID to item.id))
            .build()

        WorkManager.Companion.getInstance(applicationContext).enqueueUniqueWork(
            "apk_install_queue",
            ExistingWorkPolicy.APPEND,
            work
        )
    }
}
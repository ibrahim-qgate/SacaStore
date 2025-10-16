package ae.tii.saca_store.domain.repos

import ae.tii.saca_store.domain.AppInfo
import android.content.Context
import android.net.Uri
import java.util.concurrent.ConcurrentLinkedQueue

interface IDownloadRepo {
    suspend fun startDownload(app: AppInfo): Long
    fun getDownloadedFileUri(downloadId: Long): Uri?
    fun installNext(context: Context, downloadId: Long)
    fun getPendingDownloadIds(): ConcurrentLinkedQueue<Long>
}
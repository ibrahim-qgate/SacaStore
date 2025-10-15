package ae.tii.saca_store.domain.repos

import ae.tii.saca_store.domain.AppInfo
import android.content.Context
import android.net.Uri

interface IDownloadRepo {
    fun startDownload(app: AppInfo): Long
    fun getDownloadedFileUri(downloadId: Long): Uri?
    fun installNext(context: Context)
}
package ae.tii.saca_store.domain.repos

import ae.tii.saca_store.domain.AppInfo
import android.content.Context
import java.io.File

interface IDownloadRepo {
    fun startDownload(app: AppInfo): Long
    suspend fun downloadApkFile(context: Context, appInfo: AppInfo): File?
    fun installApkFile(context: Context, apkFile: File, singleApk: Boolean = true)
}
package ae.tii.saca_store.domain.repos

import ae.tii.saca_store.domain.AppInfo

interface IDownloadRepo {
    fun startDownload(app: AppInfo): Long
}
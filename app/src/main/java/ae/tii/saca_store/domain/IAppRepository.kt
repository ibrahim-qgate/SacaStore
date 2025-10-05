package ae.tii.saca_store.domain

import ae.tii.saca_store.util.NetworkResponse

interface IAppRepository {
    suspend fun getAppList(): NetworkResponse<List<AppInfo>>
}
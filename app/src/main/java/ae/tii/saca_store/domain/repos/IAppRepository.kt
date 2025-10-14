package ae.tii.saca_store.domain.repos

import ae.tii.saca_store.domain.AppInfo
import ae.tii.saca_store.util.NetworkResponse

interface IAppRepository {
    suspend fun getAppListResponse(cvdAccessToken: String): NetworkResponse<List<AppInfo>>
}
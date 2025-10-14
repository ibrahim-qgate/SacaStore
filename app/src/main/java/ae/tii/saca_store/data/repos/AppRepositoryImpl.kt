package ae.tii.saca_store.data.repos

import ae.tii.saca_store.data.remote.ApiService
import ae.tii.saca_store.domain.AppInfo
import ae.tii.saca_store.domain.repos.IAppRepository
import ae.tii.saca_store.util.NetworkResponse
import ae.tii.saca_store.util.toDomain

class AppRepositoryImpl(private val apiService: ApiService) : IAppRepository {

    override suspend fun getAppListResponse(cvdAccessToken: String): NetworkResponse<List<AppInfo>> {
        return try {
            val response = apiService.getAppsList(authToken = "Bearer $cvdAccessToken")

            if (response.isSuccessful) {
                val appsList = response.body()?.appList?.map { it.toDomain() } ?: emptyList()
                if (appsList.isNotEmpty()) {
                    NetworkResponse.Success(data = appsList)
                } else {
                    NetworkResponse.Error(error = "No apps found in the response")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = when (response.code()) {
                    401 -> "Unauthorized access"
                    403 -> "Forbidden request"
                    404 -> "Apps not found"
                    500 -> "Internal server error"
                    else -> errorBody ?: "Unknown error occurred (code ${response.code()})"
                }

                NetworkResponse.Error(error = errorMessage)
            }
        } catch (e: Exception) {
            NetworkResponse.Error(error = e.localizedMessage ?: "Unexpected error")
        }
    }
}
package ae.tii.saca_store.data

import ae.tii.saca_store.data.remote.ApiService
import ae.tii.saca_store.domain.AppInfo
import ae.tii.saca_store.domain.IAppRepository
import ae.tii.saca_store.util.NetworkResponse
import ae.tii.saca_store.util.toDomain

class AppRepositoryImpl(private val apiService: ApiService) : IAppRepository {
    override suspend fun getAppList(): NetworkResponse<List<AppInfo>> {
        return try {
            val response = apiService.getAppsList()

            if (response.isSuccessful) {
                val appsList = response.body()?.record?.apps?.map { it.toDomain() } ?: emptyList()
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


/*
val appsList = listOf(
    AppInfo(
        name = "Aurora Store",
        packageName = "com.example.chatapp",
        version = "1.2.3",
        downloadUrl = "https://github.com/ibrahim-qgate/host_apk/releases/download/teams/AuroraStore-4.7.5.apk",
        iconUrl = "https://github.com/johndoe/my-apk-repo/releases/download/v1.0.0/chatapp-icon.png",
        description = "A secure and fast messaging app.",
        abi = "universal"
    ),
    AppInfo(
        name = "F-Driod",
        packageName = "com.example.weatherpro",
        version = "5.4.1",
        downloadUrl = "https://github.com/ibrahim-qgate/host_apk/releases/download/teams/F-Droid.apk",
        iconUrl = "https://github.com/johndoe/my-apk-repo/releases/download/v1.0.0/weatherpro-icon.png",
        description = "Accurate weather forecasts worldwide.",
        abi = "universal"
    ),
    AppInfo(
        name = "Microsoft Authenticator",
        packageName = "com.example.notekeeper",
        version = "2.0.0",
        downloadUrl = "https://github.com/ibrahim-qgate/host_apk/releases/download/teams/Microsoft.Authenticator_6.2509.6046_apkcombo.com.1.apk",
        iconUrl = null,
        description = null,
        abi = "arm64-v8a"
    ),
    AppInfo(
        name = "Microsoft Teams",
        packageName = "com.example.notekeeper",
        version = "2.0.0",
        downloadUrl = "https://github.com/ibrahim-qgate/host_apk/releases/download/teams/microsoft.teams_minAPI26-x86_64-nodpi.apk",
        iconUrl = null,
        description = null,
        abi = "x86_64"
    ),
    AppInfo(
        name = "Outlook",
        packageName = "com.example.notekeeper",
        version = "2.0.0",
        downloadUrl = "https://github.com/ibrahim-qgate/host_apk/releases/download/teams/microsoft_outlook_minAPI28-universal-nodpi_.apk",
        iconUrl = null,
        description = null,
        abi = "universal"
    )
)
return NetworkResponse.Success(data = appsList)*/

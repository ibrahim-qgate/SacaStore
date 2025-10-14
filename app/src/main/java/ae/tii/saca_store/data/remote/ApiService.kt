package ae.tii.saca_store.data.remote

import ae.tii.saca_store.data.dtos.AppsListResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface ApiService {

    @GET("saca-api/downloader/applist")
    suspend fun getAppsList(
        @Header("Authorization") authToken: String,
        @Query("cvdid") cvdId: Int = 123
    ): Response<AppsListResponse>

}
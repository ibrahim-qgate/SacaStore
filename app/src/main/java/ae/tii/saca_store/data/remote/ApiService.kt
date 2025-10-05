package ae.tii.saca_store.data.remote

import retrofit2.Response
import retrofit2.http.GET

interface ApiService {

    @GET("b/68dce59fae596e708f021cf7")
    suspend fun getAppsList(): Response<AppsListResponse>
}
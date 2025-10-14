package ae.tii.saca_store.data.dtos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class AppsListResponse(
    @SerialName("Status") var status: Boolean? = null,
    @SerialName("AppList") var appList: ArrayList<App> = arrayListOf()
)

@Serializable
data class App(
    @SerialName("abi")
    val abi: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("downloadUrl")
    val downloadUrl: String? = null,
    @SerialName("iconUrl")
    val iconUrl: String? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("packageName")
    val packageName: String? = null,
    @SerialName("version")
    val version: String? = null
)





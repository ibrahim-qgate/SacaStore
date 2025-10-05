package ae.tii.saca_store.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class AppsListResponse(
    /*@SerialName("metadata")
    val metadata: Metadata?,*/
    @SerialName("record")
    val record: Record?
)

/*@Serializable
data class Metadata(
    @SerialName("createdAt")
    val createdAt: String?,
    @SerialName("id")
    val id: String?,
    @SerialName("name")
    val name: String?,
    @SerialName("private")
    val `private`: Boolean?
)*/

@Serializable
data class Record(
    @SerialName("apps")
    val apps: List<App>? = emptyList()
)

@Serializable
data class App(
    @SerialName("abi")
    val abi: String?,
    @SerialName("description")
    val description: String?,
    @SerialName("downloadUrl")
    val downloadUrl: String?,
    @SerialName("iconUrl")
    val iconUrl: String?,
    @SerialName("name")
    val name: String?,
    @SerialName("packageName")
    val packageName: String?,
    @SerialName("version")
    val version: String?
)





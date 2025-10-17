package ae.tii.saca_store.domain

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Keep
data class AppInfo(
    @SerialName("name") val name: String,
    @SerialName("packageName") val packageName: String,
    @SerialName("version") val version: String,
    @SerialName("downloadUrl") val downloadUrl: String,
    @SerialName("iconUrl") val iconUrl: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("abi") val abi: String = ""
)
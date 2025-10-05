package ae.tii.saca_store.domain

import kotlinx.serialization.Serializable

@Serializable
data class AppInfo(
    val name: String,
    val packageName: String,
    val version: String,
    val downloadUrl: String,
    val iconUrl: String? = null,
    val description: String? = null,
    val abi: String = ""
)
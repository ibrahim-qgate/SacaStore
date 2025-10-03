package ae.tii.saca_store.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen : NavKey {
    @Serializable
    data object AppsList : Screen

    @Serializable
    data class AppDetail(val appName: String) : Screen
}
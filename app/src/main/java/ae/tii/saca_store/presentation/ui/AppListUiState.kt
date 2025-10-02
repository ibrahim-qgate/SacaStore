package ae.tii.saca_store.presentation.ui

import ae.tii.saca_store.domain.AppInfo

sealed class AppListUiState {
    object Loading : AppListUiState()
    data class Success(val apps: List<AppInfo>) : AppListUiState()
    data class Error(val message: String) : AppListUiState()
}
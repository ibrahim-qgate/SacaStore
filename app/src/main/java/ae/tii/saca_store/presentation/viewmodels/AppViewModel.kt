package ae.tii.saca_store.presentation.viewmodels

import ae.tii.saca_store.domain.AppInfo
import ae.tii.saca_store.domain.repos.IAppRepository
import ae.tii.saca_store.domain.repos.IDownloadRepo
import ae.tii.saca_store.presentation.ui.AppListUiState
import ae.tii.saca_store.util.NetworkResponse
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val repository: IAppRepository,
    private val downloadRepo: IDownloadRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow<AppListUiState>(AppListUiState.Loading)
    val uiState: StateFlow<AppListUiState> = _uiState.asStateFlow()

    init {
        fetchAppList()
    }

    fun fetchAppList() {
        viewModelScope.launch {
            _uiState.value = AppListUiState.Loading
            try {
                val networkResponse = withContext(Dispatchers.IO) { repository.getAppList() }
                when (networkResponse) {
                    is NetworkResponse.Error -> {
                        _uiState.value =
                            AppListUiState.Error(networkResponse.error)
                    }

                    is NetworkResponse.Success<*> -> {

                        startAppsDownload(networkResponse.data as? List<AppInfo> ?: emptyList())

                        _uiState.value =
                            AppListUiState.Success(
                                networkResponse.data as? List<AppInfo> ?: emptyList()
                            )
                    }
                }

            } catch (e: Exception) {
                _uiState.value = AppListUiState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun startDownload(appInfo: AppInfo) {
        downloadRepo.startDownload(appInfo)
    }


    private fun startAppsDownload(appsList: List<AppInfo>) {
        appsList.forEach { downloadRepo.startDownload(it) }
    }

}
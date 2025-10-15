package ae.tii.saca_store.presentation.viewmodels

import ae.tii.saca_store.domain.AppInfo
import ae.tii.saca_store.domain.repos.IAppRepository
import ae.tii.saca_store.domain.repos.IDownloadRepo
import ae.tii.saca_store.presentation.ui.AppListUiState
import ae.tii.saca_store.service.DownloadService
import ae.tii.saca_store.service.SacaExecuter
import ae.tii.saca_store.util.NetworkResponse
import android.content.Context
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
    private val sacaExecuter: SacaExecuter,
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
                val networkResponse = withContext(Dispatchers.IO) {
                    val cvdToken = sacaExecuter.getCvdAccessToken()
                    repository.getAppListResponse(cvdAccessToken = cvdToken)
                }
                when (networkResponse) {
                    is NetworkResponse.Error -> {
                        _uiState.value =
                            AppListUiState.Error(networkResponse.error)
                    }

                    is NetworkResponse.Success<*> -> {
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

    fun startDownload(context: Context, appInfo: AppInfo) {
        viewModelScope.launch(Dispatchers.IO) {
//            downloadRepo.startDownload(appInfo)
            DownloadService.start(
                context.applicationContext,
                withAction = DownloadService.ACTION_SINGLE_APP_DOWNLOAD,
                singleAppDownload = appInfo
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        sacaExecuter.unbindToService()
    }

}
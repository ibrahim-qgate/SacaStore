package ae.tii.saca_store.presentation.viewmodels

import ae.tii.saca_store.domain.IAppRepository
import ae.tii.saca_store.presentation.ui.AppListUiState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(
    private val repository: IAppRepository
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
                val appList = withContext(Dispatchers.IO) { repository.getAppList() }
                _uiState.value = AppListUiState.Success(appList)
            } catch (e: Exception) {
                _uiState.value = AppListUiState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }


}
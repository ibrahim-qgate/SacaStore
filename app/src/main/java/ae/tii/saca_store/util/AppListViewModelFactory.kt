package ae.tii.saca_store.util

import ae.tii.saca_store.data.AppRepositoryImpl
import ae.tii.saca_store.domain.IAppRepository
import ae.tii.saca_store.presentation.viewmodels.AppListViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class AppListViewModelFactory() : ViewModelProvider.Factory {
    private val repository: IAppRepository = AppRepositoryImpl()


    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppListViewModel::class.java)) {
            return AppListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

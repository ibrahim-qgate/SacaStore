package ae.tii.saca_store.presentation.ui

import ae.tii.saca_store.presentation.ui.composables.AppListItem
import ae.tii.saca_store.presentation.viewmodels.AppViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun AppListScreen(viewModel: AppViewModel) {


    val uiState by viewModel.uiState.collectAsState()

    when (uiState) {
        is AppListUiState.Loading -> {
            CircularProgressIndicator()
        }

        is AppListUiState.Success -> {
            LazyColumn {
                items((uiState as AppListUiState.Success).apps) { app ->
                    AppListItem(app)
                }
            }
        }

        is AppListUiState.Error -> {
            Text("Error: ${(uiState as AppListUiState.Error).message}")
        }
    }
}

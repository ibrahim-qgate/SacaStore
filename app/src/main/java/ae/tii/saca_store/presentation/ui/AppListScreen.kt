package ae.tii.saca_store.presentation.ui

import ae.tii.saca_store.presentation.viewmodels.AppListViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

@Composable
fun AppListScreen(
    viewModel: AppListViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    when (uiState) {
        is AppListUiState.Loading -> {
            CircularProgressIndicator()
        }

        is AppListUiState.Success -> {
            LazyColumn {
                items((uiState as AppListUiState.Success).apps) { app ->
                    Text(text = app.name)
                }
            }
        }

        is AppListUiState.Error -> {
            Text("Error: ${(uiState as AppListUiState.Error).message}")
        }
    }
}

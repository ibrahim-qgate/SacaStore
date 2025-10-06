package ae.tii.saca_store.presentation.ui

import ae.tii.saca_store.presentation.ui.composables.AppListItem
import ae.tii.saca_store.presentation.viewmodels.AppViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun AppListScreen(viewModel: AppViewModel) {


    val uiState by viewModel.uiState.collectAsState()

    when (uiState) {
        is AppListUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is AppListUiState.Success -> {
            LazyColumn {
                items((uiState as AppListUiState.Success).apps) { app ->
                    AppListItem(app) {
                        viewModel.startDownload(it)
                    }
                }
            }
        }

        is AppListUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Error: ${(uiState as AppListUiState.Error).message}")
            }
        }
    }
}

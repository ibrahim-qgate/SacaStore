package ae.tii.saca_store.presentation.ui

import ae.tii.saca_store.presentation.ui.composables.AppListItem
import ae.tii.saca_store.presentation.ui.composables.EmptyAppsList
import ae.tii.saca_store.presentation.ui.composables.ErrorUi
import ae.tii.saca_store.presentation.viewmodels.AppViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppListScreen(viewModel: AppViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    when (uiState) {
        is AppListUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Loading apps...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        is AppListUiState.Success -> {
            val apps = (uiState as AppListUiState.Success).apps
            if (apps.isEmpty()) {
                EmptyAppsList(viewModel)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    items(apps) { app ->
                        AppListItem(app) {
                            viewModel.startDownload(it)
                        }
                    }
                }
            }
        }


        is AppListUiState.Error -> {
            val errorMessage = (uiState as AppListUiState.Error).message
            ErrorUi(errorMessage, viewModel)
        }
    }
}

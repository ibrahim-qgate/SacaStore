package ae.tii.saca_store.presentation.ui

import ae.tii.saca_store.presentation.ui.composables.AppListItem
import ae.tii.saca_store.presentation.viewmodels.AppViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun AppListScreen(viewModel: AppViewModel) {

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    when (uiState) {
        is AppListUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is AppListUiState.Success -> {
            LazyColumn {
                items((uiState as AppListUiState.Success).apps) { app ->
                    AppListItem(app) {
                        viewModel.startDownload(context, it)
                    }
                }
            }
        }

        is AppListUiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Error: ${(uiState as AppListUiState.Error).message}",
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

package ae.tii.saca_store.presentation.ui

import ae.tii.saca_store.presentation.ui.theme.AppDownloaderTheme
import ae.tii.saca_store.presentation.viewmodels.AppListViewModel
import ae.tii.saca_store.util.AppListViewModelFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider

class AppListActivity : ComponentActivity() {

    val factory = AppListViewModelFactory()


    private val viewModel: AppListViewModel =
        ViewModelProvider(this, factory)[AppListViewModel::class.java]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppDownloaderTheme {
                Scaffold(modifier = Modifier.Companion.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        AppListScreen(viewModel)
                    }
                }
            }
        }
    }
}
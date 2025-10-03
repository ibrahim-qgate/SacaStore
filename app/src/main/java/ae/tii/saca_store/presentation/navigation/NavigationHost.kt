package ae.tii.saca_store.presentation.navigation

import ae.tii.saca_store.presentation.ui.AppListScreen
import ae.tii.saca_store.presentation.viewmodels.AppViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay

@Composable
fun NavigationHost(
    modifier: Modifier = Modifier,
    viewModel: AppViewModel
) {

    val backStack = remember { mutableStateListOf<Screen>(Screen.AppsList) }

    NavDisplay(
        modifier = modifier, backStack = backStack,
        entryDecorators = listOf(),
        entryProvider = entryProvider {
            entry<Screen.AppsList> {
                AppListScreen(viewModel)
            }
            entry<Screen.AppDetail> {}
        })
}
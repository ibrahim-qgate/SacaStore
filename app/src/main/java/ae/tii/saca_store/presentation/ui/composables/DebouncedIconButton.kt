package ae.tii.saca_store.presentation.ui.composables

import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DebouncedIconButton(
    onClick: () -> Unit,
    debounceMillis: Long = 1000L, // default 1 second debounce
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    var clickable by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()


    IconButton(
        onClick = {
            if (clickable && enabled) {
                clickable = false
                onClick()
                // Re-enable after debounceMillis
                coroutineScope.launch {
                    delay(debounceMillis)
                    clickable = true
                }
            }
        },
        enabled = enabled && clickable
    ) {
        content()
    }
}

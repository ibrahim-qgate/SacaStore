package ae.tii.saca_store.util

sealed interface NetworkResponse<out T> {
    data class Success<out T>(val data: T) : NetworkResponse<T>
    data class Error(val error: String) : NetworkResponse<Nothing>
}

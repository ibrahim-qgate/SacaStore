package ae.tii.saca_store.util

import ae.tii.saca_store.data.dtos.App
import ae.tii.saca_store.domain.AppInfo
import android.util.Log


fun App.toDomain(): AppInfo {
    return AppInfo(
        name = name.orEmpty(),
        packageName = packageName.orEmpty(),
        version = version.orEmpty(),
        downloadUrl = downloadUrl.orEmpty(),
        iconUrl = iconUrl,
        description = description,
        abi = abi.orEmpty()
    )
}


fun <T> repeatUntilNotNull(
    times: Int = 5,
    delayMillis: Long = 600,
    block: () -> T?
): T? {
    try {
        repeat(times = times) {
            val result = block()
            Log.i("repeatUntilNotNull", "Received result: $result")
            val isValid = when (result) {
                null -> false
                is String -> result.isNotEmpty()
                else -> true
            }
            if (isValid) return result
            if (delayMillis > 0) Thread.sleep(delayMillis)
        }
        return null
    } catch (e: Exception) {
        Log.e("repeatUntilNotNull", "Error $e")
        return null
    }
}

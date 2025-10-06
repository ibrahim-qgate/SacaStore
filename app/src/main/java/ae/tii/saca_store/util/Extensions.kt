package ae.tii.saca_store.util

import ae.tii.saca_store.data.dtos.App
import ae.tii.saca_store.domain.AppInfo


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

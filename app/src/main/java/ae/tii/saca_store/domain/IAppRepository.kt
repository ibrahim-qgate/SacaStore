package ae.tii.saca_store.domain

interface IAppRepository {
    fun getAppList(): List<AppInfo>
}
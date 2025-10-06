package ae.tii.saca_store.di

import ae.tii.saca_store.data.repos.AppRepositoryImpl
import ae.tii.saca_store.data.remote.ApiService
import ae.tii.saca_store.data.repos.DownloadRepoImpl
import ae.tii.saca_store.domain.repos.IAppRepository
import ae.tii.saca_store.domain.repos.IDownloadRepo
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppModules {

    @Provides
    @Singleton
    fun providesAppRepository(apiService: ApiService): IAppRepository {
        return AppRepositoryImpl(apiService)
    }

    @Provides
    @Singleton
    fun providesDownloadRepository(@ApplicationContext context: Context): IDownloadRepo {
        return DownloadRepoImpl(context)
    }

}
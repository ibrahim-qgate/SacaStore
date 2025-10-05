package ae.tii.saca_store.di

import ae.tii.saca_store.data.AppRepositoryImpl
import ae.tii.saca_store.data.remote.ApiService
import ae.tii.saca_store.domain.IAppRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
}
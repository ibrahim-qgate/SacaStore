package ae.tii.saca_store.di

import ae.tii.saca_store.data.repos.AppRepositoryImpl
import ae.tii.saca_store.data.remote.ApiService
import ae.tii.saca_store.data.repos.DownloadRepoImpl
import ae.tii.saca_store.domain.repos.IAppRepository
import ae.tii.saca_store.domain.repos.IDownloadRepo
import ae.tii.saca_store.domain.room.AppDatabase
import ae.tii.saca_store.domain.room.DownloadDao
import android.content.Context
import androidx.room.Room
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
    fun providesDownloadRepository(
        @ApplicationContext context: Context,
        dao: DownloadDao
    ): IDownloadRepo {
        return DownloadRepoImpl(context, dao)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "app_downloader.db"
        )
            .fallbackToDestructiveMigration(true)
            .build()
    }

    @Provides
    fun provideDownloadDao(db: AppDatabase): DownloadDao = db.downloadDao()
}
package hoon.example.androidsandbox.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hoon.example.androidsandbox.data.kvs.KvsClientConfig
import hoon.example.androidsandbox.data.kvs.repository.KvsMasterRepositoryImpl
import hoon.example.androidsandbox.data.kvs.repository.KvsViewerRepositoryImpl
import hoon.example.androidsandbox.domain.kvs.repository.KvsMasterRepository
import hoon.example.androidsandbox.domain.kvs.repository.KvsViewerRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class KvsModule {

    @Binds
    @Singleton
    abstract fun bindKvsMasterRepository(
        impl: KvsMasterRepositoryImpl
    ): KvsMasterRepository

    @Binds
    @Singleton
    abstract fun bindKvsViewerRepository(
        impl: KvsViewerRepositoryImpl
    ): KvsViewerRepository

    companion object {
        @Provides
        @Singleton
        fun provideKvsClientConfig(): KvsClientConfig {
            return KvsClientConfig()
        }
    }
}

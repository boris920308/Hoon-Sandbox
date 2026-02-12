package hoon.example.androidsandbox.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hoon.example.androidsandbox.data.kvs.KvsClientConfig
import hoon.example.androidsandbox.data.kvs.repository.KvsMasterRepositoryImpl
import hoon.example.androidsandbox.domain.kvs.repository.KvsMasterRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class KvsModule {

    @Binds
    @Singleton
    abstract fun bindKvsMasterRepository(
        impl: KvsMasterRepositoryImpl
    ): KvsMasterRepository

    companion object {
        @Provides
        @Singleton
        fun provideKvsClientConfig(): KvsClientConfig {
            return KvsClientConfig()
        }
    }
}

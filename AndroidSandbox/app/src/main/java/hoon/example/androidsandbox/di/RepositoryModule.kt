package hoon.example.androidsandbox.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hoon.example.androidsandbox.data.counter.repository.NameRepositoryImpl
import hoon.example.androidsandbox.domain.counter.repository.NameRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindNameRepository(
        nameRepositoryImpl: NameRepositoryImpl
    ): NameRepository
}

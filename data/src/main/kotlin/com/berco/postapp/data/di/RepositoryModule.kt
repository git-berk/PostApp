package com.berco.postapp.data.di

import com.berco.postapp.data.repository.PostRepositoryImpl
import com.berco.postapp.domain.repository.PostRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindPostRepository(impl: PostRepositoryImpl): PostRepository
}
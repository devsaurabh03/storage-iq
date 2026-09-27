package com.storage.intelligence.di

import com.storage.intelligence.data.scanner.FileHasher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ScannerModule {

    @Provides
    @Singleton
    fun provideFileHasher(): FileHasher = FileHasher()
}

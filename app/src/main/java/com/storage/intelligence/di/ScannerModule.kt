package com.storage.intelligence.di

import android.content.Context
import com.storage.intelligence.data.scanner.FileHasher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ScannerModule {

    @Provides
    @Singleton
    fun provideFileHasher(
        @ApplicationContext context: Context
    ): FileHasher = FileHasher(context)
}

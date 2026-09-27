package com.storage.intelligence.di

import android.content.Context
import androidx.room.Room
import com.storage.intelligence.core.common.DefaultDispatcherProvider
import com.storage.intelligence.core.common.DispatcherProvider
import com.storage.intelligence.core.database.StorageDatabase
import com.storage.intelligence.core.database.dao.DuplicateGroupDao
import com.storage.intelligence.core.database.dao.FileMetadataDao
import com.storage.intelligence.core.database.dao.PerformanceSnapshotDao
import com.storage.intelligence.core.database.dao.SnapshotDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()

    @Provides
    @Singleton
    fun provideStorageDatabase(
        @ApplicationContext context: Context
    ): StorageDatabase {
        return Room.databaseBuilder(
            context,
            StorageDatabase::class.java,
            "storage_iq.db"
        )
            .addMigrations(
                StorageDatabase.MIGRATION_1_2,
                StorageDatabase.MIGRATION_2_3,
                StorageDatabase.MIGRATION_3_4
            )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideFileMetadataDao(db: StorageDatabase): FileMetadataDao = db.fileMetadataDao()

    @Provides
    fun provideSnapshotDao(db: StorageDatabase): SnapshotDao = db.snapshotDao()

    @Provides
    fun provideDuplicateGroupDao(db: StorageDatabase): DuplicateGroupDao = db.duplicateGroupDao()

    @Provides
    fun providePerformanceSnapshotDao(db: StorageDatabase): PerformanceSnapshotDao = db.performanceSnapshotDao()
}

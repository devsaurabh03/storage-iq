package com.storage.intelligence.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.storage.intelligence.core.database.dao.DuplicateGroupDao
import com.storage.intelligence.core.database.dao.FileMetadataDao
import com.storage.intelligence.core.database.dao.PerformanceSnapshotDao
import com.storage.intelligence.core.database.dao.SnapshotDao
import com.storage.intelligence.core.database.entity.DuplicateGroupEntity
import com.storage.intelligence.core.database.entity.FileMetadataEntity
import com.storage.intelligence.core.database.entity.PerformanceSnapshotEntity
import com.storage.intelligence.core.database.entity.StorageSnapshotEntity

@Database(
    entities = [
        FileMetadataEntity::class,
        StorageSnapshotEntity::class,
        DuplicateGroupEntity::class,
        PerformanceSnapshotEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class StorageDatabase : RoomDatabase() {
    abstract fun fileMetadataDao(): FileMetadataDao
    abstract fun snapshotDao(): SnapshotDao
    abstract fun duplicateGroupDao(): DuplicateGroupDao
    abstract fun performanceSnapshotDao(): PerformanceSnapshotDao

    companion object {
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE storage_snapshots ADD COLUMN totalDeviceBytes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE storage_snapshots ADD COLUMN freeBytes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE storage_snapshots ADD COLUMN appsBytes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE storage_snapshots ADD COLUMN documentsBytes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE storage_snapshots ADD COLUMN audioBytes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE storage_snapshots ADD COLUMN unaccountedBytes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE storage_snapshots ADD COLUMN whatsappVideosBytes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE storage_snapshots ADD COLUMN whatsappImagesBytes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE storage_snapshots ADD COLUMN whatsappDocsBytes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE storage_snapshots ADD COLUMN whatsappAudioBytes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE storage_snapshots ADD COLUMN telegramVideosBytes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE storage_snapshots ADD COLUMN telegramImagesBytes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE storage_snapshots ADD COLUMN telegramDocsBytes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE storage_snapshots ADD COLUMN telegramAudioBytes INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS performance_snapshots (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestampMs INTEGER NOT NULL,
                        ramPressure TEXT NOT NULL,
                        totalRamBytes INTEGER NOT NULL,
                        usedRamBytes INTEGER NOT NULL,
                        availableRamBytes INTEGER NOT NULL,
                        cpuLoadPercent INTEGER NOT NULL,
                        cpuFrequencyGhz REAL NOT NULL,
                        thermalState TEXT NOT NULL,
                        batteryLevel INTEGER NOT NULL,
                        batteryTempCelsius REAL NOT NULL,
                        isCharging INTEGER NOT NULL,
                        storageUsedBytes INTEGER NOT NULL,
                        storageTotalBytes INTEGER NOT NULL,
                        backgroundActiveAppCount INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE file_metadata ADD COLUMN isOptimized INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE file_metadata ADD COLUMN originalSizeBytes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE file_metadata ADD COLUMN compressedFilePath TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE file_metadata ADD COLUMN compressionAlgorithm TEXT DEFAULT NULL")
            }
        }
    }
}

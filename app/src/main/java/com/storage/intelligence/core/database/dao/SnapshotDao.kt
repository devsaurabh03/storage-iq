package com.storage.intelligence.core.database.dao

import androidx.room.*
import com.storage.intelligence.core.database.entity.DuplicateGroupEntity
import com.storage.intelligence.core.database.entity.StorageSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: StorageSnapshotEntity)

    @Query("SELECT * FROM storage_snapshots ORDER BY timestampMs DESC LIMIT :limit")
    fun getRecentSnapshotsFlow(limit: Int): Flow<List<StorageSnapshotEntity>>

    @Query("SELECT * FROM storage_snapshots WHERE timestampMs >= :sinceTimestampMs ORDER BY timestampMs ASC")
    suspend fun getSnapshotsSince(sinceTimestampMs: Long): List<StorageSnapshotEntity>

    @Query("SELECT * FROM storage_snapshots ORDER BY timestampMs ASC LIMIT 1")
    suspend fun getOldestSnapshot(): StorageSnapshotEntity?

    @Query("SELECT * FROM storage_snapshots WHERE timestampMs >= :startTimeMs AND timestampMs <= :endTimeMs ORDER BY timestampMs ASC")
    fun getSnapshotsInRangeFlow(startTimeMs: Long, endTimeMs: Long): Flow<List<StorageSnapshotEntity>>

    @Query("SELECT * FROM storage_snapshots WHERE timestampMs >= :startTimeMs AND timestampMs <= :endTimeMs ORDER BY timestampMs ASC")
    suspend fun getSnapshotsInRange(startTimeMs: Long, endTimeMs: Long): List<StorageSnapshotEntity>
}

@Dao
interface DuplicateGroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGroups(groups: List<DuplicateGroupEntity>)

    @Query("SELECT * FROM duplicate_groups ORDER BY recoverableSizeBytes DESC")
    fun getAllDuplicateGroupsFlow(): Flow<List<DuplicateGroupEntity>>

    @Query("DELETE FROM duplicate_groups")
    suspend fun deleteAll()
}

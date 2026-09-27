package com.storage.intelligence.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.storage.intelligence.core.database.entity.PerformanceSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PerformanceSnapshotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: PerformanceSnapshotEntity)

    @Query("SELECT * FROM performance_snapshots ORDER BY timestampMs DESC LIMIT :limit")
    fun getRecentSnapshotsFlow(limit: Int): Flow<List<PerformanceSnapshotEntity>>

    @Query("SELECT * FROM performance_snapshots WHERE timestampMs >= :sinceMs ORDER BY timestampMs ASC")
    suspend fun getSnapshotsSince(sinceMs: Long): List<PerformanceSnapshotEntity>

    @Query("SELECT * FROM performance_snapshots ORDER BY timestampMs ASC LIMIT 1")
    suspend fun getOldestSnapshot(): PerformanceSnapshotEntity?

    @Query("DELETE FROM performance_snapshots WHERE timestampMs < :beforeMs")
    suspend fun deleteSnapshotsBefore(beforeMs: Long)
}

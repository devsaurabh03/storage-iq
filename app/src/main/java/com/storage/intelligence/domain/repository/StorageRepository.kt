package com.storage.intelligence.domain.repository

import android.content.Context
import com.storage.intelligence.core.common.SearchFilter
import com.storage.intelligence.data.scanner.ScanProgress
import com.storage.intelligence.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.io.File

interface StorageRepository {
    /** Full scan: all MediaStore rows, directory traversal, stale-row reconciliation. */
    fun scanStorage(): Flow<ScanProgress>
    /** Delta scan: only rows modified after sinceTimestampSec; skips directory traversal & reconciliation. */
    fun scanStorageDelta(sinceTimestampSec: Long): Flow<ScanProgress>
    fun getStorageOverviewFlow(): Flow<StorageOverview>
    fun getFilesByCategoryFlow(category: StorageCategory): Flow<List<FileItem>>
    fun getLargeFilesFlow(minSizeBytes: Long): Flow<List<FileItem>>
    fun getScreenshotsFlow(): Flow<List<FileItem>>
    fun getScreenRecordingsFlow(): Flow<List<FileItem>>
    fun getOldDownloadsFlow(beforeTimestampMs: Long): Flow<List<FileItem>>
    fun getMessagingMediaFlow(sourceApp: String): Flow<List<FileItem>>
    fun searchFilesFlow(filter: SearchFilter): Flow<List<FileItem>>
    fun getOtherStorageFlow(): Flow<OtherStorageBreakdown>
    fun getMessagingReportFlow(): Flow<MessagingStorageReport>
    fun getReconciliationFlow(): Flow<StorageReconciliation>
    fun getStaleFilesFlow(daysUnopened: Int): Flow<List<FileItem>>
    fun getOptimizedFilesFlow(): Flow<List<FileItem>>
    suspend fun optimizeStaleFiles(context: Context, files: List<FileItem>): Long
    suspend fun decompressAndAccessFile(context: Context, fileItem: FileItem): File?
    suspend fun restoreOptimizedFile(context: Context, fileItem: FileItem): Boolean
    suspend fun deleteFiles(files: List<FileItem>): Boolean
    suspend fun getFilesByHash(hash: String): List<FileItem>
    suspend fun getFilesBySize(sizeBytes: Long): List<FileItem>
}

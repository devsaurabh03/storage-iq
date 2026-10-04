package com.storage.intelligence.data.scanner

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.storage.intelligence.core.database.dao.FileMetadataDao
import com.storage.intelligence.core.database.entity.FileMetadataEntity
import com.storage.intelligence.domain.model.StorageCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class ScanProgress(
    val scannedCount: Int,
    val totalCount: Int,
    val currentPhase: String,
    val isComplete: Boolean = false
)

@Singleton
class MediaStoreScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileMetadataDao: FileMetadataDao
) {
    /**
     * Incremental storage scan.
     *
     * @param sinceTimestampSec  If > 0, only files modified after this Unix-second timestamp
     *                           are fetched from MediaStore (delta scan). Pass 0 for a full scan.
     * @param reconcileDeleted   If true, stale DB rows whose files no longer exist are removed.
     *                           Set false for quick delta scans; true for periodic full reconciliation.
     */
    fun scanStorageIncremental(
        sinceTimestampSec: Long = 0L,
        reconcileDeleted: Boolean = true
    ): Flow<ScanProgress> = flow {
        val resolver = context.contentResolver
        val filesToSave = mutableListOf<FileMetadataEntity>()

        val isDelta = sinceTimestampSec > 0L
        val logTag = if (isDelta) "delta since ${sinceTimestampSec}s" else "full"
        com.storage.intelligence.core.logging.AppLogger.i("MediaStoreScanner", "Beginning $logTag storage scan...")
        emit(ScanProgress(0, 0, if (isDelta) "Checking for new files..." else "Querying MediaStore..."))

        try {
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Files.FileColumns.WIDTH,
                MediaStore.Files.FileColumns.HEIGHT,
                MediaStore.Files.FileColumns.MEDIA_TYPE
            )

            // Delta filter: only fetch files changed since last scan
            val selection = if (isDelta) {
                "${MediaStore.Files.FileColumns.SIZE} > 0 AND ${MediaStore.Files.FileColumns.DATE_MODIFIED} > ?"
            } else {
                "${MediaStore.Files.FileColumns.SIZE} > 0"
            }
            val selectionArgs = if (isDelta) arrayOf(sinceTimestampSec.toString()) else null

            val uri: Uri = MediaStore.Files.getContentUri("external")
            val cursor: Cursor? = resolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
            )

        val activeIds = HashSet<Long>()
        var total = 0
        var scanned = 0

        cursor?.use { c ->
            total = c.count
            val idCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val pathCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val widthCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
            val heightCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
            val mediaTypeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

            emit(ScanProgress(0, total, "Updating your storage insights…"))

            while (c.moveToNext()) {
                scanned++
                val id = c.getLong(idCol)
                activeIds.add(id)

                val path = c.getString(pathCol) ?: continue
                val name = c.getString(nameCol) ?: File(path).name
                val size = c.getLong(sizeCol)
                val mime = c.getString(mimeCol) ?: "application/octet-stream"
                val modifiedTimeSec = c.getLong(dateCol)
                val modifiedTimeMs = modifiedTimeSec * 1000L
                val width = c.getInt(widthCol)
                val height = c.getInt(heightCol)
                val mediaType = c.getInt(mediaTypeCol)

                // Check cache fingerprint
                val cached = fileMetadataDao.getByPath(path)
                if (cached != null && cached.sizeBytes == size && cached.modifiedTimeMs == modifiedTimeMs) {
                    // File is unchanged, keep existing analysis & hashes
                } else {
                    val category = classifyFile(path, mime, mediaType)
                    val isScreenshot = path.contains("Screenshot", ignoreCase = true) || name.contains("Screenshot", ignoreCase = true)
                    val isScreenRec = path.contains("Screenrecord", ignoreCase = true) || name.contains("Screenrecord", ignoreCase = true)
                    val isDownload = path.contains("Download", ignoreCase = true)
                    val sourceApp = detectSourceApp(path)

                    val entity = FileMetadataEntity(
                        id = id,
                        path = path,
                        name = name,
                        sizeBytes = size,
                        mimeType = mime,
                        modifiedTimeMs = modifiedTimeMs,
                        category = category.name,
                        width = width,
                        height = height,
                        isScreenshot = isScreenshot,
                        isScreenRecording = isScreenRec,
                        isDownload = isDownload,
                        sourceApp = sourceApp
                    )
                    filesToSave.add(entity)
                }

                if (filesToSave.size >= 150) {
                    fileMetadataDao.insertOrUpdateAll(filesToSave)
                    filesToSave.clear()
                }

                if (scanned % 150 == 0 || scanned == total) {
                    emit(ScanProgress(scanned, total, "Updating your storage insights…"))
                }
            }

            if (filesToSave.isNotEmpty()) {
                fileMetadataDao.insertOrUpdateAll(filesToSave)
                filesToSave.clear()
            }
        }

        // Direct directory scanning phase — only run on full scans to avoid overhead
        if (!isDelta) {
            scanDirectDirectories(filesToSave)
            if (filesToSave.isNotEmpty()) {
                fileMetadataDao.insertOrUpdateAll(filesToSave)
                filesToSave.clear()
            }
        }

        // MediaStore Delta Reconciliation: only remove stale rows on full scans
        if (reconcileDeleted) {
            val dbIds = fileMetadataDao.getAllIds()
            val deletedIds = dbIds.filter { !activeIds.contains(it) && !File(fileMetadataDao.getById(it)?.path ?: "").exists() }
            if (deletedIds.isNotEmpty()) {
                fileMetadataDao.deleteByIds(deletedIds)
                com.storage.intelligence.core.logging.AppLogger.i("MediaStoreScanner", "Removed ${deletedIds.size} stale DB records.")
            }
        }

        com.storage.intelligence.core.logging.AppLogger.i("MediaStoreScanner",
            "Scan complete ($logTag): $scanned media items processed.")
        emit(ScanProgress(total, total, "Insights up to date", isComplete = true))
    } catch (e: Exception) {
        com.storage.intelligence.core.logging.AppLogger.e("MediaStoreScanner", "Error during storage scan", e)
        emit(ScanProgress(0, 0, "Scan interrupted: ${e.localizedMessage}", isComplete = true))
    }
}

    private suspend fun scanDirectDirectories(filesToSave: MutableList<FileMetadataEntity>) {
        val root = android.os.Environment.getExternalStorageDirectory() ?: return
        val targetDirs = listOf(
            File(root, "Android/media/com.whatsapp/WhatsApp/Media"),
            File(root, "WhatsApp/Media"),
            File(root, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media"),
            File(root, "WhatsApp Business/Media"),
            File(root, "Telegram"),
            File(root, "Android/media/org.telegram.messenger"),
            File(root, "Download")
        )

        for (dir in targetDirs) {
            if (dir.exists() && dir.canRead()) {
                traverseDirectory(dir, filesToSave)
            }
        }
    }

    private suspend fun traverseDirectory(dir: File, filesToSave: MutableList<FileMetadataEntity>) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                if (!file.name.startsWith(".")) {
                    traverseDirectory(file, filesToSave)
                }
            } else if (file.isFile && file.length() > 0) {
                val path = file.absolutePath
                val cached = fileMetadataDao.getByPath(path)
                if (cached == null) {
                    val name = file.name
                    val size = file.length()
                    val mime = getMimeType(path)
                    val category = classifyFile(path, mime, 0)
                    val isScreenshot = path.contains("Screenshot", ignoreCase = true) || name.contains("Screenshot", ignoreCase = true)
                    val isScreenRec = path.contains("Screenrecord", ignoreCase = true) || name.contains("Screenrecord", ignoreCase = true)
                    val isDownload = path.contains("Download", ignoreCase = true)
                    val sourceApp = detectSourceApp(path)

                    val entity = FileMetadataEntity(
                        id = path.hashCode().toLong(),
                        path = path,
                        name = name,
                        sizeBytes = size,
                        mimeType = mime,
                        modifiedTimeMs = file.lastModified(),
                        category = category.name,
                        width = 0,
                        height = 0,
                        isScreenshot = isScreenshot,
                        isScreenRecording = isScreenRec,
                        isDownload = isDownload,
                        sourceApp = sourceApp
                    )
                    filesToSave.add(entity)

                    if (filesToSave.size >= 250) {
                        fileMetadataDao.insertOrUpdateAll(filesToSave)
                        filesToSave.clear()
                    }
                }
            }
        }
    }

    private fun getMimeType(path: String): String {
        val extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(path)
        if (extension != null) {
            val type = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
            if (type != null) return type
        }
        val lower = path.lowercase()
        return when {
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".webp") -> "image/webp"
            lower.endsWith(".mp4") || lower.endsWith(".mkdir") || lower.endsWith(".3gp") -> "video/mp4"
            lower.endsWith(".opus") || lower.endsWith(".m4a") || lower.endsWith(".mp3") || lower.endsWith(".aac") -> "audio/mpeg"
            lower.endsWith(".pdf") -> "application/pdf"
            lower.endsWith(".apk") -> "application/vnd.android.package-archive"
            else -> "application/octet-stream"
        }
    }

    private fun classifyFile(path: String, mime: String, mediaType: Int): StorageCategory {
        return when {
            path.contains("WhatsApp", ignoreCase = true) || path.contains("com.whatsapp", ignoreCase = true) -> StorageCategory.WHATSAPP
            path.contains("Telegram", ignoreCase = true) || path.contains("org.telegram", ignoreCase = true) || path.contains("challegram", ignoreCase = true) -> StorageCategory.TELEGRAM
            path.contains("Screenshot", ignoreCase = true) -> StorageCategory.SCREENSHOTS
            path.contains("Screenrecord", ignoreCase = true) -> StorageCategory.SCREEN_RECORDINGS
            path.contains("Download", ignoreCase = true) -> StorageCategory.DOWNLOADS
            mime.startsWith("video/") || mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> StorageCategory.VIDEOS
            mime.startsWith("image/") || mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> StorageCategory.PHOTOS
            mime.startsWith("audio/") || mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO -> StorageCategory.AUDIO
            isDocument(path, mime) -> StorageCategory.DOCUMENTS
            else -> StorageCategory.OTHER
        }
    }

    private fun isDocument(path: String, mime: String): Boolean {
        if (mime == "application/pdf" || mime.contains("document") || mime.contains("msword") ||
            mime.contains("officedocument") || mime.startsWith("text/")
        ) return true
        val lower = path.lowercase()
        return lower.endsWith(".pdf") || lower.endsWith(".docx") || lower.endsWith(".doc") ||
                lower.endsWith(".xlsx") || lower.endsWith(".xls") || lower.endsWith(".pptx") ||
                lower.endsWith(".txt") || lower.endsWith(".epub")
    }

    private fun detectSourceApp(path: String): String? {
        return when {
            path.contains("WhatsApp", ignoreCase = true) || path.contains("com.whatsapp", ignoreCase = true) -> "WhatsApp"
            path.contains("Telegram", ignoreCase = true) || path.contains("org.telegram", ignoreCase = true) || path.contains("challegram", ignoreCase = true) -> "Telegram"
            else -> null
        }
    }
}

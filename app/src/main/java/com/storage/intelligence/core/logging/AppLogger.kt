package com.storage.intelligence.core.logging

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Centralized, high-performance file logger.
 * Writes diagnostic and operational logs to dedicated storage directories on the mobile device:
 * 1. Public Downloads: /storage/emulated/0/Download/StorageIQ_Logs/
 * 2. Public Documents: /storage/emulated/0/Documents/StorageIQ_Logs/
 * 3. App-specific external files: /Android/data/com.storage.intelligence/files/logs/
 */
object AppLogger {

    private const val MAX_LOG_FILE_SIZE_BYTES = 2 * 1024 * 1024L // 2 MB
    private const val MAX_BACKUP_FILES = 3

    private val logDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val logScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val logChannel = Channel<String>(capacity = 1000)

    private var appContext: Context? = null
    private var isInitialized = false

    fun init(context: Context) {
        appContext = context.applicationContext
        if (isInitialized) return
        isInitialized = true

        startLogWriter()

        // Write an immediate synchronous heartbeat entry so the file is created right away
        val startupMsg = "[${logDateFormat.format(Date())}] [INFO] [AppLogger] StorageIQ logging started. Primary target: ${getLogsDirectoryPath()}"
        writeToFile(startupMsg)

        i("AppLogger", "File logger initialized. Folders: ${getAllLogDirectories().joinToString { it.absolutePath }}")
    }

    private fun getAllLogDirectories(): List<File> {
        val context = appContext
        val dirs = mutableListOf<File>()

        // 1. Public Downloads directory (most accessible to users in Files app)
        try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val publicDownloads = File(downloadDir, "StorageIQ_Logs")
            if (!publicDownloads.exists()) publicDownloads.mkdirs()
            if (publicDownloads.exists() && publicDownloads.canWrite()) {
                dirs.add(publicDownloads)
            }
        } catch (_: Exception) {}

        // 2. Public Documents directory
        try {
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val publicDocs = File(docsDir, "StorageIQ_Logs")
            if (!publicDocs.exists()) publicDocs.mkdirs()
            if (publicDocs.exists() && publicDocs.canWrite()) {
                dirs.add(publicDocs)
            }
        } catch (_: Exception) {}

        // 3. Root external storage directory (if All Files Access granted)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                val rootStorage = File(Environment.getExternalStorageDirectory(), "StorageIQ_Logs")
                if (!rootStorage.exists()) rootStorage.mkdirs()
                if (rootStorage.exists() && rootStorage.canWrite()) {
                    dirs.add(rootStorage)
                }
            }
        } catch (_: Exception) {}

        // 4. App external files directory (always writable without permissions)
        try {
            if (context != null) {
                val appExternal = context.getExternalFilesDir("logs") ?: File(context.filesDir, "logs")
                if (!appExternal.exists()) appExternal.mkdirs()
                dirs.add(appExternal)
            }
        } catch (_: Exception) {}

        return dirs.distinctBy { it.absolutePath }
    }

    private fun startLogWriter() {
        logScope.launch {
            for (entry in logChannel) {
                writeToFile(entry)
            }
        }
    }

    private fun writeToFile(entry: String) {
        val targets = getAllLogDirectories()
        val todayStr = fileDateFormat.format(Date())

        for (dir in targets) {
            try {
                if (!dir.exists()) dir.mkdirs()
                val logFile = File(dir, "storageiq_$todayStr.log")

                // Check rotation
                if (logFile.exists() && logFile.length() > MAX_LOG_FILE_SIZE_BYTES) {
                    rotateLogFiles(dir, todayStr)
                }

                FileWriter(logFile, true).use { writer ->
                    writer.appendLine(entry)
                }

                // Notify Android MediaScanner so files appear immediately in device file managers
                appContext?.let { ctx ->
                    try {
                        MediaScannerConnection.scanFile(
                            ctx,
                            arrayOf(logFile.absolutePath),
                            arrayOf("text/plain"),
                            null
                        )
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {
                // Prevent crash during write error
            }
        }
    }

    private fun rotateLogFiles(dir: File, dateStr: String) {
        try {
            for (i in MAX_BACKUP_FILES downTo 1) {
                val src = if (i == 1) File(dir, "storageiq_$dateStr.log") else File(dir, "storageiq_${dateStr}_${i - 1}.log")
                val dest = File(dir, "storageiq_${dateStr}_$i.log")
                if (src.exists()) {
                    if (dest.exists()) dest.delete()
                    src.renameTo(dest)
                }
            }
        } catch (_: Exception) {}
    }

    fun d(tag: String, message: String) {
        try { Log.d(tag, message) } catch (_: Throwable) { println("[$tag] $message") }
        enqueue("DEBUG", tag, message)
    }

    fun i(tag: String, message: String) {
        try { Log.i(tag, message) } catch (_: Throwable) { println("[$tag] $message") }
        enqueue("INFO", tag, message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        try { Log.w(tag, message, throwable) } catch (_: Throwable) { println("[$tag] $message") }
        val msg = if (throwable != null) "$message\n${getStackTraceString(throwable)}" else message
        enqueue("WARN", tag, msg)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        try { Log.e(tag, message, throwable) } catch (_: Throwable) { System.err.println("[$tag] $message") }
        val msg = if (throwable != null) "$message\n${getStackTraceString(throwable)}" else message
        enqueue("ERROR", tag, msg)
    }

    private fun enqueue(level: String, tag: String, message: String) {
        val timestamp = logDateFormat.format(Date())
        val formatted = "[$timestamp] [$level] [$tag] $message"
        logChannel.trySend(formatted)
    }

    private fun getStackTraceString(t: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        t.printStackTrace(pw)
        return sw.toString()
    }

    fun getLogsDirectoryPath(): String {
        val publicDir = getAllLogDirectories().firstOrNull {
            it.absolutePath.contains("Download") || it.absolutePath.contains("Documents")
        }
        return publicDir?.absolutePath ?: getAllLogDirectories().firstOrNull()?.absolutePath ?: "Download/StorageIQ_Logs"
    }

    fun getLatestLogs(maxLines: Int = 250): String {
        val dirs = getAllLogDirectories()
        if (dirs.isEmpty()) return "Logs initializing..."

        val todayStr = fileDateFormat.format(Date())
        for (dir in dirs) {
            val logFile = File(dir, "storageiq_$todayStr.log")
            if (logFile.exists() && logFile.length() > 0) {
                return try {
                    val lines = logFile.readLines()
                    lines.takeLast(maxLines).joinToString("\n")
                } catch (e: Exception) {
                    "Error reading ${logFile.name}: ${e.message}"
                }
            }
        }

        // Check any existing .log files in dirs
        for (dir in dirs) {
            val anyLog = dir.listFiles { _, name -> name.endsWith(".log") }?.maxByOrNull { it.lastModified() }
            if (anyLog != null && anyLog.length() > 0) {
                return try {
                    anyLog.readLines().takeLast(maxLines).joinToString("\n")
                } catch (e: Exception) {
                    "Error reading ${anyLog.name}: ${e.message}"
                }
            }
        }

        return "No log records found yet. Perform a scan or optimization to generate diagnostic entries."
    }

    /**
     * Explicitly exports current logs into Downloads/StorageIQ_Logs via MediaStore (Scoped Storage compliant).
     * Guaranteed to appear in the user's Downloads list.
     */
    fun exportLogsToPublicStorage(context: Context): String {
        val logContent = getLatestLogs(1000)
        val fileName = "StorageIQ_Logs_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.txt"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/StorageIQ_Logs")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(logContent.toByteArray())
                    }
                    return "Download/StorageIQ_Logs/$fileName"
                }
            } catch (_: Exception) {}
        }

        // Direct file fallback
        val downloadFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "StorageIQ_Logs").apply {
            if (!exists()) mkdirs()
        }
        val targetFile = File(downloadFolder, fileName)
        targetFile.writeText(logContent)

        MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), arrayOf("text/plain"), null)
        return targetFile.absolutePath
    }

    /**
     * Launches Android system share dialog with log file / text.
     */
    fun shareLogs(context: Context) {
        try {
            val content = getLatestLogs(500)
            val cacheFile = File(context.cacheDir, "storageiq_diagnostic_log.txt")
            cacheFile.writeText(content)

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "StorageIQ Diagnostic Logs")
                putExtra(Intent.EXTRA_TEXT, content)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share StorageIQ Logs").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Log.e("AppLogger", "Failed to share logs", e)
        }
    }

    fun clearLogs() {
        for (dir in getAllLogDirectories()) {
            try {
                dir.listFiles { _, name -> name.endsWith(".log") || name.endsWith(".txt") }?.forEach { it.delete() }
            } catch (_: Exception) {}
        }
        i("AppLogger", "Log files cleared by user")
    }
}

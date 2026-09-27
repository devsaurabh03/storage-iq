package com.storage.intelligence.core.logging

import android.content.Context
import android.os.Environment
import android.util.Log
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
 * 1. App-specific external files: /Android/data/com.storage.intelligence/files/logs/
 * 2. Public Documents folder: /storage/emulated/0/Documents/StorageIQ_Logs/ (if permitted)
 */
object AppLogger {

    private const val MAX_LOG_FILE_SIZE_BYTES = 2 * 1024 * 1024L // 2 MB
    private const val MAX_BACKUP_FILES = 3

    private val logDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val logScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val logChannel = Channel<String>(capacity = 1000)

    private var appLogsDir: File? = null
    private var publicLogsDir: File? = null
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        try {
            val appExternal = context.getExternalFilesDir("logs") ?: File(context.filesDir, "logs")
            if (!appExternal.exists()) appExternal.mkdirs()
            appLogsDir = appExternal

            // Try creating public documents logs directory for easy user access via file managers
            try {
                val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val publicDir = File(docsDir, "StorageIQ_Logs")
                if (!publicDir.exists()) publicDir.mkdirs()
                if (publicDir.exists() && publicDir.canWrite()) {
                    publicLogsDir = publicDir
                }
            } catch (_: Exception) {
                // Scoped storage fallback
            }

            isInitialized = true
            startLogWriter()

            i("AppLogger", "File logger initialized. Primary logs folder: ${appLogsDir?.absolutePath}")
            if (publicLogsDir != null) {
                i("AppLogger", "Public logs mirror folder: ${publicLogsDir?.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e("AppLogger", "Failed to initialize AppLogger", e)
        }
    }

    private fun startLogWriter() {
        logScope.launch {
            for (entry in logChannel) {
                writeToFile(entry)
            }
        }
    }

    private fun writeToFile(entry: String) {
        val targets = listOfNotNull(appLogsDir, publicLogsDir)
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
        return publicLogsDir?.absolutePath ?: appLogsDir?.absolutePath ?: "Logs folder initializing..."
    }

    fun getAppLogsDirectoryPath(): String {
        return appLogsDir?.absolutePath ?: ""
    }

    fun getPublicLogsDirectoryPath(): String? {
        return publicLogsDir?.absolutePath
    }

    fun getLatestLogs(maxLines: Int = 200): String {
        val dir = appLogsDir ?: publicLogsDir ?: return "No logs recorded yet."
        return try {
            val todayStr = fileDateFormat.format(Date())
            val logFile = File(dir, "storageiq_$todayStr.log")
            if (!logFile.exists()) {
                val allLogFiles = dir.listFiles { _, name -> name.endsWith(".log") }?.sortedByDescending { it.lastModified() }
                if (allLogFiles.isNullOrEmpty()) return "No log files found in directory: ${dir.absolutePath}"
                val lines = allLogFiles.first().readLines()
                lines.takeLast(maxLines).joinToString("\n")
            } else {
                val lines = logFile.readLines()
                lines.takeLast(maxLines).joinToString("\n")
            }
        } catch (e: Exception) {
            "Error reading log file: ${e.message}"
        }
    }

    fun clearLogs() {
        val targets = listOfNotNull(appLogsDir, publicLogsDir)
        for (dir in targets) {
            try {
                dir.listFiles { _, name -> name.endsWith(".log") }?.forEach { it.delete() }
            } catch (_: Exception) {}
        }
        i("AppLogger", "Log files cleared by user")
    }
}

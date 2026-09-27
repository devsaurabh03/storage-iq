package com.storage.intelligence.core.ui.preview

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.storage.intelligence.domain.model.FileItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

enum class PreviewType {
    VIDEO,
    IMAGE,
    AUDIO,
    DOCUMENT,
    UNKNOWN
}

data class PreviewState(
    val isVisible: Boolean = false,
    val fileItem: FileItem? = null,
    val previewType: PreviewType = PreviewType.UNKNOWN,
    val onActionKeep: (() -> Unit)? = null,
    val onActionDelete: (() -> Unit)? = null
)

@Singleton
class FilePreviewController @Inject constructor() {

    private val _previewState = MutableStateFlow(PreviewState())
    val previewState: StateFlow<PreviewState> = _previewState.asStateFlow()

    fun previewFile(
        fileItem: FileItem,
        onActionKeep: (() -> Unit)? = null,
        onActionDelete: (() -> Unit)? = null
    ) {
        val type = detectPreviewType(fileItem)
        _previewState.value = PreviewState(
            isVisible = true,
            fileItem = fileItem,
            previewType = type,
            onActionKeep = onActionKeep,
            onActionDelete = onActionDelete
        )
    }

    fun dismiss() {
        _previewState.value = PreviewState(isVisible = false)
    }

    fun openWithExternalApp(context: Context, fileItem: FileItem) {
        try {
            val file = File(fileItem.path)
            val uri: Uri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (_: Exception) {
                Uri.fromFile(file)
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, fileItem.mimeType.ifEmpty { "*/*" })
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {
            try {
                val file = File(fileItem.path)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.fromFile(file), "*/*")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(intent, "Open with").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (_: Exception) {}
        }
    }

    private fun detectPreviewType(fileItem: FileItem): PreviewType {
        val mime = fileItem.mimeType.lowercase()
        val name = fileItem.name.lowercase()
        return when {
            mime.startsWith("video/") || name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".webm") || name.endsWith(".avi") -> PreviewType.VIDEO
            mime.startsWith("image/") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp") || name.endsWith(".heic") -> PreviewType.IMAGE
            mime.startsWith("audio/") || name.endsWith(".mp3") || name.endsWith(".m4a") || name.endsWith(".aac") || name.endsWith(".opus") || name.endsWith(".wav") -> PreviewType.AUDIO
            mime.contains("pdf") || mime.contains("document") || mime.contains("sheet") || mime.contains("text") ||
                    name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx") || name.endsWith(".txt") || name.endsWith(".xlsx") -> PreviewType.DOCUMENT
            else -> PreviewType.UNKNOWN
        }
    }
}

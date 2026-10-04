package com.storage.intelligence.core.common

/**
 * Centralized, reusable detector and classifier for WhatsApp & WhatsApp Business media.
 *
 * Utilizes multiple signals:
 * - MediaStore RELATIVE_PATH (API 29+)
 * - Standard and scoped storage path conventions
 * - Organizational subfolder naming (Sent vs Received, Images, Video, Documents, Voice Notes)
 *
 * Never claims to detect message-level "Forwarded" status; reliably identifies
 * physical storage collections (Sent vs Received).
 */
object WhatsAppMediaDetector {

    const val PACKAGE_WHATSAPP = "com.whatsapp"
    const val PACKAGE_WHATSAPP_BUSINESS = "com.whatsapp.w4b"

    private val WHATSAPP_PATH_PATTERNS = listOf(
        "Android/media/com.whatsapp/WhatsApp/Media",
        "WhatsApp/Media",
        "Android/media/com.whatsapp.w4b/WhatsApp Business/Media",
        "WhatsApp Business/Media"
    )

    enum class WhatsAppFolderType {
        IMAGES,
        VIDEOS,
        ANIMATED_GIFS,
        DOCUMENTS,
        AUDIO,
        VOICE_NOTES,
        STICKERS,
        WALLPAPERS,
        OTHER
    }

    /**
     * Determines whether a file belongs to standard WhatsApp or WhatsApp Business.
     */
    fun isWhatsAppMedia(path: String, relativePath: String? = null): Boolean {
        val checkPath = relativePath ?: path
        val lower = checkPath.lowercase()
        return lower.contains("whatsapp") || lower.contains(PACKAGE_WHATSAPP)
    }

    /**
     * Determines whether a file belongs specifically to WhatsApp Business.
     */
    fun isWhatsAppBusinessMedia(path: String, relativePath: String? = null): Boolean {
        val checkPath = relativePath ?: path
        val lower = checkPath.lowercase()
        return lower.contains(PACKAGE_WHATSAPP_BUSINESS) || lower.contains("whatsapp business")
    }

    /**
     * Checks if the media is located in WhatsApp's internal 'Sent' folder.
     * Media in this folder was transmitted by the user.
     */
    fun isSentMedia(path: String, relativePath: String? = null): Boolean {
        val checkPath = relativePath ?: path
        val normalized = checkPath.replace('\\', '/')
        return normalized.contains("/Sent/", ignoreCase = true) ||
                normalized.endsWith("/Sent", ignoreCase = true)
    }

    /**
     * Checks if the media is received / inbound (in public or regular WhatsApp media folders,
     * excluding Sent and Private folders).
     */
    fun isReceivedMedia(path: String, relativePath: String? = null): Boolean {
        if (!isWhatsAppMedia(path, relativePath)) return false
        val checkPath = relativePath ?: path
        val normalized = checkPath.replace('\\', '/')
        val isSent = normalized.contains("/Sent/", ignoreCase = true)
        val isPrivate = normalized.contains("/Private/", ignoreCase = true)
        return !isSent && !isPrivate
    }

    /**
     * Classifies the specific WhatsApp media type based on directory convention.
     */
    fun getWhatsAppFolderType(path: String): WhatsAppFolderType {
        val lower = path.lowercase().replace('\\', '/')
        return when {
            lower.contains("whatsapp animated gifs") -> WhatsAppFolderType.ANIMATED_GIFS
            lower.contains("whatsapp images") -> WhatsAppFolderType.IMAGES
            lower.contains("whatsapp video") -> WhatsAppFolderType.VIDEOS
            lower.contains("whatsapp documents") || lower.contains("whatsapp document") -> WhatsAppFolderType.DOCUMENTS
            lower.contains("whatsapp voice notes") -> WhatsAppFolderType.VOICE_NOTES
            lower.contains("whatsapp audio") -> WhatsAppFolderType.AUDIO
            lower.contains("whatsapp stickers") -> WhatsAppFolderType.STICKERS
            lower.contains("whatsapp wallpapers") -> WhatsAppFolderType.WALLPAPERS
            else -> WhatsAppFolderType.OTHER
        }
    }
}

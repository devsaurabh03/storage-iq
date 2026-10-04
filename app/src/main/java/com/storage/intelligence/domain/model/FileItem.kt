package com.storage.intelligence.domain.model

data class FileItem(
    val id: Long,
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val modifiedTimeMs: Long,
    val category: StorageCategory,
    val sha256Hash: String? = null,
    val perceptualHash: Long? = null,
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0,
    val isScreenshot: Boolean = false,
    val isScreenRecording: Boolean = false,
    val isDownload: Boolean = false,
    val sourceApp: String? = null, // e.g. WhatsApp, Telegram
    val isSelectedForDeletion: Boolean = false,
    val isDuplicate: Boolean = false,
    val duplicateCopyCount: Int = 0,
    val isOptimized: Boolean = false,
    val originalSizeBytes: Long = 0L,
    val compressedFilePath: String? = null,
    val compressionAlgorithm: String? = null
) {
    val isForwarded: Boolean
        get() = path.contains("/Sent/", ignoreCase = true)

    val isForwardedManyTimes: Boolean
        get() = duplicateCopyCount >= 3

    val isGreetingOrGoodMorning: Boolean
        get() {
            val isMedia = category == StorageCategory.PHOTOS || mimeType.startsWith("image/") ||
                    category == StorageCategory.VIDEOS || mimeType.startsWith("video/")
            if (!isMedia) return false

            val lowerName = name.lowercase()
            val lowerPath = path.lowercase()

            // 1. Multilingual lexical analysis (Hindi, Tamil, Telugu, Marathi, Gujarati, Bengali, etc.)
            if (com.storage.intelligence.core.common.GreetingLexicon.matchesGreetingLexicon(name, path)) {
                return true
            }

            // 2. Must be from messaging apps (WhatsApp or Telegram)
            val isMessagingApp = sourceApp.equals("WhatsApp", ignoreCase = true) ||
                    sourceApp.equals("Telegram", ignoreCase = true) ||
                    lowerPath.contains("whatsapp") || lowerPath.contains("telegram")

            if (!isMessagingApp) return false

            // 3. Temporal pattern: Received during morning peak greeting hours (5:00 AM - 10:30 AM local time)
            val calendar = java.util.Calendar.getInstance().apply { timeInMillis = modifiedTimeMs }
            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            val minute = calendar.get(java.util.Calendar.MINUTE)
            val isMorningRush = (hour in 5..9) || (hour == 10 && minute <= 30)

            // 4. Typical greeting card image size: 25 KB to 1.25 MB
            val isGreetingCardSize = sizeBytes in 25_000L..1_250_000L

            // 5. Short video clip (<= 35 seconds) if video
            val isShortClip = (category == StorageCategory.VIDEOS || mimeType.startsWith("video/")) &&
                    (durationMs in 1..35_000L)

            if (isMorningRush && (isGreetingCardSize || isShortClip)) {
                // If forwarded or shared across multiple chats, very high confidence greeting
                if (isDuplicate || isForwarded || duplicateCopyCount >= 2) {
                    return true
                }

                // If image has postcard / square / portrait card aspect ratio
                if (width > 0 && height > 0) {
                    val ratio = width.toFloat() / height.toFloat()
                    val isSquare = ratio in 0.90f..1.10f
                    val isPoster = ratio in 0.52f..0.85f // 9:16 or 3:4 portrait card
                    if (isSquare || isPoster) {
                        return true
                    }
                } else {
                    return true
                }
            }

            return false
        }
}

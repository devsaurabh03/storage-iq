package com.storage.intelligence.domain.model

import androidx.compose.ui.graphics.Color
import com.storage.intelligence.core.ui.theme.*

enum class StorageCategory(val displayName: String, val color: Color) {
    VIDEOS("Videos", CategoryVideoColor),
    PHOTOS("Photos", CategoryPhotoColor),
    APPS("Apps", CategoryAppColor),
    WHATSAPP("WhatsApp", CategoryWhatsAppColor),
    TELEGRAM("Telegram", CategoryTelegramColor),
    DOWNLOADS("Downloads", CategoryDownloadColor),
    DOCUMENTS("Documents", CategoryDocumentColor),
    AUDIO("Audio", CategoryAudioColor),
    SCREENSHOTS("Screenshots", CategoryScreenshotColor),
    SCREEN_RECORDINGS("Screen Recordings", CategoryVideoColor),
    OTHER("Other", CategoryOtherColor)
}

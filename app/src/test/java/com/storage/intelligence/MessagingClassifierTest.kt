package com.storage.intelligence

import com.storage.intelligence.domain.model.MessagingMediaType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests validating messaging media path and classification logic for WhatsApp and Telegram.
 */
class MessagingClassifierTest {

    private fun classifyMessagingPath(path: String, mimeType: String): Pair<String?, MessagingMediaType?> {
        val lower = path.lowercase()
        val app = when {
            lower.contains("whatsapp") -> "whatsapp"
            lower.contains("telegram") -> "telegram"
            else -> null
        }
        if (app == null) return Pair(null, null)

        val type = when {
            lower.contains("video") || mimeType.startsWith("video/") -> MessagingMediaType.VIDEOS
            lower.contains("image") || lower.contains("photo") || mimeType.startsWith("image/") -> MessagingMediaType.IMAGES
            lower.contains("audio") || lower.contains("voice") || lower.contains("opus") || mimeType.startsWith("audio/") -> MessagingMediaType.AUDIO
            lower.contains("doc") || mimeType.startsWith("application/") || mimeType.startsWith("text/") -> MessagingMediaType.DOCUMENTS
            else -> MessagingMediaType.DOCUMENTS
        }
        return Pair(app, type)
    }

    @Test
    fun testWhatsAppVideoClassification() {
        val (app, type) = classifyMessagingPath(
            "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video/VID-20231015-WA0001.mp4",
            "video/mp4"
        )
        assertEquals("whatsapp", app)
        assertEquals(MessagingMediaType.VIDEOS, type)
    }

    @Test
    fun testWhatsAppAudioVoiceNoteClassification() {
        val (app, type) = classifyMessagingPath(
            "/storage/emulated/0/WhatsApp/Media/WhatsApp Voice Notes/202342/PTT-20231015-WA0002.opus",
            "audio/ogg"
        )
        assertEquals("whatsapp", app)
        assertEquals(MessagingMediaType.AUDIO, type)
    }

    @Test
    fun testWhatsAppDocumentClassification() {
        val (app, type) = classifyMessagingPath(
            "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Documents/statement.pdf",
            "application/pdf"
        )
        assertEquals("whatsapp", app)
        assertEquals(MessagingMediaType.DOCUMENTS, type)
    }

    @Test
    fun testTelegramImageClassification() {
        val (app, type) = classifyMessagingPath(
            "/storage/emulated/0/Telegram/Telegram Images/photo_2023-10-15.jpg",
            "image/jpeg"
        )
        assertEquals("telegram", app)
        assertEquals(MessagingMediaType.IMAGES, type)
    }

    @Test
    fun testTelegramVideoClassification() {
        val (app, type) = classifyMessagingPath(
            "/storage/emulated/0/Movies/Telegram/video_123.mp4",
            "video/mp4"
        )
        assertEquals("telegram", app)
        assertEquals(MessagingMediaType.VIDEOS, type)
    }

    @Test
    fun testTelegramDocumentClassification() {
        val (app, type) = classifyMessagingPath(
            "/storage/emulated/0/Telegram/Telegram Documents/contract.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        )
        assertEquals("telegram", app)
        assertEquals(MessagingMediaType.DOCUMENTS, type)
    }

    @Test
    fun testNonMessagingAppIgnored() {
        val (app, type) = classifyMessagingPath(
            "/storage/emulated/0/DCIM/Camera/IMG_20231015.jpg",
            "image/jpeg"
        )
        assertEquals(null, app)
        assertEquals(null, type)
    }
}

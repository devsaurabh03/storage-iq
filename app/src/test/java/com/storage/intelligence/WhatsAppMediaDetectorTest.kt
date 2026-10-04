package com.storage.intelligence

import com.storage.intelligence.core.common.WhatsAppMediaDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppMediaDetectorTest {

    @Test
    fun testWhatsAppAndBusinessDetection() {
        val standardPath = "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/IMG-20231015-WA0001.jpg"
        val businessPath = "/storage/emulated/0/Android/media/com.whatsapp.w4b/WhatsApp Business/Media/WhatsApp Business Images/IMG-20231015-WA0002.jpg"
        val telegramPath = "/storage/emulated/0/Telegram/Telegram Images/photo.jpg"

        assertTrue(WhatsAppMediaDetector.isWhatsAppMedia(standardPath))
        assertTrue(WhatsAppMediaDetector.isWhatsAppMedia(businessPath))
        assertFalse(WhatsAppMediaDetector.isWhatsAppMedia(telegramPath))

        assertFalse(WhatsAppMediaDetector.isWhatsAppBusinessMedia(standardPath))
        assertTrue(WhatsAppMediaDetector.isWhatsAppBusinessMedia(businessPath))
    }

    @Test
    fun testSentVsReceivedClassification() {
        val sentPath = "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/Sent/IMG-20231015-WA0010.jpg"
        val receivedPath = "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/IMG-20231015-WA0011.jpg"
        val privatePath = "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/Private/IMG-20231015-WA0012.jpg"

        assertTrue(WhatsAppMediaDetector.isSentMedia(sentPath))
        assertFalse(WhatsAppMediaDetector.isSentMedia(receivedPath))

        assertFalse(WhatsAppMediaDetector.isReceivedMedia(sentPath))
        assertTrue(WhatsAppMediaDetector.isReceivedMedia(receivedPath))
        assertFalse(WhatsAppMediaDetector.isReceivedMedia(privatePath))
    }

    @Test
    fun testFolderTypeClassification() {
        assertEquals(
            WhatsAppMediaDetector.WhatsAppFolderType.IMAGES,
            WhatsAppMediaDetector.getWhatsAppFolderType("/storage/WhatsApp/Media/WhatsApp Images/test.jpg")
        )
        assertEquals(
            WhatsAppMediaDetector.WhatsAppFolderType.VIDEOS,
            WhatsAppMediaDetector.getWhatsAppFolderType("/storage/WhatsApp/Media/WhatsApp Video/test.mp4")
        )
        assertEquals(
            WhatsAppMediaDetector.WhatsAppFolderType.ANIMATED_GIFS,
            WhatsAppMediaDetector.getWhatsAppFolderType("/storage/WhatsApp/Media/WhatsApp Animated Gifs/test.mp4")
        )
        assertEquals(
            WhatsAppMediaDetector.WhatsAppFolderType.DOCUMENTS,
            WhatsAppMediaDetector.getWhatsAppFolderType("/storage/WhatsApp/Media/WhatsApp Documents/doc.pdf")
        )
        assertEquals(
            WhatsAppMediaDetector.WhatsAppFolderType.VOICE_NOTES,
            WhatsAppMediaDetector.getWhatsAppFolderType("/storage/WhatsApp/Media/WhatsApp Voice Notes/note.opus")
        )
    }
}

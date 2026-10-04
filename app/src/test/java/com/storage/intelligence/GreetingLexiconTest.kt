package com.storage.intelligence

import com.storage.intelligence.core.common.GreetingLexicon
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GreetingLexiconTest {

    @Test
    fun testEnglishGreetings() {
        assertTrue(GreetingLexicon.matchesGreetingLexicon("Good_Morning_Sun.jpg", "/storage/WhatsApp/Media/WhatsApp Images"))
        assertTrue(GreetingLexicon.matchesGreetingLexicon("IMG-20231015-WA0012-gm.jpg", "/storage/WhatsApp Images"))
        assertTrue(GreetingLexicon.matchesGreetingLexicon("daily_quote_wishes.png", "/storage/WhatsApp Images"))
        assertTrue(GreetingLexicon.matchesGreetingLexicon("Sweet_Dreams_GN.jpeg", "/storage/WhatsApp Images"))
    }

    @Test
    fun testHindiAndSanskritGreetings() {
        // Devanagari
        assertTrue(GreetingLexicon.matchesGreetingLexicon("सुप्रभात_संदेश.jpg", "/storage/WhatsApp Images"))
        assertTrue(GreetingLexicon.matchesGreetingLexicon("शुभ_प्रभात.jpeg", "/storage/WhatsApp Images"))
        assertTrue(GreetingLexicon.matchesGreetingLexicon("राधे_राधे.png", "/storage/WhatsApp Images"))
        assertTrue(GreetingLexicon.matchesGreetingLexicon("जय_श्री_कृष्ण.jpg", "/storage/WhatsApp Images"))
        // Romanized
        assertTrue(GreetingLexicon.matchesGreetingLexicon("suprabhat_wishes.jpg", "/storage/WhatsApp Images"))
        assertTrue(GreetingLexicon.matchesGreetingLexicon("radhe_radhe_darshan.jpeg", "/storage/WhatsApp Images"))
        assertTrue(GreetingLexicon.matchesGreetingLexicon("har_har_mahadev.jpg", "/storage/WhatsApp Images"))
    }

    @Test
    fun testMarathiGreetings() {
        assertTrue(GreetingLexicon.matchesGreetingLexicon("शुभ_सकाळ_मित्रानो.jpg", "/storage/WhatsApp Images"))
        assertTrue(GreetingLexicon.matchesGreetingLexicon("shubh_sakal.jpeg", "/storage/WhatsApp Images"))
        assertTrue(GreetingLexicon.matchesGreetingLexicon("ram_krishna_hari.jpg", "/storage/WhatsApp Images"))
    }

    @Test
    fun testGujaratiGreetings() {
        assertTrue(GreetingLexicon.matchesGreetingLexicon("શુભ_સવાર.jpg", "/storage/WhatsApp Images"))
        assertTrue(GreetingLexicon.matchesGreetingLexicon("shubh_savar.jpeg", "/storage/WhatsApp Images"))
        assertTrue(GreetingLexicon.matchesGreetingLexicon("jai_swaminarayan.jpg", "/storage/WhatsApp Images"))
    }

    @Test
    fun testTamilGreetings() {
        assertTrue(GreetingLexicon.matchesGreetingLexicon("காலை_வணக்கம்.jpg", "/storage/WhatsApp Images"))
        assertTrue(GreetingLexicon.matchesGreetingLexicon("kaalai_vanakkam.png", "/storage/WhatsApp Images"))
        assertTrue(GreetingLexicon.matchesGreetingLexicon("kalaivanakkam_wishes.jpg", "/storage/WhatsApp Images"))
    }

    @Test
    fun testTeluguGreetings() {
        assertTrue(GreetingLexicon.matchesGreetingLexicon("శుభోదయం_మిత్రులారా.jpg", "/storage/WhatsApp Images"))
        assertTrue(GreetingLexicon.matchesGreetingLexicon("shubhodayam_card.jpeg", "/storage/WhatsApp Images"))
        assertTrue(GreetingLexicon.matchesGreetingLexicon("subhodayam.png", "/storage/WhatsApp Images"))
    }

    @Test
    fun testKannadaGreetings() {
        assertTrue(GreetingLexicon.matchesGreetingLexicon("ಶುಭೋದಯ.jpg", "/storage/WhatsApp Images"))
        assertTrue(GreetingLexicon.matchesGreetingLexicon("shubhodaya_namaskara.png", "/storage/WhatsApp Images"))
    }

    @Test
    fun testMalayalamGreetings() {
        assertTrue(GreetingLexicon.matchesGreetingLexicon("സുപ്രഭാതം_ആശംസകൾ.jpg", "/storage/WhatsApp Images"))
        assertTrue(GreetingLexicon.matchesGreetingLexicon("suprabhatham.jpeg", "/storage/WhatsApp Images"))
    }

    @Test
    fun testBengaliAndPunjabiAndUrduGreetings() {
        // Bengali
        assertTrue(GreetingLexicon.matchesGreetingLexicon("শুভ_সকাল.jpg", "/storage/WhatsApp Images"))
        assertTrue(GreetingLexicon.matchesGreetingLexicon("shubhoshokal_wishes.png", "/storage/WhatsApp Images"))
        // Punjabi
        assertTrue(GreetingLexicon.matchesGreetingLexicon("ਸਤਿ_ਸ੍ਰੀ_ਅਕਾਲ.jpg", "/storage/WhatsApp Images"))
        assertTrue(GreetingLexicon.matchesGreetingLexicon("shubh_saver.jpeg", "/storage/WhatsApp Images"))
        // Urdu
        assertTrue(GreetingLexicon.matchesGreetingLexicon("صبح_بخیر.jpg", "/storage/WhatsApp Images"))
        assertTrue(GreetingLexicon.matchesGreetingLexicon("subah_bakhair.jpeg", "/storage/WhatsApp Images"))
        assertTrue(GreetingLexicon.matchesGreetingLexicon("jumma_mubarak.png", "/storage/WhatsApp Images"))
    }

    @Test
    fun testFestivalsAndAuspiciousWishes() {
        assertTrue(GreetingLexicon.matchesGreetingLexicon("Happy_Diwali_Card.jpg", "/storage/WhatsApp Images"))
        assertTrue(GreetingLexicon.matchesGreetingLexicon("दीपावली_की_शुभकामनाएं.png", "/storage/WhatsApp Images"))
        assertTrue(GreetingLexicon.matchesGreetingLexicon("pongal_wishes.jpeg", "/storage/WhatsApp Images"))
        assertTrue(GreetingLexicon.matchesGreetingLexicon("eid_mubarak_2024.jpg", "/storage/WhatsApp Images"))
    }

    @Test
    fun testNonGreetingMediaIsNotFalselyMatched() {
        assertFalse(GreetingLexicon.matchesGreetingLexicon("invoice_2023_october.pdf", "/storage/WhatsApp Documents"))
        assertFalse(GreetingLexicon.matchesGreetingLexicon("camera_photo_10023.jpg", "/storage/DCIM/Camera"))
        assertFalse(GreetingLexicon.matchesGreetingLexicon("presentation_slides.pptx", "/storage/Download"))
        assertFalse(GreetingLexicon.matchesGreetingLexicon("personal_selfie.jpg", "/storage/DCIM/Camera"))
    }
}

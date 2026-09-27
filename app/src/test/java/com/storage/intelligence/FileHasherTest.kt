package com.storage.intelligence

import com.storage.intelligence.data.scanner.FileHasher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class FileHasherTest {

    private val hasher = FileHasher()

    @Test
    fun `computeSha256 produces consistent hex digest for given content`() {
        val input = "StorageIQ Test Content".toByteArray()
        val stream1 = ByteArrayInputStream(input)
        val stream2 = ByteArrayInputStream(input)

        val hash1 = hasher.computeSha256(stream1)
        val hash2 = hasher.computeSha256(stream2)

        assertEquals(hash1, hash2)
        assertEquals(64, hash1.length) // SHA-256 hex length
    }

    @Test
    fun `calculateHammingDistance computes exact bit difference`() {
        val hashA = 0b10101010L
        val hashB = 0b10100010L

        val distance = hasher.calculateHammingDistance(hashA, hashB)
        assertEquals(1, distance)
    }

    @Test
    fun `calculateHammingDistance for identical hashes is zero`() {
        val hash = 0xABCD1234567890L
        assertEquals(0, hasher.calculateHammingDistance(hash, hash))
    }
}

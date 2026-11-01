package com.kingpaging.qwelcome.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class QrCodeShareCacheTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `same network and sanitized name collisions never replace earlier shares`() {
        val directory = temporaryFolder.newFolder()
        val cache = QrCodeShareCache(directory)
        val originalBytes = byteArrayOf(1, 2, 3)
        val first = cache.write("Office/Guest") { it.write(originalBytes) }
        val second = cache.write("Office/Guest") { it.write(byteArrayOf(4)) }
        val collision = cache.write("Office?Guest") { it.write(byteArrayOf(5)) }

        assertNotEquals(first, second)
        assertNotEquals(first, collision)
        assertArrayEquals(originalBytes, first.readBytes())
        assertEquals(3, directory.listFiles()!!.size)
    }

    @Test
    fun `pruning removes expired shares and preserves recent receiver images`() {
        var now = 200_000_000L
        val directory = temporaryFolder.newFolder()
        val cache = QrCodeShareCache(directory) { now }
        val old = cache.write("Old") { it.write(1) }
        now += QrCodeShareCache.SHARE_RETENTION_MILLIS
        val recent = cache.write("Recent") { it.write(2) }
        now += 1
        cache.pruneExpired()

        assertFalse(old.exists())
        assertTrue(recent.exists())
    }

    @Test
    fun `failed encoding removes its incomplete image`() {
        val directory = temporaryFolder.newFolder()
        val cache = QrCodeShareCache(directory)

        try {
            cache.write("Office") {
                it.write(byteArrayOf(1, 2))
                throw IOException("Storage failed")
            }
            throw AssertionError("Expected IOException")
        } catch (_: IOException) {
            assertTrue(directory.listFiles()!!.isEmpty())
        }
    }
}

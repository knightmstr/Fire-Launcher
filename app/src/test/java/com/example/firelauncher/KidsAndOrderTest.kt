package com.example.firelauncher

import com.example.firelauncher.data.AppSortOrder
import com.example.firelauncher.data.PinHasher
import com.example.firelauncher.data.PrefsCodec
import com.example.firelauncher.data.WallpaperSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KidsAndOrderTest {

    @Test
    fun `pin hash verifies the right pin only`() {
        val stored = PinHasher.hash("1234")
        assertTrue(PinHasher.verify("1234", stored))
        assertFalse(PinHasher.verify("4321", stored))
        assertFalse(PinHasher.verify("1234", null))
        assertFalse(PinHasher.verify("1234", "garbage"))
        assertFalse(stored.contains("1234"))
    }

    @Test
    fun `pin hashes are salted`() {
        assertNotEquals(PinHasher.hash("0000"), PinHasher.hash("0000"))
    }

    @Test
    fun `pin validation requires exactly four digits`() {
        assertTrue(PinHasher.isValidPin("0007"))
        assertFalse(PinHasher.isValidPin("123"))
        assertFalse(PinHasher.isValidPin("12345"))
        assertFalse(PinHasher.isValidPin("12a4"))
    }

    @Test
    fun `custom order keeps arranged apps first and appends new apps alphabetically`() {
        val installed = listOf("zoom", "netflix", "abc", "plex")
        val result = PrefsCodec.applyOrderWithRemainder(
            installed, listOf("plex", "netflix", "uninstalled"), key = { it }, fallbackSort = { it }
        )
        assertEquals(listOf("plex", "netflix", "abc", "zoom"), result)
    }

    @Test
    fun `enum parsing falls back safely`() {
        assertEquals(WallpaperSource.MY_PHOTOS, WallpaperSource.fromName(null))
        assertEquals(WallpaperSource.REDDIT, WallpaperSource.fromName("reddit"))
        assertEquals(AppSortOrder.CUSTOM, AppSortOrder.fromName("CUSTOM"))
    }
}

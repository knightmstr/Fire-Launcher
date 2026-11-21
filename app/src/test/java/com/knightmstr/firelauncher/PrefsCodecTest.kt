package com.knightmstr.firelauncher

import com.knightmstr.firelauncher.data.LauncherPrefsStore
import com.knightmstr.firelauncher.data.LauncherSettings
import com.knightmstr.firelauncher.data.PrefsCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class PrefsCodecTest {

    @Test
    fun `list round-trips and drops blanks and duplicates`() {
        val encoded = PrefsCodec.encodeList(listOf("a", " b ", "", "a", "c"))
        assertEquals(listOf("a", "b", "c"), PrefsCodec.decodeList(encoded))
        assertEquals(emptyList<String>(), PrefsCodec.decodeList(null))
        assertEquals(emptyList<String>(), PrefsCodec.decodeList(""))
    }

    @Test
    fun `toggle appends new favourites and removes existing ones`() {
        assertEquals(listOf("a", "b", "c"), PrefsCodec.toggle(listOf("a", "b"), "c"))
        assertEquals(listOf("a", "c"), PrefsCodec.toggle(listOf("a", "b", "c"), "b"))
    }

    @Test
    fun `move shifts by delta and clamps at the ends`() {
        val order = listOf("a", "b", "c", "d")
        assertEquals(listOf("b", "a", "c", "d"), PrefsCodec.move(order, "b", -1))
        assertEquals(listOf("a", "c", "b", "d"), PrefsCodec.move(order, "b", 1))
        assertEquals(order, PrefsCodec.move(order, "a", -1))
        assertEquals(order, PrefsCodec.move(order, "d", 5))
        assertEquals(listOf("b", "c", "d", "a"), PrefsCodec.move(order, "a", 10))
        assertEquals(order, PrefsCodec.move(order, "missing", 1))
    }

    @Test
    fun `applyOrder follows stored order and drops unknown keys`() {
        val items = listOf("x", "y", "z")
        assertEquals(listOf("z", "x"), PrefsCodec.applyOrder(items, listOf("z", "gone", "x")) { it })
    }

    @Test
    fun `labels are sanitised so delimiters cannot corrupt storage`() {
        val entry = PrefsCodec.encodeLabel("com.netflix", "Net|flix\nTV")
        assertEquals(mapOf("com.netflix" to "Net-flix TV"), PrefsCodec.decodeLabels(setOf(entry)))
        assertEquals(PrefsCodec.MAX_LABEL_LENGTH, PrefsCodec.sanitizeLabel("x".repeat(100)).length)
        assertTrue(PrefsCodec.decodeLabels(setOf("|nopkg", "noLabel|", "garbage")).isEmpty())
    }

    @Test
    fun `withoutLabel removes only the matching package`() {
        val set = setOf("com.a|A", "com.ab|AB")
        assertEquals(setOf("com.ab|AB"), PrefsCodec.withoutLabel(set, "com.a"))
    }

    @Test
    fun `settings options cycle and wrap`() {
        val opts = LauncherSettings.WALLPAPER_INTERVAL_OPTIONS
        assertEquals(opts[1], LauncherSettings.nextOption(opts, opts[0]))
        assertEquals(opts[0], LauncherSettings.nextOption(opts, opts.last()))
        assertEquals(opts[0], LauncherSettings.nextOption(opts, 999))
        assertEquals("Off", LauncherSettings.describeMinutes(0))
        assertEquals("3 hr", LauncherSettings.describeMinutes(180))
        assertEquals("15 min", LauncherSettings.describeMinutes(15))
    }

    @Test
    fun `temperature unit defaults follow the device region`() {
        assertFalse(LauncherPrefsStore.defaultIsCelsius(Locale.US))
        assertTrue(LauncherPrefsStore.defaultIsCelsius(Locale("en", "AU")))
        assertTrue(LauncherPrefsStore.defaultIsCelsius(Locale.UK))
    }

    @Test
    fun `old tile size names map onto the slider range`() {
        assertEquals(140, LauncherSettings.tileWidthFromLegacySize(null))
        assertEquals(140, LauncherSettings.tileWidthFromLegacySize("SMALL"))
        assertEquals(115, LauncherSettings.tileWidthFromLegacySize("EXTRA_SMALL"))
        assertEquals(170, LauncherSettings.tileWidthFromLegacySize("EXTRA_LARGE"))
        assertEquals(90, LauncherSettings.clampTileWidth(10))
        assertEquals(170, LauncherSettings.clampTileWidth(400))
    }
}

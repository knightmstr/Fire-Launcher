package com.knightmstr.onyxtv

import com.knightmstr.onyxtv.upload.SetupValidation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupValidationTest {

    @Test
    fun `package names must look like package names`() {
        assertTrue(SetupValidation.isPackageName("com.netflix.ninja"))
        assertTrue(SetupValidation.isPackageName("org.xbmc.kodi"))
        assertTrue(SetupValidation.isPackageName("a"))
        for (bad in listOf(null, "", ".", "..", "../etc", "com..x", "com.x.", ".com.x", "com/x", "com x",
            "com.x;rm", "com.x\n", "<script>", "a".repeat(201))) {
            assertFalse("should reject: $bad", SetupValidation.isPackageName(bad))
        }
    }

    @Test
    fun `merge keeps order, drops junk and keeps uninstalled stored favourites`() {
        val installed = setOf("a.one", "b.two", "c.three")
        val merged = SetupValidation.mergeFavorites(
            requested = listOf("c.three", "evil", "a.one", "c.three", "x.notinstalled"),
            installed = installed,
            current = listOf("a.one", "z.uninstalled")
        )
        assertEquals(listOf("c.three", "a.one", "z.uninstalled"), merged)
    }

    @Test
    fun `merge keeps dock folders where they were`() {
        val installed = setOf("a.one", "b.two", "c.three")
        val merged = SetupValidation.mergeFavorites(
            requested = listOf("c.three", "a.one"),
            installed = installed,
            current = listOf("a.one", "folder:f1", "c.three", "folder:f2")
        )
        assertEquals(listOf("c.three", "folder:f1", "a.one", "folder:f2"), merged)
    }

    @Test
    fun `merge caps the list`() {
        val installed = (1..500).map { "p.app$it" }.toSet()
        val merged = SetupValidation.mergeFavorites(installed.toList(), installed, emptyList())
        assertEquals(SetupValidation.MAX_FAVORITES, merged.size)
    }
}

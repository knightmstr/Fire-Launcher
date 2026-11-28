package com.knightmstr.onyxtv

import com.knightmstr.onyxtv.data.AppFolder
import com.knightmstr.onyxtv.data.AppFolders
import com.knightmstr.onyxtv.data.AppFolders.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppFoldersTest {

    private val start = State(emptyList(), listOf("a.netflix", "b.plex", "c.kodi"))

    @Test
    fun `create puts the folder where the app was`() {
        val s = AppFolders.create(start, "b.plex")
        assertEquals(listOf(AppFolder("f1", "Folder", listOf("b.plex"))), s.folders)
        assertEquals(listOf("a.netflix", "folder:f1", "c.kodi"), s.favorites)
        val s2 = AppFolders.create(s, "z.notfav", "Games")
        assertEquals("f2", s2.folders[1].id)
        assertEquals("folder:f2", s2.favorites.last())
        assertEquals("Folder 2", AppFolders.defaultName(listOf(AppFolder("f1", "Folder", listOf("x")))))
        assertEquals("Folder 3", AppFolders.defaultName(listOf(AppFolder("f1", "Folder", listOf("x")), AppFolder("f2", "Folder 2", listOf("y")))))
    }

    @Test
    fun `moving into a folder takes the app off the dock and out of other folders`() {
        var s = AppFolders.create(start, "b.plex")
        s = AppFolders.moveInto(s, "a.netflix", "f1")
        assertEquals(listOf("b.plex", "a.netflix"), s.folders[0].packages)
        assertEquals(listOf("folder:f1", "c.kodi"), s.favorites)
        s = AppFolders.create(s, "c.kodi", "Two")
        s = AppFolders.moveInto(s, "a.netflix", "f2")
        assertEquals(listOf("b.plex"), s.folders[0].packages)
        assertEquals(listOf("c.kodi", "a.netflix"), s.folders[1].packages)
    }

    @Test
    fun `taking the last app out removes the folder`() {
        var s = AppFolders.create(start, "b.plex")
        s = AppFolders.takeOut(s, "b.plex")
        assertTrue(s.folders.isEmpty())
        assertEquals(listOf("a.netflix", "b.plex", "c.kodi"), s.favorites)
    }

    @Test
    fun `taking out goes right after the folder`() {
        var s = AppFolders.create(start, "a.netflix")
        s = AppFolders.moveInto(s, "c.kodi", "f1")
        s = AppFolders.takeOut(s, "c.kodi")
        assertEquals(listOf("folder:f1", "c.kodi", "b.plex"), s.favorites)
    }

    @Test
    fun `deleting a folder puts its apps back where it was`() {
        var s = AppFolders.create(start, "b.plex")
        s = AppFolders.moveInto(s, "x.extra", "f1")
        s = AppFolders.delete(s, "f1")
        assertTrue(s.folders.isEmpty())
        assertEquals(listOf("a.netflix", "b.plex", "x.extra", "c.kodi"), s.favorites)
    }

    @Test
    fun `rename keeps names clean and ignores blanks`() {
        var s = AppFolders.create(start, "b.plex")
        s = AppFolders.rename(s, "f1", "Movies\tand\nTV" + "!".repeat(50))
        assertEquals(AppFolders.MAX_NAME_LENGTH, s.folders[0].name.length)
        assertFalse(s.folders[0].name.contains('\t') || s.folders[0].name.contains('\n'))
        assertEquals(s, AppFolders.rename(s, "f1", "   "))
    }

    @Test
    fun `storage round trip and junk lines`() {
        val folders = listOf(AppFolder("f1", "Movies", listOf("a.netflix", "b.plex")), AppFolder("f3", "Games", listOf("c.kodi")))
        assertEquals(folders, AppFolders.decode(AppFolders.encode(folders)))
        val junk = "f1\tOk\ta.b\nnot a line\nx9\tBad id\ta.b\nf1\tDuplicate\ta.b\nf2\tBad pkgs\t../x,ok.pkg"
        assertEquals(listOf(AppFolder("f1", "Ok", listOf("a.b")), AppFolder("f2", "Bad pkgs", listOf("ok.pkg"))), AppFolders.decode(junk))
        assertTrue(AppFolders.decode(null).isEmpty())
    }

    @Test
    fun `folder keys`() {
        assertTrue(AppFolders.isFolderKey("folder:f12"))
        assertFalse(AppFolders.isFolderKey("folder:../x"))
        assertFalse(AppFolders.isFolderKey("com.folder.app"))
        assertEquals("f12", AppFolders.idOf("folder:f12"))
    }
}

package com.knightmstr.onyxtv

import com.knightmstr.onyxtv.data.DriveFolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveFolderTest {

    // Shape of Drive's embedded folder page (trimmed): file entries, a sub-folder, a non-image.
    private val html = """
        <html><body><div class="flip-entries">
        <div class="flip-entry" id="entry-1PhotoAAAAAAAAAAAA" tabindex="0" role="link">
          <div class="flip-entry-info"><a href="https://drive.google.com/file/d/1PhotoAAAAAAAAAAAA/view?usp=drive_web" target="_blank">
          <div class="flip-entry-thumb"><img src="https://lh3.googleusercontent.com/x" alt=""></div>
          <div class="flip-entry-title">Beach &amp; sunset.JPG</div></a></div></div>
        <div class="flip-entry" id="entry-1SubFolderBBBBBBBB" tabindex="0" role="link">
          <div class="flip-entry-info"><a href="https://drive.google.com/drive/folders/1SubFolderBBBBBBBB" target="_blank">
          <div class="flip-entry-title">Holiday.jpg</div></a></div></div>
        <div class="flip-entry" id="entry-1DocCCCCCCCCCCCCCC" tabindex="0" role="link">
          <div class="flip-entry-info"><a href="https://drive.google.com/file/d/1DocCCCCCCCCCCCCCC/view" target="_blank">
          <div class="flip-entry-title">notes.pdf</div></a></div></div>
        <div class="flip-entry" id="entry-1PhotoDDDDDDDDDDDD" tabindex="0" role="link">
          <div class="flip-entry-info"><a href="https://drive.google.com/file/d/1PhotoDDDDDDDDDDDD/view" target="_blank">
          <div class="flip-entry-title">IMG_0042.webp</div></a></div></div>
        </div></body></html>
    """.trimIndent()

    @Test
    fun `entries are read in order with folders marked`() {
        val e = DriveFolder.parseEntries(html)
        assertEquals(listOf("1PhotoAAAAAAAAAAAA", "1SubFolderBBBBBBBB", "1DocCCCCCCCCCCCCCC", "1PhotoDDDDDDDDDDDD"), e.map { it.id })
        assertEquals("Beach & sunset.JPG", e[0].title)
        assertTrue(e[1].isFolder)
    }

    @Test
    fun `only picture files become photo links`() {
        assertEquals(
            listOf(
                "https://drive.google.com/uc?export=view&id=1PhotoAAAAAAAAAAAA",
                "https://drive.google.com/uc?export=view&id=1PhotoDDDDDDDDDDDD"
            ),
            DriveFolder.photoUrls(html)
        )
    }

    @Test
    fun `junk pages give nothing`() {
        assertTrue(DriveFolder.photoUrls("").isEmpty())
        assertTrue(DriveFolder.photoUrls("<html>Sign in to continue</html>").isEmpty())
        assertTrue(DriveFolder.photoUrls("<div id=\"entry-\"><div class=\"flip-entry-title\">a.jpg</div>").isEmpty())
    }

    @Test
    fun `photo count is capped`() {
        val many = (1..300).joinToString("") { i ->
            val id = "1Photo" + i.toString().padStart(12, '0')
            "<div class=\"flip-entry\" id=\"entry-$id\"><a href=\"https://drive.google.com/file/d/$id/view\">" +
                "<div class=\"flip-entry-title\">p$i.jpg</div></a></div>"
        }
        assertEquals(DriveFolder.MAX_PHOTOS, DriveFolder.photoUrls(many).size)
    }

    @Test
    fun `folder ids are checked before any url is built`() {
        try {
            DriveFolder.embeddedUrl("../../evil")
            throw AssertionError("should refuse")
        } catch (e: IllegalArgumentException) {
            // expected
        }
        assertEquals("https://drive.google.com/embeddedfolderview?id=1FoLdErIdAbCdEfGh", DriveFolder.embeddedUrl("1FoLdErIdAbCdEfGh"))
        assertEquals("https://drive.google.com/embeddedfolderview?id=1FoLdErIdAbCdEfGh&resourcekey=0-k",
            DriveFolder.embeddedUrl("1FoLdErIdAbCdEfGh", "0-k"))
        try {
            DriveFolder.embeddedUrl("1FoLdErIdAbCdEfGh", "x&evil=1")
            throw AssertionError("should refuse")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }
}

package com.knightmstr.onyxtv

import com.knightmstr.onyxtv.data.WallpaperLinks
import com.knightmstr.onyxtv.data.WallpaperLinks.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperLinksTest {

    private fun c(s: String) = WallpaperLinks.classify(s)

    @Test
    fun `plain image links pass through`() {
        assertEquals(Kind.Image("https://i.redd.it/abc.jpg"), c("https://i.redd.it/abc.jpg"))
        assertEquals(Kind.Image("https://example.com/a.png?x=1"), c("  https://example.com/a.png?x=1 "))
        assertEquals(Kind.Image("https://example.com/a.jpg"), c("example.com/a.jpg"))
    }

    @Test
    fun `google drive share links become direct links`() {
        val direct = Kind.Image("https://drive.google.com/uc?export=view&id=1AbCdEfGhIjKlMnOp_-Q")
        assertEquals(direct, c("https://drive.google.com/file/d/1AbCdEfGhIjKlMnOp_-Q/view?usp=sharing"))
        assertEquals(direct, c("https://drive.google.com/file/d/1AbCdEfGhIjKlMnOp_-Q/view"))
        assertEquals(direct, c("https://drive.google.com/open?id=1AbCdEfGhIjKlMnOp_-Q"))
        assertEquals(direct, c("https://drive.google.com/uc?id=1AbCdEfGhIjKlMnOp_-Q&export=download"))
        assertTrue(c("https://drive.google.com/file/d/short/view") is Kind.Unsupported)
        assertTrue(c("https://drive.google.com/file/d/bad\"id/view") is Kind.Unsupported)
    }

    @Test
    fun `drive folders become feeds`() {
        val folder = Kind.DriveFolder("1FoLdErIdAbCdEfGh_-")
        for (s in listOf("https://drive.google.com/drive/folders/1FoLdErIdAbCdEfGh_-?usp=sharing",
            "https://drive.google.com/drive/folders/1FoLdErIdAbCdEfGh_-",
            "https://drive.google.com/drive/u/0/folders/1FoLdErIdAbCdEfGh_-",
            "drive.google.com/embeddedfolderview?id=1FoLdErIdAbCdEfGh_-#grid")) {
            assertEquals(s, folder, c(s))
        }
        assertEquals("https://drive.google.com/drive/folders/1FoLdErIdAbCdEfGh_-", folder.url)
        assertEquals(folder, WallpaperLinks.driveFolderOf(folder.url))
        val keyed = c("https://drive.google.com/drive/folders/1FoLdErIdAbCdEfGh_-?resourcekey=0-AbC_d&usp=sharing")
        assertEquals(Kind.DriveFolder("1FoLdErIdAbCdEfGh_-", "0-AbC_d"), keyed)
        assertEquals("https://drive.google.com/drive/folders/1FoLdErIdAbCdEfGh_-?resourcekey=0-AbC_d", (keyed as Kind.DriveFolder).url)
        assertEquals(keyed, WallpaperLinks.driveFolderOf(keyed.url))
        assertTrue(c("https://drive.google.com/drive/folders/1FoLdErIdAbCdEfGh_-?resourcekey=a%26b") is Kind.Unsupported)
        assertNull(WallpaperLinks.driveFolderOf("https://drive.google.com/uc?export=view&id=1FoLdErIdAbCdEfGh_-"))
        assertTrue(c("https://drive.google.com/drive/folders/") is Kind.Unsupported)
        assertTrue(c("https://drive.google.com/drive/folders/bad<id>") is Kind.Unsupported)
    }

    @Test
    fun `dropbox links ask for the raw file`() {
        assertEquals(
            Kind.Image("https://www.dropbox.com/scl/fi/abc123/beach.jpg?rlkey=xyz&raw=1"),
            c("https://www.dropbox.com/scl/fi/abc123/beach.jpg?rlkey=xyz&dl=0")
        )
        assertEquals(Kind.Image("https://www.dropbox.com/s/abc/beach.jpg?raw=1"), c("https://dropbox.com/s/abc/beach.jpg?dl=1"))
    }

    @Test
    fun `onedrive links use the shares API`() {
        val k = c("https://1drv.ms/i/s!AbCdEf")
        assertTrue(k is Kind.Image)
        val url = (k as Kind.Image).url
        assertTrue(url.startsWith("https://api.onedrive.com/v1.0/shares/u!"))
        assertTrue(url.endsWith("/root/content"))
        // base64url of "https://1drv.ms/i/s!AbCdEf" without padding
        assertEquals("https://api.onedrive.com/v1.0/shares/u!aHR0cHM6Ly8xZHJ2Lm1zL2kvcyFBYkNkRWY/root/content", url)
    }

    @Test
    fun `google photos explains itself`() {
        assertTrue(c("https://photos.app.goo.gl/AbC123") is Kind.Unsupported)
        assertTrue(c("https://photos.google.com/share/xyz") is Kind.Unsupported)
    }

    @Test
    fun `subreddits in any common form`() {
        val sub = Kind.Subreddit("EarthPorn")
        for (s in listOf("r/EarthPorn", "/r/EarthPorn/", "reddit.com/r/EarthPorn", "https://www.reddit.com/r/EarthPorn/",
            "https://old.reddit.com/r/EarthPorn", "https://www.reddit.com/r/EarthPorn/top/?t=all", "https://m.reddit.com/r/EarthPorn/hot")) {
            assertEquals(s, sub, c(s))
        }
        assertEquals("https://www.reddit.com/r/EarthPorn", sub.url)
        assertEquals("EarthPorn", WallpaperLinks.subredditOf("https://www.reddit.com/r/EarthPorn"))
        assertNull(WallpaperLinks.subredditOf("https://i.redd.it/x.jpg"))
        assertTrue(c("r/a") is Kind.Unsupported)
        assertTrue(c("r/" + "x".repeat(30)) is Kind.Unsupported)
        assertTrue(c("https://www.reddit.com/r/x.y") is Kind.Unsupported)
    }

    @Test
    fun `reddit posts`() {
        assertEquals(Kind.RedditPost("1abcde"), c("https://www.reddit.com/r/EarthPorn/comments/1abcde/sunset_over_darwin/"))
        assertEquals(Kind.RedditPost("1abcde"), c("https://redd.it/1abcde"))
        assertEquals(Kind.RedditPost("1abcde"), c("https://www.reddit.com/comments/1ABCDE"))
        assertTrue(c("https://www.reddit.com/r/EarthPorn/s/AbCdEf") is Kind.Unsupported)
        assertTrue(c("https://www.reddit.com/user/someone") is Kind.Unsupported)
    }

    @Test
    fun `junk is refused`() {
        for (s in listOf("", "   ", "javascript:alert(1)", "file:///etc/passwd", "https://user:pw@example.com/a.jpg",
            "https://exa mple.com/a.jpg", "ftp://example.com/a.jpg", "https://" + "a".repeat(3000))) {
            assertTrue(s.take(30), c(s) is Kind.Unsupported)
        }
    }

    @Test
    fun `lookalike hosts are not treated as reddit or drive`() {
        assertEquals(Kind.Image("https://evilreddit.com/r/EarthPorn"), c("https://evilreddit.com/r/EarthPorn"))
        assertEquals(Kind.Image("https://drive.google.com.evil.io/file/d/1AbCdEfGhIjKlMnOp/view"),
            c("https://drive.google.com.evil.io/file/d/1AbCdEfGhIjKlMnOp/view"))
    }
}

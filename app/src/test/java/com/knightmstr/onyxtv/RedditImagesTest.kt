package com.knightmstr.onyxtv

import com.knightmstr.onyxtv.data.RedditImages
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RedditImagesTest {

    private fun post(vararg pairs: Pair<String, Any>) = JSONObject().apply { pairs.forEach { (k, v) -> put(k, v) } }
    private fun listing(vararg posts: JSONObject) = JSONObject().put("data", JSONObject().put("children",
        JSONArray(posts.map { JSONObject().put("kind", "t3").put("data", it) })))

    @Test
    fun `listing keeps safe images and skips nsfw, video and other hosts`() {
        val l = listing(
            post("id" to "a", "url" to "https://i.redd.it/a.jpg", "title" to "A"),
            post("id" to "b", "url" to "https://i.redd.it/b.jpg", "over_18" to true),
            post("id" to "c", "url" to "https://v.redd.it/c", "is_video" to true),
            post("id" to "d", "url" to "https://evil.example/d.jpg"),
            post("id" to "e", "url" to "https://www.reddit.com/gallery/e"),
            post("id" to "f", "url" to "https://example.com/page", "preview" to JSONObject().put("images",
                JSONArray().put(JSONObject().put("source", JSONObject().put("url", "https://preview.redd.it/f.jpg?width=3840&amp;s=x")))))
        )
        val urls = RedditImages.parseListing(l, "Test").map { it.url }
        assertEquals(listOf("https://i.redd.it/a.jpg", "https://preview.redd.it/f.jpg?width=3840&s=x"), urls)
    }

    @Test
    fun `gallery posts give every image in order`() {
        val gallery = post(
            "id" to "g", "is_gallery" to true,
            "gallery_data" to JSONObject().put("items", JSONArray()
                .put(JSONObject().put("media_id", "m2")).put(JSONObject().put("media_id", "m1"))
                .put(JSONObject().put("media_id", "bad"))),
            "media_metadata" to JSONObject()
                .put("m1", JSONObject().put("e", "Image").put("s", JSONObject().put("u", "https://preview.redd.it/m1.jpg")))
                .put("m2", JSONObject().put("e", "Image").put("s", JSONObject().put("u", "https://preview.redd.it/m2.jpg")))
                .put("bad", JSONObject().put("e", "Image").put("s", JSONObject().put("u", "https://evil.example/x.jpg")))
        )
        val root = JSONArray().put(listing(gallery))
        val r = RedditImages.parsePost(root)
        assertEquals(listOf("https://preview.redd.it/m2.jpg", "https://preview.redd.it/m1.jpg"), r.urls)
        assertFalse(r.nsfw)
    }

    @Test
    fun `nsfw posts and subreddits are flagged`() {
        val r = RedditImages.parsePost(JSONArray().put(listing(post("id" to "n", "url" to "https://i.redd.it/n.jpg", "over_18" to true))))
        assertTrue(r.nsfw)
        assertTrue(r.urls.isEmpty())
        assertTrue(RedditImages.isNsfwSubreddit(JSONObject().put("data", JSONObject().put("over18", true))))
        assertFalse(RedditImages.isNsfwSubreddit(JSONObject().put("data", JSONObject().put("over18", false))))
    }

    @Test
    fun `only reddit and imgur image hosts over https`() {
        assertTrue(RedditImages.isAllowedImageHost("https://i.redd.it/a.jpg"))
        assertTrue(RedditImages.isAllowedImageHost("https://i.imgur.com/a.jpg"))
        assertFalse(RedditImages.isAllowedImageHost("http://i.redd.it/a.jpg"))
        assertFalse(RedditImages.isAllowedImageHost("https://i.redd.it.evil.example/a.jpg"))
        assertFalse(RedditImages.isAllowedImageHost("https://evilredd.it/a.jpg"))
        assertFalse(RedditImages.isAllowedImageHost("not a url"))
    }

    @Test
    fun `network helper only talks to reddit json urls we build`() {
        try {
            RedditImages.fetchJson("https://evil.example/r/x.json")
            throw AssertionError("should refuse")
        } catch (e: IllegalArgumentException) {
            // expected
        }
        assertTrue(RedditImages.listingUrl("EarthPorn").startsWith("https://www.reddit.com/r/EarthPorn/"))
    }
}

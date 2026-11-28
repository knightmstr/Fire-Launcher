package com.knightmstr.onyxtv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Turns whatever was pasted into "My links" into what gets saved, checking it online first:
 *
 *  - an image or cloud share link → the direct link, once it's confirmed to open as an image
 *  - a subreddit → `https://www.reddit.com/r/Name`, if it exists, isn't 18+, and has pictures
 *  - a Reddit post → the picture(s) in it (every image of a gallery)
 *  - a public Google Drive folder → the folder link, if it lists photos and the first one opens
 *
 * Used by both the TV's own dialog and the phone page, so they behave the same.
 */
object WallpaperLinkResolver {

    /** Most pictures one gallery post adds. */
    const val MAX_GALLERY_IMAGES = 20

    sealed class Result {
        /** [urls] to save; [what] is a short description for the confirmation message. */
        data class Ok(val urls: List<String>, val what: String) : Result()
        data class Error(val message: String) : Result()
    }

    suspend fun resolve(raw: String): Result = withContext(Dispatchers.IO) {
        when (val kind = WallpaperLinks.classify(raw)) {
            is WallpaperLinks.Kind.Unsupported -> Result.Error(kind.message)

            is WallpaperLinks.Kind.Image -> when (val r = WallpaperUrlValidator.validate(kind.url)) {
                is WallpaperUrlValidator.Result.Ok -> Result.Ok(listOf(kind.url), "picture")
                is WallpaperUrlValidator.Result.Error -> Result.Error(r.message)
            }

            is WallpaperLinks.Kind.Subreddit -> subreddit(kind)

            is WallpaperLinks.Kind.RedditPost -> post(kind.id)

            is WallpaperLinks.Kind.DriveFolder -> driveFolder(kind)
        }
    }

    private fun subreddit(kind: WallpaperLinks.Kind.Subreddit): Result {
        val name = kind.name
        val about = RedditImages.fetchJson(RedditImages.aboutUrl(name))?.let { runCatching { JSONObject(it) }.getOrNull() }
            ?: return Result.Error("Couldn't find r/$name. Check the name, or it may be private.")
        if (RedditImages.isNsfwSubreddit(about)) return Result.Error("r/$name is marked 18+, so it can't be used.")
        val listing = RedditImages.fetchJson(RedditImages.listingUrl(name))?.let { runCatching { JSONObject(it) }.getOrNull() }
            ?: return Result.Error("Couldn't load r/$name right now. Try again in a minute.")
        if (RedditImages.parseListing(listing, name).isEmpty()) {
            return Result.Error("No pictures found in r/$name right now.")
        }
        return Result.Ok(listOf(kind.url), "r/$name")
    }

    private suspend fun driveFolder(kind: WallpaperLinks.Kind.DriveFolder): Result {
        val html = DriveFolder.fetchHtml(kind.id, kind.resourceKey)
            ?: return Result.Error(
                "Couldn't open that Drive folder. In Drive, set it to Share → Anyone with the link, then try again."
            )
        val photos = DriveFolder.photoUrls(html)
        if (photos.isEmpty()) return Result.Error("No photos (JPEG, PNG or WebP) found in that Drive folder.")
        // The folder can be public while its files aren't; check one actually opens.
        return when (val r = WallpaperUrlValidator.validate(photos.first())) {
            is WallpaperUrlValidator.Result.Ok -> Result.Ok(
                listOf(kind.url),
                "Drive folder (${photos.size} photo${if (photos.size == 1) "" else "s"})"
            )
            is WallpaperUrlValidator.Result.Error -> Result.Error(
                "The folder is shared but its photos didn't open. Check the photos are shared too, and under 15 MB."
            )
        }
    }

    private fun post(id: String): Result {
        val root = RedditImages.fetchJson(RedditImages.postUrl(id))?.let { runCatching { JSONArray(it) }.getOrNull() }
            ?: return Result.Error("Couldn't open that Reddit post.")
        val images = RedditImages.parsePost(root)
        if (images.nsfw) return Result.Error("That post is marked 18+, so it can't be used.")
        if (images.urls.isEmpty()) return Result.Error("That post has no pictures.")
        val urls = images.urls.take(MAX_GALLERY_IMAGES)
        return Result.Ok(urls, if (urls.size == 1) "picture" else "${urls.size} pictures")
    }
}

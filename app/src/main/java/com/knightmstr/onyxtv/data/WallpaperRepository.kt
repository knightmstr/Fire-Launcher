package com.knightmstr.onyxtv.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Supplies wallpapers from the user's own photos (default) or r/TVWallpapersSFW,
 * and cycles through them on a user-configurable interval.
 */
class WallpaperRepository(
    private val scope: CoroutineScope,
    /** Minutes between automatic changes; 0 disables rotation. */
    private val rotationIntervalMinutes: Flow<Int>,
    private val source: Flow<WallpaperSource>,
    private val localPhotos: LocalPhotoSource
) {
    companion object {

        val FALLBACK_WALLPAPERS = listOf(
            WallpaperModel(
                id = "wp_01",
                title = "Twilight Over Yosemite Valley (4K)",
                url = "https://images.unsplash.com/photo-1426604966848-d7adac402bff?auto=format&fit=crop&w=2560&q=85",
                author = "u/NatureLover4K"
            ),
            WallpaperModel(
                id = "wp_02",
                title = "Deep Space Carina Nebula & Cosmic Dust (OLED)",
                url = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=2560&q=85",
                author = "u/AstroCinema"
            ),
            WallpaperModel(
                id = "wp_03",
                title = "Mount Fuji at Golden Hour with Cherry Blossoms",
                url = "https://images.unsplash.com/photo-1490806843957-31f4c9a91c65?auto=format&fit=crop&w=2560&q=85",
                author = "u/TokyoDriftViews"
            ),
            WallpaperModel(
                id = "wp_04",
                title = "Lofoten Islands Emerald Aurora Borealis Night",
                url = "https://images.unsplash.com/photo-1517411032315-54ef2cb783bb?auto=format&fit=crop&w=2560&q=85",
                author = "u/NordicLightWalker"
            ),
            WallpaperModel(
                id = "wp_05",
                title = "Tokyo Rainy Shibuya Cyberpunk Reflections 4K",
                url = "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?auto=format&fit=crop&w=2560&q=85",
                author = "u/NeonCityScape"
            ),
            WallpaperModel(
                id = "wp_06",
                title = "Italian Dolomites Alpine Lakes & Mist",
                url = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=2560&q=85",
                author = "u/AlpineDreamer"
            ),
            WallpaperModel(
                id = "wp_07",
                title = "Big Sur Pacific Coastline Sunset Horizon",
                url = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=2560&q=85",
                author = "u/CoastToCoastTV"
            ),
            WallpaperModel(
                id = "wp_08",
                title = "Sunbeams Through Redwood Forest Canopy",
                url = "https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=2560&q=85",
                author = "u/ForestWhispers"
            )
        )
    }

    // Start empty so nothing is downloaded before we know whether local photos exist
    private val _wallpapers = MutableStateFlow<List<WallpaperModel>>(emptyList())
    val wallpapers: StateFlow<List<WallpaperModel>> = _wallpapers.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)

    private val _currentWallpaper = MutableStateFlow<WallpaperModel?>(null)
    val currentWallpaper: StateFlow<WallpaperModel?> = _currentWallpaper.asStateFlow()

    /** Number of the user's own photos found on the last scan. */
    private val _localPhotoCount = MutableStateFlow(0)
    val localPhotoCount: StateFlow<Int> = _localPhotoCount.asStateFlow()

    private var redditCache: List<WallpaperModel>? = null
    private val reloadTick = MutableStateFlow(0)

    init {
        // Load from the chosen source; My photos falls back to Reddit only when no photos exist,
        // so with local photos the launcher makes no wallpaper network requests at all.
        scope.launch {
            combine(source, reloadTick) { src, _ -> src }.collectLatest { src ->
                val local = if (src == WallpaperSource.MY_PHOTOS) localPhotos.load() else emptyList()
                _localPhotoCount.value = local.size
                val list = local.ifEmpty { redditWallpapers() }
                applyList(list)
            }
        }

        // Auto-rotation timer; restarts whenever the interval setting changes
        scope.launch {
            rotationIntervalMinutes.collectLatest { minutes ->
                if (minutes <= 0) return@collectLatest
                while (isActive) {
                    delay(minutes * 60_000L)
                    nextWallpaper()
                }
            }
        }
    }

    /** Re-scans local photos (e.g. after the photo permission is granted or on resume). */
    fun reload() {
        reloadTick.value += 1
    }

    private fun applyList(list: List<WallpaperModel>) {
        if (list.isEmpty() || list.map { it.id } == _wallpapers.value.map { it.id }) return
        _wallpapers.value = list
        _currentIndex.value = 0
        _currentWallpaper.value = list.first()
    }

    private suspend fun redditWallpapers(): List<WallpaperModel> =
        redditCache ?: fetchRedditWallpapers().ifEmpty { FALLBACK_WALLPAPERS }.also { redditCache = it }

    /**
     * Advances to the next wallpaper in the list.
     */
    fun nextWallpaper() {
        val list = _wallpapers.value
        if (list.isEmpty()) return
        val nextIdx = (_currentIndex.value + 1) % list.size
        _currentIndex.value = nextIdx
        _currentWallpaper.value = list[nextIdx]
    }

    /**
     * Manually select a wallpaper by index or model.
     */
    fun setWallpaper(wallpaper: WallpaperModel) {
        val idx = _wallpapers.value.indexOfFirst { it.id == wallpaper.id }
        if (idx >= 0) {
            _currentIndex.value = idx
        }
        _currentWallpaper.value = wallpaper
    }

    /**
     * Queries r/TVWallpapersSFW JSON endpoint and extracts 16:9 images.
     */
    private suspend fun fetchRedditWallpapers(): List<WallpaperModel> = withContext(Dispatchers.IO) {
        val parsedList = mutableListOf<WallpaperModel>()
        try {
            val url = URL("https://www.reddit.com/r/TVWallpapersSFW/hot.json?limit=40")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 7000
                setRequestProperty("User-Agent", "android:com.knightmstr.onyxtv:v1.0 (Fire OS Leanback)")
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val jsonString = reader.use { it.readText() }
                val root = JSONObject(jsonString)
                val children = root.getJSONObject("data").getJSONArray("children")

                for (i in 0 until children.length()) {
                    val childData = children.getJSONObject(i).getJSONObject("data")
                    val isNsfw = childData.optBoolean("over_18", false)
                    val isVideo = childData.optBoolean("is_video", false)
                    if (isNsfw || isVideo) continue

                    var imgUrl = childData.optString("url", "")
                    val isDirectImage = imgUrl.endsWith(".jpg", true) ||
                            imgUrl.endsWith(".jpeg", true) ||
                            imgUrl.endsWith(".png", true) ||
                            imgUrl.endsWith(".webp", true)

                    if (!isDirectImage && childData.has("preview")) {
                        try {
                            val images = childData.getJSONObject("preview").getJSONArray("images")
                            if (images.length() > 0) {
                                val sourceUrl = images.getJSONObject(0).getJSONObject("source").getString("url")
                                imgUrl = sourceUrl.replace("&amp;", "&")
                            }
                        } catch (_: Exception) {
                            // continue with direct url
                        }
                    }

                    val isSafeUrl = imgUrl.startsWith("https://", ignoreCase = true) &&
                            try {
                                val host = java.net.URI(imgUrl).host?.lowercase() ?: ""
                                host == "i.redd.it" ||
                                        host == "preview.redd.it" ||
                                        host == "external-preview.redd.it" ||
                                        host.endsWith(".redd.it") ||
                                        host == "i.imgur.com" ||
                                        host.endsWith(".imgur.com") ||
                                        host == "images.unsplash.com"
                            } catch (_: Exception) {
                                false
                            }

                    if (isSafeUrl) {
                        parsedList.add(
                            WallpaperModel(
                                id = childData.optString("id", "post_$i"),
                                title = childData.optString("title", "TV Wallpaper").take(120),
                                url = imgUrl,
                                author = "u/" + childData.optString("author", "reddit_user").take(40),
                                permalink = "https://reddit.com" + childData.optString("permalink", "").take(200),
                                source = "reddit"
                            )
                        )
                    }
                }

            }
        } catch (_: Exception) {
            // Offline or restricted: caller uses the curated fallbacks
        }
        parsedList
    }
}

package com.example.firelauncher.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
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
 * Manages fetching wallpapers from r/TVWallpapersSFW and automatically
 * cycling through them every 30 minutes.
 */
class WallpaperRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        const val ROTATION_INTERVAL_MS = 30 * 60 * 1000L // 30 minutes

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

    private val _wallpapers = MutableStateFlow<List<WallpaperModel>>(FALLBACK_WALLPAPERS)
    val wallpapers: StateFlow<List<WallpaperModel>> = _wallpapers.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)

    private val _currentWallpaper = MutableStateFlow<WallpaperModel?>(FALLBACK_WALLPAPERS.first())
    val currentWallpaper: StateFlow<WallpaperModel?> = _currentWallpaper.asStateFlow()

    init {
        // Start background fetch from Reddit
        scope.launch {
            fetchRedditWallpapers()
        }

        // Start 30-minute auto-rotation timer
        scope.launch {
            while (isActive) {
                delay(ROTATION_INTERVAL_MS)
                nextWallpaper()
            }
        }
    }

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
    suspend fun fetchRedditWallpapers() = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://www.reddit.com/r/TVWallpapersSFW/hot.json?limit=40")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 7000
                setRequestProperty("User-Agent", "android:com.example.firelauncher:v1.0 (Fire OS Leanback)")
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val jsonString = reader.use { it.readText() }
                val root = JSONObject(jsonString)
                val children = root.getJSONObject("data").getJSONArray("children")

                val parsedList = mutableListOf<WallpaperModel>()

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

                if (parsedList.isNotEmpty()) {
                    _wallpapers.value = parsedList
                    _currentWallpaper.value = parsedList.first()
                }
            }
        } catch (_: Exception) {
            // Keep fallback wallpapers if offline or restricted
        }
    }
}

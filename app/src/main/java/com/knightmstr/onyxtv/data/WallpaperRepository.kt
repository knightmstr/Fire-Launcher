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

/**
 * Supplies wallpapers from the user's own photos (default), r/TVWallpapersSFW, or "My links"
 * (picture links and subreddits), and cycles through them on a user-configurable interval.
 */
class WallpaperRepository(
    private val scope: CoroutineScope,
    /** Minutes between automatic changes; 0 disables rotation. */
    private val rotationIntervalMinutes: Flow<Int>,
    private val source: Flow<WallpaperSource>,
    private val localPhotos: LocalPhotoSource,
    /** Image URLs the user added, used when the source is [WallpaperSource.CUSTOM_URL]. */
    private val customUrls: Flow<List<String>>
) {
    companion object {
        private const val DEFAULT_SUBREDDIT = "TVWallpapersSFW"
        private const val SUBREDDIT_CACHE_MS = 60 * 60 * 1000L
        private const val PER_SUBREDDIT = 25
        private const val MAX_FEEDS = 10


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
    private val subredditCache = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, List<WallpaperModel>>>()
    private val reloadTick = MutableStateFlow(0)

    init {
        // Load from the chosen source; My photos falls back to Reddit only when no photos exist,
        // so with local photos the launcher makes no wallpaper network requests at all.
        scope.launch {
            combine(source, customUrls, reloadTick) { src, urls, _ -> src to urls }
                .collectLatest { (src, urls) ->
                    val chosen = when (src) {
                        WallpaperSource.MY_PHOTOS -> localPhotos.load()
                        WallpaperSource.CUSTOM_URL -> customWallpapers(urls)
                        WallpaperSource.REDDIT -> emptyList()
                    }
                    _localPhotoCount.value = if (src == WallpaperSource.MY_PHOTOS) chosen.size else 0
                    // Any chosen source falls back to Reddit/curated when it's empty.
                    val list = chosen.ifEmpty { redditWallpapers() }
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

    /**
     * My links: pictures first, in the order added, then pictures pulled from any subreddits and
     * Drive folders in the list (a few from each, mixed together).
     */
    private suspend fun customWallpapers(urls: List<String>): List<WallpaperModel> {
        val pictures = ArrayList<WallpaperModel>()
        val feedSources = ArrayList<suspend () -> List<WallpaperModel>>()
        for (url in urls) {
            val sub = WallpaperLinks.subredditOf(url)
            if (sub != null) {
                if (feedSources.size < MAX_FEEDS) feedSources += { subredditWallpapers(sub) }
                continue
            }
            val folder = WallpaperLinks.driveFolderOf(url)
            if (folder != null) {
                if (feedSources.size < MAX_FEEDS) feedSources += { driveFolderWallpapers(folder.id, folder.resourceKey) }
                continue
            }
            pictures += WallpaperModel(
                id = "custom_${url.hashCode()}",
                title = "My link",
                url = url,
                author = "Added by you",
                permalink = url,
                source = WallpaperModel.SOURCE_CUSTOM
            )
        }
        val feeds = feedSources.map { it() }
        val mixed = ArrayList<WallpaperModel>()
        val longest = feeds.maxOfOrNull { it.size } ?: 0
        for (i in 0 until longest) feeds.forEach { feed -> feed.getOrNull(i)?.let { mixed += it } }
        return pictures + mixed.distinctBy { it.url }
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

    /** Pictures from one subreddit, cached for [SUBREDDIT_CACHE_MS] so rotation doesn't hammer Reddit. */
    private suspend fun subredditWallpapers(name: String): List<WallpaperModel> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        subredditCache[name]?.let { (at, list) -> if (now - at < SUBREDDIT_CACHE_MS) return@withContext list }
        val list = RedditImages.fetchJson(RedditImages.listingUrl(name))
            ?.let { runCatching { RedditImages.parseListing(JSONObject(it), name, limit = PER_SUBREDDIT) }.getOrNull() }
            .orEmpty()
        // Keep the last good list if Reddit is briefly unreachable.
        if (list.isNotEmpty() || subredditCache[name] == null) subredditCache[name] = now to list
        subredditCache[name]?.second.orEmpty()
    }

    /** Photos in a public Drive folder, cached like subreddits; new photos show up within the hour. */
    private suspend fun driveFolderWallpapers(id: String, resourceKey: String?): List<WallpaperModel> = withContext(Dispatchers.IO) {
        val key = "drive:$id"
        val now = System.currentTimeMillis()
        subredditCache[key]?.let { (at, list) -> if (now - at < SUBREDDIT_CACHE_MS) return@withContext list }
        val list = DriveFolder.fetchHtml(id, resourceKey)?.let { DriveFolder.photoUrls(it) }.orEmpty().map { url ->
            WallpaperModel(
                id = "drive_${url.hashCode()}",
                title = "Google Drive",
                url = url,
                author = "Your Drive folder",
                permalink = WallpaperLinks.driveFolderUrl(id, resourceKey),
                source = WallpaperModel.SOURCE_CUSTOM
            )
        }
        if (list.isNotEmpty() || subredditCache[key] == null) subredditCache[key] = now to list
        subredditCache[key]?.second.orEmpty()
    }

    /** r/TVWallpapersSFW, the built-in Reddit source. */
    private suspend fun fetchRedditWallpapers(): List<WallpaperModel> = subredditWallpapers(DEFAULT_SUBREDDIT)
}

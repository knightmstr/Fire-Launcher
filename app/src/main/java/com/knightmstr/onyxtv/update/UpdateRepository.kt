package com.knightmstr.onyxtv.update

import android.content.Context
import android.os.Build
import com.knightmstr.onyxtv.BuildConfig
import com.knightmstr.onyxtv.data.UpdateChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/**
 * Checks GitHub Releases for a newer Onyx TV, downloads the APK, verifies it, and hands it
 * to [UpdateInstaller].
 *
 * Flow:  check() → Available → download() → (verify SHA-256 + package) → install → Installing
 */
class UpdateRepository(private val context: Context) {

    companion object {
        private const val API_LATEST = "https://api.github.com/repos/${BuildConfig.UPDATE_REPO}/releases/latest"
        private const val API_LIST = "https://api.github.com/repos/${BuildConfig.UPDATE_REPO}/releases?per_page=20"
        private const val MAX_APK_BYTES = 100L * 1024 * 1024
        private const val MAX_NOTES_CHARS = 1500
    }

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    val installedVersionName: String = BuildConfig.VERSION_NAME
    private val installedVersionCode: Int = BuildConfig.VERSION_CODE

    private val updatesDir: File get() = File(context.cacheDir, "updates").apply { mkdirs() }

    // ------------------------------------------------------------------ 1. Check

    /**
     * Asks GitHub for the newest release on the chosen [channel]. Quiet = don't show errors
     * (background checks). Stable = latest full release; Beta = newest release including
     * pre-releases.
     */
    suspend fun check(quiet: Boolean, channel: UpdateChannel) {
        val busy = _state.value
        if (busy is UpdateState.Downloading || busy is UpdateState.Installing) return
        if (!quiet) _state.value = UpdateState.Checking

        val result = runCatching { fetchRelease(channel) }
        _state.value = result.fold(
            onSuccess = { release ->
                when {
                    release == null -> UpdateState.UpToDate(installedVersionName)
                    UpdateVersions.isNewer(release, installedVersionCode) -> UpdateState.Available(release)
                    else -> UpdateState.UpToDate(installedVersionName)
                }
            },
            onFailure = {
                if (quiet) busy.takeUnless { it is UpdateState.Checking } ?: UpdateState.Idle
                else UpdateState.Failed("Couldn't reach GitHub. Check the internet connection.", null)
            }
        )
        if (result.isSuccess) cleanOldDownloads()
    }

    private suspend fun fetchRelease(channel: UpdateChannel): ReleaseInfo? = withContext(Dispatchers.IO) {
        when (channel) {
            UpdateChannel.STABLE -> {
                // GitHub's /releases/latest already excludes drafts and pre-releases.
                val json = JSONObject(httpGetText(API_LATEST))
                if (json.optBoolean("draft") || json.optBoolean("prerelease")) null else parseRelease(json)
            }
            UpdateChannel.BETA -> {
                // Newest installable release of any kind (pre-releases included), by version code.
                val text = httpGetText(API_LIST)
                if (!text.trimStart().startsWith("[")) return@withContext null
                val releases = JSONArray(text)
                var best: ReleaseInfo? = null
                for (i in 0 until releases.length()) {
                    val json = releases.getJSONObject(i)
                    if (json.optBoolean("draft")) continue
                    val info = parseRelease(json) ?: continue
                    if (best == null || info.versionCode > best.versionCode) best = info
                }
                best
            }
        }
    }

    /** Builds a [ReleaseInfo] from one release's JSON, or null if it has no installable APK. */
    private fun parseRelease(json: JSONObject): ReleaseInfo? {
        val tag = json.optString("tag_name")
        val versionCode = UpdateVersions.versionCodeFromTag(tag) ?: return null

        val assets = json.optJSONArray("assets") ?: return null
        var apk: JSONObject? = null
        var checksums: JSONObject? = null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            val name = a.optString("name")
            when {
                name.endsWith("-release.apk") -> apk = a
                name.endsWith(".sha256") -> checksums = a
            }
        }
        val apkAsset = apk ?: return null

        return ReleaseInfo(
            tag = tag,
            versionName = tag.removePrefix("v"),
            versionCode = versionCode,
            notes = UpdateVersions.cleanNotes(json.optString("body")).take(MAX_NOTES_CHARS),
            apkName = apkAsset.optString("name"),
            apkUrl = apkAsset.optString("browser_download_url"),
            apkSizeBytes = apkAsset.optLong("size"),
            checksumsUrl = checksums?.optString("browser_download_url")
        )
    }

    // ------------------------------------------------------------------ 2. Download & verify

    suspend fun downloadAndInstall(release: ReleaseInfo) {
        if (!UpdateInstaller.canInstall(context)) {
            _state.value = UpdateState.NeedsInstallPermission(release)
            return
        }
        try {
            _state.value = UpdateState.Downloading(release, 0f)
            val apk = download(release)
            verify(release, apk)
            _state.value = UpdateState.Installing(release)
            UpdateInstaller.install(context, apk)
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            _state.value = UpdateState.Failed(e.message ?: "Update failed", release)
        }
    }

    private suspend fun download(release: ReleaseInfo): File = withContext(Dispatchers.IO) {
        require(release.apkUrl.startsWith("https://")) { "Refusing a non-HTTPS download" }
        require(release.apkSizeBytes in 1..MAX_APK_BYTES) { "Unexpected update size" }

        val target = File(updatesDir, release.apkName)
        val partial = File(updatesDir, "${release.apkName}.part")
        val conn = openConnection(release.apkUrl)
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) error("Download failed (HTTP ${conn.responseCode})")
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: release.apkSizeBytes
            var lastReported = -1
            conn.inputStream.use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read = 0L
                    while (true) {
                        ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        read += n
                        if (read > MAX_APK_BYTES) error("Update is larger than expected")
                        output.write(buffer, 0, n)
                        val percent = (read * 100 / total).toInt()
                        if (percent != lastReported) {
                            lastReported = percent
                            _state.value = UpdateState.Downloading(release, (read.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        if (!partial.renameTo(target)) error("Couldn't save the update")
        target
    }

    /**
     * Two checks before installing:
     *  1. SHA-256 matches the checksum file published with the release (catches corrupt downloads).
     *  2. The APK really is Onyx TV and really is newer.
     * Android itself then refuses the install unless it's signed with the same key as the
     * installed app, so a tampered APK can't replace Onyx TV.
     */
    private suspend fun verify(release: ReleaseInfo, apk: File) = withContext(Dispatchers.IO) {
        if (apk.length() != release.apkSizeBytes) error("Download incomplete, please try again")

        // A checksum is required: without one there's no way to confirm the download is intact,
        // so refuse rather than install an unverified APK.
        val checksumsUrl = release.checksumsUrl
            ?: error("This release has no checksum file, can't verify the download")
        val expected = UpdateVersions.checksumFor(httpGetText(checksumsUrl), release.apkName)
            ?: error("Release has no checksum for ${release.apkName}")
        if (sha256(apk) != expected) {
            apk.delete()
            error("Downloaded file is corrupted, please try again")
        }

        val info = context.packageManager.getPackageArchiveInfo(apk.absolutePath, 0)
            ?: error("Downloaded file isn't a valid app")
        if (info.packageName != context.packageName) error("Downloaded app isn't Onyx TV")
        @Suppress("DEPRECATION")
        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode.toInt() else info.versionCode
        if (code <= installedVersionCode) error("Downloaded version isn't newer than this one")
    }

    // ------------------------------------------------------------------ Helpers

    fun onInstallPermissionMaybeGranted() {
        val s = _state.value
        if (s is UpdateState.NeedsInstallPermission && UpdateInstaller.canInstall(context)) {
            _state.value = UpdateState.Available(s.release)
        }
    }

    /** Called by [InstallResultReceiver] when Android reports the outcome. */
    fun onInstallResult(success: Boolean, message: String?) {
        val release = (_state.value as? UpdateState.Installing)?.release
        _state.value = if (success) UpdateState.UpToDate(release?.versionName ?: installedVersionName)
        else UpdateState.Failed(message ?: "Install was cancelled", release)
    }

    fun dismissError() {
        if (_state.value is UpdateState.Failed) _state.value = UpdateState.Idle
    }

    /** Keeps the cache tidy: only the APK of the newest known release is kept. */
    private fun cleanOldDownloads() {
        val keep = (_state.value as? UpdateState.Available)?.release?.apkName
        updatesDir.listFiles()?.filter { it.name != keep }?.forEach { it.delete() }
    }

    private fun openConnection(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 20_000
            instanceFollowRedirects = true // GitHub redirects downloads to its CDN
            setRequestProperty("User-Agent", "OnyxTV/${BuildConfig.VERSION_NAME} (Fire TV)")
            setRequestProperty("Accept", "application/vnd.github+json, application/octet-stream")
        }

    private fun httpGetText(url: String): String {
        val conn = openConnection(url)
        try {
            when (conn.responseCode) {
                HttpURLConnection.HTTP_OK -> return conn.inputStream.bufferedReader().use { it.readText() }
                HttpURLConnection.HTTP_NOT_FOUND -> return "{}" // no releases yet
                else -> error("HTTP ${conn.responseCode}")
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

package com.knightmstr.onyxtv.update

/** A published GitHub release that contains an installable APK. */
data class ReleaseInfo(
    val tag: String,
    val versionName: String,
    val versionCode: Int,
    val notes: String,
    val apkName: String,
    val apkUrl: String,
    val apkSizeBytes: Long,
    val checksumsUrl: String?
)

/** Everything the update UI can show. */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpToDate(val currentVersion: String) : UpdateState
    data class Available(val release: ReleaseInfo) : UpdateState
    /** [progress] is 0f..1f, or null when the server didn't send a size. */
    data class Downloading(val release: ReleaseInfo, val progress: Float?) : UpdateState
    /** Android needs the user to allow Onyx TV to install apps first. */
    data class NeedsInstallPermission(val release: ReleaseInfo) : UpdateState
    /** Handed to Android's installer; the confirmation screen is showing. */
    data class Installing(val release: ReleaseInfo) : UpdateState
    data class Failed(val message: String, val release: ReleaseInfo?) : UpdateState
}

/** Pure helpers shared with the release workflow's numbering scheme (unit tested). */
object UpdateVersions {

    private val SEMVER = Regex("""^v?(\d+)\.(\d+)(?:\.(\d+))?(?:-(?:alpha|beta|rc)\.(\d+))?""")

    /** A stable release ranks above all of its own pre-releases. */
    const val STABLE_PRE = 999

    /**
     * Same formula as .github/workflows/release.yml:
     * `(MAJOR*10000 + MINOR*100 + PATCH) * 1000 + PRE`, where PRE is 999 for a stable release and
     * the pre-release number (1..998) for `-beta.N` / `-rc.N` / `-alpha.N`.
     *
     *   "v1.2.3"        -> 10203999   (stable)
     *   "v1.2.3-beta.1" -> 10203001   (beta, ranks below the stable 1.2.3)
     *   "v1.3.0-beta.1" -> 10300001   (ranks above stable 1.2.3)
     *
     * Returns null for tags we can't order: "v1_x", or a "-suffix" that isn't a recognised,
     * numbered pre-release (so a malformed pre-release never outranks a real stable).
     */
    fun versionCodeFromTag(tag: String): Int? {
        val trimmed = tag.trim()
        val m = SEMVER.find(trimmed) ?: return null
        val major = m.groupValues[1].toInt()
        val minor = m.groupValues[2].toInt()
        val patch = m.groupValues[3].toIntOrNull() ?: 0
        if (minor > 99 || patch > 99) return null

        val preNumber = m.groupValues[4]
        val pre = when {
            !trimmed.contains('-') -> STABLE_PRE
            preNumber.isNotEmpty() -> preNumber.toInt().coerceIn(1, STABLE_PRE - 1)
            else -> return null // has a '-' but not a recognised -beta.N / -rc.N / -alpha.N
        }
        return (major * 10000 + minor * 100 + patch) * 1000 + pre
    }

    fun isNewer(release: ReleaseInfo, installedVersionCode: Int): Boolean =
        release.versionCode > installedVersionCode

    private val MD_LINK = Regex("""\[([^\]]+)]\([^)]+\)""")
    private val BY_AUTHOR = Regex("""\s+by @\S+(\s+in\s+\S+)?\s*$""")
    private val URL = Regex("""https?://\S+""")

    /**
     * Turns GitHub's generated release notes into short plain lines for a TV screen:
     * drops "What's Changed" / "Full Changelog" / "New Contributors", "by @user in <url>"
     * suffixes, links and markdown symbols. Returns "" if nothing useful is left.
     */
    fun cleanNotes(markdown: String): String {
        val out = mutableListOf<String>()
        var skipSection = false
        for (raw in markdown.lineSequence()) {
            val line = raw.trim()
            if (line.startsWith("#")) {
                val heading = line.trimStart('#').trim().lowercase()
                skipSection = heading.startsWith("new contributors")
                if (skipSection || heading.startsWith("what's changed")) continue
            }
            if (skipSection || line.isEmpty() || line.contains("Full Changelog", ignoreCase = true)) continue
            val text = line
                .replace(MD_LINK, "$1")
                .replace(BY_AUTHOR, "")
                .replace(URL, "")
                .replace("**", "")
                .replace("`", "")
                .trimStart('#', '*', '-', ' ')
                .trim()
            if (text.isNotEmpty()) out += "• $text"
        }
        return out.joinToString("\n")
    }

    /** Finds the hash for [fileName] in `sha256sum` output ("<hash>  <name>" per line). */
    fun checksumFor(sha256sumOutput: String, fileName: String): String? =
        sha256sumOutput.lineSequence()
            .map { it.trim().split(Regex("""\s+\*?"""), limit = 2) }
            .firstOrNull { it.size == 2 && it[1].trim() == fileName }
            ?.get(0)
            ?.lowercase()
            ?.takeIf { it.length == 64 && it.all { c -> c in '0'..'9' || c in 'a'..'f' } }
}

package com.example.firelauncher.update

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
    /** Android needs the user to allow FireLauncher to install apps first. */
    data class NeedsInstallPermission(val release: ReleaseInfo) : UpdateState
    /** Handed to Android's installer; the confirmation screen is showing. */
    data class Installing(val release: ReleaseInfo) : UpdateState
    data class Failed(val message: String, val release: ReleaseInfo?) : UpdateState
}

/** Pure helpers shared with the release workflow's numbering scheme (unit tested). */
object UpdateVersions {

    private val SEMVER = Regex("""^v?(\d+)\.(\d+)(?:\.(\d+))?""")

    /**
     * Same formula as .github/workflows/release.yml: MAJOR*10000 + MINOR*100 + PATCH.
     * "v1.2.3" -> 10203, "v1.2" -> 10200. Returns null for tags like "v1_x".
     */
    fun versionCodeFromTag(tag: String): Int? {
        val m = SEMVER.find(tag.trim()) ?: return null
        val (major, minor) = m.groupValues[1].toInt() to m.groupValues[2].toInt()
        val patch = m.groupValues[3].toIntOrNull() ?: 0
        if (minor > 99 || patch > 99) return null
        return major * 10000 + minor * 100 + patch
    }

    fun isNewer(release: ReleaseInfo, installedVersionCode: Int): Boolean =
        release.versionCode > installedVersionCode

    /** Finds the hash for [fileName] in `sha256sum` output ("<hash>  <name>" per line). */
    fun checksumFor(sha256sumOutput: String, fileName: String): String? =
        sha256sumOutput.lineSequence()
            .map { it.trim().split(Regex("""\s+\*?"""), limit = 2) }
            .firstOrNull { it.size == 2 && it[1].trim() == fileName }
            ?.get(0)
            ?.lowercase()
            ?.takeIf { it.length == 64 && it.all { c -> c in '0'..'9' || c in 'a'..'f' } }
}

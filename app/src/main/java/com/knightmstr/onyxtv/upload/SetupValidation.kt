package com.knightmstr.onyxtv.upload

/**
 * Pure checks for values the phone sends. Nothing from the network is trusted: package names must
 * look like package names and refer to an installed app, lists are de-duplicated and capped.
 */
object SetupValidation {

    /** Android package names: letters, digits, '_' and '.', and a sane length. */
    private val PACKAGE_NAME = Regex("^[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)*$")
    const val MAX_PACKAGE_LENGTH = 200

    /** Most favourites the phone can set in one go. */
    const val MAX_FAVORITES = 100

    /** Largest JSON body any setup endpoint accepts. */
    const val MAX_JSON_BYTES = 16 * 1024

    fun isPackageName(value: String?): Boolean =
        value != null && value.length <= MAX_PACKAGE_LENGTH && PACKAGE_NAME.matches(value)

    /**
     * Builds the favourites list to store from what the phone asked for.
     *
     * Keeps only well-formed, installed, distinct packages from [requested], capped at
     * [MAX_FAVORITES]. Favourites in [current] that aren't installed right now (an app being
     * reinstalled, say) are kept at the end so the phone, which can't see them, can't wipe them.
     */
    fun mergeFavorites(requested: List<String>, installed: Set<String>, current: List<String>): List<String> {
        val chosen = requested.asSequence()
            .filter { isPackageName(it) && it in installed }
            .distinct()
            .take(MAX_FAVORITES)
            .toList()
        val keptAside = current.filter { it !in installed && it !in chosen }
        return chosen + keptAside
    }
}

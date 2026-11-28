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

    const val MAX_URL_LENGTH = 2048

    /** https only, with a host, no credentials, no whitespace. The TV checks the image itself. */
    fun isHttpsUrl(url: String?): Boolean {
        if (url.isNullOrEmpty() || url.length > MAX_URL_LENGTH || url.any { it.isWhitespace() || it.isISOControl() }) {
            return false
        }
        val uri = try {
            java.net.URI(url)
        } catch (e: Exception) {
            return false
        }
        return uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank() && uri.rawUserInfo == null
    }

    private val IPV4 = Regex("^(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}$")

    /**
     * True if a Host header names the TV the way the TV's own link does: an IPv4 address or
     * localhost, with an optional port. A host *name* means the browser reached us through DNS,
     * which is what a DNS-rebinding attack relies on.
     */
    fun isLocalHost(host: String?): Boolean {
        if (host.isNullOrBlank() || host.length > 64) return false
        val name = host.substringBeforeLast(':').takeIf { ':' in host } ?: host
        val port = if (':' in host) host.substringAfterLast(':') else null
        if (port != null && (port.isEmpty() || port.length > 5 || port.any { !it.isDigit() })) return false
        return name == "localhost" || IPV4.matches(name)
    }
}
